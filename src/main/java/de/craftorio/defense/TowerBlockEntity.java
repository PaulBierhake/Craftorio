package de.craftorio.defense;

import de.craftorio.blueprint.Blueprints;
import de.craftorio.economy.Credits;
import de.craftorio.energy.EnergyBuffer;
import de.craftorio.menu.TowerMenu;
import de.craftorio.menu.SplitIntData;
import de.craftorio.registry.ModBlockEntities;
import de.craftorio.registry.ModBlocks;
import de.craftorio.registry.ModDataComponents;
import de.craftorio.defense.arena.Arenas;
import net.minecraft.core.component.DataComponentMap;
import de.craftorio.registry.ModItems;
import de.craftorio.team.Team;
import de.craftorio.team.TeamData;
import de.craftorio.team.TeamRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Shoots enemies in range (by its target mode), takes damage from them and becomes a ruin at 0 HP. */
public final class TowerBlockEntity extends BlockEntity implements MenuProvider {

    private final TowerType type;
    private int upgradeLevel = 1;
    private int health;
    private int cooldown;
    private TargetMode targetMode = TargetMode.FIRST;
    private final EnergyBuffer energy;
    private final ItemStackHandler ammo = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ammoItem());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> health;
                case 1 -> maxHealth();
                case 2 -> upgradeLevel;
                case 3 -> SplitIntData.low(energy.getEnergyStored());
                case 4 -> SplitIntData.high(energy.getEnergyStored());
                case 5 -> targetMode.ordinal();
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
        this.energy = new EnergyBuffer(Math.max(1, type.energyCapacity()), 200, 0, this::setChanged);
        this.health = TowerStats.maxHealth(type, 1);
    }

    public TowerType type() {
        return type;
    }

    public int upgradeLevel() {
        return upgradeLevel;
    }

    public int health() {
        return health;
    }

    public int maxHealth() {
        return TowerStats.maxHealth(type, upgradeLevel);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public ItemStackHandler ammo() {
        return ammo;
    }

    public Item ammoItem() {
        return type == TowerType.GUN ? ModItems.CARTRIDGE.get() : ModItems.BOLT.get();
    }

    /** Neither the tower itself nor the arena reserve can supply its next shot. */
    public boolean lacksSupply(TowerDefense.Zone zone) {
        if (type.usesEnergy()) {
            return energy.getEnergyStored() < type.energyPerShot() && zone.energy() < type.energyPerShot();
        }
        return ammo.getStackInSlot(0).isEmpty() && zone.ammo(ammoItem()) <= 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TowerBlockEntity tower) {
        if (tower.cooldown > 0) {
            tower.cooldown--;
            return;
        }
        tower.cooldown = tower.fire((ServerLevel) level) ? tower.type.cooldown() : 5;
    }

    public TargetMode targetMode() {
        return targetMode;
    }

    public void cycleTargetMode() {
        targetMode = targetMode.next();
        setChanged();
    }

    /** Base range, +30 % on a mountain plateau, reduced by the fog mutator. */
    public double range() {
        double range = type.range();
        if (level != null && level.getBlockState(worldPosition.below()).is(ModBlocks.ARENA_CLIFF.get())) {
            range *= 1.3;
        }
        if (level instanceof ServerLevel serverLevel && Arenas.isArena(serverLevel)) {
            range *= TowerDefense.get(serverLevel.getServer()).activeMutator(Arenas.slotAt(worldPosition)).rangeFactor();
        }
        return range;
    }

    private boolean fire(ServerLevel level) {
        double range = range();
        Vec3 muzzle = worldPosition.getCenter().add(0, 0.8, 0);
        // Camouflaged enemies (forest thicket) are only spotted within half the range.
        List<TdEnemy> targets = level.getEntitiesOfClass(TdEnemy.class, new AABB(worldPosition).inflate(range),
                        enemy -> enemy.isAlive() && enemy.position().distanceTo(muzzle) <= (enemy.camouflaged() ? range / 2 : range)).stream()
                .sorted(targetMode.order())
                .limit(type.targets())
                .toList();
        if (targets.isEmpty() || !payForShot()) {
            return false;
        }
        float damage = (float) TowerStats.damage(type, upgradeLevel);
        Vec3 from = muzzle;
        for (TdEnemy target : targets) {
            Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
            trail(level, from, to, switch (type) {
                case CROSSBOW -> ParticleTypes.CRIT;
                case GUN -> ParticleTypes.SMOKE;
                case TESLA -> ParticleTypes.ELECTRIC_SPARK;
                case LASER -> ParticleTypes.END_ROD;
            });
            target.invulnerableTime = 0;
            target.hurt(type.energyWeapon() ? level.damageSources().magic() : level.damageSources().generic(), damage);
            if (type == TowerType.TESLA) {
                from = to; // chain lightning jumps on
            }
        }
        SoundEvent sound = switch (type) {
            case CROSSBOW -> SoundEvents.CROSSBOW_SHOOT;
            case GUN -> SoundEvents.FIREWORK_ROCKET_BLAST;
            case TESLA -> SoundEvents.BEACON_POWER_SELECT;
            case LASER -> SoundEvents.GUARDIAN_ATTACK;
        };
        level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 0.6F, 1.2F);
        return true;
    }

    /** Uses the tower's own ammunition or energy first, then the arena reserve filled by the arena feeder. */
    private boolean payForShot() {
        if (type.usesEnergy()) {
            return energy.consume(type.energyPerShot()) || drawFromArena(defense -> defense.drawEnergy(Arenas.slotAt(worldPosition), type.energyPerShot()));
        }
        if (!ammo.getStackInSlot(0).isEmpty()) {
            ammo.extractItem(0, 1, false);
            return true;
        }
        return drawFromArena(defense -> defense.drawAmmo(Arenas.slotAt(worldPosition), ammoItem()));
    }

    private boolean drawFromArena(java.util.function.Predicate<TowerDefense> draw) {
        return level instanceof ServerLevel serverLevel && Arenas.isArena(serverLevel) && draw.test(TowerDefense.get(serverLevel.getServer()));
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        int steps = (int) Math.max(2, from.distanceTo(to) * 2);
        for (int i = 0; i <= steps; i++) {
            Vec3 point = from.lerp(to, (double) i / steps);
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
    }

    /** Called by enemies. At 0 HP the tower turns into a ruin that keeps its type and upgrade level. */
    public void damage(double amount) {
        if (level == null || isRemoved()) {
            return;
        }
        health -= (int) Math.ceil(amount);
        setChanged();
        if (health <= 0) {
            Level world = this.level;
            BlockPos pos = worldPosition;
            TowerType towerType = type;
            int keptLevel = upgradeLevel;
            dropAmmo();
            world.setBlock(pos, ModBlocks.TOWER_RUIN.get().defaultBlockState(), Block.UPDATE_ALL);
            if (world.getBlockEntity(pos) instanceof TowerRuinBlockEntity ruin) {
                ruin.remember(towerType, keptLevel);
            }
            world.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.7F, 1.4F);
            if (world instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    public long repairCost() {
        return TowerStats.repairCost(health, maxHealth());
    }

    public void repair() {
        health = maxHealth();
        setChanged();
    }

    /** Restores a tower from its ruin with full health. */
    void restore(int keptLevel) {
        upgradeLevel = Math.max(1, Math.min(TowerStats.MAX_LEVEL, keptLevel));
        health = maxHealth();
        setChanged();
    }

    public List<SizedIngredient> upgradeMaterials(int toLevel) {
        Item material = switch (TowerStats.upgradeMaterial(toLevel)) {
            case IRON_PLATE -> ModItems.IRON_PLATE.get();
            case COPPER_CABLE -> ModItems.COPPER_CABLE.get();
            case IRON_GEAR -> ModItems.IRON_GEAR.get();
            case MOTOR -> ModItems.MOTOR.get();
        };
        return List.of(SizedIngredient.of(material, TowerStats.upgradeAmount(toLevel)));
    }

    /** Upgrade to the next level, paid by the player's team and inventory. */
    public boolean upgrade(ServerPlayer player) {
        if (upgradeLevel >= TowerStats.MAX_LEVEL) {
            return false;
        }
        int next = upgradeLevel + 1;
        long credits = TowerStats.upgradeCredits(next);
        List<SizedIngredient> materials = upgradeMaterials(next);
        TeamRegistry registry = TeamData.registry(player.server);
        Team team = registry.ensureTeam(player.getUUID(), player.getGameProfile().getName());
        if (!Blueprints.hasAll(player.getInventory(), materials, 1)) {
            player.displayClientMessage(Component.translatable("craftorio.workbench.missing").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!TeamData.maySpend(player)) {
            return false;
        }
        if (!registry.withdraw(team.id(), credits)) {
            player.displayClientMessage(Component.translatable("craftorio.blueprint.status.not_enough_credits").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Blueprints.take(player.getInventory(), materials, 1);
        int oldMax = maxHealth();
        upgradeLevel = next;
        health += maxHealth() - oldMax;
        setChanged();
        player.displayClientMessage(Component.translatable("craftorio.tower.upgraded", upgradeLevel, Credits.format(credits)).withStyle(ChatFormatting.GREEN), true);
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

    /** Sets level and health from a tower item taken out of the depot. */
    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        ModDataComponents.TowerState state = input.get(ModDataComponents.TOWER_STATE.get());
        if (state != null) {
            upgradeLevel = Math.max(1, Math.min(TowerStats.MAX_LEVEL, state.level()));
            health = Math.max(0, Math.min(maxHealth(), state.health()));
        }
    }

    /** Broken towers drop an item that remembers level and health. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!TowerStats.isPristine(type, upgradeLevel, health)) {
            components.set(ModDataComponents.TOWER_STATE.get(), new ModDataComponents.TowerState(upgradeLevel, health));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("target", targetMode.name());
        tag.putInt("level", upgradeLevel);
        tag.putInt("health", health);
        tag.putInt("energy", energy.getEnergyStored());
        tag.put("ammo", ammo.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        upgradeLevel = Math.max(1, tag.getInt("level"));
        try {
            targetMode = TargetMode.valueOf(tag.getString("target"));
        } catch (IllegalArgumentException missing) {
            targetMode = TargetMode.FIRST;
        }
        health = tag.contains("health") ? tag.getInt("health") : maxHealth();
        energy.setEnergy(tag.getInt("energy"));
        ammo.deserializeNBT(registries, tag.getCompound("ammo"));
    }
}
