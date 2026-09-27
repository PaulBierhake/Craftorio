package de.craftorio.economy;

/** Formatting for the team currency. */
public final class Credits {
    public static final String SYMBOL = "¢";

    private Credits() {
    }

    /** {@code 1234567} becomes {@code "1.234.567 ¢"}. */
    public static String format(long amount) {
        String digits = Long.toString(amount);
        boolean negative = digits.startsWith("-");
        if (negative) {
            digits = digits.substring(1);
        }
        StringBuilder out = new StringBuilder(digits.length() + 8);
        int firstGroup = digits.length() % 3 == 0 ? 3 : digits.length() % 3;
        out.append(digits, 0, firstGroup);
        for (int i = firstGroup; i < digits.length(); i += 3) {
            out.append('.').append(digits, i, i + 3);
        }
        return (negative ? "-" : "") + out + " " + SYMBOL;
    }
}
