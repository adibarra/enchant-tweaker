package com.adibarra.enchanttweaker.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.function.EnchantRandomlyLootFunction;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.MerchantInventory;
import net.minecraft.village.SimpleMerchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.GameMode;

public class TweakGameTest implements FabricGameTest {

    // experience scaling

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 35;
            try {
                // base=7, step=2 -> 7 + 2*35 = 77
                int xp = player.getNextLevelExperience();
                helper.assertTrue(xp == 77, "XP scaling at level 35 should be 77 (got " + xp + ")");
            } finally {
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling");
        try {
            ETTestHelper.setFeature("xp_scaling", false);
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 35;
            // vanilla tier 3: 9*level - 158 = 9*35 - 158
            int xp = player.getNextLevelExperience();
            helper.assertTrue(xp == 157, "Vanilla XP at level 35 should be 157 (got " + xp + ")");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingCustomValues(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "10");
            ETTestHelper.setConfigValue("xp_scaling_step", "5");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 20;
            try {
                // 10 + 5*20 = 110
                int xp = player.getNextLevelExperience();
                helper.assertTrue(xp == 110,
                    "XP scaling with base=10, step=5 at level 20 should be 110 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingOverflowClamped(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            // long multiplication prevents integer overflow
            ETTestHelper.setConfigValue("xp_scaling_step", "2000000000");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 2;
            try {
                // large scaling results clamp to integer maximum
                int xp = player.getNextLevelExperience();
                helper.assertTrue(xp == Integer.MAX_VALUE,
                    "XP scaling should clamp int overflow to exactly Integer.MAX_VALUE (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingNegativeStepClamped(TestContext helper) {
        // negative scaling clamps experience requirements to one

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "-10");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 5;
            try {
                // 7 + (-10)*5 = -43 -> clamp to 1
                int xp = player.getNextLevelExperience();
                helper.assertTrue(xp == 1, "Negative step should clamp getNextLevelExperience to 1 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // disableEnchantments

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsRandomSelection(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                helper.assertFalse(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                    "Sharpness should not be available for random selection when disabled");
                helper.assertTrue(Enchantments.SMITE.isAvailableForRandomSelection(),
                    "Smite should still be available for random selection");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsFullIdentifier(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "minecraft:sharpness");
            try {
                helper.assertFalse(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                    "Full enchantment identifiers should match exactly");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsExplicitLootList(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "swift_sneak");
            try {
                LootContextParameterSet parameters = new LootContextParameterSet.Builder(helper.getWorld())
                    .build(LootContextTypes.EMPTY);
                LootContext context = new LootContext.Builder(parameters).random(1).build(Optional.empty());
                LootFunction function = EnchantRandomlyLootFunction.create().add(Enchantments.SWIFT_SNEAK).build();
                ItemStack result = function.apply(new ItemStack(Items.BOOK), context);

                helper.assertTrue(result.isOf(Items.BOOK),
                    "An explicit loot list containing only a disabled enchantment must leave the book unchanged");
                helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SWIFT_SNEAK, result) == 0,
                    "Explicit enchant_randomly lists must not bypass disable_enchantments");

                LootFunction allowedFunction = EnchantRandomlyLootFunction.create().add(Enchantments.SMITE).build();
                ItemStack allowed = allowedFunction.apply(new ItemStack(Items.DIAMOND_SWORD), context);
                helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SMITE, allowed) > 0,
                    "A non-disabled explicit enchantment should still be applied");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsBookOffer(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                helper.assertFalse(Enchantments.SHARPNESS.isAvailableForEnchantedBookOffer(),
                    "Sharpness should not be available for book offers when disabled");
                helper.assertTrue(Enchantments.SMITE.isAvailableForEnchantedBookOffer(),
                    "Smite should still be available for book offers");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsMaxLevel(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsEmpty(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "");
            try {
                helper.assertTrue(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                    "Sharpness should be available when disable list is empty");
                helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() > 0,
                    "Sharpness max level should be normal when disable list is empty");
            } finally {
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // godArmor

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godArmorEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_armor");
        try {
            ETTestHelper.setFeature("god_armor", true);
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
                "Protection should accept Blast Protection when enabled");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godArmorDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_armor");
        try {
            ETTestHelper.setFeature("god_armor", false);
            try {
                helper.assertTrue(!ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
                    "Protection should not accept Blast Protection when disabled");
            } finally {
                ETTestHelper.setFeature("god_armor", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godArmorAllTypes(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // godWeapons

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SMITE),
                "Sharpness should accept Smite when enabled");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.BANE_OF_ARTHROPODS),
                "Sharpness should accept Bane when enabled");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", false);
            try {
                helper.assertTrue(!ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SMITE),
                    "Sharpness should not accept Smite when disabled");
            } finally {
                ETTestHelper.setFeature("god_weapons", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // infiniteMending

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void infiniteMendingEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", true);
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.MENDING),
                "Infinity should accept Mending when enabled");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void infiniteMendingDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", false);
            try {
                helper.assertTrue(!ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.MENDING),
                    "Infinity should not accept Mending when disabled");
            } finally {
                ETTestHelper.setFeature("infinite_mending", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // noMendingUnbreaking

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", true);
            try {
                helper.assertFalse(ETTestHelper.canAccept(Enchantments.MENDING, Enchantments.UNBREAKING),
                    "Mending should not accept Unbreaking when no_mending_unbreaking enabled");
            } finally {
                ETTestHelper.setFeature("no_mending_unbreaking", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", false);
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.MENDING, Enchantments.UNBREAKING),
                "Mending should accept Unbreaking when no_mending_unbreaking disabled");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // multishotPiercing

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void multishotPiercingEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("multishot_piercing");
        try {
            ETTestHelper.setFeature("multishot_piercing", true);
            try {
                helper.assertTrue(ETTestHelper.canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
                    "Multishot should accept Piercing when enabled");
            } finally {
                ETTestHelper.setFeature("multishot_piercing", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void multishotPiercingReverse(TestContext helper) {
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void multishotPiercingDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("multishot_piercing");
        try {
            ETTestHelper.setFeature("multishot_piercing", false);
            try {
                helper.assertTrue(!ETTestHelper.canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
                    "Multishot should not accept Piercing when disabled");
            } finally {
                ETTestHelper.setFeature("multishot_piercing", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // tridentWeapons

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsEnabled(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsDisabled(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // axeWeapons

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axeWeaponsEnabled(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axeWeaponsDisabled(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowLooting

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowLootingEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_looting");
        try {
            ETTestHelper.setFeature("bow_looting", true);
            ItemStack bow = new ItemStack(Items.BOW);
            ItemStack crossbow = new ItemStack(Items.CROSSBOW);
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, bow),
                "Looting should be acceptable on bow");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, crossbow),
                "Looting should be acceptable on crossbow");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowLootingDisabled(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // axesNotTools

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axesNotToolsEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axes_not_tools");
        try {
            ETTestHelper.setFeature("axes_not_tools", true);
            ServerWorld world = helper.getWorld();
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            ZombieEntity target = EntityType.ZOMBIE.create(world);
            ZombieEntity attacker = EntityType.ZOMBIE.create(world);
            axe.getItem().postHit(axe, target, attacker);
            helper.assertTrue(axe.getDamage() == 1,
                "Axe should take 1 durability with axes_not_tools enabled (got " + axe.getDamage() + ")");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axesNotToolsDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axes_not_tools");
        try {
            ETTestHelper.setFeature("axes_not_tools", false);
            ServerWorld world = helper.getWorld();
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            ZombieEntity target = EntityType.ZOMBIE.create(world);
            ZombieEntity attacker = EntityType.ZOMBIE.create(world);
            try {
                axe.getItem().postHit(axe, target, attacker);
                helper.assertTrue(axe.getDamage() == 2,
                    "Axe should take 2 durability with axes_not_tools disabled (got " + axe.getDamage() + ")");
            } finally {
                ETTestHelper.setFeature("axes_not_tools", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // thorns normally damages armor after triggering backlash
    // level seven always triggers backlash

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noThornsBacklashEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_thorns_backlash");
        try {
            ETTestHelper.setFeature("no_thorns_backlash", true);
            ZombieEntity user = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            ZombieEntity attacker = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
            // equip thorns seven armor to guarantee backlash
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.addEnchantment(Enchantments.THORNS, 7);
            user.equipStack(EquipmentSlot.CHEST, chest);
            try {
                float attackerHealth = attacker.getHealth();
                int armorDamage = 0;
                for (int attempt = 0; attempt < 100 && armorDamage == 0; attempt++) {
                    EnchantmentHelper.onUserDamaged(user, attacker);
                    armorDamage = user.getEquippedStack(EquipmentSlot.CHEST).getDamage();
                }
                helper.assertTrue(attacker.getHealth() == attackerHealth,
                    "NoThornsBacklash must not damage the attacker");
                helper.assertTrue(armorDamage > 0, "NoThornsBacklash must preserve Thorns armor durability damage");
            } finally {
                ETTestHelper.setFeature("no_thorns_backlash", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noThornsBacklashDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_thorns_backlash");
        try {
            ETTestHelper.setFeature("no_thorns_backlash", false);
            ZombieEntity user = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            ZombieEntity attacker = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            chest.addEnchantment(Enchantments.THORNS, 7);
            user.equipStack(EquipmentSlot.CHEST, chest);
            try {
                int armorDamage = 0;
                for (int attempt = 0; attempt < 100 && armorDamage == 0; attempt++) {
                    EnchantmentHelper.onUserDamaged(user, attacker);
                    armorDamage = user.getEquippedStack(EquipmentSlot.CHEST).getDamage();
                }
                helper.assertTrue(armorDamage > 0, "Vanilla Thorns should damage armor when backlash is enabled");
            } finally {
                ETTestHelper.setFeature("no_thorns_backlash", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // loyalVoidTridents

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
            ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new TridentEntity(world, owner, tridentStack);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            trident.setPos(abs.getX(), world.getBottomY() - 5.0, abs.getZ());
            world.spawnEntity(trident);
            trident.tick();
            try {
                Field dealtDamage = TridentEntity.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertTrue((boolean) dealtDamage.get(trident),
                    "dealtDamage should be true below world bottom when loyal_void_tridents enabled");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", false);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
            ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new TridentEntity(world, owner, tridentStack);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            trident.setPos(abs.getX(), world.getBottomY() - 5.0, abs.getZ());
            world.spawnEntity(trident);
            try {
                trident.tick();
                Field dealtDamage = TridentEntity.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertFalse((boolean) dealtDamage.get(trident),
                    "dealtDamage should stay false when loyal_void_tridents disabled");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // soul speed damage uses a deterministic seeded random

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_soul_speed_backlash");
        try {
            ETTestHelper.setFeature("no_soul_speed_backlash", true);
            helper.setBlockState(new BlockPos(0, 1, 0), Blocks.SOUL_SAND.getDefaultState());
            ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            ItemStack boots = new ItemStack(Items.GOLDEN_BOOTS);
            boots.addEnchantment(Enchantments.SOUL_SPEED, 1);
            zombie.equipStack(EquipmentSlot.FEET, boots);
            zombie.tick(); // land on soul sand so getLandingBlockState() / isOnSoulSpeedBlock() pass
            try {
                Method removeBoost = LivingEntity.class.getDeclaredMethod("removeSoulSpeedBoost");
                removeBoost.setAccessible(true);
                Method addBoost = LivingEntity.class.getDeclaredMethod("addSoulSpeedBoostIfNeeded");
                addBoost.setAccessible(true);
                // the mixin prevents soul speed boot durability loss
                removeBoost.invoke(zombie);
                addBoost.invoke(zombie);
                helper.assertTrue(zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() == 0,
                    "Soul Speed boots should not take damage when no_soul_speed_backlash enabled (got "
                        + zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() + ")");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                ETTestHelper.setFeature("no_soul_speed_backlash", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashDisabled(TestContext helper) {
        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_soul_speed_backlash");
        try {
            ETTestHelper.setFeature("no_soul_speed_backlash", false);
            ServerWorld world = helper.getWorld();
            helper.setBlockState(new BlockPos(0, 1, 0), Blocks.SOUL_SAND.getDefaultState());
            ZombieEntity zombie = new ZombieEntity(world) {
                private final Random soulSpeedRandom = Random.create(5120L);

                @Override
                public Random getRandom() {
                    return soulSpeedRandom;
                }
            };
            BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            zombie.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            world.spawnEntity(zombie);
            ItemStack boots = new ItemStack(Items.GOLDEN_BOOTS);
            boots.addEnchantment(Enchantments.SOUL_SPEED, 1);
            zombie.equipStack(EquipmentSlot.FEET, boots);

            Method removeBoost = LivingEntity.class.getDeclaredMethod("removeSoulSpeedBoost");
            removeBoost.setAccessible(true);
            Method addBoost = LivingEntity.class.getDeclaredMethod("addSoulSpeedBoostIfNeeded");
            addBoost.setAccessible(true);
            removeBoost.invoke(zombie);
            addBoost.invoke(zombie);
            helper.assertTrue(zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() == 1,
                "Yarn Random seed 5120 must trigger one Soul Speed durability loss");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
        helper.complete();
    }

    // better mending repairs the entire inventory

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void betterMendingEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("better_mending");
        try {
            ETTestHelper.setFeature("better_mending", true);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingSword.addEnchantment(Enchantments.MENDING, 1);
            mendingSword.setDamage(100);
            // slot 20 is an ordinary inventory slot
            player.getInventory().setStack(20, mendingSword);

            BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 50);
            ETTestHelper.repairPlayerGears(orb, player, 50);

            helper.assertTrue(player.getInventory().getStack(20).getDamage() < 100,
                "BetterMending should repair inventory item (not just equipped items)");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void betterMendingDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("better_mending");
        try {
            ETTestHelper.setFeature("better_mending", false);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingSword.addEnchantment(Enchantments.MENDING, 1);
            mendingSword.setDamage(100);
            player.getInventory().setStack(20, mendingSword);

            BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 50);
            try {
                ETTestHelper.repairPlayerGears(orb, player, 50);
                helper.assertTrue(player.getInventory().getStack(20).getDamage() == 100,
                    "Vanilla mending should NOT repair non-equipped inventory item when better_mending disabled");
            } finally {
                ETTestHelper.setFeature("better_mending", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // betterMending: mainhand priority

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void betterMendingMainHandPriority(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("better_mending");
        try {
            ETTestHelper.setFeature("better_mending", true);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            // damaged mending sword in mainhand (slot 0)
            ItemStack mainhandSword = new ItemStack(Items.DIAMOND_SWORD);
            mainhandSword.addEnchantment(Enchantments.MENDING, 1);
            mainhandSword.setDamage(100);
            player.getInventory().setStack(0, mainhandSword);
            // damaged mending sword in inventory (slot 20)
            ItemStack inventorySword = new ItemStack(Items.DIAMOND_SWORD);
            inventorySword.addEnchantment(Enchantments.MENDING, 1);
            inventorySword.setDamage(100);
            player.getInventory().setStack(20, inventorySword);

            BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            // small amount of XP so it only repairs one item
            ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 5);
            ETTestHelper.repairPlayerGears(orb, player, 5);

            helper.assertTrue(player.getInventory().getStack(0).getDamage() < 100,
                "BetterMending should prioritize mainhand over inventory");
            helper.assertTrue(player.getInventory().getStack(20).getDamage() == 100,
                "BetterMending should NOT repair inventory item when mainhand needs repair");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowInfinityFix

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.INFINITY, 1);
            player.getInventory().setStack(0, bow);
            // vanilla blocks infinity bows without arrows
            TypedActionResult<ItemStack> result = bow.getItem().use(world, player, Hand.MAIN_HAND);
            try {
                helper.assertTrue(result.getResult().isAccepted(),
                    "BowInfinityFix should allow Infinity bow to fire without arrows");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", false);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.INFINITY, 1);
            player.getInventory().setStack(0, bow);
            try {
                TypedActionResult<ItemStack> result = bow.getItem().use(world, player, Hand.MAIN_HAND);
                helper.assertTrue(result.getResult().isAccepted() == false,
                    "Vanilla bow with Infinity should NOT fire without arrows when bow_infinity_fix disabled");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixNoInfinity(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            // bow without Infinity: mixin should not activate
            ItemStack bow = new ItemStack(Items.BOW);
            player.getInventory().setStack(0, bow);
            try {
                TypedActionResult<ItemStack> result = bow.getItem().use(world, player, Hand.MAIN_HAND);
                helper.assertTrue(result.getResult().isAccepted() == false,
                    "Bow without Infinity should NOT fire without arrows even with bow_infinity_fix enabled");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixSuppressedByMoreInfinity(TestContext helper) {
        // more infinity suppresses bow infinity fix

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ETTestHelper.setFeature("more_infinity", true);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.INFINITY, 1);
            player.getInventory().setStack(0, bow);
            // more infinity takes precedence without arrows
            TypedActionResult<ItemStack> result = bow.getItem().use(world, player, Hand.MAIN_HAND);
            try {
                helper.assertFalse(result.getResult().isAccepted(),
                    "BowInfinityFix should be suppressed when more_infinity is enabled");
            } finally {
                ETTestHelper.setFeature("more_infinity", false);
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixFiresArrow(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ETTestHelper.setFeature("more_infinity", false);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            // move the mock player into the loaded test structure
            BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
            player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.INFINITY, 1);
            player.getInventory().setStack(0, bow);
            // no arrows anywhere in the inventory
            Box search = player.getBoundingBox().expand(64.0);
            int arrowsBefore = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            try {
                // draw then fully release the bow
                bow.getItem().use(world, player, Hand.MAIN_HAND);
                bow.getItem().onStoppedUsing(bow, world, player, 0);

                int arrowsAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
                helper.assertTrue(arrowsAfter == arrowsBefore + 1,
                    "Infinity bow with no arrows should spawn exactly one arrow on release (before=" + arrowsBefore
                        + ", after=" + arrowsAfter + ")");
                helper.assertTrue(player.getInventory().count(Items.ARROW) == 0,
                    "Firing an Infinity bow with no arrows must not add or consume inventory arrows (got "
                        + player.getInventory().count(Items.ARROW) + ")");
                helper.assertTrue(bow.getDamage() == 1,
                    "Bow should take 1 durability after firing (confirms shootAll ran; got " + bow.getDamage() + ")");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixDisabledDoesNotFire(TestContext helper) {
        // vanilla cannot fire an arrowless infinity bow

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", false);
            ETTestHelper.setFeature("more_infinity", false);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
            player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.INFINITY, 1);
            player.getInventory().setStack(0, bow);
            Box search = player.getBoundingBox().expand(64.0);
            int arrowsBefore = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            try {
                bow.getItem().use(world, player, Hand.MAIN_HAND);
                bow.getItem().onStoppedUsing(bow, world, player, 0);
                int arrowsAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
                helper.assertTrue(arrowsAfter == arrowsBefore,
                    "Infinity bow with feature disabled must NOT fire without arrows (before=" + arrowsBefore
                        + ", after=" + arrowsAfter + ")");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // godWeapons: all combinations

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsAllCombinations(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsSelfReject(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            helper.assertFalse(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SHARPNESS),
                "Sharpness should NOT accept itself");
            helper.assertFalse(ETTestHelper.canAccept(Enchantments.SMITE, Enchantments.SMITE),
                "Smite should NOT accept itself");
            helper.assertFalse(ETTestHelper.canAccept(Enchantments.BANE_OF_ARTHROPODS, Enchantments.BANE_OF_ARTHROPODS),
                "Bane should NOT accept itself");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // loyalVoidTridents: edge cases

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsNoLoyalty(TestContext helper) {
        // trident without Loyalty should not be rescued from void

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            // no Loyalty enchantment
            ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new TridentEntity(world, owner, tridentStack);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            trident.setPos(abs.getX(), world.getBottomY() - 5.0, abs.getZ());
            world.spawnEntity(trident);
            trident.tick();
            try {
                Field dealtDamage = TridentEntity.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertFalse((boolean) dealtDamage.get(trident),
                    "Trident WITHOUT Loyalty should NOT have dealtDamage set in void");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsOwnerlessFallsIntoVoid(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            double startY = world.getBottomY() - 5.0;
            trident = new TridentEntity(world, abs.getX(), startY, abs.getZ(), tridentStack);
            trident.setVelocity(0, -2.0, 0);
            world.spawnEntity(trident);
            try {
                trident.tick();
                helper.assertTrue(trident.getY() < startY, "Ownerless trident should keep falling");
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsStopsDescending(TestContext helper) {
        // rescued tridents stop falling and return upward

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.addEnchantment(Enchantments.LOYALTY, 3);
            ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new TridentEntity(world, owner, tridentStack);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            double startY = world.getBottomY() - 5.0;
            trident.setPos(abs.getX(), startY, abs.getZ());
            trident.setVelocity(0, -2.0, 0);
            world.spawnEntity(trident);
            // tick until loyalty return begins
            for (int i = 0; i < 5; i++)
                trident.tick();
            helper.assertTrue(trident.getY() > startY,
                "Loyal trident should start returning upward after being rescued from void (Y=" + trident.getY()
                    + ", started at " + startY + ")");
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // axesNotTools: multiple hits

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axesNotToolsMultipleHits(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axes_not_tools");
        try {
            ETTestHelper.setFeature("axes_not_tools", true);
            ServerWorld world = helper.getWorld();
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            ZombieEntity target = EntityType.ZOMBIE.create(world);
            ZombieEntity attacker = EntityType.ZOMBIE.create(world);
            // 3 hits should cause 3 durability (not 6)
            axe.getItem().postHit(axe, target, attacker);
            axe.getItem().postHit(axe, target, attacker);
            axe.getItem().postHit(axe, target, attacker);
            helper.assertTrue(axe.getDamage() == 3,
                "3 axe hits with axes_not_tools should cause 3 durability damage (got " + axe.getDamage() + ")");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    private static final float PB_REDUCED = 3.6f; // getInflictedDamage(10, 16)

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void protectionBypassMagicEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                    "Magic damage should bypass protection entirely via a real hit (health delta " + delta
                        + ", expected 10.0)");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void protectionBypassWitherEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "wither");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().wither(), 10.0f);
                helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                    "Wither damage should bypass protection entirely via a real hit (health delta " + delta
                        + ", expected 10.0)");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void protectionBypassDisabledStillReduces(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", false);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(delta - PB_REDUCED) < 0.05f,
                    "Magic damage with bypass disabled should be reduced by protection (health delta " + delta
                        + ", expected ~" + PB_REDUCED + ")");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void protectionBypassGenericNotAffected(TestContext helper) {
        // generic damage is not a configured protection bypass
        // protection still reduces its damage

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic,wither,dragon_breath");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().generic(), 10.0f);
                helper.assertTrue(Math.abs(delta - PB_REDUCED) < 0.05f,
                    "Generic damage should still be reduced by protection (health delta " + delta + ", expected ~"
                        + PB_REDUCED + ")");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void protectionBypassModdedColonBranch(TestContext helper) {
        // known namespaced ids bypass protection
        // unknown namespaced ids safely do not match

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            try {
                ETTestHelper.setConfigValue("protection_bypass_types", "minecraft:magic");
                float bypassed = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(),
                    10.0f);
                helper.assertTrue(Math.abs(bypassed - 10.0f) < 0.05f,
                    "'minecraft:magic' should resolve and bypass protection (health delta " + bypassed
                        + ", expected 10.0)");

                ETTestHelper.setConfigValue("protection_bypass_types", "nonexistent:foo");
                float reduced = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(reduced - PB_REDUCED) < 0.05f,
                    "Unknown namespaced id should not crash and should not bypass (health delta " + reduced
                        + ", expected ~" + PB_REDUCED + ")");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void protectionBypassDragonBreath(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic,dragon_breath");
            try {
                float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().dragonBreath(),
                    10.0f);
                helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                    "Dragon breath should bypass protection when listed (health delta " + delta + ", expected 10.0)");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void protectionBypassWhitespaceTokens(TestContext helper) {
        // malformed list tokens do not discard valid entries

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("protection_bypass_enabled",
            "protection_bypass_types");
        try {
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic , bad id! , wither ,");
            try {
                float magic = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(), 10.0f);
                helper.assertTrue(Math.abs(magic - 10.0f) < 0.05f,
                    "Whitespace-padded 'magic' should bypass (health delta " + magic + ", expected 10.0)");
                float wither = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().wither(), 10.0f);
                helper.assertTrue(Math.abs(wither - 10.0f) < 0.05f,
                    "Whitespace-padded 'wither' should bypass (health delta " + wither + ", expected 10.0)");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void protectionBypassPrecedesMoreProtection(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base",
            "protection_bypass_enabled", "protection_bypass_types");
        try {
            ETTestHelper.setFeature("more_protection", true);
            ETTestHelper.setConfigValue("more_protection_base", "0.96");
            ETTestHelper.setFeature("protection_bypass_enabled", true);
            ETTestHelper.setConfigValue("protection_bypass_types", "magic");
            try {
                float bypassed = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(),
                    10.0f);
                helper.assertTrue(Math.abs(bypassed - 10.0f) < 0.05f,
                    "configured damage must bypass MoreProtection as well as vanilla Protection");

                float protectedDamage = protectedZombieDamageDelta(helper,
                    helper.getWorld().getDamageSources().generic(), 10.0f);
                float expected = (float) (10.0 * Math.pow(0.96, 16));
                helper.assertTrue(Math.abs(protectedDamage - expected) < 0.05f,
                    "unlisted damage should retain MoreProtection scaling (got " + protectedDamage + ", expected ~"
                        + expected + ")");
            } finally {
                ETTestHelper.setConfigValue("protection_bypass_types", "");
                ETTestHelper.setFeature("protection_bypass_enabled", false);
                ETTestHelper.setFeature("more_protection", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // villagerTradeLimits

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMaxUsesEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_max_uses");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
                ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.use();
                offer.use();
                offer.use();
                helper.assertTrue(offer.isDisabled(), "Enchantment trade should be disabled after 3 uses (max_uses=3)");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMaxUsesDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_max_uses");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", false);
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
                ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.use();
                offer.use();
                offer.use();
                helper.assertFalse(offer.isDisabled(),
                    "Enchantment trade should NOT be disabled when feature is off (uses=3, vanilla maxUses=12)");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsPerEnchantOverride(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_max_uses", "trade_sharpness");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
                "trade_sharpness", "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setConfigValue("trade_sharpness", "2");
                ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.use();
                offer.use();
                helper.assertTrue(offer.isDisabled(),
                    "Sharpness trade should be disabled after 2 uses (trade_sharpness=2)");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsDisabledEnchantment(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits", "trade_mending");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("trade_mending",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("trade_mending", "0");
                ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1));
                TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
                helper.assertTrue(offer.isDisabled(),
                    "Mending trade should be disabled immediately when trade_mending=0");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsRestockEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_restock");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_restock",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_restock", "true");
                ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.use();
                offer.use();
                offer.resetUses();
                helper.assertTrue(offer.getUses() == 0,
                    "Enchantment trade should restock when enchant_trade_restock=true (uses=" + offer.getUses() + ")");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsRestockDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("villager_trade_limits",
            "enchant_trade_restock");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_restock",
                "villager_trade_limits");
            try {
                ETTestHelper.setFeature("villager_trade_limits", true);
                ETTestHelper.setConfigValue("enchant_trade_restock", "false");
                ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
                offer.use();
                offer.use();
                offer.resetUses();
                helper.assertTrue(offer.getUses() == 2,
                    "Enchantment trade should NOT restock when enchant_trade_restock=false (uses=" + offer.getUses()
                        + ")");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsNoRestockList(TestContext helper) {
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
                    .forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                TradeOffer sharpOffer = new TradeOffer(new TradedItem(Items.EMERALD, 10), sharpBook, 12, 1, 0.2f);
                sharpOffer.use();
                sharpOffer.resetUses();
                ItemStack mendingBook = EnchantedBookItem
                    .forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1));
                TradeOffer mendingOffer = new TradeOffer(new TradedItem(Items.EMERALD, 10), mendingBook, 12, 1, 0.2f);
                mendingOffer.use();
                mendingOffer.resetUses();
                helper.assertTrue(sharpOffer.getUses() == 1,
                    "Sharpness trade in no_restock list should NOT restock (uses=" + sharpOffer.getUses() + ")");
                helper.assertTrue(mendingOffer.getUses() == 0,
                    "Mending trade NOT in no_restock list should restock (uses=" + mendingOffer.getUses() + ")");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsNonEnchantedUnaffected(TestContext helper) {
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
                TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 1), new ItemStack(Items.WHEAT, 6), 12,
                    1, 0.2f);
                offer.use();
                offer.use();
                offer.use();
                offer.resetUses();
                helper.assertTrue(offer.getUses() == 0,
                    "Non-enchanted trade should still restock normally (uses=" + offer.getUses() + ")");
                helper.assertFalse(offer.isDisabled(),
                    "Non-enchanted trade should not be affected by enchant_trade_max_uses");
            } finally {
                ETTestHelper.restoreConfig(originalConfig);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    // bowLooting: guard negatives

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowLootingIdentityGuardNegative(TestContext helper) {
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowLootingNonBowGuard(TestContext helper) {
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // tridentWeapons: DamageEnchantment branch + guards

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsDamageEnchantBranch(TestContext helper) {
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsNonTridentGuardAndFallthrough(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // weapon tweaks: cross-tweak Looting acceptability

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void weaponTweaksCrossLootingAcceptability(TestContext helper) {
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // axeWeapons: guard negatives + enchant-table presence

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axeWeaponsGuardNegatives(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void axeWeaponsEnchantTablePresence(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("axe_weapons");
        try {
            ETTestHelper.setFeature("axe_weapons", true);
            try {
                List<EnchantmentLevelEntry> axeEntries = EnchantmentHelper.getPossibleEntries(
                    helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.DIAMOND_AXE), false);
                helper.assertTrue(containsEnchant(axeEntries, Enchantments.LOOTING),
                    "Looting must be offered on an axe from the enchant table when axe_weapons is on");
                helper.assertTrue(containsEnchant(axeEntries, Enchantments.FIRE_ASPECT),
                    "Fire Aspect must be offered on an axe from the enchant table when axe_weapons is on");
                helper.assertTrue(containsEnchant(axeEntries, Enchantments.KNOCKBACK),
                    "Knockback must be offered on an axe from the enchant table when axe_weapons is on");

                // pickaxes never receive axe weapon enchantments
                List<EnchantmentLevelEntry> pickEntries = EnchantmentHelper.getPossibleEntries(
                    helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.DIAMOND_PICKAXE), false);
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
            List<EnchantmentLevelEntry> offEntries = EnchantmentHelper.getPossibleEntries(
                helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.DIAMOND_AXE), false);
            helper.assertFalse(containsEnchant(offEntries, Enchantments.LOOTING),
                "Looting must be absent from axe enchant-table entries when axe_weapons is off");
            helper.assertFalse(containsEnchant(offEntries, Enchantments.FIRE_ASPECT),
                "Fire Aspect must be absent from axe enchant-table entries when axe_weapons is off");
            helper.assertFalse(containsEnchant(offEntries, Enchantments.KNOCKBACK),
                "Knockback must be absent from axe enchant-table entries when axe_weapons is off");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // godWeapons: fall-through, Impaling, removeConflicts

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsNonDamageEnchantFallthrough(TestContext helper) {
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsImpaling(TestContext helper) {
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void godWeaponsRemoveConflicts(TestContext helper) {
        EnchantmentLevelEntry picked = new EnchantmentLevelEntry(Enchantments.SMITE, 1);

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("god_weapons");
        try {
            ETTestHelper.setFeature("god_weapons", true);
            try {
                List<EnchantmentLevelEntry> list = new ArrayList<>();
                list.add(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                EnchantmentHelper.removeConflicts(list, picked);
                helper.assertTrue(list.size() == 1,
                    "Sharpness should survive removeConflicts against Smite when god_weapons on (size " + list.size()
                        + ")");
            } finally {
                ETTestHelper.setFeature("god_weapons", false);
            }

            ETTestHelper.setFeature("god_weapons", false);
            try {
                List<EnchantmentLevelEntry> list = new ArrayList<>();
                list.add(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                EnchantmentHelper.removeConflicts(list, picked);
                helper.assertTrue(list.isEmpty(),
                    "Sharpness should be removed by removeConflicts against Smite when god_weapons off (size "
                        + list.size() + ")");
            } finally {
                ETTestHelper.setFeature("god_weapons", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // infiniteMending: real anvil merge

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void infiniteMendingAnvilMerge(TestContext helper) {
        // infinity and mending must combine through a real anvil

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", true);
            try {
                ItemStack bow = new ItemStack(Items.BOW);
                bow.addEnchantment(Enchantments.INFINITY, 1);
                ItemStack mendingBook = EnchantedBookItem
                    .forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1));
                ItemStack out = anvilCombine(helper, bow, mendingBook);
                helper.assertFalse(out.isEmpty(), "Infinity and Mending anvil merge must produce an output");
                helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.INFINITY, out) == 1,
                    "Merged bow should keep Infinity 1 (got " + EnchantmentHelper.getLevel(Enchantments.INFINITY, out)
                        + ")");
                helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.MENDING, out) == 1,
                    "Merged bow should gain Mending 1 (got " + EnchantmentHelper.getLevel(Enchantments.MENDING, out)
                        + ")");
            } finally {
                ETTestHelper.setFeature("infinite_mending", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void infiniteMendingAnvilMergeDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("infinite_mending");
        try {
            ETTestHelper.setFeature("infinite_mending", false);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.INFINITY, 1);
            ItemStack mendingBook = EnchantedBookItem
                .forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1));
            ItemStack out = anvilCombine(helper, bow, mendingBook);
            helper.assertTrue(out.isEmpty(), "Disabled Infinity and Mending anvil operation should produce no output");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void infiniteMendingNoOverApplication(TestContext helper) {
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // noMendingUnbreaking: real anvil combine

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingAnvilBlocks(TestContext helper) {
        // unbreaking cannot transfer to a mending pickaxe

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", true);
            try {
                ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
                pick.addEnchantment(Enchantments.MENDING, 1);
                ItemStack unbreakingBook = EnchantedBookItem
                    .forEnchantment(new EnchantmentLevelEntry(Enchantments.UNBREAKING, 3));
                ItemStack out = anvilCombine(helper, pick, unbreakingBook);
                helper.assertTrue(out.isEmpty(),
                    "Blocked Mending and Unbreaking anvil operation should produce no output");
            } finally {
                ETTestHelper.setFeature("no_mending_unbreaking", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingAnvilAllowsWhenDisabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", false);
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.addEnchantment(Enchantments.MENDING, 1);
            ItemStack unbreakingBook = EnchantedBookItem
                .forEnchantment(new EnchantmentLevelEntry(Enchantments.UNBREAKING, 3));
            ItemStack out = anvilCombine(helper, pick, unbreakingBook);
            helper.assertFalse(out.isEmpty(), "Vanilla Mending and Unbreaking anvil merge must produce an output");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.MENDING, out) == 1,
                "Vanilla anvil should keep Mending 1 (got " + EnchantmentHelper.getLevel(Enchantments.MENDING, out)
                    + ")");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.UNBREAKING, out) == 3,
                "Vanilla anvil should add Unbreaking 3 (got " + EnchantmentHelper.getLevel(Enchantments.UNBREAKING, out)
                    + ")");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingFallthroughUnrelated(TestContext helper) {
        // unrelated enchantments still merge with mending items

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("no_mending_unbreaking");
        try {
            ETTestHelper.setFeature("no_mending_unbreaking", true);
            try {
                ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
                pick.addEnchantment(Enchantments.MENDING, 1);
                ItemStack efficiencyBook = EnchantedBookItem
                    .forEnchantment(new EnchantmentLevelEntry(Enchantments.EFFICIENCY, 3));
                ItemStack out = anvilCombine(helper, pick, efficiencyBook);
                helper.assertFalse(out.isEmpty(), "Mending and Efficiency anvil merge must produce an output");
                helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.MENDING, out) == 1,
                    "Mending should be preserved (got " + EnchantmentHelper.getLevel(Enchantments.MENDING, out) + ")");
                helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, out) == 3,
                    "Efficiency should merge (unrelated pair) (got "
                        + EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, out) + ")");
            } finally {
                ETTestHelper.setFeature("no_mending_unbreaking", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // disableEnchantments: generation paths

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsGetPossibleEntries(TestContext helper) {
        // disabled enchantments are absent from enchanting table entries
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                List<EnchantmentLevelEntry> entries = EnchantmentHelper
                    .getPossibleEntries(helper.getWorld().getEnabledFeatures(), 30, sword, true);
                helper.assertFalse(containsEnchant(entries, Enchantments.SHARPNESS),
                    "Disabled Sharpness must be absent from enchant-table entries");
                helper.assertTrue(containsEnchant(entries, Enchantments.SMITE),
                    "Non-disabled Smite should remain in enchant-table entries");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            // feature-off mirror: Sharpness returns
            List<EnchantmentLevelEntry> mirror = EnchantmentHelper
                .getPossibleEntries(helper.getWorld().getEnabledFeatures(), 30, sword, true);
            helper.assertTrue(containsEnchant(mirror, Enchantments.SHARPNESS),
                "Sharpness should be present again once the feature is off");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsMultiEntry(TestContext helper) {

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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsBookFactory(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("disable_enchantments_enabled",
            "disable_enchantments");
        try {

            Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
                "disable_enchantments_enabled");
            try {
                ETTestHelper.setFeature("disable_enchantments_enabled", true);
                ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
                Entity anchor = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
                Random random = Random.create(0L);
                Class<?> cls = Class.forName("net.minecraft.village.TradeOffers$EnchantBookFactory");
                Constructor<?> ctor = cls.getDeclaredConstructor(int.class);
                ctor.setAccessible(true);
                Object factory = ctor.newInstance(1);
                Method create = cls.getDeclaredMethod("create", Entity.class, Random.class);
                create.setAccessible(true);
                boolean sawSharpness = false;
                boolean sawNonSharpness = false;
                for (int i = 0; i < 500; i++) {
                    TradeOffer offer = (TradeOffer) create.invoke(factory, anchor, random);
                    var enchants = EnchantmentHelper.getEnchantments(offer.getSellItem());
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsWhitespaceTrimming(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", " sharpness , , mending ,");
            try {
                helper.assertFalse(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                    "Trimmed 'sharpness' disabled");
                helper.assertFalse(Enchantments.MENDING.isAvailableForRandomSelection(), "Trimmed 'mending' disabled");
                helper.assertTrue(Enchantments.SMITE.isAvailableForRandomSelection(), "Smite still available");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsNamespacedAndUnknown(TestContext helper) {
        // full identifiers match exactly and unknown entries do nothing

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            try {
                ETTestHelper.setConfigValue("disable_enchantments", "minecraft:sharpness");
                helper.assertFalse(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                    "minecraft:sharpness should disable the exact namespaced enchantment");

                ETTestHelper.setConfigValue("disable_enchantments", "notarealenchant");
                helper.assertTrue(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                    "An unknown id should be a harmless no-op");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsFeatureGateOff(TestContext helper) {
        // disabling remains inactive when the master switch is off

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                helper.assertTrue(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                    "Sharpness must stay available when the master switch is off");
                helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() > 0,
                    "Sharpness max level must be normal when the master switch is off");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // xpScaling: real leveling + boundaries

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingRealLeveling(TestContext helper) {
        // real experience gains use the configured level requirements

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 0;
            player.experienceProgress = 0.0f;
            try {
                player.addExperience(7);
                helper.assertTrue(player.experienceLevel == 1,
                    "Adding 7 XP at L0 (need 7) should reach L1 (got " + player.experienceLevel + ")");
                player.addExperience(9);
                helper.assertTrue(player.experienceLevel == 2,
                    "Adding 9 XP at L1 (need 9) should reach L2 (got " + player.experienceLevel + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingZeroBoundary(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "0");
            ETTestHelper.setConfigValue("xp_scaling_step", "0");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 10;
            try {
                int xp = player.getNextLevelExperience();
                helper.assertTrue(xp == 1,
                    "base=0,step=0 must clamp getNextLevelExperience to the lower bound 1 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingLevelZero(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 0;
            try {
                int xp = player.getNextLevelExperience();
                helper.assertTrue(xp == 7, "base=7,step=2 at L0 should be 7 (got " + xp + ")");
            } finally {
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingDefaultFallback(TestContext helper) {
        // invalid bases fall back to the default value

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "abc");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 10;
            try {
                int xp = player.getNextLevelExperience();
                helper.assertTrue(xp == 27, "Non-numeric base should fall back to 7 -> 7 + 2*10 = 27 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingBeyondIntRange(TestContext helper) {
        // out-of-range steps fall back to the default value

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling", "xp_scaling_base",
            "xp_scaling_step");
        try {
            ETTestHelper.setFeature("xp_scaling", true);
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "9999999999");
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 2;
            try {
                int xp = player.getNextLevelExperience();
                helper.assertTrue(xp == 11,
                    "Out-of-int-range step should fall back to default 2 -> 7 + 2*2 = 11 (got " + xp + ")");
            } finally {
                ETTestHelper.setConfigValue("xp_scaling_base", "7");
                ETTestHelper.setConfigValue("xp_scaling_step", "2");
                ETTestHelper.setFeature("xp_scaling", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void xpScalingOffGateMidTier(TestContext helper) {
        // disabled scaling uses the vanilla mid-tier formula

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("xp_scaling");
        try {
            ETTestHelper.setFeature("xp_scaling", false);
            ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
            player.experienceLevel = 20;
            int xp = player.getNextLevelExperience();
            helper.assertTrue(xp == 62, "Vanilla XP at L20 should be 62 (got " + xp + ")");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // loyalVoidTridents: real void positions

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsAboveBottomNotRescued(TestContext helper) {
        // tridents above the world bottom are not rescued

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
            ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new TridentEntity(world, owner, tridentStack);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            double startY = world.getBottomY() + 10.0;
            trident.setPos(abs.getX(), startY, abs.getZ());
            trident.setVelocity(0, -1.0, 0);
            world.spawnEntity(trident);
            trident.tick();
            try {
                Field dealtDamage = TridentEntity.class.getDeclaredField("dealtDamage");
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
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsDisabledLostToVoid(TestContext helper) {
        // feature OFF: a loyalty trident dropped below the void-kill plane
        // is discarded by vanilla on the next tick

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", false);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
            ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new TridentEntity(world, owner, tridentStack);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            trident.setPos(abs.getX(), world.getBottomY() - 65.0, abs.getZ());
            world.spawnEntity(trident);
            trident.tick();
            try {
                helper.assertTrue(trident.isRemoved(),
                    "A loyalty trident below the void-kill plane should be discarded when the feature is off");
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsEnabledSurvivesVoid(TestContext helper) {
        // loyalty tridents at the bottom survive and return

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.addEnchantment(Enchantments.LOYALTY, 3);
            ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new TridentEntity(world, owner, tridentStack);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            double startY = world.getBottomY() - 1.0;
            trident.setPos(abs.getX(), startY, abs.getZ());
            trident.setVelocity(0, -2.0, 0);
            world.spawnEntity(trident);
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
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsExactBoundary(TestContext helper) {
        // the world-bottom boundary triggers rescue

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("loyal_void_tridents");
        TridentEntity trident = null;
        try {
            ETTestHelper.setFeature("loyal_void_tridents", true);
            ServerWorld world = helper.getWorld();
            ItemStack tridentStack = new ItemStack(Items.TRIDENT);
            tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
            ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            trident = new TridentEntity(world, owner, tridentStack);
            BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
            trident.setPos(abs.getX(), world.getBottomY(), abs.getZ());
            world.spawnEntity(trident);
            trident.tick();
            try {
                Field dealtDamage = TridentEntity.class.getDeclaredField("dealtDamage");
                dealtDamage.setAccessible(true);
                helper.assertTrue((boolean) dealtDamage.get(trident),
                    "Y == bottomY should trigger the rescue (dealtDamage true)");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            } finally {
                ETTestHelper.setFeature("loyal_void_tridents", false);
            }
            helper.complete();

        } finally {
            if (trident != null)
                trident.discard();
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowInfinityFix: short-draw bail

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixShortDrawBail(TestContext helper) {
        // insufficient bow draw prevents all firing logic

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ETTestHelper.setFeature("more_infinity", false);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
            player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.INFINITY, 1);
            player.getInventory().setStack(0, bow);
            Box search = player.getBoundingBox().expand(64.0);
            int arrowsBefore = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            try {
                bow.getItem().use(world, player, Hand.MAIN_HAND);
                // immediate release keeps pull progress below 0.1
                bow.getItem().onStoppedUsing(bow, world, player, bow.getMaxUseTime());
                int arrowsAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
                helper.assertTrue(arrowsAfter == arrowsBefore, "A barely-drawn Infinity bow must NOT fire (before="
                    + arrowsBefore + ", after=" + arrowsAfter + ")");
                helper.assertTrue(bow.getDamage() == 0,
                    "A barely-drawn bow should take no durability damage (got " + bow.getDamage() + ")");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // villagerTradeLimits: most-restrictive-wins + more real paths

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMostRestrictiveWins(TestContext helper) {
        // multi-enchant trades use the lowest configured limit

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses", "trade_looting",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setConfigValue("trade_sharpness", "5");
            ETTestHelper.setConfigValue("trade_looting", "2");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.addEnchantment(Enchantments.SHARPNESS, 1);
            sword.addEnchantment(Enchantments.LOOTING, 1);
            TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), sword, 12, 1, 0.2f);
            try {
                offer.use();
                helper.assertFalse(offer.isDisabled(), "After 1 use the trade should still be enabled (min limit 2)");
                offer.use();
                helper.assertTrue(offer.isDisabled(),
                    "After 2 uses the most-restrictive limit (min(5,2)=2) should disable the trade");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setConfigValue("trade_looting", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsGlobalFallbackParticipatesInMinimum(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses", "trade_looting",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
            ETTestHelper.setConfigValue("trade_sharpness", "50");
            ETTestHelper.setConfigValue("trade_looting", "-1");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.addEnchantment(Enchantments.SHARPNESS, 1);
            sword.addEnchantment(Enchantments.LOOTING, 1);
            TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), sword, 12, 1, 0.2f);
            try {
                offer.use();
                offer.use();
                helper.assertFalse(offer.isDisabled(), "Global fallback should allow two uses");
                offer.use();
                helper.assertTrue(offer.isDisabled(), "Global fallback should cap the trade at three uses");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setConfigValue("trade_looting", "-1");
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMostRestrictiveZeroDisables(TestContext helper) {
        // any per-enchant limit of 0 disables the whole multi-enchant trade

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses", "trade_looting",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setConfigValue("trade_sharpness", "5");
            ETTestHelper.setConfigValue("trade_looting", "0");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.addEnchantment(Enchantments.SHARPNESS, 1);
            sword.addEnchantment(Enchantments.LOOTING, 1);
            TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), sword, 12, 1, 0.2f);
            try {
                helper.assertTrue(offer.isDisabled(),
                    "trade_looting=0 should disable the multi-enchant trade immediately");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setConfigValue("trade_looting", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsGlobalZeroDisablesAll(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "0");
            ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
            try {
                helper.assertTrue(offer.isDisabled(),
                    "enchant_trade_max_uses=0 should disable an enchant trade at 0 uses");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsOverridesVanillaMaxUses(TestContext helper) {
        // per-enchantment limits replace vanilla offer limits

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setConfigValue("trade_sharpness", "50");
            ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
            try {
                for (int i = 0; i < 49; i++)
                    offer.use();
                helper.assertFalse(offer.isDisabled(),
                    "After 49 uses the trade should remain below the configured maxUses 50");
                offer.use();
                helper.assertTrue(offer.isDisabled(), "The trade should disable on its configured 50th use");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsUnderLimitEnabled(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
            ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
            offer.use();
            offer.use();
            try {
                helper.assertFalse(offer.isDisabled(), "2 uses under a limit of 3 should stay enabled");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsResetUsesFeatureOff(TestContext helper) {
        // disabled limits preserve vanilla restocking behavior

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_restock",
            "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", false);
            ETTestHelper.setConfigValue("enchant_trade_restock", "false");
            ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
            offer.use();
            offer.use();
            offer.resetUses();
            try {
                helper.assertTrue(offer.getUses() == 0,
                    "Vanilla resetUses should run when the feature is off (uses=" + offer.getUses() + ")");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_restock", "true");
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsNoRestockListEdges(TestContext helper) {
        // list parsing trims entries and ignores empty values

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_no_restock",
            "enchant_trade_restock", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_restock", "true");
            ETTestHelper.setConfigValue("enchant_trade_no_restock", " mending , sharpness ,, ");
            TradeOffer mending = new TradeOffer(new TradedItem(Items.EMERALD, 10),
                EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1)), 12, 1, 0.2f);
            TradeOffer sharpness = new TradeOffer(new TradedItem(Items.EMERALD, 10),
                EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1)), 12, 1, 0.2f);
            TradeOffer looting = new TradeOffer(new TradedItem(Items.EMERALD, 10),
                EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.LOOTING, 1)), 12, 1, 0.2f);
            mending.use();
            mending.resetUses();
            sharpness.use();
            sharpness.resetUses();
            looting.use();
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsGarbagePerEnchant(TestContext helper) {
        // invalid per-enchantment limits use the global default

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "trade_sharpness", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            try {
                ETTestHelper.setConfigValue("trade_sharpness", "-5");
                TradeOffer neg = new TradeOffer(new TradedItem(Items.EMERALD, 10), book.copy(), 12, 1, 0.2f);
                neg.use();
                neg.use();
                neg.use();
                helper.assertFalse(neg.isDisabled(),
                    "Negative per-enchant value should fall back to global (-1) -> not disabled");

                ETTestHelper.setConfigValue("trade_sharpness", "abc");
                TradeOffer garbage = new TradeOffer(new TradedItem(Items.EMERALD, 10), book.copy(), 12, 1, 0.2f);
                garbage.use();
                garbage.use();
                garbage.use();
                helper.assertFalse(garbage.isDisabled(),
                    "Non-numeric per-enchant value should fall back to global (-1) -> not disabled");
            } finally {
                ETTestHelper.setConfigValue("trade_sharpness", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsRealPurchasability(TestContext helper) {
        // disabled trades leave the merchant output slot empty

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", true);
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            try {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "0");
                MerchantInventory disabledInv = buildMerchantInventory(player, book.copy());
                disabledInv.updateOffers();
                helper.assertTrue(disabledInv.getStack(2).isEmpty(),
                    "A disabled enchant trade should leave the merchant output slot empty");

                ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
                MerchantInventory enabledInv = buildMerchantInventory(player, book.copy());
                enabledInv.updateOffers();
                helper.assertFalse(enabledInv.getStack(2).isEmpty(),
                    "An under-limit enchant trade should populate the merchant output slot");
            } finally {
                ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
                ETTestHelper.setFeature("villager_trade_limits", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // betterMending: recursive leftover-XP must not over-repair

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void betterMendingLeftoverXpNoOverRepair(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("better_mending", "more_mending");
        try {
            ETTestHelper.setFeature("better_mending", true);
            // keep more mending disabled for vanilla repair costs
            ETTestHelper.setFeature("more_mending", false);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            // mainhand repairs first and leaves experience
            ItemStack mainhand = new ItemStack(Items.DIAMOND_SWORD);
            mainhand.addEnchantment(Enchantments.MENDING, 1);
            mainhand.setDamage(4);
            player.getInventory().setStack(0, mainhand);
            // inventory repairs use only the remaining experience
            ItemStack inv = new ItemStack(Items.DIAMOND_SWORD);
            inv.addEnchantment(Enchantments.MENDING, 1);
            inv.setDamage(100);
            player.getInventory().setStack(20, inv);

            BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 5);
            try {
                ETTestHelper.repairPlayerGears(orb, player, 5);
                helper.assertTrue(player.getInventory().getStack(0).getDamage() == 0,
                    "Mainhand item should be fully repaired first (got " + player.getInventory().getStack(0).getDamage()
                        + ")");
                helper.assertTrue(player.getInventory().getStack(20).getDamage() == 94,
                    "Recursive repair must use the leftover XP (3 -> 6 durability, damage 94), NOT the orb's "
                        + "full budget (would over-repair to 90) (got " + player.getInventory().getStack(20).getDamage()
                        + ")");
            } finally {
                ETTestHelper.setFeature("better_mending", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowLooting: enchant-table reach

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowLootingEnchantTablePresence(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_looting");
        try {
            ETTestHelper.setFeature("bow_looting", true);
            try {
                List<EnchantmentLevelEntry> bowEntries = EnchantmentHelper
                    .getPossibleEntries(helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.BOW), false);
                helper.assertTrue(containsEnchant(bowEntries, Enchantments.LOOTING),
                    "Looting must be offered on a bow from the enchant table when bow_looting is on");
                List<EnchantmentLevelEntry> crossbowEntries = EnchantmentHelper.getPossibleEntries(
                    helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.CROSSBOW), false);
                helper.assertTrue(containsEnchant(crossbowEntries, Enchantments.LOOTING),
                    "Looting must be offered on a crossbow from the enchant table when bow_looting is on");
                // pickaxes never gain bow looting enchantment entries
                List<EnchantmentLevelEntry> pickEntries = EnchantmentHelper.getPossibleEntries(
                    helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.DIAMOND_PICKAXE), false);
                helper.assertFalse(containsEnchant(pickEntries, Enchantments.LOOTING),
                    "Looting must NOT be offered on a pickaxe via bow_looting");
            } finally {
                ETTestHelper.setFeature("bow_looting", false);
            }
            // disabled bow looting leaves vanilla table entries
            List<EnchantmentLevelEntry> offEntries = EnchantmentHelper
                .getPossibleEntries(helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.BOW), false);
            helper.assertFalse(containsEnchant(offEntries, Enchantments.LOOTING),
                "Looting must be absent from bow enchant-table entries when bow_looting is off");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // tridentWeapons: all granted sword enchantments reach the enchanting table

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsEnchantTablePresence(TestContext helper) {
        ItemStack trident = new ItemStack(Items.TRIDENT);

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("trident_weapons");
        try {
            ETTestHelper.setFeature("trident_weapons", true);
            try {
                List<EnchantmentLevelEntry> entries = EnchantmentHelper
                    .getPossibleEntries(helper.getWorld().getEnabledFeatures(), 30, trident, false);
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
            List<EnchantmentLevelEntry> off = EnchantmentHelper
                .getPossibleEntries(helper.getWorld().getEnabledFeatures(), 30, trident, false);
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // disableEnchantments: anvil application blocked

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsAnvilBlocked(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("disable_enchantments",
            "disable_enchantments_enabled");
        try {
            ETTestHelper.setFeature("disable_enchantments_enabled", true);
            ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
            try {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                ItemStack sharpBook = EnchantedBookItem
                    .forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
                ItemStack out = anvilCombine(helper, sword, sharpBook);
                helper.assertTrue(out.isEmpty(), "Disabled Sharpness anvil operation should produce no output");
            } finally {
                ETTestHelper.setConfigValue("disable_enchantments", "");
                ETTestHelper.setFeature("disable_enchantments_enabled", false);
            }
            // disabled enchantments apply normally when the feature is off
            ItemStack sword2 = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack sharpBook2 = EnchantedBookItem
                .forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            ItemStack out2 = anvilCombine(helper, sword2, sharpBook2);
            helper.assertFalse(out2.isEmpty(), "Sharpness anvil merge with feature off must produce an output");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out2) == 1,
                "With the feature off, Sharpness 1 should transfer onto the anvil output (got "
                    + EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out2) + ")");
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // bowInfinityFix: onStoppedUsing infinity guard (real fire path)

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixNoInfinityDoesNotFireArrow(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("bow_infinity_fix", "more_infinity");
        try {
            ETTestHelper.setFeature("bow_infinity_fix", true);
            ETTestHelper.setFeature("more_infinity", false);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
            player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack bow = new ItemStack(Items.BOW); // deliberately no Infinity
            player.getInventory().setStack(0, bow);
            Box search = player.getBoundingBox().expand(64.0);
            int arrowsBefore = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            try {
                bow.getItem().use(world, player, Hand.MAIN_HAND);
                bow.getItem().onStoppedUsing(bow, world, player, 0);
                int arrowsAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
                helper.assertTrue(arrowsAfter == arrowsBefore,
                    "A non-Infinity bow with no arrows must NOT fire even with the feature on (before=" + arrowsBefore
                        + ", after=" + arrowsAfter + ")");
                helper.assertTrue(bow.getDamage() == 0,
                    "A non-Infinity bow that fired nothing should take no durability (got " + bow.getDamage() + ")");
            } finally {
                ETTestHelper.setFeature("bow_infinity_fix", false);
            }
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // villagerTradeLimits: master switch gates both facets

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMasterSwitchGatesBothFacets(TestContext helper) {

        Map<String, String> wave3OriginalConfig = ETTestHelper.snapshotConfig("enchant_trade_max_uses",
            "enchant_trade_no_restock", "enchant_trade_restock", "villager_trade_limits");
        try {
            ETTestHelper.setFeature("villager_trade_limits", false);
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "1");
            ETTestHelper.setConfigValue("enchant_trade_restock", "false");
            ETTestHelper.setConfigValue("enchant_trade_no_restock", "sharpness");
            ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
            try {
                offer.use();
                offer.use();
                helper.assertFalse(offer.isDisabled(),
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
            helper.complete();

        } finally {
            ETTestHelper.restoreConfig(wave3OriginalConfig);
        }
    }

    // private helpers use the read-only ettesthelper

    /**
     * combines two stacks through a real anvil
     */
    private static ItemStack anvilCombine(TestContext helper, ItemStack first, ItemStack second) {
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, first, second);
        handler.updateResult();
        return handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
    }

    /**
     * spawns a fully healed zombie with protection four armor
     */
    private static ZombieEntity spawnProtectedZombie(TestContext helper) {
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            ItemStack armor = new ItemStack(switch (slot) {
                case HEAD -> Items.DIAMOND_HELMET;
                case CHEST -> Items.DIAMOND_CHESTPLATE;
                case LEGS -> Items.DIAMOND_LEGGINGS;
                case FEET -> Items.DIAMOND_BOOTS;
                default -> Items.DIAMOND_HELMET;
            });
            armor.addEnchantment(Enchantments.PROTECTION, 4);
            entity.equipStack(slot, armor);
        }
        entity.setHealth(entity.getMaxHealth());
        return entity;
    }

    private static float protectedZombieDamageDelta(TestContext helper, DamageSource source, float amount) {
        ZombieEntity zombie = spawnProtectedZombie(helper);
        float before = zombie.getHealth();
        zombie.damage(source, amount);
        return before - zombie.getHealth();
    }

    private static boolean containsEnchant(List<EnchantmentLevelEntry> entries, Enchantment enchantment) {
        for (EnchantmentLevelEntry entry : entries) {
            if (entry.enchantment == enchantment)
                return true;
        }
        return false;
    }

    /**
     * builds an inventory with an enchanted-book offer the payment occupies slot
     * zero
     */
    private static MerchantInventory buildMerchantInventory(PlayerEntity player, ItemStack sellBook) {
        SimpleMerchant merchant = new SimpleMerchant(player);
        TradeOfferList offers = new TradeOfferList();
        offers.add(new TradeOffer(new TradedItem(Items.EMERALD, 10), sellBook, 12, 1, 0.2f));
        merchant.setOffersFromServer(offers);
        MerchantInventory inv = new MerchantInventory(merchant);
        inv.setStack(0, new ItemStack(Items.EMERALD, 64));
        return inv;
    }
}
