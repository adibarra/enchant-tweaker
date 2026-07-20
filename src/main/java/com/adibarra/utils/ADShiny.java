package com.adibarra.utils;

import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

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

    /** Applies the complete shiny-name style decision for an enchantment name. */
    public static void applyNameStyle(
            MutableText text, int level, int maxLevel, boolean cursed, float chargedRoll) {
        if (!shouldColorGold(level, maxLevel, cursed)) return;
        text.formatted(Formatting.YELLOW);
        if (chargedRoll < 0.005f) {
            text.formatted(Formatting.OBFUSCATED);
        }
    }
}
