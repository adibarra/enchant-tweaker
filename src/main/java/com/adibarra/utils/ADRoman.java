package com.adibarra.utils;

import org.jetbrains.annotations.Nullable;

/** converts enchantment levels to Roman numerals */
@SuppressWarnings("unused")
public class ADRoman {

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV",
            "I"};

    /**
     * translation-key prefix vanilla uses for enchantment level display, e.g.
     * {@code "enchantment.level.5"}
     */
    private static final String LEVEL_PREFIX = "enchantment.level.";

    /** upper clamp for #toRoman(int) */
    private static final int MAX_ROMAN = 3999;

    private ADRoman() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /**
     * converts an integer to its Roman numeral representation using a greedy
     * algorithm
     */
    public static String toRoman(int num) {
        if (num <= 0)
            return "";
        if (num > MAX_ROMAN)
            num = MAX_ROMAN;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (num >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_SYMBOLS[i]);
                num -= ROMAN_VALUES[i];
            }
        }
        return sb.toString();
    }

    /**
     * computes the enchantment level translation override shared by both mixin
     * injectors
     */
    public static @Nullable String enchantmentLevelOverride(int level) {
        if (level == 0)
            return "0";
        if (level > 10)
            return toRoman(level);
        return null;
    }

    /** returns a Roman numeral override for an enchantment-level key */
    public static @Nullable String levelKeyOverride(String key) {
        if (key == null || !key.startsWith(LEVEL_PREFIX))
            return null;
        try {
            int level = Integer.parseInt(key.substring(LEVEL_PREFIX.length()));
            return enchantmentLevelOverride(level);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
