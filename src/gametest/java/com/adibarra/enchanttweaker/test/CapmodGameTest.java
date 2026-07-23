package com.adibarra.enchanttweaker.test;

import java.util.Map;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.world.GameMode;

import com.adibarra.enchanttweaker.ETMixinPlugin;

public class CapmodGameTest implements FabricGameTest {

    // capmod master switch

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(false);
        ETTestHelper.setEnchantCap("sharpness", 10);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "Sharpness should be vanilla 5 when capmod disabled (got " + Enchantments.SHARPNESS.getMaxLevel()
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodVanillaPassthrough(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled");
        ETTestHelper.setCapmod(true);
        try {
            // all caps are -1 in the GameTest config verify vanilla
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "Sharpness -1 should passthrough to vanilla 5");
            helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 3,
                "Unbreaking -1 should passthrough to vanilla 3");
            helper.assertTrue(Enchantments.LOOTING.getMaxLevel() == 3, "Looting -1 should passthrough to vanilla 3");
            helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 4,
                "Protection -1 should passthrough to vanilla 4");
            helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 1, "Mending -1 should passthrough to vanilla 1");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodPassthroughDoesNotCacheVanillaLevel(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        try {
            ETTestHelper.setCapmod(true);
            ETTestHelper.setEnchantCap("sharpness", -1);
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 5) == 5,
                "a missing cap should preserve vanilla level 5");
            helper.assertTrue(ETMixinPlugin.getCapmodLevel("sharpness", 10) == 10,
                "a missing cap should preserve a later vanilla level 10");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // capmod DamageEnchant

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodDamageCustom(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness", "smite",
            "bane_of_arthropods");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ETTestHelper.setEnchantCap("smite", 7);
        ETTestHelper.setEnchantCap("bane_of_arthropods", 8);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 10, "Sharpness should be 10");
            helper.assertTrue(Enchantments.SMITE.getMaxLevel() == 7, "Smite should be 7");
            helper.assertTrue(Enchantments.BANE_OF_ARTHROPODS.getMaxLevel() == 8, "Bane of Arthropods should be 8");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodDamageClampMax(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 300);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 255, "Sharpness 300 should clamp to 255");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodDamageZero(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 0);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 0, "Sharpness should be 0");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // capmod GenericEnchant

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodGenericCustom(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "unbreaking", "efficiency");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("unbreaking", 10);
        ETTestHelper.setEnchantCap("efficiency", 8);
        try {
            helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 10, "Unbreaking should be 10");
            helper.assertTrue(Enchantments.EFFICIENCY.getMaxLevel() == 8, "Efficiency should be 8");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodGenericClampMax(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "unbreaking");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("unbreaking", 300);
        try {
            helper.assertTrue(Enchantments.UNBREAKING.getMaxLevel() == 255, "Unbreaking 300 should clamp to 255");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodGenericAllRemaining(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "depth_strider",
            "fire_aspect", "frost_walker", "impaling", "knockback", "loyalty", "lure", "piercing", "power", "punch",
            "quick_charge", "respiration", "riptide", "soul_speed", "sweeping_edge", "swift_sneak", "thorns");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("depth_strider", 5);
        ETTestHelper.setEnchantCap("fire_aspect", 4);
        ETTestHelper.setEnchantCap("frost_walker", 4);
        ETTestHelper.setEnchantCap("impaling", 8);
        ETTestHelper.setEnchantCap("knockback", 5);
        ETTestHelper.setEnchantCap("loyalty", 6);
        ETTestHelper.setEnchantCap("lure", 6);
        ETTestHelper.setEnchantCap("piercing", 8);
        ETTestHelper.setEnchantCap("power", 8);
        ETTestHelper.setEnchantCap("punch", 5);
        ETTestHelper.setEnchantCap("quick_charge", 5);
        ETTestHelper.setEnchantCap("respiration", 5);
        ETTestHelper.setEnchantCap("riptide", 6);
        ETTestHelper.setEnchantCap("soul_speed", 5);
        ETTestHelper.setEnchantCap("sweeping_edge", 5);
        ETTestHelper.setEnchantCap("swift_sneak", 5);
        ETTestHelper.setEnchantCap("thorns", 5);
        try {
            helper.assertTrue(Enchantments.DEPTH_STRIDER.getMaxLevel() == 5, "Depth Strider should be 5");
            helper.assertTrue(Enchantments.FIRE_ASPECT.getMaxLevel() == 4, "Fire Aspect should be 4");
            helper.assertTrue(Enchantments.FROST_WALKER.getMaxLevel() == 4, "Frost Walker should be 4");
            helper.assertTrue(Enchantments.IMPALING.getMaxLevel() == 8, "Impaling should be 8");
            helper.assertTrue(Enchantments.KNOCKBACK.getMaxLevel() == 5, "Knockback should be 5");
            helper.assertTrue(Enchantments.LOYALTY.getMaxLevel() == 6, "Loyalty should be 6");
            helper.assertTrue(Enchantments.LURE.getMaxLevel() == 6, "Lure should be 6");
            helper.assertTrue(Enchantments.PIERCING.getMaxLevel() == 8, "Piercing should be 8");
            helper.assertTrue(Enchantments.POWER.getMaxLevel() == 8, "Power should be 8");
            helper.assertTrue(Enchantments.PUNCH.getMaxLevel() == 5, "Punch should be 5");
            helper.assertTrue(Enchantments.QUICK_CHARGE.getMaxLevel() == 5, "Quick Charge should be 5");
            helper.assertTrue(Enchantments.RESPIRATION.getMaxLevel() == 5, "Respiration should be 5");
            helper.assertTrue(Enchantments.RIPTIDE.getMaxLevel() == 6, "Riptide should be 6");
            helper.assertTrue(Enchantments.SOUL_SPEED.getMaxLevel() == 5, "Soul Speed should be 5");
            helper.assertTrue(Enchantments.SWEEPING_EDGE.getMaxLevel() == 5, "Sweeping Edge should be 5");
            helper.assertTrue(Enchantments.SWIFT_SNEAK.getMaxLevel() == 5, "Swift Sneak should be 5");
            helper.assertTrue(Enchantments.THORNS.getMaxLevel() == 5, "Thorns should be 5");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // capmod LuckEnchant

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodLuckCustom(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "looting", "fortune",
            "luck_of_the_sea");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("looting", 10);
        ETTestHelper.setEnchantCap("fortune", 8);
        ETTestHelper.setEnchantCap("luck_of_the_sea", 6);
        try {
            helper.assertTrue(Enchantments.LOOTING.getMaxLevel() == 10, "Looting should be 10");
            helper.assertTrue(Enchantments.FORTUNE.getMaxLevel() == 8, "Fortune should be 8");
            helper.assertTrue(Enchantments.LUCK_OF_THE_SEA.getMaxLevel() == 6, "Luck of the Sea should be 6");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // capmod ProtectionEnchant

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodProtectionCustom(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "protection",
            "fire_protection");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("protection", 10);
        ETTestHelper.setEnchantCap("fire_protection", 8);
        try {
            helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 10, "Protection should be 10");
            helper.assertTrue(Enchantments.FIRE_PROTECTION.getMaxLevel() == 8, "Fire Protection should be 8");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodProtectionAllTypes(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "protection",
            "fire_protection", "feather_falling", "blast_protection", "projectile_protection");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("protection", 6);
        ETTestHelper.setEnchantCap("fire_protection", 6);
        ETTestHelper.setEnchantCap("feather_falling", 6);
        ETTestHelper.setEnchantCap("blast_protection", 6);
        ETTestHelper.setEnchantCap("projectile_protection", 6);
        try {
            helper.assertTrue(Enchantments.PROTECTION.getMaxLevel() == 6, "Protection should be 6");
            helper.assertTrue(Enchantments.FIRE_PROTECTION.getMaxLevel() == 6, "Fire Protection should be 6");
            helper.assertTrue(Enchantments.FEATHER_FALLING.getMaxLevel() == 6, "Feather Falling should be 6");
            helper.assertTrue(Enchantments.BLAST_PROTECTION.getMaxLevel() == 6, "Blast Protection should be 6");
            helper.assertTrue(Enchantments.PROJECTILE_PROTECTION.getMaxLevel() == 6,
                "Projectile Protection should be 6");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // capmod SpecialEnchant

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodSpecialCustom(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "mending", "infinity",
            "silk_touch");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 5);
        ETTestHelper.setEnchantCap("infinity", 3);
        ETTestHelper.setEnchantCap("silk_touch", 2);
        try {
            helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 5, "Mending should be 5");
            helper.assertTrue(Enchantments.INFINITY.getMaxLevel() == 3, "Infinity should be 3");
            helper.assertTrue(Enchantments.SILK_TOUCH.getMaxLevel() == 2, "Silk Touch should be 2");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodSpecialClampMax(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "mending");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 300);
        try {
            helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 255, "Mending 300 should clamp to 255");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodSpecialAllRemaining(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "aqua_affinity",
            "binding_curse", "channeling", "multishot", "vanishing_curse");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("aqua_affinity", 3);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        ETTestHelper.setEnchantCap("channeling", 3);
        ETTestHelper.setEnchantCap("multishot", 5);
        ETTestHelper.setEnchantCap("vanishing_curse", 3);
        try {
            helper.assertTrue(Enchantments.AQUA_AFFINITY.getMaxLevel() == 3, "Aqua Affinity should be 3");
            helper.assertTrue(Enchantments.BINDING_CURSE.getMaxLevel() == 5, "Binding Curse should be 5");
            helper.assertTrue(Enchantments.CHANNELING.getMaxLevel() == 3, "Channeling should be 3");
            helper.assertTrue(Enchantments.MULTISHOT.getMaxLevel() == 5, "Multishot should be 5");
            helper.assertTrue(Enchantments.VANISHING_CURSE.getMaxLevel() == 3, "Vanishing Curse should be 3");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // capmod: negative values other than -1

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodNegativeClamp(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", -5);
        try {
            // any negative value should pass through to vanilla (getCapmodLevel returns
            // vanilla for cap < 0)
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "Sharpness -5 should passthrough to vanilla 5 (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // capmod: /enchant command regression (issue #46)

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodEnchantCommandRegression(TestContext helper) {
        // issue #46: /enchant command should respect capmod-raised levels
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        player.getInventory().setStack(0, sword);
        player.getInventory().selectedSlot = 0;
        MinecraftServer server = helper.getWorld().getServer();
        try {
            // dispatch via the command manager (same path as a player
            server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(4),
                "enchant @s minecraft:sharpness 10");
            int level = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, player.getInventory().getStack(0));
            helper.assertTrue(level == 10, "Sword should have Sharpness 10 after /enchant command (got " + level + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodValueOf1(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 1);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 1,
                "Sharpness 1 should give max level 1 (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodDisableEnchantmentsHeadCancelWins(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness", "smite",
            "disable_enchantments_enabled", "disable_enchantments");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ETTestHelper.setEnchantCap("smite", 7);
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 0,
                "Disabled Sharpness should report max level 0 even with capmod cap 10 (HEAD cancel wins, got "
                    + Enchantments.SHARPNESS.getMaxLevel() + ")");
            helper.assertTrue(Enchantments.SMITE.getMaxLevel() == 7,
                "Non-disabled Smite should still honor capmod cap 7 (got " + Enchantments.SMITE.getMaxLevel() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodOverflowNonNumericCap(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        try {
            ETTestHelper.setConfigValue("sharpness", "9999999999");
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "Overflowing cap '9999999999' should fall back to vanilla 5 (got "
                    + Enchantments.SHARPNESS.getMaxLevel() + ")");

            ETTestHelper.setConfigValue("sharpness", "abc");
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "Non-numeric cap 'abc' should fall back to vanilla 5 (got " + Enchantments.SHARPNESS.getMaxLevel()
                    + ")");

            ETTestHelper.setConfigValue("sharpness", "2147483647");
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 255,
                "Cap Integer.MAX_VALUE should clamp to 255 (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodEnchantTableAboveVanilla(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        try {
            int raised = maxOfferedLevel(helper, Enchantments.SHARPNESS, new ItemStack(Items.BOOK));
            helper.assertTrue(raised > 5,
                "With capmod cap 10, an enchant-table Sharpness entry above vanilla 5 should be offered (got max "
                    + raised + ")");

            // cap back to passthrough: the same gameplay must never exceed
            ETTestHelper.setEnchantCap("sharpness", -1);
            int vanilla = maxOfferedLevel(helper, Enchantments.SHARPNESS, new ItemStack(Items.BOOK));
            helper.assertTrue(vanilla > 0 && vanilla <= 5,
                "With capmod off, Sharpness enchant-table entries must stay within vanilla 1..5 (got max " + vanilla
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodModDisabledPassthrough(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness", "mod_enabled");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 10,
                "Baseline: capmod cap 10 should apply while mod_enabled (got " + Enchantments.SHARPNESS.getMaxLevel()
                    + ")");

            ETTestHelper.setConfigValue("mod_enabled", "false");
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 5,
                "With mod_enabled=false the capmod inject must passthrough to vanilla 5 (got "
                    + Enchantments.SHARPNESS.getMaxLevel() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void capmodEnchantCommandRejectsAboveRaisedCap(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        player.getInventory().setStack(0, sword);
        player.getInventory().selectedSlot = 0;
        MinecraftServer server = helper.getWorld().getServer();
        try {
            // one above the raised cap: rejected by the command's level<=getMaxLevel()
            // check
            server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(4),
                "enchant @s minecraft:sharpness 11");
            int overCap = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, player.getInventory().getStack(0));
            helper.assertTrue(overCap == 0,
                "Sharpness 11 exceeds the raised cap of 10 and must be rejected (got " + overCap + ")");

            // exactly at the raised cap: applies on the same path
            server.getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(4),
                "enchant @s minecraft:sharpness 10");
            int atCap = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, player.getInventory().getStack(0));
            helper.assertTrue(atCap == 10,
                "Sharpness 10 is exactly at the raised cap and must apply (got " + atCap + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    private static int maxOfferedLevel(TestContext helper, Enchantment target, ItemStack stack) {
        FeatureSet features = helper.getWorld().getEnabledFeatures();
        int best = 0;
        for (int power = 1; power <= 300; power++) {
            for (EnchantmentLevelEntry entry : EnchantmentHelper.getPossibleEntries(features, power, stack, false)) {
                if (entry.enchantment == target && entry.level > best)
                    best = entry.level;
            }
        }
        return best;
    }
}
