package com.adibarra.enchanttweaker.test;

import java.util.Map;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

import com.adibarra.enchanttweaker.ETMixinPlugin;

public class CapmodGameTest {

    // capmod master switch

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(false);
        ETTestHelper.setEnchantCap("sharpness", 10);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 5,
                "Sharpness should be vanilla 5 when capmod disabled (got " + maxLevel(helper, Enchantments.SHARPNESS)
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodVanillaPassthrough(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled");
        ETTestHelper.setCapmod(true);
        try {
            // all caps are -1 in the GameTest config verify vanilla
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 5,
                "Sharpness -1 should passthrough to vanilla 5");
            helper.assertTrue(maxLevel(helper, Enchantments.UNBREAKING) == 3,
                "Unbreaking -1 should passthrough to vanilla 3");
            helper.assertTrue(maxLevel(helper, Enchantments.LOOTING) == 3,
                "Looting -1 should passthrough to vanilla 3");
            helper.assertTrue(maxLevel(helper, Enchantments.PROTECTION) == 4,
                "Protection -1 should passthrough to vanilla 4");
            helper.assertTrue(maxLevel(helper, Enchantments.MENDING) == 1,
                "Mending -1 should passthrough to vanilla 1");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodPassthroughDoesNotCacheVanillaLevel(GameTestHelper helper) {
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
        helper.succeed();
    }

    // capmod DamageEnchant

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodDamageCustom(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness", "smite",
            "bane_of_arthropods");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ETTestHelper.setEnchantCap("smite", 7);
        ETTestHelper.setEnchantCap("bane_of_arthropods", 8);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 10, "Sharpness should be 10");
            helper.assertTrue(maxLevel(helper, Enchantments.SMITE) == 7, "Smite should be 7");
            helper.assertTrue(maxLevel(helper, Enchantments.BANE_OF_ARTHROPODS) == 8, "Bane of Arthropods should be 8");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodDamageClampMax(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 300);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 255, "Sharpness 300 should clamp to 255");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodDamageZero(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 0);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 0, "Sharpness should be 0");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // capmod GenericEnchant

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodGenericCustom(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "unbreaking", "efficiency");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("unbreaking", 10);
        ETTestHelper.setEnchantCap("efficiency", 8);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.UNBREAKING) == 10, "Unbreaking should be 10");
            helper.assertTrue(maxLevel(helper, Enchantments.EFFICIENCY) == 8, "Efficiency should be 8");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodGenericClampMax(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "unbreaking");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("unbreaking", 300);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.UNBREAKING) == 255, "Unbreaking 300 should clamp to 255");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodGenericAllRemaining(GameTestHelper helper) {
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
            helper.assertTrue(maxLevel(helper, Enchantments.DEPTH_STRIDER) == 5, "Depth Strider should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.FIRE_ASPECT) == 4, "Fire Aspect should be 4");
            helper.assertTrue(maxLevel(helper, Enchantments.FROST_WALKER) == 4, "Frost Walker should be 4");
            helper.assertTrue(maxLevel(helper, Enchantments.IMPALING) == 8, "Impaling should be 8");
            helper.assertTrue(maxLevel(helper, Enchantments.KNOCKBACK) == 5, "Knockback should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.LOYALTY) == 6, "Loyalty should be 6");
            helper.assertTrue(maxLevel(helper, Enchantments.LURE) == 6, "Lure should be 6");
            helper.assertTrue(maxLevel(helper, Enchantments.PIERCING) == 8, "Piercing should be 8");
            helper.assertTrue(maxLevel(helper, Enchantments.POWER) == 8, "Power should be 8");
            helper.assertTrue(maxLevel(helper, Enchantments.PUNCH) == 5, "Punch should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.QUICK_CHARGE) == 5, "Quick Charge should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.RESPIRATION) == 5, "Respiration should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.RIPTIDE) == 6, "Riptide should be 6");
            helper.assertTrue(maxLevel(helper, Enchantments.SOUL_SPEED) == 5, "Soul Speed should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.SWEEPING_EDGE) == 5, "Sweeping Edge should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.SWIFT_SNEAK) == 5, "Swift Sneak should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.THORNS) == 5, "Thorns should be 5");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // capmod LuckEnchant

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodLuckCustom(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "looting", "fortune",
            "luck_of_the_sea");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("looting", 10);
        ETTestHelper.setEnchantCap("fortune", 8);
        ETTestHelper.setEnchantCap("luck_of_the_sea", 6);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.LOOTING) == 10, "Looting should be 10");
            helper.assertTrue(maxLevel(helper, Enchantments.FORTUNE) == 8, "Fortune should be 8");
            helper.assertTrue(maxLevel(helper, Enchantments.LUCK_OF_THE_SEA) == 6, "Luck of the Sea should be 6");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // capmod ProtectionEnchant

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodProtectionCustom(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "protection",
            "fire_protection");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("protection", 10);
        ETTestHelper.setEnchantCap("fire_protection", 8);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.PROTECTION) == 10, "Protection should be 10");
            helper.assertTrue(maxLevel(helper, Enchantments.FIRE_PROTECTION) == 8, "Fire Protection should be 8");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodProtectionAllTypes(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "protection",
            "fire_protection", "feather_falling", "blast_protection", "projectile_protection");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("protection", 6);
        ETTestHelper.setEnchantCap("fire_protection", 6);
        ETTestHelper.setEnchantCap("feather_falling", 6);
        ETTestHelper.setEnchantCap("blast_protection", 6);
        ETTestHelper.setEnchantCap("projectile_protection", 6);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.PROTECTION) == 6, "Protection should be 6");
            helper.assertTrue(maxLevel(helper, Enchantments.FIRE_PROTECTION) == 6, "Fire Protection should be 6");
            helper.assertTrue(maxLevel(helper, Enchantments.FEATHER_FALLING) == 6, "Feather Falling should be 6");
            helper.assertTrue(maxLevel(helper, Enchantments.BLAST_PROTECTION) == 6, "Blast Protection should be 6");
            helper.assertTrue(maxLevel(helper, Enchantments.PROJECTILE_PROTECTION) == 6,
                "Projectile Protection should be 6");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // capmod SpecialEnchant

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodSpecialCustom(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "mending", "infinity",
            "silk_touch");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 5);
        ETTestHelper.setEnchantCap("infinity", 3);
        ETTestHelper.setEnchantCap("silk_touch", 2);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.MENDING) == 5, "Mending should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.INFINITY) == 3, "Infinity should be 3");
            helper.assertTrue(maxLevel(helper, Enchantments.SILK_TOUCH) == 2, "Silk Touch should be 2");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodSpecialClampMax(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "mending");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 300);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.MENDING) == 255, "Mending 300 should clamp to 255");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodSpecialAllRemaining(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "aqua_affinity",
            "binding_curse", "channeling", "multishot", "vanishing_curse");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("aqua_affinity", 3);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        ETTestHelper.setEnchantCap("channeling", 3);
        ETTestHelper.setEnchantCap("multishot", 5);
        ETTestHelper.setEnchantCap("vanishing_curse", 3);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.AQUA_AFFINITY) == 3, "Aqua Affinity should be 3");
            helper.assertTrue(maxLevel(helper, Enchantments.BINDING_CURSE) == 5, "Binding Curse should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.CHANNELING) == 3, "Channeling should be 3");
            helper.assertTrue(maxLevel(helper, Enchantments.MULTISHOT) == 5, "Multishot should be 5");
            helper.assertTrue(maxLevel(helper, Enchantments.VANISHING_CURSE) == 3, "Vanishing Curse should be 3");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // capmod: negative values other than -1

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodNegativeClamp(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", -5);
        try {
            // any negative value should pass through to vanilla (getCapmodLevel returns
            // vanilla for cap < 0)
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 5,
                "Sharpness -5 should passthrough to vanilla 5 (got " + maxLevel(helper, Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // capmod: /enchant command regression (issue #46)

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodEnchantCommandRegression(GameTestHelper helper) {
        // issue #46: /enchant command should respect capmod-raised levels
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        player.getInventory().setItem(0, sword);
        player.getInventory().setSelectedSlot(0);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            // dispatch via the command manager (same path as a player
            server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS),
                "enchant @s minecraft:sharpness 10");
            int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.SHARPNESS),
                player.getInventory().getItem(0));
            helper.assertTrue(level == 10, "Sword should have Sharpness 10 after /enchant command (got " + level + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodValueOf1(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 1);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 1,
                "Sharpness 1 should give max level 1 (got " + maxLevel(helper, Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodDisableEnchantmentsHeadCancelWins(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness", "smite",
            "disable_enchantments_enabled", "disable_enchantments");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ETTestHelper.setEnchantCap("smite", 7);
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 0,
                "Disabled Sharpness should report max level 0 even with capmod cap 10 (HEAD cancel wins, got "
                    + maxLevel(helper, Enchantments.SHARPNESS) + ")");
            helper.assertTrue(maxLevel(helper, Enchantments.SMITE) == 7,
                "Non-disabled Smite should still honor capmod cap 7 (got " + maxLevel(helper, Enchantments.SMITE)
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodOverflowNonNumericCap(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        try {
            ETTestHelper.setConfigValue("sharpness", "9999999999");
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 5,
                "Overflowing cap '9999999999' should fall back to vanilla 5 (got "
                    + maxLevel(helper, Enchantments.SHARPNESS) + ")");

            ETTestHelper.setConfigValue("sharpness", "abc");
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 5,
                "Non-numeric cap 'abc' should fall back to vanilla 5 (got " + maxLevel(helper, Enchantments.SHARPNESS)
                    + ")");

            ETTestHelper.setConfigValue("sharpness", "2147483647");
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 255,
                "Cap Integer.MAX_VALUE should clamp to 255 (got " + maxLevel(helper, Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodEnchantTableAboveVanilla(GameTestHelper helper) {
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
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodModDisabledPassthrough(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness", "mod_enabled");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        try {
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 10,
                "Baseline: capmod cap 10 should apply while mod_enabled (got "
                    + maxLevel(helper, Enchantments.SHARPNESS) + ")");

            ETTestHelper.setConfigValue("mod_enabled", "false");
            helper.assertTrue(maxLevel(helper, Enchantments.SHARPNESS) == 5,
                "With mod_enabled=false the capmod inject must passthrough to vanilla 5 (got "
                    + maxLevel(helper, Enchantments.SHARPNESS) + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        structure = "fabric-gametest-api-v1:empty")
    public void capmodEnchantCommandRejectsAboveRaisedCap(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("capmod_enabled", "sharpness");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("sharpness", 10);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        player.getInventory().setItem(0, sword);
        player.getInventory().setSelectedSlot(0);
        MinecraftServer server = helper.getLevel().getServer();
        try {
            // one above the raised cap: rejected by the command's level<=getMaxLevel()
            // check
            server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS),
                "enchant @s minecraft:sharpness 11");
            int overCap = EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.SHARPNESS),
                player.getInventory().getItem(0));
            helper.assertTrue(overCap == 0,
                "Sharpness 11 exceeds the raised cap of 10 and must be rejected (got " + overCap + ")");

            // exactly at the raised cap: applies on the same path
            server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS),
                "enchant @s minecraft:sharpness 10");
            int atCap = EnchantmentHelper.getItemEnchantmentLevel(enchantment(helper, Enchantments.SHARPNESS),
                player.getInventory().getItem(0));
            helper.assertTrue(atCap == 10,
                "Sharpness 10 is exactly at the raised cap and must apply (got " + atCap + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    private static int maxOfferedLevel(GameTestHelper helper, ResourceKey<Enchantment> target, ItemStack stack) {
        int best = 0;
        for (int power = 1; power <= 300; power++) {
            // streams are single-use, so build a fresh candidate stream per power
            Stream<Holder<Enchantment>> candidates = helper.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).listElements().map(candidate -> (Holder<Enchantment>) candidate);
            for (EnchantmentInstance entry : EnchantmentHelper.getAvailableEnchantmentResults(power, stack,
                candidates)) {
                if (entry.enchantment().is(target) && entry.level() > best)
                    best = entry.level();
            }
        }
        return best;
    }

    private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static int maxLevel(GameTestHelper helper, ResourceKey<Enchantment> key) {
        return enchantment(helper, key).value().getMaxLevel();
    }
}
