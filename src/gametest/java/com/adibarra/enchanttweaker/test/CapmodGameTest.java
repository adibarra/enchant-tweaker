package com.adibarra.enchanttweaker.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

public class CapmodGameTest implements FabricGameTest {

    // ─── Capmod master switch ────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDisabled(TestContext helper) {
        ETTestHelper.setCapmod(false);
        ETTestHelper.setEnchantCap("sharpness", 10);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "Sharpness should be vanilla 5 when capmod disabled (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
        } finally {
            ETTestHelper.setCapmod(true);
            ETTestHelper.setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodVanillaPassthrough(TestContext helper) {
        ETTestHelper.setCapmod(true);
        // all caps already -1 from run/gametest config; just verify passthrough
        helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,    "Sharpness -1 should passthrough to vanilla 5");
        helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 3,   "Unbreaking -1 should passthrough to vanilla 3");
        helper.assertTrue(Enchantments.LOOTING.getMaxLevel() == 3,      "Looting -1 should passthrough to vanilla 3");
        helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 4,   "Protection -1 should passthrough to vanilla 4");
        helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 1,      "Mending -1 should passthrough to vanilla 1");
        helper.complete();
    }

    // ─── Capmod DamageEnchant ────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDamageCustom(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ETTestHelper.setEnchantCap("smite", 7);
        ETTestHelper.setEnchantCap("bane_of_arthropods", 8);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 10,         "Sharpness should be 10");
            helper.assertTrue(Enchantments.SMITE.getMaxLevel() == 7,              "Smite should be 7");
            helper.assertTrue(Enchantments.BANE_OF_ARTHROPODS.getMaxLevel() == 8, "Bane of Arthropods should be 8");
        } finally {
            ETTestHelper.setEnchantCap("sharpness", -1);
            ETTestHelper.setEnchantCap("smite", -1);
            ETTestHelper.setEnchantCap("bane_of_arthropods", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDamageClampMax(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 300);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 255, "Sharpness 300 should clamp to 255");
        } finally {
            ETTestHelper.setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodDamageZero(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 0);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 0, "Sharpness should be 0");
        } finally {
            ETTestHelper.setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    // ─── Capmod GenericEnchant ───────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodGenericCustom(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("unbreaking", 10);
        ETTestHelper.setEnchantCap("efficiency", 8);
        try {
            helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 10, "Unbreaking should be 10");
            helper.assertTrue(Enchantments.EFFICIENCY.getMaxLevel() == 8,  "Efficiency should be 8");
        } finally {
            ETTestHelper.setEnchantCap("unbreaking", -1);
            ETTestHelper.setEnchantCap("efficiency", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodGenericClampMax(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("unbreaking", 300);
        try {
            helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 255, "Unbreaking 300 should clamp to 255");
        } finally {
            ETTestHelper.setEnchantCap("unbreaking", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodGenericAllRemaining(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("depth_strider",  5);
        ETTestHelper.setEnchantCap("fire_aspect",    4);
        ETTestHelper.setEnchantCap("frost_walker",   4);
        ETTestHelper.setEnchantCap("impaling",       8);
        ETTestHelper.setEnchantCap("knockback",      5);
        ETTestHelper.setEnchantCap("loyalty",        6);
        ETTestHelper.setEnchantCap("lure",           6);
        ETTestHelper.setEnchantCap("piercing",       8);
        ETTestHelper.setEnchantCap("power",          8);
        ETTestHelper.setEnchantCap("punch",          5);
        ETTestHelper.setEnchantCap("quick_charge",   5);
        ETTestHelper.setEnchantCap("respiration",    5);
        ETTestHelper.setEnchantCap("riptide",        6);
        ETTestHelper.setEnchantCap("soul_speed",     5);
        ETTestHelper.setEnchantCap("sweeping_edge",  5);
        ETTestHelper.setEnchantCap("swift_sneak",    5);
        ETTestHelper.setEnchantCap("thorns",         5);
        try {
            helper.assertTrue(Enchantments.DEPTH_STRIDER.getMaxLevel()    == 5, "Depth Strider should be 5");
            helper.assertTrue(Enchantments.FIRE_ASPECT.getMaxLevel()      == 4, "Fire Aspect should be 4");
            helper.assertTrue(Enchantments.FROST_WALKER.getMaxLevel()     == 4, "Frost Walker should be 4");
            helper.assertTrue(Enchantments.IMPALING.getMaxLevel()         == 8, "Impaling should be 8");
            helper.assertTrue(Enchantments.KNOCKBACK.getMaxLevel()        == 5, "Knockback should be 5");
            helper.assertTrue(Enchantments.LOYALTY.getMaxLevel()          == 6, "Loyalty should be 6");
            helper.assertTrue(Enchantments.LURE.getMaxLevel()             == 6, "Lure should be 6");
            helper.assertTrue(Enchantments.PIERCING.getMaxLevel()         == 8, "Piercing should be 8");
            helper.assertTrue(Enchantments.POWER.getMaxLevel()            == 8, "Power should be 8");
            helper.assertTrue(Enchantments.PUNCH.getMaxLevel()            == 5, "Punch should be 5");
            helper.assertTrue(Enchantments.QUICK_CHARGE.getMaxLevel()     == 5, "Quick Charge should be 5");
            helper.assertTrue(Enchantments.RESPIRATION.getMaxLevel()      == 5, "Respiration should be 5");
            helper.assertTrue(Enchantments.RIPTIDE.getMaxLevel()          == 6, "Riptide should be 6");
            helper.assertTrue(Enchantments.SOUL_SPEED.getMaxLevel()       == 5, "Soul Speed should be 5");
            helper.assertTrue(Enchantments.SWEEPING_EDGE.getMaxLevel()    == 5, "Sweeping Edge should be 5");
            helper.assertTrue(Enchantments.SWIFT_SNEAK.getMaxLevel()      == 5, "Swift Sneak should be 5");
            helper.assertTrue(Enchantments.THORNS.getMaxLevel()           == 5, "Thorns should be 5");
        } finally {
            ETTestHelper.setEnchantCap("depth_strider",  -1);
            ETTestHelper.setEnchantCap("fire_aspect",    -1);
            ETTestHelper.setEnchantCap("frost_walker",   -1);
            ETTestHelper.setEnchantCap("impaling",       -1);
            ETTestHelper.setEnchantCap("knockback",      -1);
            ETTestHelper.setEnchantCap("loyalty",        -1);
            ETTestHelper.setEnchantCap("lure",           -1);
            ETTestHelper.setEnchantCap("piercing",       -1);
            ETTestHelper.setEnchantCap("power",          -1);
            ETTestHelper.setEnchantCap("punch",          -1);
            ETTestHelper.setEnchantCap("quick_charge",   -1);
            ETTestHelper.setEnchantCap("respiration",    -1);
            ETTestHelper.setEnchantCap("riptide",        -1);
            ETTestHelper.setEnchantCap("soul_speed",     -1);
            ETTestHelper.setEnchantCap("sweeping_edge",  -1);
            ETTestHelper.setEnchantCap("swift_sneak",    -1);
            ETTestHelper.setEnchantCap("thorns",         -1);
        }
        helper.complete();
    }

    // ─── Capmod LuckEnchant ──────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodLuckCustom(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("looting", 10);
        ETTestHelper.setEnchantCap("fortune", 8);
        ETTestHelper.setEnchantCap("luck_of_the_sea", 6);
        try {
            helper.assertTrue(Enchantments.LOOTING.getMaxLevel() == 10,        "Looting should be 10");
            helper.assertTrue(Enchantments.FORTUNE.getMaxLevel() == 8,         "Fortune should be 8");
            helper.assertTrue(Enchantments.LUCK_OF_THE_SEA.getMaxLevel() == 6, "Luck of the Sea should be 6");
        } finally {
            ETTestHelper.setEnchantCap("looting", -1);
            ETTestHelper.setEnchantCap("fortune", -1);
            ETTestHelper.setEnchantCap("luck_of_the_sea", -1);
        }
        helper.complete();
    }

    // ─── Capmod ProtectionEnchant ────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodProtectionCustom(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("protection", 10);
        ETTestHelper.setEnchantCap("fire_protection", 8);
        try {
            helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 10,     "Protection should be 10");
            helper.assertTrue(Enchantments.FIRE_PROTECTION.getMaxLevel() == 8, "Fire Protection should be 8");
        } finally {
            ETTestHelper.setEnchantCap("protection", -1);
            ETTestHelper.setEnchantCap("fire_protection", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodProtectionAllTypes(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("protection", 6);
        ETTestHelper.setEnchantCap("fire_protection", 6);
        ETTestHelper.setEnchantCap("feather_falling", 6);
        ETTestHelper.setEnchantCap("blast_protection", 6);
        ETTestHelper.setEnchantCap("projectile_protection", 6);
        try {
            helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 6,            "Protection should be 6");
            helper.assertTrue(Enchantments.FIRE_PROTECTION.getMaxLevel() == 6,       "Fire Protection should be 6");
            helper.assertTrue(Enchantments.FEATHER_FALLING.getMaxLevel() == 6,       "Feather Falling should be 6");
            helper.assertTrue(Enchantments.BLAST_PROTECTION.getMaxLevel() == 6,      "Blast Protection should be 6");
            helper.assertTrue(Enchantments.PROJECTILE_PROTECTION.getMaxLevel() == 6, "Projectile Protection should be 6");
        } finally {
            ETTestHelper.setEnchantCap("protection", -1);
            ETTestHelper.setEnchantCap("fire_protection", -1);
            ETTestHelper.setEnchantCap("feather_falling", -1);
            ETTestHelper.setEnchantCap("blast_protection", -1);
            ETTestHelper.setEnchantCap("projectile_protection", -1);
        }
        helper.complete();
    }

    // ─── Capmod SpecialEnchant ───────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodSpecialCustom(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 5);
        ETTestHelper.setEnchantCap("infinity", 3);
        ETTestHelper.setEnchantCap("silk_touch", 2);
        try {
            helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 5,    "Mending should be 5");
            helper.assertTrue(Enchantments.INFINITY.getMaxLevel() == 3,   "Infinity should be 3");
            helper.assertTrue(Enchantments.SILK_TOUCH.getMaxLevel() == 2, "Silk Touch should be 2");
        } finally {
            ETTestHelper.setEnchantCap("mending", -1);
            ETTestHelper.setEnchantCap("infinity", -1);
            ETTestHelper.setEnchantCap("silk_touch", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodSpecialClampMax(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 300);
        try {
            helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 255, "Mending 300 should clamp to 255");
        } finally {
            ETTestHelper.setEnchantCap("mending", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodSpecialAllRemaining(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("aqua_affinity",   3);
        ETTestHelper.setEnchantCap("binding_curse",   5);
        ETTestHelper.setEnchantCap("channeling",      3);
        ETTestHelper.setEnchantCap("multishot",       5);
        ETTestHelper.setEnchantCap("vanishing_curse", 3);
        try {
            helper.assertTrue(Enchantments.AQUA_AFFINITY.getMaxLevel()   == 3, "Aqua Affinity should be 3");
            helper.assertTrue(Enchantments.BINDING_CURSE.getMaxLevel()   == 5, "Binding Curse should be 5");
            helper.assertTrue(Enchantments.CHANNELING.getMaxLevel()      == 3, "Channeling should be 3");
            helper.assertTrue(Enchantments.MULTISHOT.getMaxLevel()       == 5, "Multishot should be 5");
            helper.assertTrue(Enchantments.VANISHING_CURSE.getMaxLevel() == 3, "Vanishing Curse should be 3");
        } finally {
            ETTestHelper.setEnchantCap("aqua_affinity",   -1);
            ETTestHelper.setEnchantCap("binding_curse",   -1);
            ETTestHelper.setEnchantCap("channeling",      -1);
            ETTestHelper.setEnchantCap("multishot",       -1);
            ETTestHelper.setEnchantCap("vanishing_curse", -1);
        }
        helper.complete();
    }

    // ─── Capmod: negative values other than -1 ────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodNegativeClamp(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", -5);
        try {
            // Any negative value should pass through to vanilla (getCapmodLevel returns vanilla for cap < 0)
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "Sharpness -5 should passthrough to vanilla 5 (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
        } finally {
            ETTestHelper.setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void capmodValueOf1(TestContext helper) {
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 1);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 1,
                "Sharpness 1 should give max level 1 (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
        } finally {
            ETTestHelper.setEnchantCap("sharpness", -1);
        }
        helper.complete();
    }
}
