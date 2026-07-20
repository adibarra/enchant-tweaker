package com.adibarra.enchanttweaker.test;

import com.adibarra.utils.ADShiny;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ShinyNameGameTest implements FabricGameTest {

    // below and at the maximum level for a non-curse

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void colorAtMaxLevel(TestContext helper) {
        helper.assertTrue(!ADShiny.shouldColorGold(4, 5, false), "level 4 of 5 (not cursed) should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(5, 5, false),  "level 5 of 5 (not cursed) SHOULD recolor");
        helper.complete();
    }

    // threshold boundary around max level (non-curse)

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void thresholdBoundary(TestContext helper) {
        // multi-level enchant (max 5): only at or above max recolors
        helper.assertTrue(!ADShiny.shouldColorGold(4, 5, false), "max-1 should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(5, 5, false),  "max SHOULD recolor");
        helper.assertTrue(ADShiny.shouldColorGold(6, 5, false),  "max+1 (over max) SHOULD recolor");

        // `maxLevel` == 1 enchant (e.g. Mending): every applied level is at or above max
        helper.assertTrue(!ADShiny.shouldColorGold(0, 1, false), "level 0 of 1 should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(1, 1, false),  "level 1 of 1 (at max) SHOULD recolor");
        helper.assertTrue(ADShiny.shouldColorGold(2, 1, false),  "level 2 of 1 (over max) SHOULD recolor");
        helper.complete();
    }

    // cursed at (or above) max level is never recolored

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cursedNeverColored(TestContext helper) {
        boolean cursed = Enchantments.BINDING_CURSE.isCursed();
        int maxLevel = Enchantments.BINDING_CURSE.getMaxLevel();
        helper.assertTrue(cursed, "Binding Curse should report isCursed()==true");

        helper.assertTrue(!ADShiny.shouldColorGold(maxLevel, maxLevel, cursed),     "cursed enchant at max level should NOT recolor");
        helper.assertTrue(!ADShiny.shouldColorGold(maxLevel + 1, maxLevel, cursed), "cursed enchant over max level should NOT recolor");
        // contrast: same level/max but not cursed -> recolors
        helper.assertTrue(ADShiny.shouldColorGold(maxLevel, maxLevel, false),       "non-cursed enchant at max level SHOULD recolor");
        helper.complete();
    }

    // surprising branch: maxLevel <= 0

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void maxLevelZeroOrNegative(TestContext helper) {
        helper.assertTrue(ADShiny.shouldColorGold(0, 0, false),  "level 0 of max 0 (not cursed) SHOULD recolor (0 is not < 0)");
        helper.assertTrue(!ADShiny.shouldColorGold(0, 0, true),  "level 0 of max 0 (cursed) should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(5, 0, false),  "level 5 of max 0 (not cursed) SHOULD recolor (above max)");
        helper.assertTrue(ADShiny.shouldColorGold(0, -5, false), "level 0 of max -5 (not cursed) SHOULD recolor (above max)");
        helper.assertTrue(!ADShiny.shouldColorGold(0, -5, true), "level 0 of max -5 (cursed) should NOT recolor");
        helper.complete();
    }

    // negative levels

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void negativeLevels(TestContext helper) {
        // a negative level below a positive max is correctly "below max" -> no recolor
        helper.assertTrue(!ADShiny.shouldColorGold(-1, 5, false),                "level -1 of max 5 should NOT recolor (below max)");
        helper.assertTrue(!ADShiny.shouldColorGold(Integer.MIN_VALUE, 5, false), "level MIN_VALUE of max 5 should NOT recolor (below max)");
        // equal negative level and max: not < -> recolor iff not cursed
        helper.assertTrue(ADShiny.shouldColorGold(-1, -1, false),                "level -1 of max -1 (not cursed) SHOULD recolor");
        helper.assertTrue(!ADShiny.shouldColorGold(-5, -5, true),                "level -5 of max -5 (cursed) should NOT recolor");
        helper.complete();
    }

    // int extremes: comparison never overflows

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void extremeLevelsNoOverflow(TestContext helper) {
        // the decision uses a `<` comparison (not subtraction), so it is immune to int overflow
        // even at the widest possible spread between level and maxLevel
        helper.assertTrue(ADShiny.shouldColorGold(Integer.MAX_VALUE, Integer.MAX_VALUE, false),  "MAX == MAX (not cursed) SHOULD recolor");
        helper.assertTrue(!ADShiny.shouldColorGold(Integer.MAX_VALUE, Integer.MAX_VALUE, true),  "MAX == MAX (cursed) should NOT recolor");
        helper.assertTrue(ADShiny.shouldColorGold(Integer.MIN_VALUE, Integer.MIN_VALUE, false),  "MIN == MIN (not cursed) SHOULD recolor");
        helper.assertTrue(!ADShiny.shouldColorGold(Integer.MIN_VALUE, Integer.MAX_VALUE, false), "MIN level vs MAX max should NOT recolor (below max, no overflow)");
        helper.assertTrue(ADShiny.shouldColorGold(Integer.MAX_VALUE, Integer.MIN_VALUE, false),  "MAX level vs MIN max SHOULD recolor (above max, no overflow)");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void fullStyleBelowMaxIsUnchanged(TestContext helper) {
        MutableText text = Text.literal("Sharpness IV");
        ADShiny.applyNameStyle(text, 4, 5, false, 0.0f);
        helper.assertTrue(text.getStyle().getColor() == null,
            "below-max name should retain its existing color");
        helper.assertFalse(text.getStyle().isObfuscated(),
            "below-max name should never become obfuscated");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void fullStyleGoldBoundaryIsNotCharged(TestContext helper) {
        MutableText text = Text.literal("Sharpness V");
        ADShiny.applyNameStyle(text, 5, 5, false, 0.005f);
        helper.assertTrue(text.getStyle().getColor() != null
                && text.getStyle().getColor().getRgb() == Formatting.YELLOW.getColorValue(),
            "max-level name should be yellow");
        helper.assertFalse(text.getStyle().isObfuscated(),
            "the exact 0.005 charged boundary should not be obfuscated");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void fullStyleChargedAndCurseBranches(TestContext helper) {
        MutableText charged = Text.literal("Sharpness V");
        ADShiny.applyNameStyle(charged, 5, 5, false, Math.nextDown(0.005f));
        helper.assertTrue(charged.getStyle().getColor() != null
                && charged.getStyle().getColor().getRgb() == Formatting.YELLOW.getColorValue(),
            "charged max-level name should remain yellow");
        helper.assertTrue(charged.getStyle().isObfuscated(),
            "roll immediately below 0.005 should be obfuscated");

        MutableText cursed = Text.literal("Binding Curse");
        ADShiny.applyNameStyle(cursed, 2, 1, true, 0.0f);
        helper.assertTrue(cursed.getStyle().getColor() == null && !cursed.getStyle().isObfuscated(),
            "curses must remain unstyled even on a charged roll");
        helper.complete();
    }
}
