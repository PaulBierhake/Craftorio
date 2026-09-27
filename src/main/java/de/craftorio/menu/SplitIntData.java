package de.craftorio.menu;

/** Menu data slots sync as shorts; larger values are sent as two 16-bit halves. */
public final class SplitIntData {
    private SplitIntData() {
    }

    public static int low(int value) {
        return value & 0xFFFF;
    }

    public static int high(int value) {
        return (value >>> 16) & 0xFFFF;
    }

    public static int join(int low, int high) {
        return (low & 0xFFFF) | ((high & 0xFFFF) << 16);
    }
}
