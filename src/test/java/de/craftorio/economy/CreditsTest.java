package de.craftorio.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreditsTest {
    @Test
    void groupsThousands() {
        assertEquals("0 ¢", Credits.format(0));
        assertEquals("999 ¢", Credits.format(999));
        assertEquals("1.000 ¢", Credits.format(1000));
        assertEquals("1.234.567 ¢", Credits.format(1_234_567));
        assertEquals("-12.345 ¢", Credits.format(-12_345));
        assertEquals("-9.223.372.036.854.775.808 ¢", Credits.format(Long.MIN_VALUE));
    }
}
