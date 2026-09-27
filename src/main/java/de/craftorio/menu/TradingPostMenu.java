package de.craftorio.menu;

import de.craftorio.economy.Economy;
import de.craftorio.economy.block.TradingPostBlock;
import de.craftorio.economy.block.TradingPostBlockEntity;
import de.craftorio.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The trading post's counter: put items in, see what they are worth, sell them with a button. */
public final class TradingPostMenu extends MachineMenuBase {
    public static final int SELL_BUTTON = 0;
    public static final int COUNTER_X = 26;
    public static final int COUNTER_Y = 17;

    private final TradingPostBlockEntity post;
    private final String owner;
    /** Total earned as four 16-bit parts (data slots sync as shorts). */
    private final int[] earned = new int[4];

    public TradingPostMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, (TradingPostBlockEntity) inventory.player.level().getBlockEntity(buf.readBlockPos()), buf.readUtf());
    }

    public TradingPostMenu(int containerId, Inventory inventory, TradingPostBlockEntity post, String owner) {
        super(ModMenus.TRADING_POST.get(), containerId, post);
        this.post = post;
        this.owner = owner;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new SlotItemHandler(post.counter(), column + row * 3, COUNTER_X + column * 18, COUNTER_Y + row * 18));
            }
        }
        addPlayerInventory(inventory);
        for (int part = 0; part < 4; part++) {
            int shift = part * 16;
            int index = part;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return (int) ((post.totalEarned() >>> shift) & 0xFFFF);
                }

                @Override
                public void set(int value) {
                    earned[index] = value & 0xFFFF;
                }
            });
        }
    }

    public String owner() {
        return owner;
    }

    public long totalEarned() {
        long total = 0;
        for (int part = 0; part < 4; part++) {
            total |= (long) earned[part] << (part * 16);
        }
        return total;
    }

    /** What the counter's items would sell for right now (prices are known on both sides). */
    public long counterValue() {
        long value = 0;
        for (int slot = 0; slot < TradingPostBlockEntity.COUNTER_SLOTS; slot++) {
            var stack = slots.get(slot).getItem();
            value += Economy.unitPrice(stack) * stack.getCount();
        }
        return value;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == SELL_BUTTON && player instanceof ServerPlayer serverPlayer) {
            TradingPostBlock.sellCounter(post, serverPlayer);
            return true;
        }
        return false;
    }
}
