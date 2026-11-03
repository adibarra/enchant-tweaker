package com.adibarra.enchanttweaker.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantments;

import com.adibarra.utils.ADShiny;

public class ShinyNameGameTest implements FabricGameTest {

    // below and at the maximum level for a non-curse

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void colorAtMaxLevel(GameTestHelper helper) {
        helper.assertTrue(!ADShiny.shouldColorGold(4, 5, false), "level 4 of 5 (not cursed) should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(5, 5, false), "level 5 of 5 (not cursed) SHOULD recolor");
        helper.succeed();
    }

    // threshold boundary around max level (non-curse)

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void thresholdBoundary(GameTestHelper helper) {
        // multi-level enchant (max 5): only at or above max recolors
        helper.assertTrue(!ADShiny.shouldColorGold(4, 5, false), "max-1 should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(5, 5, false), "max SHOULD recolor");
        helper.assertTrue(ADShiny.shouldColorGold(6, 5, false), "max+1 (over max) SHOULD recolor");

        // levels at or above one recolor
        helper.assertTrue(!ADShiny.shouldColorGold(0, 1, false), "level 0 of 1 should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(1, 1, false), "level 1 of 1 (at max) SHOULD recolor");
        helper.assertTrue(ADShiny.shouldColorGold(2, 1, false), "level 2 of 1 (over max) SHOULD recolor");
        helper.succeed();
    }

    // cursed at (or above) max level is never recolored

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void cursedNeverColored(GameTestHelper helper) {
        boolean cursed = Enchantments.BINDING_CURSE.isCurse();
        int maxLevel = Enchantments.BINDING_CURSE.getMaxLevel();
        helper.assertTrue(cursed, "Binding Curse should report isCursed()==true");

        helper.assertTrue(!ADShiny.shouldColorGold(maxLevel, maxLevel, cursed),
            "cursed enchant at max level should NOT recolor");
        helper.assertTrue(!ADShiny.shouldColorGold(maxLevel + 1, maxLevel, cursed),
            "cursed enchant over max level should NOT recolor");
        // same non-cursed values recolor
        helper.assertTrue(ADShiny.shouldColorGold(maxLevel, maxLevel, false),
            "non-cursed enchant at max level SHOULD recolor");
        helper.succeed();
    }

    // surprising branch: maxLevel <= 0

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void maxLevelZeroOrNegative(GameTestHelper helper) {
        helper.assertTrue(ADShiny.shouldColorGold(0, 0, false),
            "level 0 of max 0 (not cursed) SHOULD recolor (0 is not < 0)");
        helper.assertTrue(!ADShiny.shouldColorGold(0, 0, true), "level 0 of max 0 (cursed) should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(5, 0, false),
            "level 5 of max 0 (not cursed) SHOULD recolor (above max)");
        helper.assertTrue(ADShiny.shouldColorGold(0, -5, false),
            "level 0 of max -5 (not cursed) SHOULD recolor (above max)");
        helper.assertTrue(!ADShiny.shouldColorGold(0, -5, true), "level 0 of max -5 (cursed) should NOT recolor");
        helper.succeed();
    }

    // negative levels

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void negativeLevels(GameTestHelper helper) {
        // negative levels below positive maxima do not recolor
        helper.assertTrue(!ADShiny.shouldColorGold(-1, 5, false), "level -1 of max 5 should NOT recolor (below max)");
        helper.assertTrue(!ADShiny.shouldColorGold(Integer.MIN_VALUE, 5, false),
            "level MIN_VALUE of max 5 should NOT recolor (below max)");
        // equal negative values recolor unless cursed
        helper.assertTrue(ADShiny.shouldColorGold(-1, -1, false), "level -1 of max -1 (not cursed) SHOULD recolor");
        helper.assertTrue(!ADShiny.shouldColorGold(-5, -5, true), "level -5 of max -5 (cursed) should NOT recolor");
        helper.succeed();
    }

    // int extremes: comparison never overflows

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void extremeLevelsNoOverflow(GameTestHelper helper) {
        // comparison avoids arithmetic overflow at integer extremes
        helper.assertTrue(ADShiny.shouldColorGold(Integer.MAX_VALUE, Integer.MAX_VALUE, false),
            "MAX == MAX (not cursed) SHOULD recolor");
        helper.assertTrue(!ADShiny.shouldColorGold(Integer.MAX_VALUE, Integer.MAX_VALUE, true),
            "MAX == MAX (cursed) should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(Integer.MIN_VALUE, Integer.MIN_VALUE, false),
            "MIN == MIN (not cursed) SHOULD recolor");
        helper.assertTrue(!ADShiny.shouldColorGold(Integer.MIN_VALUE, Integer.MAX_VALUE, false),
            "MIN level vs MAX max should NOT recolor (below max, no overflow)");
        helper.assertTrue(ADShiny.shouldColorGold(Integer.MAX_VALUE, Integer.MIN_VALUE, false),
            "MAX level vs MIN max SHOULD recolor (above max, no overflow)");
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void fullStyleBelowMaxIsUnchanged(GameTestHelper helper) {
        MutableComponent text = Component.literal("Sharpness IV").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
        ADShiny.applyNameStyle(text, 4, 5, false, 0.0f);
        helper.assertTrue(
            text.getStyle().getColor() != null
                && text.getStyle().getColor().getValue() == ChatFormatting.AQUA.getColor(),
            "below-max name should preserve its existing color");
        helper.assertTrue(text.getStyle().isBold(), "below-max name should preserve its existing bold style");
        helper.assertFalse(text.getStyle().isObfuscated(), "below-max name should never become obfuscated");
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void fullStyleGoldBoundaryIsNotCharged(GameTestHelper helper) {
        MutableComponent text = Component.literal("Sharpness V");
        ADShiny.applyNameStyle(text, 5, 5, false, 0.005f);
        helper.assertTrue(
            text.getStyle().getColor() != null
                && text.getStyle().getColor().getValue() == ChatFormatting.YELLOW.getColor(),
            "max-level name should be yellow");
        helper.assertFalse(text.getStyle().isObfuscated(), "the exact 0.005 charged boundary should not be obfuscated");
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void fullStyleChargedAndCurseBranches(GameTestHelper helper) {
        MutableComponent charged = Component.literal("Sharpness V");
        ADShiny.applyNameStyle(charged, 5, 5, false, Math.nextDown(0.005f));
        helper.assertTrue(
            charged.getStyle().getColor() != null
                && charged.getStyle().getColor().getValue() == ChatFormatting.YELLOW.getColor(),
            "charged max-level name should remain yellow");
        helper.assertTrue(charged.getStyle().isObfuscated(), "roll immediately below 0.005 should be obfuscated");

        MutableComponent cursed = Component.literal("Binding Curse");
        ADShiny.applyNameStyle(cursed, 2, 1, true, 0.0f);
        helper.assertTrue(cursed.getStyle().getColor() == null && !cursed.getStyle().isObfuscated(),
            "curses must remain unstyled even on a charged roll");
        helper.succeed();
    }
}
