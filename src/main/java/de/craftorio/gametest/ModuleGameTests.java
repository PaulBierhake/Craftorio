package de.craftorio.gametest;

import de.craftorio.Craftorio;
import de.craftorio.fluid.FluidMachineType;
import de.craftorio.machine.DrillTier;
import de.craftorio.machine.MachineType;
import de.craftorio.machine.ProcessingMachineBlockEntity;
import de.craftorio.module.BeaconBlockEntity;
import de.craftorio.module.ModuleKind;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Modules, productivity rules and beacons (package U11d). */
@GameTestHolder(Craftorio.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModuleGameTests {
    private static final String LARGE = "empty_large";

    private ModuleGameTests() {
    }

    private static ProcessingMachineBlockEntity assembler(GameTestHelper helper, BlockPos pos, String recipe) {
        helper.setBlock(pos, ModBlocks.ASSEMBLER_2.get());
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        machine.setSelectedRecipe(Craftorio.id("assembling/" + recipe));
        helper.onEachTick(() -> machine.energy().setEnergy(20_000));
        return machine;
    }

    private static ItemStack module(ModuleKind kind, int tier) {
        return new ItemStack(ModItems.module(kind, tier).get());
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aSpeedModuleShortensACraftByTwentyPercent(GameTestHelper helper) {
        ProcessingMachineBlockEntity plain = assembler(helper, new BlockPos(1, 1, 1), "red_science");
        ProcessingMachineBlockEntity fast = assembler(helper, new BlockPos(3, 1, 1), "red_science");
        fast.modules().inventory().insertItem(0, module(ModuleKind.SPEED, 1), false);
        for (ProcessingMachineBlockEntity machine : new ProcessingMachineBlockEntity[]{plain, fast}) {
            machine.items().insertItem(0, new ItemStack(Items.COPPER_INGOT, 5), false);
            machine.items().insertItem(1, new ItemStack(ModItems.IRON_GEAR.get(), 5), false);
        }
        helper.runAtTickTime(30, () -> {
            helper.assertValueEqual(plain.recipeTime(), 133, "5 s at speed 0.75");
            helper.assertValueEqual(fast.recipeTime(), 111, "5 s at speed 0.75 × 1.2");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void modulesCanNotBeTakenOutByInsertersOrTheWrongItems(GameTestHelper helper) {
        ProcessingMachineBlockEntity machine = assembler(helper, new BlockPos(2, 1, 2), "iron_gear");
        helper.assertTrue(!machine.modules().inventory().isItemValid(0, new ItemStack(Items.IRON_INGOT)), "only modules fit");
        helper.assertTrue(machine.modules().inventory().isItemValid(0, module(ModuleKind.PRODUCTIVITY, 1)), "modules fit");
        helper.assertValueEqual(machine.modules().inventory().getSlotLimit(0), 1, "one module per slot");
        helper.assertTrue(machine.automation().insertItem(0, module(ModuleKind.SPEED, 1), true).getCount() == 1, "inserters do not fill modules");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aProductivityModuleIsRefusedInARecipeForAmmunition(GameTestHelper helper) {
        ProcessingMachineBlockEntity machine = assembler(helper, new BlockPos(2, 1, 2), "grenade");
        machine.items().insertItem(0, new ItemStack(Items.COAL, 10), false);
        machine.items().insertItem(1, new ItemStack(Items.IRON_INGOT, 10), false);
        machine.modules().inventory().insertItem(0, module(ModuleKind.PRODUCTIVITY, 1), false);

        helper.runAtTickTime(40, () -> {
            helper.assertTrue(machine.productivityBlocked(), "the machine refuses the recipe");
            helper.assertValueEqual(machine.progressPercent(), -1, "and does not work");
            machine.modules().inventory().setStackInSlot(0, ItemStack.EMPTY);
        });
        helper.runAtTickTime(120, () -> {
            helper.assertTrue(!machine.productivityBlocked(), "fine without the module");
            helper.assertTrue(machine.progressPercent() >= 0 || !machine.items().getStackInSlot(machine.type().outputSlot()).isEmpty(), "works again");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 500)
    public static void productivityGivesAnExtraProductForEveryTenCrafts(GameTestHelper helper) {
        ProcessingMachineBlockEntity machine = assembler(helper, new BlockPos(2, 1, 2), "iron_gear");
        machine.modules().inventory().insertItem(0, module(ModuleKind.PRODUCTIVITY, 3), false); // +10 %, −15 % speed
        machine.items().insertItem(0, new ItemStack(Items.IRON_INGOT, 60), false);

        helper.runAtTickTime(400, () -> {
            int crafts = (60 - machine.items().getStackInSlot(0).getCount()) / 2;
            int gears = machine.items().getStackInSlot(machine.type().outputSlot()).getCount();
            helper.assertTrue(crafts >= 20, "at least twenty crafts done: " + crafts);
            helper.assertValueEqual(gears, crafts + crafts / 10, "one extra gear per ten crafts, for no extra ingots");
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void aBeaconGivesHalfItsEffectToMachinesWithinNineByNine(GameTestHelper helper) {
        BlockPos beaconPos = new BlockPos(8, 2, 8);
        helper.setBlock(beaconPos, ModBlocks.BEACON.get());
        BeaconBlockEntity beacon = helper.getBlockEntity(beaconPos);
        beacon.modules().insertItem(0, module(ModuleKind.SPEED, 1), false);
        beacon.modules().insertItem(1, module(ModuleKind.SPEED, 1), false);
        helper.assertTrue(!beacon.modules().isItemValid(0, module(ModuleKind.PRODUCTIVITY, 1)), "no productivity in beacons");
        helper.onEachTick(() -> beacon.energy().setEnergy(10_000));

        ProcessingMachineBlockEntity near = assembler(helper, new BlockPos(12, 2, 8), "iron_gear");   // 4 blocks away
        ProcessingMachineBlockEntity above = assembler(helper, new BlockPos(8, 3, 11), "iron_gear");   // 3 blocks, one up
        ProcessingMachineBlockEntity far = assembler(helper, new BlockPos(13, 2, 8), "iron_gear");     // 5 blocks away
        ProcessingMachineBlockEntity high = assembler(helper, new BlockPos(8, 4, 8), "iron_gear");     // two up
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(beacon.active(), "the beacon runs");
            helper.assertValueEqual(near.moduleEffects().speed(), 20.0, "two speed modules at half strength: +20 %");
            helper.assertValueEqual(near.moduleEffects().energy(), 50.0, "and +50 % energy");
            helper.assertValueEqual(above.moduleEffects().speed(), 20.0, "one block up is still in reach");
            helper.assertValueEqual(far.moduleEffects().speed(), 0.0, "five blocks away is out of reach");
            helper.assertValueEqual(high.moduleEffects().speed(), 0.0, "two blocks up is out of reach");
            helper.succeed();
        });
    }

    @GameTest(template = LARGE, timeoutTicks = 200)
    public static void beaconsAddUpAndNeedPower(GameTestHelper helper) {
        BeaconBlockEntity[] beacons = new BeaconBlockEntity[3];
        for (int i = 0; i < 3; i++) {
            BlockPos pos = new BlockPos(6 + i, 2, 6);
            helper.setBlock(pos, ModBlocks.BEACON.get());
            beacons[i] = helper.getBlockEntity(pos);
            beacons[i].modules().insertItem(0, module(ModuleKind.SPEED, 3), false); // +50 % speed
        }
        beacons[0].energy().setEnergy(10_000);
        beacons[1].energy().setEnergy(10_000);
        helper.onEachTick(() -> {
            beacons[0].energy().setEnergy(10_000);
            beacons[1].energy().setEnergy(10_000);
        });
        ProcessingMachineBlockEntity machine = assembler(helper, new BlockPos(7, 2, 8), "iron_gear");
        helper.runAtTickTime(60, () -> {
            helper.assertValueEqual(machine.moduleEffects().speed(), 50.0, "two powered beacons with +50 % each at half strength; the unpowered one gives nothing");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void machinesHaveTheModuleSlotsOfFactorio(GameTestHelper helper) {
        helper.assertValueEqual(MachineType.ASSEMBLER.moduleSlots(), 0, "assembling machine 1");
        helper.assertValueEqual(MachineType.ASSEMBLER_2.moduleSlots(), 2, "assembling machine 2");
        helper.assertValueEqual(MachineType.ASSEMBLER_3.moduleSlots(), 4, "assembling machine 3");
        helper.assertValueEqual(MachineType.ELECTRIC_FURNACE.moduleSlots(), 2, "electric furnace");
        helper.assertValueEqual(MachineType.STONE_FURNACE.moduleSlots() + MachineType.STEEL_FURNACE.moduleSlots(), 0, "fuel furnaces");
        helper.assertValueEqual(DrillTier.ELECTRIC.moduleSlots(), 3, "electric mining drill");
        helper.assertValueEqual(DrillTier.DEEP.moduleSlots(), 4, "deep drill");
        helper.assertValueEqual(DrillTier.BURNER.moduleSlots(), 0, "burner drill");
        helper.assertValueEqual(FluidMachineType.CHEMICAL_PLANT.moduleSlots(), 3, "chemical plant");
        helper.assertValueEqual(FluidMachineType.OIL_REFINERY.moduleSlots(), 3, "oil refinery");
        helper.assertValueEqual(FluidMachineType.CENTRIFUGE.moduleSlots(), 2, "centrifuge");
        helper.assertValueEqual(FluidMachineType.GREENHOUSE.moduleSlots(), 0, "greenhouse");
        helper.assertValueEqual(de.craftorio.research.LaboratoryBlockEntity.MODULE_SLOTS, 2, "laboratory");
        helper.assertValueEqual(BeaconBlockEntity.SLOTS, 2, "beacon");
        helper.assertValueEqual(de.craftorio.oil.PumpjackBlockEntity.MODULE_SLOTS, 2, "pumpjack");
        helper.assertValueEqual(MachineType.ASSEMBLER_3.energyPerTick(), 375, "assembling machine 3 375 kW");
        helper.assertValueEqual(MachineType.ASSEMBLER_3.speed(), 1.25, "assembling machine 3 speed");
        helper.assertValueEqual(BeaconBlockEntity.POWER, 480, "beacon 480 kW");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void assemblingMachineThreeCraftsFasterAndTakesFourModules(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.ASSEMBLER_3.get());
        ProcessingMachineBlockEntity machine = helper.getBlockEntity(pos);
        machine.setSelectedRecipe(Craftorio.id("assembling/red_science"));
        helper.onEachTick(() -> machine.energy().setEnergy(20_000));
        for (int i = 0; i < 4; i++) {
            helper.assertTrue(machine.modules().inventory().insertItem(i, module(ModuleKind.EFFICIENCY, 1), false).isEmpty(), "slot " + i);
        }
        machine.items().insertItem(0, new ItemStack(Items.COPPER_INGOT, 5), false);
        machine.items().insertItem(1, new ItemStack(ModItems.IRON_GEAR.get(), 5), false);
        helper.runAtTickTime(30, () -> {
            helper.assertValueEqual(machine.recipeTime(), 80, "5 s at speed 1.25");
            helper.assertTrue(machine.moduleEffects().energy() == -120.0, "four efficiency modules −120 %");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aLabHasSlotsForPurpleAndYellowPacks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, ModBlocks.LABORATORY.get());
        de.craftorio.research.LaboratoryBlockEntity lab = helper.getBlockEntity(pos);
        helper.assertValueEqual(lab.packs().getSlots(), de.craftorio.research.Research.Pack.values().length, "one slot per pack kind");
        helper.assertTrue(lab.packs().insertItem(de.craftorio.research.Research.Pack.PRODUCTION.ordinal(), new ItemStack(ModItems.PRODUCTION_SCIENCE.get(), 3), false).isEmpty(), "purple packs fit");
        helper.assertTrue(lab.packs().insertItem(de.craftorio.research.Research.Pack.UTILITY.ordinal(), new ItemStack(ModItems.UTILITY_SCIENCE.get(), 3), false).isEmpty(), "yellow packs fit");
        helper.assertTrue(!lab.packs().insertItem(de.craftorio.research.Research.Pack.PRODUCTION.ordinal(), new ItemStack(ModItems.UTILITY_SCIENCE.get()), false).isEmpty(), "each slot takes its own pack only");
        helper.assertValueEqual(de.craftorio.research.Research.Pack.PRODUCTION.letter(), 'P', "letter");
        helper.assertValueEqual(de.craftorio.research.Research.Pack.UTILITY.letter(), 'U', "letter");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void theArenaHandsOutTheDiamondSealAtLevelFortyAndTheStarSealAtFifty(GameTestHelper helper) {
        helper.assertValueEqual(de.craftorio.defense.LevelPlan.keyReward(40), de.craftorio.defense.LevelPlan.KeyReward.DIAMOND_SEAL, "level 40");
        helper.assertValueEqual(de.craftorio.defense.LevelPlan.keyReward(50), de.craftorio.defense.LevelPlan.KeyReward.STAR_SEAL, "level 50");
        helper.assertValueEqual(de.craftorio.defense.LevelPlan.keyReward(45), de.craftorio.defense.LevelPlan.KeyReward.NONE, "level 45");
        helper.succeed();
    }
}
