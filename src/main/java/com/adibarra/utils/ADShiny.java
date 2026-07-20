package com.adibarra.utils;

/** decides whether enchantment names should use the shiny color */
@SuppressWarnings("unused")
public class ADShiny {

    private ADShiny() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /** checks whether an enchantment is at its non-curse maximum */
    public static boolean shouldColorGold(int level, int maxLevel, boolean cursed) {
        if (level < maxLevel) return false;
        return !cursed;
    }
}
