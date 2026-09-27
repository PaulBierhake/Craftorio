package de.craftorio.datagen;

import de.craftorio.Craftorio;
import de.craftorio.registry.ModBlocks;
import de.craftorio.machine.BurnerDrillBlock;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public final class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper fileHelper) {
        super(output, Craftorio.MOD_ID, fileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.CAP_ROCK.get(), cubeAll(ModBlocks.CAP_ROCK.get()));
        simpleBlockWithItem(ModBlocks.TRADING_POST.get(), models().cubeBottomTop("trading_post",
                modLoc("block/trading_post_side"), modLoc("block/trading_post_bottom"), modLoc("block/trading_post_top")));

        oreField(ModBlocks.IRON_ORE_FIELD.get());
        oreField(ModBlocks.COPPER_ORE_FIELD.get());
        oreField(ModBlocks.COAL_FIELD.get());

        ModelFile drillOff = models().orientable("burner_drill",
                modLoc("block/burner_drill_side"), modLoc("block/burner_drill_front"), modLoc("block/burner_drill_top"));
        ModelFile drillOn = models().orientable("burner_drill_on",
                modLoc("block/burner_drill_side"), modLoc("block/burner_drill_front_on"), modLoc("block/burner_drill_top"));
        horizontalBlock(ModBlocks.BURNER_DRILL.get(), state -> state.getValue(BurnerDrillBlock.LIT) ? drillOn : drillOff);
        simpleBlockItem(ModBlocks.BURNER_DRILL.get(), drillOff);

        // Hand-written models in src/main/resources (belt and arm geometry), rotated by facing.
        ModelFile belt = models().getExistingFile(modLoc("block/conveyor_belt"));
        horizontalBlock(ModBlocks.CONVEYOR_BELT.get(), belt);
        simpleBlockItem(ModBlocks.CONVEYOR_BELT.get(), belt);
        ModelFile inserter = models().getExistingFile(modLoc("block/inserter"));
        horizontalBlock(ModBlocks.INSERTER.get(), inserter);
        simpleBlockItem(ModBlocks.INSERTER.get(), inserter);
    }

    private void oreField(Block block) {
        simpleBlockWithItem(block, cubeAll(block));
    }
}
