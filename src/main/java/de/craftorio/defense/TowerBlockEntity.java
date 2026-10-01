package de.craftorio.defense;

import de.craftorio.blueprint.Blueprints;
import de.craftorio.defense.arena.Arenas;
import de.craftorio.defense.sim.Attack;
import de.craftorio.defense.sim.AttackKind;
import de.craftorio.defense.sim.TargetMode;
import de.craftorio.defense.sim.TowerDef;
import de.craftorio.defense.sim.Buffs;
import de.craftorio.defense.sim.DepotEconomy;
import de.craftorio.defense.sim.TdUnits;
import de.craftorio.defense.sim.TowerProfile;
import de.craftorio.defense.sim.TowerRules;
import de.craftorio.defense.sim.TowerUnit;
import de.craftorio.energy.EnergyBuffer;
import de.craftorio.menu.SplitIntData;
import de.craftorio.menu.TowerMenu;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModDataComponents;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A tower of the arena. The {@link TowerUnit} does the shooting (and is the same code the balancing simulator runs);
 * this block entity holds the state (upgrades, what was paid, ammunition), pays for shots and upgrades and sells.
 */
public final class TowerBlockEntity extends BlockEntity implements MenuProvider {
    /** +10 % range on a plateau, the only terrain effect. */
    public static final double PLATEAU_RANGE = 1.1;
    /** Basket of goods of a supply depot per round: circuits (processors from tier 4 of path 1, advanced circuits at tier 3). */
    public static final int BASKET_CIRCUITS = 10;
    public static final int BASKET_PROCESSORS = 5;

