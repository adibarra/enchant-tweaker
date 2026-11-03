package com.adibarra.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

/** decides whether enchantment names should use the shiny color */
@SuppressWarnings("unused")
public class ADShiny {

    private ADShiny() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /** checks whether an enchantment is at its non-curse maximum */
    public static boolean shouldColorGold(int level, int maxLevel, boolean cursed) {
        if (level < maxLevel)
            return false;
        return !cursed;
    }

    /** applies the complete shiny-name style decision for an enchantment name */
    public static void applyNameStyle(MutableComponent text, int level, int maxLevel, boolean cursed,
        float chargedRoll) {
        if (!shouldColorGold(level, maxLevel, cursed))
            return;
        text.withStyle(ChatFormatting.YELLOW);
        if (chargedRoll < 0.005f) {
            text.withStyle(ChatFormatting.OBFUSCATED);
        }
    }
}
