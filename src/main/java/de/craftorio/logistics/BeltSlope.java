package de.craftorio.logistics;

import net.minecraft.util.StringRepresentable;

/**
 * Slope of a belt, seen from its travel direction. An {@code UP} belt takes items from the belt behind it on the
 * same level and hands them to the belt in front one level higher; a {@code DOWN} belt takes items from the belt
 * behind it one level higher and hands them to the belt in front on its own level.
 */
public enum BeltSlope implements StringRepresentable {
    FLAT("flat"),
    UP("up"),
    DOWN("down");

    private final String key;

    BeltSlope(String key) {
        this.key = key;
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    /** Height of the belt in front relative to this belt. */
    public int outputLevel() {
        return this == UP ? 1 : 0;
    }

    /** Height of the belt behind relative to this belt. */
    public int inputLevel() {
        return this == DOWN ? 1 : 0;
    }

    public BeltSlope next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** See {@link BeltGeometry#connects}. */
    public static boolean connects(BeltSlope sender, BeltSlope receiver, int senderLevel, int receiverLevel) {
        return BeltGeometry.connects(senderLevel, sender.outputLevel(), receiverLevel, receiver.inputLevel());
    }
}