    private final TowerType type;
    private final int[] tiers = new int[TowerDef.PATHS];
    /** Coins paid for the tower and its upgrades; a sold tower pays back a share of it. */
    private long paid;
    private TargetMode targetMode;
    private @Nullable TowerUnit unit;
    /** Ability timers read from disk, handed to the unit when it is created. */
    private int[] abilityCooldowns = new int[0];
    private int[] abilityActives = new int[0];
    private final EnergyBuffer energy;
    /** Shots left of the ammunition item that is in use, and (guns) which magazine it was. */
    private int shotsLeft;
    private Magazine magazine = Magazine.NORMAL;
    private final ItemStackHandler ammo = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return type.usesItemAmmo() && (stack.is(ammoItem()) || (type == TowerType.GUN && Magazine.of(stack) != null));
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Bank and loan of a supply depot, and whether it got its basket of goods at the end of the last round. */
    private final DepotEconomy depot = new DepotEconomy();
    private boolean basketDelivered;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0, 1, 2 -> tiers[index];
                case 3 -> targetMode.ordinal();
                case 4 -> SplitIntData.low(energy.getEnergyStored());
                case 5 -> SplitIntData.high(energy.getEnergyStored());
                case 6 -> SplitIntData.low((int) Math.min(Integer.MAX_VALUE, paid));
                case 7 -> SplitIntData.high((int) Math.min(Integer.MAX_VALUE, paid));
                case 8, 9 -> abilitySeconds(0, index == 9);
                case 10, 11 -> abilitySeconds(1, index == 11);
                case 12 -> (int) Math.min(Short.MAX_VALUE, depot.bank());
                case 13 -> (int) Math.min(Short.MAX_VALUE, depot.debt());
                case 14 -> basketDelivered ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return TowerMenu.DATA_COUNT;
        }
    };

    public TowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWER.get(), pos, state);
        this.type = ((TowerBlock) state.getBlock()).towerType();
        this.targetMode = type.def().defaultTarget();
        this.energy = new EnergyBuffer(Math.max(1, type.energyCapacity()), 200, 0, this::setChanged);
    }

    public TowerType type() {
        return type;
    }

    /** Seconds until ability {@code index} is ready again, or how long it stays active; 0 if the tower has no such ability. */
    private int abilitySeconds(int index, boolean active) {
        if (unit == null && level != null && !level.isClientSide) {
            unit();
        }
        if (unit == null || index >= unit.abilityCount()) {
            return 0;
        }
        return (int) Math.ceil((active ? unit.abilityActive(index) : unit.abilityCooldown(index)) / 20.0);
    }

    public TowerDef def() {
        return type.def();
    }

    public int tier(int path) {
        return tiers[path];
    }

    public int[] tiers() {
        return tiers.clone();
    }

    /** Coins paid for this tower so far. */
    public long paid() {
        return paid;
    }

    /** What selling the tower pays: 70 % of everything paid for it (80 % for a supply depot with Banana Salvage). */
    public long sellValue() {
        return TowerRules.sellValue(paid, profile().sellShare());
    }

    /** Sets the upgrades (towers taken out of the depot, tests); paid is kept. */
    public void setTiers(int... newTiers) {
        System.arraycopy(newTiers, 0, tiers, 0, TowerDef.PATHS);
        unit = null;
        setChanged();
    }

    public void setPaid(long paid) {
        this.paid = paid;
        setChanged();
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public ItemStackHandler ammo() {
        return ammo;
    }

    /** Shots left in the loaded magazine (guns). */
    public int shotsLeft() {
        return shotsLeft;
    }

    /** Is the loaded magazine better than the plain one? */
    public boolean armourPiercing() {
        return magazine != Magazine.NORMAL;
    }

    /** The kind of the magazine that is loaded (or was loaded last). */
    public Magazine magazine() {
        return magazine;
    }

    public Item ammoItem() {
        return switch (type) {
            case GUN -> ModItems.MAGAZINE.get();
            case MORTAR -> ModItems.GRENADE.get();
            default -> ModItems.BOLT.get();
        };
    }

    /** Neither the tower itself nor the arena reserve can supply its next shot. */
    public boolean lacksSupply(TowerDefense.Zone zone) {
        if (type.usesEnergy()) {
            return energy.getEnergyStored() < type.energyPerShot() && zone.energy() < type.energyPerShot();
        }
        if (type.usesFluid()) {
            return zone.fluid() < type.fluidPerShot();
        }
        if (!type.usesItemAmmo() || shotsLeft > 0) {
            return false;
        }
        boolean reserve = zone.ammo(ammoItem()) > 0 || (type == TowerType.GUN && java.util.Arrays.stream(Magazine.values()).anyMatch(kind -> zone.ammo(kind.item()) > 0));
        return ammo.getStackInSlot(0).isEmpty() && !reserve;
    }

    public TargetMode targetMode() {
        return targetMode;
    }

    public void cycleTargetMode() {
        targetMode = TargetMode.next(targetMode, def().targets());
        if (unit != null) {
            unit.setMode(targetMode);
        }
        setChanged();
    }

    /** The tower's current profile: range, attacks and abilities with all bought upgrades. */
    public TowerProfile profile() {
        return unit().profile();
    }

    private TowerUnit unit() {
        if (unit == null) {
            unit = new TowerUnit(def(), worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5, worldPosition.asLong());
            unit.setTiers(tiers);
            unit.setMode(targetMode);
            unit.restoreAbilities(abilityCooldowns, abilityActives);
            abilityCooldowns = new int[0];
            abilityActives = new int[0];
            applyAmmo();
        }
        return unit;
    }

    private void applyAmmo() {
        if (unit != null) {
            unit.setAmmo(type == TowerType.GUN ? magazine.damageKind() : null, type == TowerType.GUN ? magazine.factor() : 1);
        }
    }

    /** Plateau +10 %, and whatever the arena's mutator does to ranges. */
    public double rangeFactor() {
        double factor = 1;
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.ARENA_CLIFF.get())) {
            factor *= PLATEAU_RANGE;
        }
        if (level instanceof ServerLevel serverLevel && TowerDefense.isArenaLevel(serverLevel)) {
            factor *= TowerDefense.get(serverLevel.getServer()).activeMutator(Arenas.slotAt(worldPosition)).rangeFactor();
        }
        return factor;
    }

    /** Targeting range in blocks, for the GUI and the range display; negative for unlimited. */
    public double range() {
        double base = profile().range;
        return base < 0 ? base : base * rangeFactor();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TowerBlockEntity tower) {
        if (level instanceof ServerLevel serverLevel) {
            tower.work(serverLevel);
        }
    }

    /** Auras from other towers, renewed twice a second. */
    private Buffs outside = Buffs.NONE;

    private void work(ServerLevel serverLevel) {
        LevelRun run = LevelRun.runFor(serverLevel, worldPosition);
        if (run == null) {
            return;
        }
        run.register(this);
        if ((serverLevel.getGameTime() + worldPosition.asLong()) % 10 == 0) {
            outside = run.aurasFor(this);
        }
        if (type.attacks()) {
            shoot(serverLevel, run);
        }
    }

    private void shoot(ServerLevel serverLevel, LevelRun run) {
        TowerUnit tower = unit();
        List<TowerUnit.Shot> shots = tower.tick(run.simulation(), (def, attack) -> payForShot(), rangeFactor(), outside);
        run.addCoins(tower.takeCoins());
        if (shots.isEmpty()) {
            return;
        }
        Vec3 muzzle = worldPosition.getCenter().add(0, 0.8, 0);
        for (TowerUnit.Shot shot : shots) {
            effects(serverLevel, muzzle, shot);
        }
        SoundEvent sound = switch (type) {
            case CROSSBOW -> SoundEvents.CROSSBOW_SHOOT;
            case GUN -> SoundEvents.FIREWORK_ROCKET_BLAST;
            case TESLA -> SoundEvents.BEACON_POWER_SELECT;
            case LASER -> SoundEvents.GUARDIAN_ATTACK;
            case FLAME -> SoundEvents.FIRECHARGE_USE;
            case MORTAR -> SoundEvents.GENERIC_EXPLODE.value();
            case DEPOT -> SoundEvents.EXPERIENCE_ORB_PICKUP;
        };
        serverLevel.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.4F, 1.2F);
    }

    /** Particles for one shot: a trail to every enemy hit (a few at most), a ring for auras and blasts. */
    private void effects(ServerLevel level, Vec3 muzzle, TowerUnit.Shot shot) {
        Attack attack = shot.attack();
        ParticleOptions particle = switch (type) {
            case CROSSBOW -> ParticleTypes.CRIT;
            case GUN -> ParticleTypes.SMOKE;
            case TESLA -> ParticleTypes.ELECTRIC_SPARK;
            case LASER -> ParticleTypes.END_ROD;
            case FLAME -> ParticleTypes.FLAME;
            case MORTAR -> ParticleTypes.SMOKE;
            case DEPOT -> ParticleTypes.HAPPY_VILLAGER;
        };
        double y = worldPosition.getY() + 1.2;
        if (attack.kind == AttackKind.AURA) {
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 24;
                level.sendParticles(ParticleTypes.SNOWFLAKE, worldPosition.getX() + 0.5 + Math.cos(angle) * attack.radius, y,
                        worldPosition.getZ() + 0.5 + Math.sin(angle) * attack.radius, 1, 0, 0, 0, 0);
            }
            return;
        }
        if (attack.kind == AttackKind.AREA) {
            level.sendParticles(ParticleTypes.EXPLOSION, shot.aimX(), y, shot.aimZ(), 1, 0, 0, 0, 0);
        }
        int trails = 0;
        for (var enemy : shot.hits()) {
            if (trails++ >= 6) {
                break;
            }
            trail(level, muzzle, new Vec3(enemy.x(), enemy.y() + 0.5, enemy.z()), particle);
        }
        if (shot.hits().isEmpty()) {
            trail(level, muzzle, new Vec3(shot.aimX(), y, shot.aimZ()), particle);
        }
    }

    /** Uses the tower's own ammunition or energy first, then the arena reserve filled by the arena feeder. */
    private boolean payForShot() {
        if (type.usesEnergy()) {
            return energy.consume(type.energyPerShot()) || drawFromArena(defense -> defense.drawEnergy(Arenas.slotAt(worldPosition), type.energyPerShot()));
        }
        if (type.usesFluid()) {
            return drawFromArena(defense -> defense.drawFluid(Arenas.slotAt(worldPosition), type.fluidPerShot()));
        }
        if (!type.usesItemAmmo()) {
            return true;
        }
        if (shotsLeft <= 0 && !loadNextItem()) {
            return false;
        }
        shotsLeft--;
        setChanged();
        return true;
    }

    /** Loads the next ammunition item from the tower's slot, then from the arena reserve (the best magazine first). */
    private boolean loadNextItem() {
        ItemStack loaded = ammo.getStackInSlot(0);
        if (!loaded.isEmpty()) {
            if (type == TowerType.GUN) {
                magazine = Magazine.of(loaded);
            }
            ammo.extractItem(0, 1, false);
        } else if (type == TowerType.GUN) {
            Magazine drawn = null;
            for (Magazine kind : Magazine.BEST_FIRST) {
                if (drawFromArena(defense -> defense.drawAmmo(Arenas.slotAt(worldPosition), kind.item()))) {
                    drawn = kind;
                    break;
                }
            }
            if (drawn == null) {
                return false;
            }
            magazine = drawn;
        } else if (!drawFromArena(defense -> defense.drawAmmo(Arenas.slotAt(worldPosition), ammoItem()))) {
            return false;
        }
        shotsLeft = def().supplyCost();
        applyAmmo();
        return true;
    }

    private boolean drawFromArena(java.util.function.Predicate<TowerDefense> draw) {
        return level instanceof ServerLevel serverLevel && TowerDefense.isArenaLevel(serverLevel) && draw.test(TowerDefense.get(serverLevel.getServer()));
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        int steps = (int) Math.max(2, Math.min(24, from.distanceTo(to) * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, (double) i / steps);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    // --- upgrades and selling

    /** The bought upgrade of this path and tier, or null. */
    public @Nullable TowerDef.Upgrade next(int path) {
        return tiers[path] >= TowerDef.TIERS ? null : def().upgrade(path, tiers[path] + 1);
    }

    /** Why this path cannot be upgraded now (a short id: maxed, crosspath, research), or null if it can, apart from money. */
    public @Nullable String lockReason(int path, java.util.Set<String> researched) {
        String reason = TowerRules.lockReason(tiers, path);
        if (reason != null) {
            return reason;
        }
        int tech = TowerRules.techRequired(tiers[path] + 1);
        return tech == 0 || researched.contains(techResearch(tech)) ? null : "research";
    }

    /** The research id for a tech step (Turmtechnik I to III). */
    public static String techResearch(int tech) {
        return de.craftorio.Craftorio.id("tower_tech_" + tech).toString();
    }

    /** Factory parts the tier needs, in addition to the coins: circuits, advanced circuits, processors and motors. */
    public List<SizedIngredient> upgradeMaterials(int toTier) {
        return switch (toTier) {
            case 3 -> List.of(SizedIngredient.of(ModItems.CIRCUIT.get(), 5));
            case 4 -> List.of(SizedIngredient.of(ModItems.ADVANCED_CIRCUIT.get(), 10));
            case 5 -> List.of(SizedIngredient.of(ModItems.PROCESSING_UNIT.get(), 5), SizedIngredient.of(ModItems.MOTOR.get(), 2));
            default -> List.of();
        };
    }

    /** Buys the next tier of a path, paid in arena coins (and parts for tier 3 and up). */
    public boolean upgrade(ServerPlayer player, int path) {
        if (!(level instanceof ServerLevel serverLevel) || path < 0 || path >= TowerDef.PATHS) {
            return false;
        }
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), player.getGameProfile().getName());
        String reason = lockReason(path, team.researched());
        if (reason != null) {
            player.displayClientMessage(Component.translatable("craftorio.tower.locked." + reason).withStyle(ChatFormatting.RED), true);
            return false;
        }
        TowerDefense defense = TowerDefense.get(player.server);
        int toTier = tiers[path] + 1;
        List<SizedIngredient> materials = upgradeMaterials(toTier);
        if (!Blueprints.hasAll(player.getInventory(), materials, 1)) {
            player.displayClientMessage(Component.translatable("craftorio.workbench.missing").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!TeamData.maySpend(player)) {
            return false;
        }
        long price = defense.upgradePrice(Arenas.slotAt(worldPosition), next(path));
        if (!defense.spend(Arenas.slotAt(worldPosition), price)) {
            player.displayClientMessage(Component.translatable("craftorio.tower.not_enough_coins", price).withStyle(ChatFormatting.RED), true);
            return false;
        }
        Blueprints.take(player.getInventory(), materials, 1);
        tiers[path] = toTier;
        paid += price;
        unit = null;
        setChanged();
        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        player.displayClientMessage(Component.translatable("craftorio.tower.upgraded", TowerRules.notation(tiers), price).withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** Uses ability {@code index}: starts its cooldown and runs its effect (see {@link Abilities}). */
    public boolean activateAbility(ServerPlayer player, int index) {
        if (!(level instanceof ServerLevel serverLevel) || !type.attacks() && profile().abilities.isEmpty()) {
            return false;
        }
        TowerUnit tower = unit();
        if (index < 0 || index >= tower.abilityCount() || tower.profile().abilities.get(index).passive()) {
            return false;
        }
        LevelRun run = LevelRun.runFor(serverLevel, worldPosition);
        if (run == null) {
            player.displayClientMessage(Component.translatable("craftorio.tower.ability.no_round").withStyle(ChatFormatting.RED), true);
            return false;
        }
        TowerProfile.Ability ability = tower.activate(index, 1.0);
        if (ability == null) {
            player.displayClientMessage(Component.translatable("craftorio.tower.ability.cooling", abilitySeconds(index, false)).withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (ability.buff() != null) {
            int ticks = (int) Math.round(ability.duration() * TdUnits.TICKS_PER_SECOND);
            tower.grantBuff("ability:" + ability.id(), ticks, ability.buff());
            if (ability.shareRadius() > 0) {
                shareBuff(serverLevel, "ability:" + ability.id(), ticks, ability);
            }
        }
        Abilities.Handler handler = Abilities.handler(ability.id());
        if (handler != null) {
            handler.activate(this, run, ability);
        }
        setChanged();
        player.displayClientMessage(Component.translatable("craftorio.tower.ability.used", Component.translatable("craftorio.ability." + ability.id())), true);
        return true;
    }

    /** Does an ability's buff (see {@link TowerProfile.Ability#buff()}) run on this tower now? */
    public boolean hasAbilityBuff(String abilityId) {
        return unit().hasBuff("ability:" + abilityId);
    }

    public DepotEconomy depot() {
        return depot;
    }

    public boolean basketDelivered() {
        return basketDelivered;
    }

    /** The end of a round: a supply depot pays its income, with the basket of goods from the arena reserve if it is complete. */
    void roundEnded(LevelRun run) {
        if (type != TowerType.DEPOT || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int tier = tiers[0];
        Item goods = tier >= 4 ? ModItems.PROCESSING_UNIT.get() : tier == 3 ? ModItems.ADVANCED_CIRCUIT.get() : ModItems.CIRCUIT.get();
        int count = tier >= 4 ? BASKET_PROCESSORS : BASKET_CIRCUITS;
        basketDelivered = drawFromArena(defense -> defense.drawGoods(Arenas.slotAt(worldPosition), goods, count));
        run.addCoins(depot.roundEnd(profile(), basketDelivered));
        setChanged();
        serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Passive abilities (Elite Defender) start when the team loses lives. */
    public void lifeLost() {
        if (unit == null) {
            return;
        }
        for (int i = 0; i < unit.abilityCount(); i++) {
            TowerProfile.Ability ability = unit.profile().abilities.get(i);
            if (ability.passive() && unit.activate(i, 1.0) != null && ability.buff() != null) {
                unit.grantBuff("ability:" + ability.id(), (int) Math.round(ability.duration() * TdUnits.TICKS_PER_SECOND), ability.buff());
            }
        }
    }

    /** Gives towers of the same kind around this one the ability's buff. */
    private void shareBuff(ServerLevel serverLevel, String id, int ticks, TowerProfile.Ability ability) {
        int r = (int) Math.ceil(ability.shareRadius());
        int left = ability.shareMax() > 0 ? ability.shareMax() : Integer.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-r, -3, -r), worldPosition.offset(r, 3, r))) {
            if (pos.equals(worldPosition) || pos.distSqr(worldPosition) > ability.shareRadius() * ability.shareRadius()) {
                continue;
            }
            if (left > 0 && serverLevel.getBlockEntity(pos) instanceof TowerBlockEntity other && other.type() == type) {
                other.unit().grantBuff(id, ticks, ability.buff());
                left--;
            }
        }
    }

    /** Sells the tower: a share of everything paid goes back to the team's coins, the plain tower item to the player. */
    public boolean sell(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel) || !TowerDefense.isArenaLevel(serverLevel)) {
            return false;
        }
        TowerDefense defense = TowerDefense.get(player.server);
        long value = sellValue();
        defense.earn(Arenas.slotAt(worldPosition), value);
        dropAmmo();
        ItemStack item = new ItemStack(ModBlocks.tower(type).get());
        BlockPos pos = worldPosition;
        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (!player.getInventory().add(item)) {
            Containers.dropItemStack(serverLevel, pos.getX(), pos.getY(), pos.getZ(), item);
        }
        player.displayClientMessage(Component.translatable("craftorio.tower.sold", value).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    public void dropAmmo() {
        if (level != null) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), ammo.getStackInSlot(0));
            ammo.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TowerMenu(containerId, inventory, this, data);
    }

    /** Sets upgrades and what was paid from a tower item taken out of the depot. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ModDataComponents.TowerState state = input.get(ModDataComponents.TOWER_STATE.get());
        if (state != null) {
            tiers[0] = clampTier(state.path1());
            tiers[1] = clampTier(state.path2());
            tiers[2] = clampTier(state.path3());
            paid = Math.max(0, state.paid());
        }
    }

    private static int clampTier(int tier) {
        return Math.max(0, Math.min(TowerDef.TIERS, tier));
    }

    /** The item of a tower that was bought remembers its upgrades and what was paid (so placing it again is free). */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (paid > 0) {
            components.set(ModDataComponents.TOWER_STATE.get(), new ModDataComponents.TowerState(tiers[0], tiers[1], tiers[2], paid));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("target", targetMode.name());
        tag.putIntArray("tiers", tiers);
        tag.putLong("paid", paid);
        tag.putInt("energy", energy.getEnergyStored());
        tag.put("ammo", ammo.serializeNBT(registries));
        tag.putInt("shots_left", shotsLeft);
        tag.putDouble("bank", depot.bank());
        tag.putDouble("debt", depot.debt());
        tag.putBoolean("basket", basketDelivered);
        tag.putString("magazine", magazine.name());
        if (unit != null) {
            tag.putIntArray("ability_cooldown", unit.abilityCooldowns());
            tag.putIntArray("ability_active", unit.abilityActives());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] saved = tag.getIntArray("tiers");
        for (int path = 0; path < TowerDef.PATHS; path++) {
            tiers[path] = path < saved.length ? clampTier(saved[path]) : 0;
        }
        paid = tag.getLong("paid");
        try {
            targetMode = TargetMode.byId(tag.getString("target"));
        } catch (IllegalArgumentException missing) {
            targetMode = def().defaultTarget();
        }
        if (!def().targets().contains(targetMode)) {
            targetMode = def().defaultTarget();
        }
        unit = null;
        abilityCooldowns = tag.getIntArray("ability_cooldown");
        abilityActives = tag.getIntArray("ability_active");
        energy.setEnergy(tag.getInt("energy"));
        ammo.deserializeNBT(registries, tag.getCompound("ammo"));
        shotsLeft = tag.getInt("shots_left");
        depot.restore(tag.getDouble("bank"), tag.getDouble("debt"));
        basketDelivered = tag.getBoolean("basket");
        try {
            magazine = tag.contains("magazine") ? Magazine.valueOf(tag.getString("magazine"))
                    : tag.getBoolean("armour_piercing") ? Magazine.ARMOUR_PIERCING : Magazine.NORMAL;
        } catch (IllegalArgumentException unknown) {
            magazine = Magazine.NORMAL;
        }
    }
}
