package com.adibarra.enchanttweaker.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;

public class TweakGameTest implements FabricGameTest {

    // experience scaling

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 35;
            try {
                // base=7, step=2 -> 7 + 2*35 = 77
                int xp = player.getXpNeededForNextLevel();
                helper.assertTrue(xp == 77, "XP scaling at level 35 should be 77 (got " + xp + ")");
            } finally {
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling");
        try {
            ETTestHelper.setFeature("xp_scaling", false);
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 35;
            // vanilla tier 3: 9*level - 158 = 9*35 - 158
            int xp = player.getXpNeededForNextLevel();
            helper.assertTrue(xp == 157, "Vanilla XP at level 35 should be 157 (got " + xp + ")");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingCustomValues(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "10");
            ETTestHelper.setConfigValue("xp_scaling_step", "5");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 20;
            try {
                // 10 + 5*20 = 110
                int xp = player.getXpNeededForNextLevel();
                helper.assertTrue(xp == 110,
                    "XP scaling with base=10, step=5 at level 20 should be 110 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingOverflowClamped(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            // long multiplication prevents integer overflow
            ETTestHelper.setConfigValue("xp_scaling_step", "2000000000");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 2;
            try {
                // large scaling results clamp to integer maximum
                int xp = player.getXpNeededForNextLevel();
                helper.assertTrue(xp == Integer.MAX_VALUE,
                    "XP scaling should clamp int overflow to exactly Integer.MAX_VALUE (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingNegativeStepClamped(GameTestHelper helper) {
        // negative scaling clamps experience requirements to one

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "-10");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 5;
            try {
                // 7 + (-10)*5 = -43 -> clamp to 1
                int xp = player.getXpNeededForNextLevel();
                helper.assertTrue(xp == 1, "Negative step should clamp getNextLevelExperience to 1 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // disableEnchantments

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsRandomSelection(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                helper.assertFalse(Enchantments.SHARPNESS.isDiscoverable(),
                    "Sharpness should not be available for random selection when disabled");
                helper.assertTrue(Enchantments.SMITE.isDiscoverable(),
                    "Smite should still be available for random selection");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsFullIdentifier(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "minecraft:sharpness");
            try {
                helper.assertFalse(Enchantments.SHARPNESS.isDiscoverable(),
                    "Full enchantment identifiers should match exactly");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsExplicitLootList(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "swift_sneak");
            try {
                LootParams parameters = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
                LootContext context = new LootContext.Builder(parameters).withOptionalRandomSeed(1)
                    .create(Optional.empty());
                LootItemFunction function = EnchantRandomlyFunction.randomEnchantment()
                    .withEnchantment(Enchantments.SWIFT_SNEAK).build();
                ItemStack result = function.apply(new ItemStack(Items.BOOK), context);

                helper.assertTrue(result.is(Items.BOOK),
                    "An explicit loot list containing only a disabled enchantment must leave the book unchanged");
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SWIFT_SNEAK, result) == 0,
                    "Explicit enchant_randomly lists must not bypass disable_enchantments");

                LootItemFunction allowedFunction = EnchantRandomlyFunction.randomEnchantment()
                    .withEnchantment(Enchantments.SMITE).build();
                ItemStack allowed = allowedFunction.apply(new ItemStack(Items.DIAMOND_SWORD), context);
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, allowed) > 0,
                    "A non-disabled explicit enchantment should still be applied");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsBookOffer(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                helper.assertFalse(Enchantments.SHARPNESS.isTradeable(),
                    "Sharpness should not be available for book offers when disabled");
                helper.assertTrue(Enchantments.SMITE.isTradeable(), "Smite should still be available for book offers");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsMaxLevel(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 0,
                    "Sharpness max level should be 0 when disabled (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
                helper.assertTrue(Enchantments.SMITE.getMaxLevel() > 0, "Smite max level should be unchanged");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsEmpty(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "");
            try {
                helper.assertTrue(Enchantments.SHARPNESS.isDiscoverable(),
                    "Sharpness should be available when disable list is empty");
                helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() > 0,
                    "Sharpness max level should be normal when disable list is empty");
            } finally {
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // godArmor

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godArmorEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_armor");
        try {
            ETTestHelper.setFeature("god_armor", true);
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
                "Protection should accept Blast Protection when enabled");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godArmorDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_armor");
        try {
            ETTestHelper.setFeature("god_armor", false);
            try {
                helper.assertTrue(!ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
                    "Protection should not accept Blast Protection when disabled");
            } finally {
                ETTestHelper.setFeature("god_armor", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godArmorAllTypes(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_armor");
        try {
            ETTestHelper.setFeature("god_armor", true);
            // different types: all allowed
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION),
                "Protection accepts Fire Protection");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.FEATHER_FALLING),
                "Protection accepts Feather Falling");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
                "Protection accepts Blast Protection");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.PROJECTILE_PROTECTION),
                "Protection accepts Projectile Protection");
            // same type: still blocked
            helper.assertTrue(!ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.PROTECTION),
                "Protection should not accept itself");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // godWeapons

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SMITE),
                "Sharpness should accept Smite when enabled");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.BANE_OF_ARTHROPODS),
                "Sharpness should accept Bane when enabled");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", false);
            try {
                helper.assertTrue(!ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SMITE),
                    "Sharpness should not accept Smite when disabled");
            } finally {
                ETTestHelper.setFeature("god_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // infiniteMending

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void infiniteMendingEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", true);
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.MENDING),
                "Infinity should accept Mending when enabled");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void infiniteMendingDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", false);
            try {
                helper.assertTrue(!ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.MENDING),
                    "Infinity should not accept Mending when disabled");
            } finally {
                ETTestHelper.setFeature("infinite_mending", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // noMendingUnbreaking

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noMendingUnbreakingEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", true);
            try {
                helper.assertFalse(ETTestHelper.canAccept(Enchantments.MENDING, Enchantments.UNBREAKING),
                    "Mending should not accept Unbreaking when no_mending_unbreaking enabled");
            } finally {
                ETTestHelper.setFeature("no_mending_unbreaking", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noMendingUnbreakingDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", false);
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.MENDING, Enchantments.UNBREAKING),
                "Mending should accept Unbreaking when no_mending_unbreaking disabled");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // multishotPiercing

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void multishotPiercingEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("multishot_piercing");
        try {
            ETTestHelper.setFeature("multishot_piercing", true);
            try {
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
                    "Multishot should accept Piercing when enabled");
            } finally {
                ETTestHelper.setFeature("multishot_piercing", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void multishotPiercingReverse(GameTestHelper helper) {
        // both enchantments must accept each other
        // the anvil checks both directions

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("multishot_piercing");
        try {
            ETTestHelper.setFeature("multishot_piercing", true);
            try {
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.PIERCING, Enchantments.MULTISHOT),
                    "Piercing should accept Multishot when enabled");
            } finally {
                ETTestHelper.setFeature("multishot_piercing", false);
            }
            ETTestHelper.setFeature("multishot_piercing", false);
            try {
                helper.assertFalse(ETTestHelper.canAccept(Enchantments.PIERCING, Enchantments.MULTISHOT),
                    "Piercing should NOT accept Multishot when disabled");
            } finally {
                ETTestHelper.setFeature("multishot_piercing", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void multishotPiercingDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("multishot_piercing");
        try {
            ETTestHelper.setFeature("multishot_piercing", false);
            try {
                helper.assertTrue(!ETTestHelper.canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
                    "Multishot should not accept Piercing when disabled");
            } finally {
                ETTestHelper.setFeature("multishot_piercing", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // tridentWeapons

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tridentWeaponsEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("trident_weapons");
        try {
            ETTestHelper.setFeature("trident_weapons", true);
            ItemStack trident = new ItemStack(Items.TRIDENT);
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, trident),
                "Fire Aspect should be acceptable on trident");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.KNOCKBACK, trident),
                "Knockback should be acceptable on trident");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, trident),
                "Looting should be acceptable on trident");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tridentWeaponsDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("trident_weapons");
        try {
            ETTestHelper.setFeature("trident_weapons", false);
            ItemStack trident = new ItemStack(Items.TRIDENT);
            try {
                helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, trident),
                    "Fire Aspect should not be acceptable on trident when disabled");
                helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.KNOCKBACK, trident),
                    "Knockback should not be acceptable on trident when disabled");
                helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.LOOTING, trident),
                    "Looting should not be acceptable on trident when disabled");
            } finally {
                ETTestHelper.setFeature("trident_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // axeWeapons

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axeWeaponsEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axe_weapons");
        try {
            ETTestHelper.setFeature("axe_weapons", true);
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, axe),
                "Fire Aspect should be acceptable on axe");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.KNOCKBACK, axe),
                "Knockback should be acceptable on axe");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, axe),
                "Looting should be acceptable on axe");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axeWeaponsDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axe_weapons");
        try {
            ETTestHelper.setFeature("axe_weapons", false);
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            try {
                helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, axe),
                    "Fire Aspect should not be acceptable on axe when disabled");
                helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.KNOCKBACK, axe),
                    "Knockback should not be acceptable on axe when disabled");
                helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.LOOTING, axe),
                    "Looting should not be acceptable on axe when disabled");
            } finally {
                ETTestHelper.setFeature("axe_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowLooting

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowLootingEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_looting");
        try {
            ETTestHelper.setFeature("bow_looting", true);
            ItemStack bow = new ItemStack(Items.BOW);
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, bow),
                "Looting should be acceptable on bow");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, crossbow),
                "Looting should be acceptable on crossbow");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowLootingDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_looting");
        try {
            ETTestHelper.setFeature("bow_looting", false);
            ItemStack bow = new ItemStack(Items.BOW);
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            try {
                helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.LOOTING, bow),
                    "Looting should not be acceptable on bow when disabled");
                helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.LOOTING, crossbow),
                    "Looting should not be acceptable on crossbow when disabled");
            } finally {
                ETTestHelper.setFeature("bow_looting", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // axesNotTools

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axesNotToolsEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axes_not_tools");
        try {
            ETTestHelper.setFeature("axes_not_tools", true);
            ServerLevel world = helper.getLevel();
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            Zombie target = EntityType.ZOMBIE.create(world);
            Zombie attacker = EntityType.ZOMBIE.create(world);
            axe.getItem().hurtEnemy(axe, target, attacker);
            helper.assertTrue(axe.getDamageValue() == 1,
                "Axe should take 1 durability with axes_not_tools enabled (got " + axe.getDamageValue() + ")");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axesNotToolsDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axes_not_tools");
        try {
            ETTestHelper.setFeature("axes_not_tools", false);
            ServerLevel world = helper.getLevel();
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            Zombie target = EntityType.ZOMBIE.create(world);
            Zombie attacker = EntityType.ZOMBIE.create(world);
            try {
                axe.getItem().hurtEnemy(axe, target, attacker);
                helper.assertTrue(axe.getDamageValue() == 2,
                    "Axe should take 2 durability with axes_not_tools disabled (got " + axe.getDamageValue() + ")");
            } finally {
                ETTestHelper.setFeature("axes_not_tools", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // thorns normally damages armor after triggering backlash
    // level seven always triggers backlash

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noThornsBacklashEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_thorns_backlash");
        try {
            ETTestHelper.setFeature("no_thorns_backlash", true);
            Zombie user = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
            // equip thorns seven armor to guarantee backlash
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.enchant(Enchantments.THORNS, 7);
            user.setItemSlot(EquipmentSlot.CHEST, chest);
            try {
                float attackerHealth = attacker.getHealth();
                int armorDamage = 0;
                for (int attempt = 0; attempt < 100 && armorDamage == 0; attempt++) {
                    EnchantmentHelper.doPostHurtEffects(user, attacker);
                    armorDamage = user.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
                }
                helper.assertTrue(attacker.getHealth() == attackerHealth,
                    "NoThornsBacklash must not damage the attacker");
                helper.assertTrue(armorDamage > 0, "NoThornsBacklash must preserve Thorns armor durability damage");
            } finally {
                ETTestHelper.setFeature("no_thorns_backlash", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noThornsBacklashDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_thorns_backlash");
        try {
            ETTestHelper.setFeature("no_thorns_backlash", false);
            Zombie user = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.enchant(Enchantments.THORNS, 7);
            user.setItemSlot(EquipmentSlot.CHEST, chest);
            try {
                int armorDamage = 0;
                for (int attempt = 0; attempt < 100 && armorDamage == 0; attempt++) {
                    EnchantmentHelper.doPostHurtEffects(user, attacker);
                    armorDamage = user.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
                }
                helper.assertTrue(armorDamage > 0, "Vanilla Thorns should damage armor when backlash is enabled");
            } finally {
                ETTestHelper.setFeature("no_thorns_backlash", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // loyalVoidTridents

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.enchant(Enchantments.LOYALTY, 1);
            Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new ThrownTrident(world, owner, tridentStack);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            trident.setPosRaw(abs.getX(), world.getMinBuildHeight() - 5.0, abs.getZ());
            world.addFreshEntity(trident);
            trident.tick();
            try {
                Field dealtDamage = ThrownTrident.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertTrue((boolean) dealtDamage.get(trident),
                    "dealtDamage should be true below world bottom when loyal_void_tridents enabled");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", false);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.enchant(Enchantments.LOYALTY, 1);
            Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new ThrownTrident(world, owner, tridentStack);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            trident.setPosRaw(abs.getX(), world.getMinBuildHeight() - 5.0, abs.getZ());
            world.addFreshEntity(trident);
            try {
                trident.tick();
                Field dealtDamage = ThrownTrident.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertFalse((boolean) dealtDamage.get(trident),
                    "dealtDamage should stay false when loyal_void_tridents disabled");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // soul speed damage uses a deterministic seeded random

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_soul_speed_backlash");
        try {
            ETTestHelper.setFeature("no_soul_speed_backlash", true);
            helper.setBlock(new BlockPos(0, 1, 0), Blocks.SOUL_SAND.defaultBlockState());
            Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            ItemStack boots = new ItemStack(Items.GOLDEN_BOOTS);
            boots.enchant(Enchantments.SOUL_SPEED, 1);
            zombie.setItemSlot(EquipmentSlot.FEET, boots);
            zombie.tick(); // land on soul sand so getLandingBlockState() / isOnSoulSpeedBlock() pass
            try {
                Method removeBoost = LivingEntity.class.getDeclaredMethod("removeSoulSpeedBoost");
                removeBoost.setAccessible(true);
                Method addBoost = LivingEntity.class.getDeclaredMethod("addSoulSpeedBoostIfNeeded");
                addBoost.setAccessible(true);
                // the mixin prevents soul speed boot durability loss
                removeBoost.invoke(zombie);
                addBoost.invoke(zombie);
                helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.FEET).getDamageValue() == 0,
                    "Soul Speed boots should not take damage when no_soul_speed_backlash enabled (got "
                        + zombie.getItemBySlot(EquipmentSlot.FEET).getDamageValue() + ")");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                ETTestHelper.setFeature("no_soul_speed_backlash", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashDisabled(GameTestHelper helper) {
        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_soul_speed_backlash");
        try {
            ETTestHelper.setFeature("no_soul_speed_backlash", false);
            ServerLevel world = helper.getLevel();
            helper.setBlock(new BlockPos(0, 1, 0), Blocks.SOUL_SAND.defaultBlockState());
            Zombie zombie = new Zombie(world) {
                private final RandomSource soulSpeedRandom = RandomSource.create(5120L);

                @Override
                public RandomSource getRandom() {
                    return soulSpeedRandom;
                }
            };
            BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
            zombie.setPosRaw(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            world.addFreshEntity(zombie);
            ItemStack boots = new ItemStack(Items.GOLDEN_BOOTS);
            boots.enchant(Enchantments.SOUL_SPEED, 1);
            zombie.setItemSlot(EquipmentSlot.FEET, boots);

            Method removeBoost = LivingEntity.class.getDeclaredMethod("removeSoulSpeedBoost");
            removeBoost.setAccessible(true);
            Method addBoost = LivingEntity.class.getDeclaredMethod("addSoulSpeedBoostIfNeeded");
            addBoost.setAccessible(true);
            removeBoost.invoke(zombie);
            addBoost.invoke(zombie);
            helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.FEET).getDamageValue() == 1,
                "Yarn Random seed 5120 must trigger one Soul Speed durability loss");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
        helper.succeed();
    }

    // better mending repairs the entire inventory

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void betterMendingEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("better_mending");
        try {
            ETTestHelper.setFeature("better_mending", true);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingSword.enchant(Enchantments.MENDING, 1);
            mendingSword.setDamageValue(100);
            // slot 20 is an ordinary inventory slot
            player.getInventory().setItem(20, mendingSword);

            BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
            ExperienceOrb orb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), 50);
            ETTestHelper.repairPlayerGears(orb, player, 50);

            helper.assertTrue(player.getInventory().getItem(20).getDamageValue() < 100,
                "BetterMending should repair inventory item (not just equipped items)");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void betterMendingDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("better_mending");
        try {
            ETTestHelper.setFeature("better_mending", false);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingSword.enchant(Enchantments.MENDING, 1);
            mendingSword.setDamageValue(100);
            player.getInventory().setItem(20, mendingSword);

            BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
            ExperienceOrb orb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), 50);
            try {
                ETTestHelper.repairPlayerGears(orb, player, 50);
                helper.assertTrue(player.getInventory().getItem(20).getDamageValue() == 100,
                    "Vanilla mending should NOT repair non-equipped inventory item when better_mending disabled");
            } finally {
                ETTestHelper.setFeature("better_mending", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // betterMending: mainhand priority

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void betterMendingMainHandPriority(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("better_mending");
        try {
            ETTestHelper.setFeature("better_mending", true);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            // damaged mending sword in mainhand (slot 0)
            ItemStack mainhandSword = new ItemStack(Items.DIAMOND_SWORD);
            mainhandSword.enchant(Enchantments.MENDING, 1);
            mainhandSword.setDamageValue(100);
            player.getInventory().setItem(0, mainhandSword);
            // damaged mending sword in inventory (slot 20)
            ItemStack inventorySword = new ItemStack(Items.DIAMOND_SWORD);
            inventorySword.enchant(Enchantments.MENDING, 1);
            inventorySword.setDamageValue(100);
            player.getInventory().setItem(20, inventorySword);

            BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
            // small amount of XP so it only repairs one item
            ExperienceOrb orb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), 5);
            ETTestHelper.repairPlayerGears(orb, player, 5);

            helper.assertTrue(player.getInventory().getItem(0).getDamageValue() < 100,
                "BetterMending should prioritize mainhand over inventory");
            helper.assertTrue(player.getInventory().getItem(20).getDamageValue() == 100,
                "BetterMending should NOT repair inventory item when mainhand needs repair");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowInfinityFix

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowInfinityFixEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.INFINITY, 1);
            player.getInventory().setItem(0, bow);
            // vanilla blocks infinity bows without arrows
            InteractionResultHolder<ItemStack> result = bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
            try {
                helper.assertTrue(result.getResult().consumesAction(),
                    "BowInfinityFix should allow Infinity bow to fire without arrows");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowInfinityFixDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", false);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.INFINITY, 1);
            player.getInventory().setItem(0, bow);
            try {
                InteractionResultHolder<ItemStack> result = bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult().consumesAction() == false,
                    "Vanilla bow with Infinity should NOT fire without arrows when bow_infinity_fix disabled");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowInfinityFixNoInfinity(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            // bow without Infinity: mixin should not activate
            ItemStack bow = new ItemStack(Items.BOW);
            player.getInventory().setItem(0, bow);
            try {
                InteractionResultHolder<ItemStack> result = bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
                helper.assertTrue(result.getResult().consumesAction() == false,
                    "Bow without Infinity should NOT fire without arrows even with bow_infinity_fix enabled");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowInfinityFixSuppressedByMoreInfinity(GameTestHelper helper) {
        // more infinity suppresses bow infinity fix

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ETTestHelper.setFeature("more_infinity", true);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.INFINITY, 1);
            player.getInventory().setItem(0, bow);
            // more infinity takes precedence without arrows
            InteractionResultHolder<ItemStack> result = bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
            try {
                helper.assertFalse(result.getResult().consumesAction(),
                    "BowInfinityFix should be suppressed when more_infinity is enabled");
            } finally {
                ETTestHelper.setFeature("more_infinity", false);
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowInfinityFixFiresArrow(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ETTestHelper.setFeature("more_infinity", false);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            // move the mock player into the loaded test structure
            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.INFINITY, 1);
            player.getInventory().setItem(0, bow);
            // no arrows anywhere in the inventory
            AABB search = player.getBoundingBox().inflate(64.0);
            int arrowsBefore = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            try {
                // draw then fully release the bow
                bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
                bow.getItem().releaseUsing(bow, world, player, 0);

                int arrowsAfter = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
                helper.assertTrue(arrowsAfter == arrowsBefore + 1,
                    "Infinity bow with no arrows should spawn exactly one arrow on release (before=" + arrowsBefore
                        + ", after=" + arrowsAfter + ")");
                helper.assertTrue(player.getInventory().countItem(Items.ARROW) == 0,
                    "Firing an Infinity bow with no arrows must not add or consume inventory arrows (got "
                        + player.getInventory().countItem(Items.ARROW) + ")");
                helper.assertTrue(bow.getDamageValue() == 1,
                    "Bow should take 1 durability after firing (confirms shootAll ran; got " + bow.getDamageValue()
                        + ")");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowInfinityFixDisabledDoesNotFire(GameTestHelper helper) {
        // vanilla cannot fire an arrowless infinity bow

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", false);
            ETTestHelper.setFeature("more_infinity", false);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.INFINITY, 1);
            player.getInventory().setItem(0, bow);
            AABB search = player.getBoundingBox().inflate(64.0);
            int arrowsBefore = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            try {
                bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
                bow.getItem().releaseUsing(bow, world, player, 0);
                int arrowsAfter = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
                helper.assertTrue(arrowsAfter == arrowsBefore,
                    "Infinity bow with feature disabled must NOT fire without arrows (before=" + arrowsBefore
                        + ", after=" + arrowsAfter + ")");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // godWeapons: all combinations

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsAllCombinations(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            // test all 3 pairs: Smite<->Bane, Sharpness<->Bane, Sharpness<->Smite
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS),
                "Smite should accept Bane of Arthropods");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.BANE_OF_ARTHROPODS, Enchantments.SMITE),
                "Bane of Arthropods should accept Smite");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.BANE_OF_ARTHROPODS, Enchantments.SHARPNESS),
                "Bane of Arthropods should accept Sharpness");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.SMITE, Enchantments.SHARPNESS),
                "Smite should accept Sharpness");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsSelfReject(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            helper.assertFalse(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SHARPNESS),
                "Sharpness should NOT accept itself");
            helper.assertFalse(ETTestHelper.canAccept(Enchantments.SMITE, Enchantments.SMITE),
                "Smite should NOT accept itself");
            helper.assertFalse(ETTestHelper.canAccept(Enchantments.BANE_OF_ARTHROPODS, Enchantments.BANE_OF_ARTHROPODS),
                "Bane should NOT accept itself");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // loyalVoidTridents: edge cases

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsNoLoyalty(GameTestHelper helper) {
        // trident without Loyalty should not be rescued from void

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            // no Loyalty enchantment
            Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new ThrownTrident(world, owner, tridentStack);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            trident.setPosRaw(abs.getX(), world.getMinBuildHeight() - 5.0, abs.getZ());
            world.addFreshEntity(trident);
            trident.tick();
            try {
                Field dealtDamage = ThrownTrident.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertFalse((boolean) dealtDamage.get(trident),
                    "Trident WITHOUT Loyalty should NOT have dealtDamage set in void");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsOwnerlessFallsIntoVoid(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.enchant(Enchantments.LOYALTY, 1);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            double startY = world.getMinBuildHeight() - 5.0;
            trident = new ThrownTrident(world, abs.getX(), startY, abs.getZ(), tridentStack);
            trident.setDeltaMovement(0, -2.0, 0);
            world.addFreshEntity(trident);
            try {
                trident.tick();
                helper.assertTrue(trident.getY() < startY, "Ownerless trident should keep falling");
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsStopsDescending(GameTestHelper helper) {
        // rescued tridents stop falling and return upward

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.enchant(Enchantments.LOYALTY, 3);
            Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new ThrownTrident(world, owner, tridentStack);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            double startY = world.getMinBuildHeight() - 5.0;
            trident.setPosRaw(abs.getX(), startY, abs.getZ());
            trident.setDeltaMovement(0, -2.0, 0);
            world.addFreshEntity(trident);
            // tick until loyalty return begins
            for (int i = 0; i < 5; i++)
                trident.tick();
            helper.assertTrue(trident.getY() > startY,
                "Loyal trident should start returning upward after being rescued from void (Y=" + trident.getY()
                    + ", started at " + startY + ")");
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // axesNotTools: multiple hits

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axesNotToolsMultipleHits(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axes_not_tools");
        try {
            ETTestHelper.setFeature("axes_not_tools", true);
            ServerLevel world = helper.getLevel();
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            Zombie target = EntityType.ZOMBIE.create(world);
            Zombie attacker = EntityType.ZOMBIE.create(world);
            // 3 hits should cause 3 durability (not 6)
            axe.getItem().hurtEnemy(axe, target, attacker);
            axe.getItem().hurtEnemy(axe, target, attacker);
            axe.getItem().hurtEnemy(axe, target, attacker);
            helper.assertTrue(axe.getDamageValue() == 3,
                "3 axe hits with axes_not_tools should cause 3 durability damage (got " + axe.getDamageValue() + ")");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    private static final float PB_REDUCED = 3.6f; // getInflictedDamage(10, 16)

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void protectionBypassMagicEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                    "Magic damage should bypass protection entirely via a real hit (health delta " + delta
                        + ", expected 10.0)");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void protectionBypassWitherEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "wither");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().wither(), 10.0f);
                helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                    "Wither damage should bypass protection entirely via a real hit (health delta " + delta
                        + ", expected 10.0)");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void protectionBypassDisabledStillReduces(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", false);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(delta - PB_REDUCED) < 0.05f,
                    "Magic damage with bypass disabled should be reduced by protection (health delta " + delta
                        + ", expected ~" + PB_REDUCED + ")");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void protectionBypassGenericNotAffected(GameTestHelper helper) {
        // generic damage is not a configured protection bypass
        // protection still reduces its damage

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic,wither,dragon_breath");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().generic(), 10.0f);
                helper.assertTrue(Math.abs(delta - PB_REDUCED) < 0.05f,
                    "Generic damage should still be reduced by protection (health delta " + delta + ", expected ~"
                        + PB_REDUCED + ")");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void protectionBypassModdedColonBranch(GameTestHelper helper) {
        // known namespaced ids bypass protection
        // unknown namespaced ids safely do not match

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            try {
                ETTestHelper.setConfigValue("protection_bypass_types", "minecraft:magic");
                float bypassed = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(bypassed - 10.0f) < 0.05f,
                    "'minecraft:magic' should resolve and bypass protection (health delta " + bypassed
                        + ", expected 10.0)");

                ETTestHelper.setConfigValue("protection_bypass_types", "nonexistent:foo");
                float reduced = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(reduced - PB_REDUCED) < 0.05f,
                    "Unknown namespaced id should not crash and should not bypass (health delta " + reduced
                        + ", expected ~" + PB_REDUCED + ")");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void protectionBypassDragonBreath(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic,dragon_breath");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().dragonBreath(),
                    10.0f);
                helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                    "Dragon breath should bypass protection when listed (health delta " + delta + ", expected 10.0)");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void protectionBypassWhitespaceTokens(GameTestHelper helper) {
        // malformed list tokens do not discard valid entries

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic , bad id! , wither ,");
            try {
                float magic = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(magic - 10.0f) < 0.05f,
                    "Whitespace-padded 'magic' should bypass (health delta " + magic + ", expected 10.0)");
                float wither = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().wither(), 10.0f);
                helper.assertTrue(Math.abs(wither - 10.0f) < 0.05f,
                    "Whitespace-padded 'wither' should bypass (health delta " + wither + ", expected 10.0)");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void protectionBypassPrecedesMoreProtection(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base",
            "protection_bypass_enabled", "protection_bypass_types");
        try {
            ETTestHelper.setFeature("more_protection", true);
            ETTestHelper.setConfigValue("more_protection_base", "0.96");
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic");
            try {
                float bypassed = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(bypassed - 10.0f) < 0.05f,
                    "configured damage must bypass MoreProtection as well as vanilla Protection");

                float protectedDamage = protectedZombieDamageDelta(helper, helper.getLevel().damageSources().generic(),
                    10.0f);
                float expected = (float) (10.0 * Math.pow(0.96, 16));
                helper.assertTrue(Math.abs(protectedDamage - expected) < 0.05f,
                    "unlisted damage should retain MoreProtection scaling (got " + protectedDamage + ", expected ~"
                        + expected + ")");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
                ETTestHelper.setFeature("more_protection", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // villagerTradeLimits

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsMaxUsesEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_max_uses");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
                ItemStack book = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.increaseUses();
                offer.increaseUses();
                offer.increaseUses();
                helper.assertTrue(offer.isOutOfStock(),
                    "Enchantment trade should be disabled after 3 uses (max_uses=3)");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsMaxUsesDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_max_uses");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", false);
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
                ItemStack book = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.increaseUses();
                offer.increaseUses();
                offer.increaseUses();
                helper.assertFalse(offer.isOutOfStock(),
                    "Enchantment trade should NOT be disabled when feature is off (uses=3, vanilla maxUses=12)");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsPerEnchantOverride(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_max_uses", "trade_sharpness");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
                "trade_sharpness", "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setConfigValue("trade_sharpness", "2");
                ItemStack book = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.increaseUses();
                offer.increaseUses();
                helper.assertTrue(offer.isOutOfStock(),
                    "Sharpness trade should be disabled after 2 uses (trade_sharpness=2)");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsDisabledEnchantment(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits", "trade_mending");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("trade_mending",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("trade_mending", "0");
                ItemStack book = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.MENDING, 1));
                MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
                helper.assertTrue(offer.isOutOfStock(),
                    "Mending trade should be disabled immediately when trade_mending=0");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsRestockEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_restock");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_restock",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_restock", "true");
                ItemStack book = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.increaseUses();
                offer.increaseUses();
                offer.resetUses();
                helper.assertTrue(offer.getUses() == 0,
                    "Enchantment trade should restock when enchant_trade_restock=true (uses=" + offer.getUses() + ")");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsRestockDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_restock");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_restock",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_restock", "false");
                ItemStack book = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.increaseUses();
                offer.increaseUses();
                offer.resetUses();
                helper.assertTrue(offer.getUses() == 2,
                    "Enchantment trade should NOT restock when enchant_trade_restock=false (uses=" + offer.getUses()
                        + ")");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsNoRestockList(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_restock", "enchant_trade_no_restock");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_no_restock",
                "enchant_trade_restock", "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_restock", "true");
                ETTestHelper.setConfigValue("enchant_trade_no_restock", "sharpness");
                ItemStack sharpBook = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                MerchantOffer sharpOffer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), sharpBook, 12, 1, 0.2f);
                sharpOffer.increaseUses();
                sharpOffer.resetUses();
                ItemStack mendingBook = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.MENDING, 1));
                MerchantOffer mendingOffer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), mendingBook, 12, 1,
                    0.2f);
                mendingOffer.increaseUses();
                mendingOffer.resetUses();
                helper.assertTrue(sharpOffer.getUses() == 1,
                    "Sharpness trade in no_restock list should NOT restock (uses=" + sharpOffer.getUses() + ")");
                helper.assertTrue(mendingOffer.getUses() == 0,
                    "Mending trade NOT in no_restock list should restock (uses=" + mendingOffer.getUses() + ")");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsNonEnchantedUnaffected(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_max_uses", "enchant_trade_restock");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
                "enchant_trade_restock", "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "1");
                ETTestHelper.setConfigValue("enchant_trade_restock", "false");
                // non-enchanted trade: emeralds for wheat
                MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.WHEAT, 6),
                    12, 1, 0.2f);
                offer.increaseUses();
                offer.increaseUses();
                offer.increaseUses();
                offer.resetUses();
                helper.assertTrue(offer.getUses() == 0,
                    "Non-enchanted trade should still restock normally (uses=" + offer.getUses() + ")");
                helper.assertFalse(offer.isOutOfStock(),
                    "Non-enchanted trade should not be affected by enchant_trade_max_uses");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    // bowLooting: guard negatives

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowLootingIdentityGuardNegative(GameTestHelper helper) {
        // only looting becomes acceptable on bows and crossbows
        // sharpness remains rejected

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_looting");
        try {
            ETTestHelper.setFeature("bow_looting", true);
            ItemStack bow = new ItemStack(Items.BOW);
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            try {
                helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.SHARPNESS, bow),
                    "Sharpness must NOT become acceptable on a bow (identity guard)");
                helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.SHARPNESS, crossbow),
                    "Sharpness must NOT become acceptable on a crossbow (identity guard)");
            } finally {
                ETTestHelper.setFeature("bow_looting", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowLootingNonBowGuard(GameTestHelper helper) {
        // looting remains rejected for non-bow items

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_looting");
        try {
            ETTestHelper.setFeature("bow_looting", true);
            try {
                helper.assertFalse(
                    ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.DIAMOND_PICKAXE)),
                    "Looting must NOT become acceptable on a pickaxe via bow_looting");
            } finally {
                ETTestHelper.setFeature("bow_looting", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // tridentWeapons: DamageEnchantment branch + guards

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tridentWeaponsDamageEnchantBranch(GameTestHelper helper) {
        // damage enchantments become acceptable on tridents
        ItemStack trident = new ItemStack(Items.TRIDENT);

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("trident_weapons");
        try {
            ETTestHelper.setFeature("trident_weapons", true);
            try {
                helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.SHARPNESS, trident),
                    "Sharpness acceptable on trident when enabled");
                helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.SMITE, trident),
                    "Smite acceptable on trident when enabled");
                helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.BANE_OF_ARTHROPODS, trident),
                    "Bane acceptable on trident when enabled");
            } finally {
                ETTestHelper.setFeature("trident_weapons", false);
            }
            ETTestHelper.setFeature("trident_weapons", false);
            try {
                helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.SHARPNESS, trident),
                    "Sharpness NOT acceptable on trident when disabled");
                helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.SMITE, trident),
                    "Smite NOT acceptable on trident when disabled");
                helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.BANE_OF_ARTHROPODS, trident),
                    "Bane NOT acceptable on trident when disabled");
            } finally {
                ETTestHelper.setFeature("trident_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tridentWeaponsNonTridentGuardAndFallthrough(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("trident_weapons");
        try {
            ETTestHelper.setFeature("trident_weapons", true);
            try {
                // non-tridents retain vanilla fire-aspect rejection
                helper.assertFalse(
                    ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, new ItemStack(Items.DIAMOND_PICKAXE)),
                    "Fire Aspect must NOT be acceptable on a pickaxe via trident_weapons");
                // sweeping edge falls through to vanilla rejection
                helper.assertFalse(
                    ETTestHelper.isAcceptableItem(Enchantments.SWEEPING_EDGE, new ItemStack(Items.TRIDENT)),
                    "Sweeping Edge must fall through to vanilla and be rejected on a trident");
            } finally {
                ETTestHelper.setFeature("trident_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // weapon tweaks: cross-tweak Looting acceptability

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void weaponTweaksCrossLootingAcceptability(GameTestHelper helper) {
        // looting works on weapon targets but not pickaxes

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axe_weapons", "bow_looting",
            "trident_weapons");
        try {
            ETTestHelper.setFeature("axe_weapons", true);
            ETTestHelper.setFeature("bow_looting", true);
            ETTestHelper.setFeature("trident_weapons", true);
            try {
                helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.DIAMOND_AXE)),
                    "Looting acceptable on axe");
                helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.BOW)),
                    "Looting acceptable on bow");
                helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.TRIDENT)),
                    "Looting acceptable on trident");
                helper.assertFalse(
                    ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.DIAMOND_PICKAXE)),
                    "Looting NOT acceptable on pickaxe");
            } finally {
                ETTestHelper.setFeature("axe_weapons", false);
                ETTestHelper.setFeature("bow_looting", false);
                ETTestHelper.setFeature("trident_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // axeWeapons: guard negatives + enchant-table presence

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axeWeaponsGuardNegatives(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axe_weapons");
        try {
            ETTestHelper.setFeature("axe_weapons", true);
            try {
                // non-axe: Looting stays rejected on a pickaxe
                helper.assertFalse(
                    ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.DIAMOND_PICKAXE)),
                    "Looting must NOT be acceptable on a pickaxe via axe_weapons");
                // axe weapons never grant protection
                helper.assertFalse(
                    ETTestHelper.isAcceptableItem(Enchantments.PROTECTION, new ItemStack(Items.DIAMOND_AXE)),
                    "Protection must NOT be granted on an axe by axe_weapons");
            } finally {
                ETTestHelper.setFeature("axe_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void axeWeaponsEnchantTablePresence(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axe_weapons");
        try {
            ETTestHelper.setFeature("axe_weapons", true);
            try {
                List<EnchantmentInstance> axeEntries = EnchantmentHelper.getAvailableEnchantmentResults(
                    helper.getLevel().enabledFeatures(), 30, new ItemStack(Items.DIAMOND_AXE), false);
                helper.assertTrue(containsEnchant(axeEntries, Enchantments.LOOTING),
                    "Looting must be offered on an axe from the enchant table when axe_weapons is on");
                helper.assertTrue(containsEnchant(axeEntries, Enchantments.FIRE_ASPECT),
                    "Fire Aspect must be offered on an axe from the enchant table when axe_weapons is on");
                helper.assertTrue(containsEnchant(axeEntries, Enchantments.KNOCKBACK),
                    "Knockback must be offered on an axe from the enchant table when axe_weapons is on");

                // pickaxes never receive axe weapon enchantments
                List<EnchantmentInstance> pickEntries = EnchantmentHelper.getAvailableEnchantmentResults(
                    helper.getLevel().enabledFeatures(), 30, new ItemStack(Items.DIAMOND_PICKAXE), false);
                helper.assertFalse(containsEnchant(pickEntries, Enchantments.LOOTING),
                    "Looting must NOT be offered on a pickaxe via axe_weapons");
                helper.assertFalse(containsEnchant(pickEntries, Enchantments.FIRE_ASPECT),
                    "Fire Aspect must NOT be offered on a pickaxe via axe_weapons");
                helper.assertFalse(containsEnchant(pickEntries, Enchantments.KNOCKBACK),
                    "Knockback must NOT be offered on a pickaxe via axe_weapons");
            } finally {
                ETTestHelper.setFeature("axe_weapons", false);
            }

            // disabled axe weapons leave vanilla enchantment availability
            List<EnchantmentInstance> offEntries = EnchantmentHelper.getAvailableEnchantmentResults(
                helper.getLevel().enabledFeatures(), 30, new ItemStack(Items.DIAMOND_AXE), false);
            helper.assertFalse(containsEnchant(offEntries, Enchantments.LOOTING),
                "Looting must be absent from axe enchant-table entries when axe_weapons is off");
            helper.assertFalse(containsEnchant(offEntries, Enchantments.FIRE_ASPECT),
                "Fire Aspect must be absent from axe enchant-table entries when axe_weapons is off");
            helper.assertFalse(containsEnchant(offEntries, Enchantments.KNOCKBACK),
                "Knockback must be absent from axe enchant-table entries when axe_weapons is off");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // godWeapons: fall-through, Impaling, removeConflicts

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsNonDamageEnchantFallthrough(GameTestHelper helper) {
        // non-damage enchantments retain vanilla compatibility

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            try {
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.UNBREAKING),
                    "Sharpness should still accept Unbreaking (non-DamageEnchantment fall-through)");
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.FIRE_ASPECT),
                    "Sharpness should still accept Fire Aspect (non-DamageEnchantment fall-through)");
            } finally {
                ETTestHelper.setFeature("god_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsImpaling(GameTestHelper helper) {
        // impaling combines with other damage enchantments
        // it never combines with itself

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            try {
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.IMPALING),
                    "Sharpness accepts Impaling");
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.IMPALING, Enchantments.SMITE),
                    "Impaling accepts Smite");
                helper.assertFalse(ETTestHelper.canAccept(Enchantments.IMPALING, Enchantments.IMPALING),
                    "Impaling must NOT accept itself");
            } finally {
                ETTestHelper.setFeature("god_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void godWeaponsRemoveConflicts(GameTestHelper helper) {
        EnchantmentInstance picked = new EnchantmentInstance(Enchantments.SMITE, 1);

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            try {
                List<EnchantmentInstance> list = new ArrayList<>();
                list.add(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                EnchantmentHelper.filterCompatibleEnchantments(list, picked);
                helper.assertTrue(list.size() == 1,
                    "Sharpness should survive removeConflicts against Smite when god_weapons on (size " + list.size()
                        + ")");
            } finally {
                ETTestHelper.setFeature("god_weapons", false);
            }

            ETTestHelper.setFeature("god_weapons", false);
            try {
                List<EnchantmentInstance> list = new ArrayList<>();
                list.add(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                EnchantmentHelper.filterCompatibleEnchantments(list, picked);
                helper.assertTrue(list.isEmpty(),
                    "Sharpness should be removed by removeConflicts against Smite when god_weapons off (size "
                        + list.size() + ")");
            } finally {
                ETTestHelper.setFeature("god_weapons", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // infiniteMending: real anvil merge

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void infiniteMendingAnvilMerge(GameTestHelper helper) {
        // infinity and mending must combine through a real anvil

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", true);
            try {
                ItemStack bow = new ItemStack(Items.BOW);
                bow.enchant(Enchantments.INFINITY, 1);
                ItemStack mendingBook = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.MENDING, 1));
                ItemStack out = anvilCombine(helper, bow, mendingBook);
                helper.assertFalse(out.isEmpty(), "Infinity and Mending anvil merge must produce an output");
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY, out) == 1,
                    "Merged bow should keep Infinity 1 (got "
                        + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY, out) + ")");
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, out) == 1,
                    "Merged bow should gain Mending 1 (got "
                        + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, out) + ")");
            } finally {
                ETTestHelper.setFeature("infinite_mending", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void infiniteMendingAnvilMergeDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", false);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.INFINITY, 1);
            ItemStack mendingBook = EnchantedBookItem
                .createForEnchantment(new EnchantmentInstance(Enchantments.MENDING, 1));
            ItemStack out = anvilCombine(helper, bow, mendingBook);
            helper.assertTrue(out.isEmpty(), "Disabled Infinity and Mending anvil operation should produce no output");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void infiniteMendingNoOverApplication(GameTestHelper helper) {
        // only infinity and mending compatibility changes

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", true);
            try {
                helper.assertFalse(ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.INFINITY),
                    "Infinity must NOT accept itself");
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.POWER),
                    "Infinity should still accept Power");
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.PUNCH),
                    "Infinity should still accept Punch");
            } finally {
                ETTestHelper.setFeature("infinite_mending", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // noMendingUnbreaking: real anvil combine

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noMendingUnbreakingAnvilBlocks(GameTestHelper helper) {
        // unbreaking cannot transfer to a mending pickaxe

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", true);
            try {
                ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
                pick.enchant(Enchantments.MENDING, 1);
                ItemStack unbreakingBook = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.UNBREAKING, 3));
                ItemStack out = anvilCombine(helper, pick, unbreakingBook);
                helper.assertTrue(out.isEmpty(),
                    "Blocked Mending and Unbreaking anvil operation should produce no output");
            } finally {
                ETTestHelper.setFeature("no_mending_unbreaking", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noMendingUnbreakingAnvilAllowsWhenDisabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", false);
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.enchant(Enchantments.MENDING, 1);
            ItemStack unbreakingBook = EnchantedBookItem
                .createForEnchantment(new EnchantmentInstance(Enchantments.UNBREAKING, 3));
            ItemStack out = anvilCombine(helper, pick, unbreakingBook);
            helper.assertFalse(out.isEmpty(), "Vanilla Mending and Unbreaking anvil merge must produce an output");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, out) == 1,
                "Vanilla anvil should keep Mending 1 (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, out) + ")");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, out) == 3,
                "Vanilla anvil should add Unbreaking 3 (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, out) + ")");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void noMendingUnbreakingFallthroughUnrelated(GameTestHelper helper) {
        // unrelated enchantments still merge with mending items

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", true);
            try {
                ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
                pick.enchant(Enchantments.MENDING, 1);
                ItemStack efficiencyBook = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.EFFICIENCY, 3));
                ItemStack out = anvilCombine(helper, pick, efficiencyBook);
                helper.assertFalse(out.isEmpty(), "Mending and Efficiency anvil merge must produce an output");
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, out) == 1,
                    "Mending should be preserved (got "
                        + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, out) + ")");
                helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY, out) == 3,
                    "Efficiency should merge (unrelated pair) (got "
                        + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.EFFICIENCY, out) + ")");
            } finally {
                ETTestHelper.setFeature("no_mending_unbreaking", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // disableEnchantments: generation paths

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsGetPossibleEntries(GameTestHelper helper) {
        // disabled enchantments are absent from enchanting table entries
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                List<EnchantmentInstance> entries = EnchantmentHelper
                    .getAvailableEnchantmentResults(helper.getLevel().enabledFeatures(), 30, sword, true);
                helper.assertFalse(containsEnchant(entries, Enchantments.SHARPNESS),
                    "Disabled Sharpness must be absent from enchant-table entries");
                helper.assertTrue(containsEnchant(entries, Enchantments.SMITE),
                    "Non-disabled Smite should remain in enchant-table entries");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            // feature-off mirror: Sharpness returns
            List<EnchantmentInstance> mirror = EnchantmentHelper
                .getAvailableEnchantmentResults(helper.getLevel().enabledFeatures(), 30, sword, true);
            helper.assertTrue(containsEnchant(mirror, Enchantments.SHARPNESS),
                "Sharpness should be present again once the feature is off");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsMultiEntry(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness,mending");
            try {
                helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 0, "Sharpness disabled (maxLevel 0)");
                helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 0, "Mending disabled (maxLevel 0)");
                helper.assertTrue(Enchantments.SMITE.getMaxLevel() > 0, "Smite unaffected");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsBookFactory(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("disable_enchantments_enabled",
            "disable_enchantments");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
                "disable_enchantments_enabled");
            try {
                ETTestHelper.setFeature("disable_enchantments_enabled", true);
                ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
                Entity anchor = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
                RandomSource random = RandomSource.create(0L);
                Class<?> cls = Class.forName("net.minecraft.village.TradeOffers$EnchantBookFactory");
                Constructor<?> ctor = cls.getDeclaredConstructor(int.class);
                ctor.setAccessible(true);
                Object factory = ctor.newInstance(1);
                Method create = cls.getDeclaredMethod("create", Entity.class, RandomSource.class);
                create.setAccessible(true);
                boolean sawSharpness = false;
                boolean sawNonSharpness = false;
                for (int i = 0; i < 500; i++) {
                    MerchantOffer offer = (MerchantOffer) create.invoke(factory, anchor, random);
                    var enchants = EnchantmentHelper.getEnchantmentsForCrafting(offer.getResult());
                    if (enchants.getLevel(Enchantments.SHARPNESS) > 0) {
                        sawSharpness = true;
                    } else if (!enchants.isEmpty()) {
                        sawNonSharpness = true;
                    }
                }
                helper.assertFalse(sawSharpness,
                    "A fresh EnchantBookFactory built after disabling Sharpness must never offer a Sharpness book");
                helper.assertTrue(sawNonSharpness,
                    "The deterministic factory fixture should still generate non-disabled enchanted-book offers");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsWhitespaceTrimming(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", " sharpness , , mending ,");
            try {
                helper.assertFalse(Enchantments.SHARPNESS.isDiscoverable(), "Trimmed 'sharpness' disabled");
                helper.assertFalse(Enchantments.MENDING.isDiscoverable(), "Trimmed 'mending' disabled");
                helper.assertTrue(Enchantments.SMITE.isDiscoverable(), "Smite still available");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsNamespacedAndUnknown(GameTestHelper helper) {
        // full identifiers match exactly and unknown entries do nothing

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            try {
                ETTestHelper.setConfigValue("disable_enchantments", "minecraft:sharpness");
                helper.assertFalse(Enchantments.SHARPNESS.isDiscoverable(),
                    "minecraft:sharpness should disable the exact namespaced enchantment");

                ETTestHelper.setConfigValue("disable_enchantments", "notarealenchant");
                helper.assertTrue(Enchantments.SHARPNESS.isDiscoverable(), "An unknown id should be a harmless no-op");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsFeatureGateOff(GameTestHelper helper) {
        // disabling remains inactive when the master switch is off

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                helper.assertTrue(Enchantments.SHARPNESS.isDiscoverable(),
                    "Sharpness must stay available when the master switch is off");
                helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() > 0,
                    "Sharpness max level must be normal when the master switch is off");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // xpScaling: real leveling + boundaries

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingRealLeveling(GameTestHelper helper) {
        // real experience gains use the configured level requirements

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 0;
            player.experienceProgress = 0.0f;
            try {
                player.giveExperiencePoints(7);
                helper.assertTrue(player.experienceLevel == 1,
                    "Adding 7 XP at L0 (need 7) should reach L1 (got " + player.experienceLevel + ")");
                player.giveExperiencePoints(9);
                helper.assertTrue(player.experienceLevel == 2,
                    "Adding 9 XP at L1 (need 9) should reach L2 (got " + player.experienceLevel + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingZeroBoundary(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "0");
            ETTestHelper.setConfigValue("xp_scaling_step", "0");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 10;
            try {
                int xp = player.getXpNeededForNextLevel();
                helper.assertTrue(xp == 1,
                    "base=0,step=0 must clamp getNextLevelExperience to the lower bound 1 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingLevelZero(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 0;
            try {
                int xp = player.getXpNeededForNextLevel();
                helper.assertTrue(xp == 7, "base=7,step=2 at L0 should be 7 (got " + xp + ")");
            } finally {
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingDefaultFallback(GameTestHelper helper) {
        // invalid bases fall back to the default value

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "abc");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 10;
            try {
                int xp = player.getXpNeededForNextLevel();
                helper.assertTrue(xp == 27, "Non-numeric base should fall back to 7 -> 7 + 2*10 = 27 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingBeyondIntRange(GameTestHelper helper) {
        // out-of-range steps fall back to the default value

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "9999999999");
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 2;
            try {
                int xp = player.getXpNeededForNextLevel();
                helper.assertTrue(xp == 11,
                    "Out-of-int-range step should fall back to default 2 -> 7 + 2*2 = 11 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void xpScalingOffGateMidTier(GameTestHelper helper) {
        // disabled scaling uses the vanilla mid-tier formula

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling");
        try {
            ETTestHelper.setFeature("xp_scaling", false);
            ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
            player.experienceLevel = 20;
            int xp = player.getXpNeededForNextLevel();
            helper.assertTrue(xp == 62, "Vanilla XP at L20 should be 62 (got " + xp + ")");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // loyalVoidTridents: real void positions

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsAboveBottomNotRescued(GameTestHelper helper) {
        // tridents above the world bottom are not rescued

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.enchant(Enchantments.LOYALTY, 1);
            Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new ThrownTrident(world, owner, tridentStack);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            double startY = world.getMinBuildHeight() + 10.0;
            trident.setPosRaw(abs.getX(), startY, abs.getZ());
            trident.setDeltaMovement(0, -1.0, 0);
            world.addFreshEntity(trident);
            trident.tick();
            try {
                Field dealtDamage = ThrownTrident.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertFalse((boolean) dealtDamage.get(trident),
                    "A trident above the world bottom should NOT be rescued (dealtDamage stays false)");
                helper.assertTrue(trident.getY() < startY, "A trident above the world bottom should keep falling (Y="
                    + trident.getY() + ", started " + startY + ")");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsDisabledLostToVoid(GameTestHelper helper) {
        // feature OFF: a loyalty trident dropped below the void-kill plane
        // is discarded by vanilla on the next tick

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", false);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.enchant(Enchantments.LOYALTY, 1);
            Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new ThrownTrident(world, owner, tridentStack);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            trident.setPosRaw(abs.getX(), world.getMinBuildHeight() - 65.0, abs.getZ());
            world.addFreshEntity(trident);
            trident.tick();
            try {
                helper.assertTrue(trident.isRemoved(),
                    "A loyalty trident below the void-kill plane should be discarded when the feature is off");
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsEnabledSurvivesVoid(GameTestHelper helper) {
        // loyalty tridents at the bottom survive and return

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.enchant(Enchantments.LOYALTY, 3);
            Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new ThrownTrident(world, owner, tridentStack);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            double startY = world.getMinBuildHeight() - 1.0;
            trident.setPosRaw(abs.getX(), startY, abs.getZ());
            trident.setDeltaMovement(0, -2.0, 0);
            world.addFreshEntity(trident);
            try {
                for (int i = 0; i < 10; i++)
                    trident.tick();
                helper.assertFalse(trident.isRemoved(), "A rescued loyalty trident must NOT be discarded to the void");
                helper.assertTrue(trident.getY() > startY,
                    "A rescued loyalty trident should climb back toward its owner (Y=" + trident.getY() + ", started "
                        + startY + ")");
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void loyalVoidTridentsExactBoundary(GameTestHelper helper) {
        // the world-bottom boundary triggers rescue

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        ThrownTrident trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerLevel world = helper.getLevel();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.enchant(Enchantments.LOYALTY, 1);
            Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new ThrownTrident(world, owner, tridentStack);
            BlockPos abs = helper.absolutePos(new BlockPos(0, 0, 0));
            trident.setPosRaw(abs.getX(), world.getMinBuildHeight(), abs.getZ());
            world.addFreshEntity(trident);
            trident.tick();
            try {
                Field dealtDamage = ThrownTrident.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertTrue((boolean) dealtDamage.get(trident),
                    "Y == bottomY should trigger the rescue (dealtDamage true)");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.succeed();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowInfinityFix: short-draw bail

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowInfinityFixShortDrawBail(GameTestHelper helper) {
        // insufficient bow draw prevents all firing logic

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ETTestHelper.setFeature("more_infinity", false);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.INFINITY, 1);
            player.getInventory().setItem(0, bow);
            AABB search = player.getBoundingBox().inflate(64.0);
            int arrowsBefore = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            try {
                bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
                // immediate release keeps pull progress below 0.1
                bow.getItem().releaseUsing(bow, world, player, bow.getUseDuration());
                int arrowsAfter = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
                helper.assertTrue(arrowsAfter == arrowsBefore, "A barely-drawn Infinity bow must NOT fire (before="
                    + arrowsBefore + ", after=" + arrowsAfter + ")");
                helper.assertTrue(bow.getDamageValue() == 0,
                    "A barely-drawn bow should take no durability damage (got " + bow.getDamageValue() + ")");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // villagerTradeLimits: most-restrictive-wins + more real paths

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsMostRestrictiveWins(GameTestHelper helper) {
        // multi-enchant trades use the lowest configured limit

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses", "trade_looting",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setConfigValue("trade_sharpness", "5");
            ETTestHelper.setConfigValue("trade_looting", "2");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 1);
            sword.enchant(Enchantments.LOOTING, 1);
            MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), sword, 12, 1, 0.2f);
            try {
                offer.increaseUses();
                helper.assertFalse(offer.isOutOfStock(), "After 1 use the trade should still be enabled (min limit 2)");
                offer.increaseUses();
                helper.assertTrue(offer.isOutOfStock(),
                    "After 2 uses the most-restrictive limit (min(5,2)=2) should disable the trade");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setConfigValue("trade_looting", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsGlobalFallbackParticipatesInMinimum(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses", "trade_looting",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
            ETTestHelper.setConfigValue("trade_sharpness", "50");
            ETTestHelper.setConfigValue("trade_looting", "-1");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 1);
            sword.enchant(Enchantments.LOOTING, 1);
            MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), sword, 12, 1, 0.2f);
            try {
                offer.increaseUses();
                offer.increaseUses();
                helper.assertFalse(offer.isOutOfStock(), "Global fallback should allow two uses");
                offer.increaseUses();
                helper.assertTrue(offer.isOutOfStock(), "Global fallback should cap the trade at three uses");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setConfigValue("trade_looting", "-1");
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsMostRestrictiveZeroDisables(GameTestHelper helper) {
        // any per-enchant limit of 0 disables the whole multi-enchant trade

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses", "trade_looting",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setConfigValue("trade_sharpness", "5");
            ETTestHelper.setConfigValue("trade_looting", "0");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 1);
            sword.enchant(Enchantments.LOOTING, 1);
            MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), sword, 12, 1, 0.2f);
            try {
                helper.assertTrue(offer.isOutOfStock(),
                    "trade_looting=0 should disable the multi-enchant trade immediately");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setConfigValue("trade_looting", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsGlobalZeroDisablesAll(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "0");
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
            try {
                helper.assertTrue(offer.isOutOfStock(),
                    "enchant_trade_max_uses=0 should disable an enchant trade at 0 uses");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsOverridesVanillaMaxUses(GameTestHelper helper) {
        // per-enchantment limits replace vanilla offer limits

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setConfigValue("trade_sharpness", "50");
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
            try {
                for (int i = 0; i < 49; i++)
                    offer.increaseUses();
                helper.assertFalse(offer.isOutOfStock(),
                    "After 49 uses the trade should remain below the configured maxUses 50");
                offer.increaseUses();
                helper.assertTrue(offer.isOutOfStock(), "The trade should disable on its configured 50th use");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsUnderLimitEnabled(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
            offer.increaseUses();
            offer.increaseUses();
            try {
                helper.assertFalse(offer.isOutOfStock(), "2 uses under a limit of 3 should stay enabled");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsResetUsesFeatureOff(GameTestHelper helper) {
        // disabled limits preserve vanilla restocking behavior

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_restock",
            "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", false);
            ETTestHelper.setConfigValue("enchant_trade_restock", "false");
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
            offer.increaseUses();
            offer.increaseUses();
            offer.resetUses();
            try {
                helper.assertTrue(offer.getUses() == 0,
                    "Vanilla resetUses should run when the feature is off (uses=" + offer.getUses() + ")");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_restock", "true");
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsNoRestockListEdges(GameTestHelper helper) {
        // list parsing trims entries and ignores empty values

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_no_restock",
            "enchant_trade_restock", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_restock", "true");
            ETTestHelper.setConfigValue("enchant_trade_no_restock", " mending , sharpness ,, ");
            MerchantOffer mending = new MerchantOffer(new ItemCost(Items.EMERALD, 10),
                EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.MENDING, 1)), 12, 1, 0.2f);
            MerchantOffer sharpness = new MerchantOffer(new ItemCost(Items.EMERALD, 10),
                EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1)), 12, 1,
                0.2f);
            MerchantOffer looting = new MerchantOffer(new ItemCost(Items.EMERALD, 10),
                EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.LOOTING, 1)), 12, 1, 0.2f);
            mending.increaseUses();
            mending.resetUses();
            sharpness.increaseUses();
            sharpness.resetUses();
            looting.increaseUses();
            looting.resetUses();
            try {
                helper.assertTrue(mending.getUses() == 1,
                    "Mending is in the no-restock list (uses=" + mending.getUses() + ")");
                helper.assertTrue(sharpness.getUses() == 1,
                    "Sharpness is in the no-restock list (uses=" + sharpness.getUses() + ")");
                helper.assertTrue(looting.getUses() == 0,
                    "Looting is not listed and should restock (uses=" + looting.getUses() + ")");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_no_restock", "");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsGarbagePerEnchant(GameTestHelper helper) {
        // invalid per-enchantment limits use the global default

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            try {
                ETTestHelper.setConfigValue("trade_sharpness", "-5");
                MerchantOffer neg = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book.copy(), 12, 1, 0.2f);
                neg.increaseUses();
                neg.increaseUses();
                neg.increaseUses();
                helper.assertFalse(neg.isOutOfStock(),
                    "Negative per-enchant value should fall back to global (-1) -> not disabled");

                ETTestHelper.setConfigValue("trade_sharpness", "abc");
                MerchantOffer garbage = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book.copy(), 12, 1, 0.2f);
                garbage.increaseUses();
                garbage.increaseUses();
                garbage.increaseUses();
                helper.assertFalse(garbage.isOutOfStock(),
                    "Non-numeric per-enchant value should fall back to global (-1) -> not disabled");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsRealPurchasability(GameTestHelper helper) {
        // disabled trades leave the merchant output slot empty

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            try {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "0");
                MerchantContainer disabledInv = buildMerchantInventory(player, book.copy());
                disabledInv.updateSellItem();
                helper.assertTrue(disabledInv.getItem(2).isEmpty(),
                    "A disabled enchant trade should leave the merchant output slot empty");

                ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
                MerchantContainer enabledInv = buildMerchantInventory(player, book.copy());
                enabledInv.updateSellItem();
                helper.assertFalse(enabledInv.getItem(2).isEmpty(),
                    "An under-limit enchant trade should populate the merchant output slot");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // betterMending: recursive leftover-XP must not over-repair

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void betterMendingLeftoverXpNoOverRepair(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("better_mending", "more_mending");
        try {
            ETTestHelper.setFeature("better_mending", true);
            // keep more mending disabled for vanilla repair costs
            ETTestHelper.setFeature("more_mending", false);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            // mainhand repairs first and leaves experience
            ItemStack mainhand = new ItemStack(Items.DIAMOND_SWORD);
            mainhand.enchant(Enchantments.MENDING, 1);
            mainhand.setDamageValue(4);
            player.getInventory().setItem(0, mainhand);
            // inventory repairs use only the remaining experience
            ItemStack inv = new ItemStack(Items.DIAMOND_SWORD);
            inv.enchant(Enchantments.MENDING, 1);
            inv.setDamageValue(100);
            player.getInventory().setItem(20, inv);

            BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
            ExperienceOrb orb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), 5);
            try {
                ETTestHelper.repairPlayerGears(orb, player, 5);
                helper.assertTrue(player.getInventory().getItem(0).getDamageValue() == 0,
                    "Mainhand item should be fully repaired first (got "
                        + player.getInventory().getItem(0).getDamageValue() + ")");
                helper.assertTrue(player.getInventory().getItem(20).getDamageValue() == 94,
                    "Recursive repair must use the leftover XP (3 -> 6 durability, damage 94), NOT the orb's "
                        + "full budget (would over-repair to 90) (got "
                        + player.getInventory().getItem(20).getDamageValue() + ")");
            } finally {
                ETTestHelper.setFeature("better_mending", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowLooting: enchant-table reach

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowLootingEnchantTablePresence(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_looting");
        try {
            ETTestHelper.setFeature("bow_looting", true);
            try {
                List<EnchantmentInstance> bowEntries = EnchantmentHelper.getAvailableEnchantmentResults(
                    helper.getLevel().enabledFeatures(), 30, new ItemStack(Items.BOW), false);
                helper.assertTrue(containsEnchant(bowEntries, Enchantments.LOOTING),
                    "Looting must be offered on a bow from the enchant table when bow_looting is on");
                List<EnchantmentInstance> crossbowEntries = EnchantmentHelper.getAvailableEnchantmentResults(
                    helper.getLevel().enabledFeatures(), 30, new ItemStack(Items.CROSSBOW), false);
                helper.assertTrue(containsEnchant(crossbowEntries, Enchantments.LOOTING),
                    "Looting must be offered on a crossbow from the enchant table when bow_looting is on");
                // pickaxes never gain bow looting enchantment entries
                List<EnchantmentInstance> pickEntries = EnchantmentHelper.getAvailableEnchantmentResults(
                    helper.getLevel().enabledFeatures(), 30, new ItemStack(Items.DIAMOND_PICKAXE), false);
                helper.assertFalse(containsEnchant(pickEntries, Enchantments.LOOTING),
                    "Looting must NOT be offered on a pickaxe via bow_looting");
            } finally {
                ETTestHelper.setFeature("bow_looting", false);
            }
            // disabled bow looting leaves vanilla table entries
            List<EnchantmentInstance> offEntries = EnchantmentHelper.getAvailableEnchantmentResults(
                helper.getLevel().enabledFeatures(), 30, new ItemStack(Items.BOW), false);
            helper.assertFalse(containsEnchant(offEntries, Enchantments.LOOTING),
                "Looting must be absent from bow enchant-table entries when bow_looting is off");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // tridentWeapons: all granted sword enchantments reach the enchanting table

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tridentWeaponsEnchantTablePresence(GameTestHelper helper) {
        ItemStack trident = new ItemStack(Items.TRIDENT);

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("trident_weapons");
        try {
            ETTestHelper.setFeature("trident_weapons", true);
            try {
                List<EnchantmentInstance> entries = EnchantmentHelper
                    .getAvailableEnchantmentResults(helper.getLevel().enabledFeatures(), 30, trident, false);
                helper.assertTrue(containsEnchant(entries, Enchantments.FIRE_ASPECT),
                    "Fire Aspect must appear on a trident from the table");
                helper.assertTrue(containsEnchant(entries, Enchantments.KNOCKBACK),
                    "Knockback must appear on a trident from the table");
                helper.assertTrue(containsEnchant(entries, Enchantments.LOOTING),
                    "Looting must appear on a trident from the table");
                helper.assertTrue(containsEnchant(entries, Enchantments.SHARPNESS),
                    "Sharpness must appear on a trident from the table");
                helper.assertTrue(containsEnchant(entries, Enchantments.SMITE),
                    "Smite must appear on a trident from the table");
                helper.assertTrue(containsEnchant(entries, Enchantments.BANE_OF_ARTHROPODS),
                    "Bane must appear on a trident from the table");
                helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.SHARPNESS, trident),
                    "Sharpness must remain anvil-acceptable on a trident");
                helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, trident),
                    "Looting must remain anvil-acceptable on a trident");
            } finally {
                ETTestHelper.setFeature("trident_weapons", false);
            }
            // disabled trident weapons leave vanilla table entries
            List<EnchantmentInstance> off = EnchantmentHelper
                .getAvailableEnchantmentResults(helper.getLevel().enabledFeatures(), 30, trident, false);
            helper.assertFalse(containsEnchant(off, Enchantments.FIRE_ASPECT),
                "Fire Aspect absent from trident table when trident_weapons off");
            helper.assertFalse(containsEnchant(off, Enchantments.KNOCKBACK),
                "Knockback absent from trident table when trident_weapons off");
            helper.assertFalse(containsEnchant(off, Enchantments.LOOTING),
                "Looting absent from trident table when trident_weapons off");
            helper.assertFalse(containsEnchant(off, Enchantments.SHARPNESS),
                "Sharpness absent from trident table when trident_weapons off");
            helper.assertFalse(containsEnchant(off, Enchantments.SMITE),
                "Smite absent from trident table when trident_weapons off");
            helper.assertFalse(containsEnchant(off, Enchantments.BANE_OF_ARTHROPODS),
                "Bane absent from trident table when trident_weapons off");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // disableEnchantments: anvil application blocked

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void disableEnchantmentsAnvilBlocked(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                ItemStack sharpBook = EnchantedBookItem
                    .createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
                ItemStack out = anvilCombine(helper, sword, sharpBook);
                helper.assertTrue(out.isEmpty(), "Disabled Sharpness anvil operation should produce no output");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            // disabled enchantments apply normally when the feature is off
            ItemStack sword2 = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack sharpBook2 = EnchantedBookItem
                .createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            ItemStack out2 = anvilCombine(helper, sword2, sharpBook2);
            helper.assertFalse(out2.isEmpty(), "Sharpness anvil merge with feature off must produce an output");
            helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out2) == 1,
                "With the feature off, Sharpness 1 should transfer onto the anvil output (got "
                    + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS, out2) + ")");
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowInfinityFix: onStoppedUsing infinity guard (real fire path)

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void bowInfinityFixNoInfinityDoesNotFireArrow(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ETTestHelper.setFeature("more_infinity", false);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack bow = new ItemStack(Items.BOW); // deliberately no Infinity
            player.getInventory().setItem(0, bow);
            AABB search = player.getBoundingBox().inflate(64.0);
            int arrowsBefore = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            try {
                bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
                bow.getItem().releaseUsing(bow, world, player, 0);
                int arrowsAfter = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
                helper.assertTrue(arrowsAfter == arrowsBefore,
                    "A non-Infinity bow with no arrows must NOT fire even with the feature on (before=" + arrowsBefore
                        + ", after=" + arrowsAfter + ")");
                helper.assertTrue(bow.getDamageValue() == 0,
                    "A non-Infinity bow that fired nothing should take no durability (got " + bow.getDamageValue()
                        + ")");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // villagerTradeLimits: master switch gates both facets

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void tradeLimitsMasterSwitchGatesBothFacets(GameTestHelper helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "enchant_trade_no_restock", "enchant_trade_restock", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", false);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "1");
            ETTestHelper.setConfigValue("enchant_trade_restock", "false");
            ETTestHelper.setConfigValue("enchant_trade_no_restock", "sharpness");
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(Enchantments.SHARPNESS, 1));
            MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 10), book, 12, 1, 0.2f);
            try {
                offer.increaseUses();
                offer.increaseUses();
                helper.assertFalse(offer.isOutOfStock(),
                    "Switch OFF: max_uses=1 must be ignored (trade still enabled after 2 uses)");
                offer.resetUses();
                helper.assertTrue(offer.getUses() == 0,
                    "Switch OFF: restock=false and no_restock must be ignored (vanilla restock runs, uses="
                        + offer.getUses() + ")");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setConfigValue("enchant_trade_restock", "true");
                ETTestHelper.setConfigValue("enchant_trade_no_restock", "");
            }
            helper.succeed();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // private helpers use the read-only ettesthelper

    /**
     * combines two stacks through a real anvil
     */
    private static ItemStack anvilCombine(GameTestHelper helper, ItemStack first, ItemStack second) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AnvilMenu handler = new AnvilMenu(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, first, second);
        handler.createResult();
        return handler.getSlot(AnvilMenu.RESULT_SLOT).getItem();
    }

    /**
     * spawns a fully healed zombie with protection four armor
     */
    private static Zombie spawnProtectedZombie(GameTestHelper helper) {
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            ItemStack armor = new ItemStack(switch (slot) {
                case HEAD -> Items.DIAMOND_HELMET;
                case CHEST -> Items.DIAMOND_CHESTPLATE;
                case LEGS -> Items.DIAMOND_LEGGINGS;
                case FEET -> Items.DIAMOND_BOOTS;
                default -> Items.DIAMOND_HELMET;
            });
            armor.enchant(Enchantments.PROTECTION, 4);
            entity.setItemSlot(slot, armor);
        }
        entity.setHealth(entity.getMaxHealth());
        return entity;
    }

    private static float protectedZombieDamageDelta(GameTestHelper helper, DamageSource source, float amount) {
        Zombie zombie = spawnProtectedZombie(helper);
        float before = zombie.getHealth();
        zombie.hurt(source, amount);
        return before - zombie.getHealth();
    }

    private static boolean containsEnchant(List<EnchantmentInstance> entries, Enchantment enchantment) {
        for (EnchantmentInstance entry : entries) {
            if (entry.enchantment == enchantment)
                return true;
        }
        return false;
    }

    /**
     * builds an inventory with an enchanted-book offer the payment occupies slot
     * zero
     */
    private static MerchantContainer buildMerchantInventory(Player player, ItemStack sellBook) {
        ClientSideMerchant merchant = new ClientSideMerchant(player);
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 10), sellBook, 12, 1, 0.2f));
        merchant.overrideOffers(offers);
        MerchantContainer inv = new MerchantContainer(merchant);
        inv.setItem(0, new ItemStack(Items.EMERALD, 64));
        return inv;
    }
}
