package com.adibarra.enchanttweaker.test;

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
import net.minecraft.item.BowItem;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.MerchantInventory;
import net.minecraft.village.SimpleMerchant;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.GameMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TweakGameTest implements FabricGameTest {

    // xpScaling

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingEnabled(TestContext helper) {
        ETTestHelper.setFeature("xp_scaling", true);
        ETTestHelper.setConfigValue("xp_scaling_base", "7");
        ETTestHelper.setConfigValue("xp_scaling_step", "2");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        player.experienceLevel = 35;
        try {
            // base=7, step=2 -> 7 + 2*35 = 77
            int xp = player.getNextLevelExperience();
            helper.assertTrue(xp == 77,
                "XP scaling at level 35 should be 77 (got " + xp + ")");
        } finally {
            ETTestHelper.setFeature("xp_scaling", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingDisabled(TestContext helper) {
        ETTestHelper.setFeature("xp_scaling", false);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        player.experienceLevel = 35;
        // vanilla tier 3: 9*level - 158 = 9*35 - 158 = 157
        int xp = player.getNextLevelExperience();
        helper.assertTrue(xp == 157,
            "Vanilla XP at level 35 should be 157 (got " + xp + ")");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingCustomValues(TestContext helper) {
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingOverflowClamped(TestContext helper) {
        ETTestHelper.setFeature("xp_scaling", true);
        ETTestHelper.setConfigValue("xp_scaling_base", "7");
        // step * level overflows int math (2_000_000_000 * 2 = 4e9 > Integer.MAX_VALUE, wraps negative)
        ETTestHelper.setConfigValue("xp_scaling_step", "2000000000");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        player.experienceLevel = 2;
        try {
            // naive int (7 + 2_000_000_000 * 2) wraps negative; long math + clamp pins it to MAX_VALUE
            // clamp(7 + 2_000_000_000L*2, 1, MAX) = clamp(4_000_000_007, 1, 2_147_483_647) = 2_147_483_647
            int xp = player.getNextLevelExperience();
            helper.assertTrue(xp == Integer.MAX_VALUE,
                "XP scaling should clamp int overflow to exactly Integer.MAX_VALUE (got " + xp + ")");
        } finally {
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ETTestHelper.setFeature("xp_scaling", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingNegativeStepClamped(TestContext helper) {
        // a negative step can drive base + step*level below 1; the mixin clamps the lower bound to 1
        // (getNextLevelExperience must never return 0 or negative)
        ETTestHelper.setFeature("xp_scaling", true);
        ETTestHelper.setConfigValue("xp_scaling_base", "7");
        ETTestHelper.setConfigValue("xp_scaling_step", "-10");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        player.experienceLevel = 5;
        try {
            // 7 + (-10)*5 = -43 -> clamp to 1
            int xp = player.getNextLevelExperience();
            helper.assertTrue(xp == 1,
                "Negative step should clamp getNextLevelExperience to 1 (got " + xp + ")");
        } finally {
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ETTestHelper.setFeature("xp_scaling", false);
        }
        helper.complete();
    }

    // disableEnchantments

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsRandomSelection(TestContext helper) {
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsBookOffer(TestContext helper) {
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsMaxLevel(TestContext helper) {
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 0,
                "Sharpness max level should be 0 when disabled (got " + Enchantments.SHARPNESS.getMaxLevel() + ")");
            helper.assertTrue(Enchantments.SMITE.getMaxLevel() > 0,
                "Smite max level should be unchanged");
        } finally {
            ETTestHelper.setConfigValue("disable_enchantments", "");
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsEmpty(TestContext helper) {
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
    }

    // godArmor

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godArmorEnabled(TestContext helper) {
        ETTestHelper.setFeature("god_armor", true);
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
            "Protection should accept Blast Protection when enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godArmorDisabled(TestContext helper) {
        ETTestHelper.setFeature("god_armor", false);
        try {
            helper.assertTrue(!ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),
                "Protection should not accept Blast Protection when disabled");
        } finally {
            ETTestHelper.setFeature("god_armor", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godArmorAllTypes(TestContext helper) {
        ETTestHelper.setFeature("god_armor", true);
        // different types: all allowed
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION),       "Protection accepts Fire Protection");
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.FEATHER_FALLING),       "Protection accepts Feather Falling");
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.BLAST_PROTECTION),      "Protection accepts Blast Protection");
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.PROJECTILE_PROTECTION), "Protection accepts Projectile Protection");
        // same type: still blocked
        helper.assertTrue(!ETTestHelper.canAccept(Enchantments.PROTECTION, Enchantments.PROTECTION),           "Protection should not accept itself");
        helper.complete();
    }

    // godWeapons

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsEnabled(TestContext helper) {
        ETTestHelper.setFeature("god_weapons", true);
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SMITE),             "Sharpness should accept Smite when enabled");
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.BANE_OF_ARTHROPODS), "Sharpness should accept Bane when enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsDisabled(TestContext helper) {
        ETTestHelper.setFeature("god_weapons", false);
        try {
            helper.assertTrue(!ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SMITE),
                "Sharpness should not accept Smite when disabled");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
        }
        helper.complete();
    }

    // infiniteMending

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void infiniteMendingEnabled(TestContext helper) {
        ETTestHelper.setFeature("infinite_mending", true);
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.MENDING),
            "Infinity should accept Mending when enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void infiniteMendingDisabled(TestContext helper) {
        ETTestHelper.setFeature("infinite_mending", false);
        try {
            helper.assertTrue(!ETTestHelper.canAccept(Enchantments.INFINITY, Enchantments.MENDING),
                "Infinity should not accept Mending when disabled");
        } finally {
            ETTestHelper.setFeature("infinite_mending", false);
        }
        helper.complete();
    }

    // noMendingUnbreaking

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingEnabled(TestContext helper) {
        ETTestHelper.setFeature("no_mending_unbreaking", true);
        try {
            helper.assertFalse(ETTestHelper.canAccept(Enchantments.MENDING, Enchantments.UNBREAKING),
                "Mending should not accept Unbreaking when no_mending_unbreaking enabled");
        } finally {
            ETTestHelper.setFeature("no_mending_unbreaking", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingDisabled(TestContext helper) {
        ETTestHelper.setFeature("no_mending_unbreaking", false);
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.MENDING, Enchantments.UNBREAKING),
            "Mending should accept Unbreaking when no_mending_unbreaking disabled");
        helper.complete();
    }

    // multishotPiercing

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void multishotPiercingEnabled(TestContext helper) {
        ETTestHelper.setFeature("multishot_piercing", true);
        try {
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
                "Multishot should accept Piercing when enabled");
        } finally {
            ETTestHelper.setFeature("multishot_piercing", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void multishotPiercingReverse(TestContext helper) {
        // the mixin targets both MultishotEnchantment and PiercingEnchantment, so the pairing must
        // hold in both directions. canCombine() (the anvil's real gate) needs canAccept true both ways
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void multishotPiercingDisabled(TestContext helper) {
        ETTestHelper.setFeature("multishot_piercing", false);
        try {
            helper.assertTrue(!ETTestHelper.canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
                "Multishot should not accept Piercing when disabled");
        } finally {
            ETTestHelper.setFeature("multishot_piercing", false);
        }
        helper.complete();
    }

    // tridentWeapons

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsEnabled(TestContext helper) {
        ETTestHelper.setFeature("trident_weapons", true);
        ItemStack trident = new ItemStack(Items.TRIDENT);
        helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, trident), "Fire Aspect should be acceptable on trident");
        helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.KNOCKBACK, trident),   "Knockback should be acceptable on trident");
        helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, trident),     "Looting should be acceptable on trident");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsDisabled(TestContext helper) {
        ETTestHelper.setFeature("trident_weapons", false);
        ItemStack trident = new ItemStack(Items.TRIDENT);
        try {
            helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, trident), "Fire Aspect should not be acceptable on trident when disabled");
            helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.KNOCKBACK, trident),   "Knockback should not be acceptable on trident when disabled");
            helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.LOOTING, trident),     "Looting should not be acceptable on trident when disabled");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.complete();
    }

    // axeWeapons

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axeWeaponsEnabled(TestContext helper) {
        ETTestHelper.setFeature("axe_weapons", true);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, axe), "Fire Aspect should be acceptable on axe");
        helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.KNOCKBACK, axe),   "Knockback should be acceptable on axe");
        helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, axe),     "Looting should be acceptable on axe");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axeWeaponsDisabled(TestContext helper) {
        ETTestHelper.setFeature("axe_weapons", false);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        try {
            helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, axe), "Fire Aspect should not be acceptable on axe when disabled");
            helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.KNOCKBACK, axe),   "Knockback should not be acceptable on axe when disabled");
            helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.LOOTING, axe),     "Looting should not be acceptable on axe when disabled");
        } finally {
            ETTestHelper.setFeature("axe_weapons", false);
        }
        helper.complete();
    }

    // bowLooting

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowLootingEnabled(TestContext helper) {
        ETTestHelper.setFeature("bow_looting", true);
        ItemStack bow = new ItemStack(Items.BOW);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, bow),      "Looting should be acceptable on bow");
        helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, crossbow), "Looting should be acceptable on crossbow");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowLootingDisabled(TestContext helper) {
        ETTestHelper.setFeature("bow_looting", false);
        ItemStack bow = new ItemStack(Items.BOW);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        try {
            helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.LOOTING, bow),      "Looting should not be acceptable on bow when disabled");
            helper.assertTrue(!ETTestHelper.isAcceptableItem(Enchantments.LOOTING, crossbow), "Looting should not be acceptable on crossbow when disabled");
        } finally {
            ETTestHelper.setFeature("bow_looting", false);
        }
        helper.complete();
    }

    // axesNotTools

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axesNotToolsEnabled(TestContext helper) {
        ETTestHelper.setFeature("axes_not_tools", true);
        ServerWorld world = helper.getWorld();
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ZombieEntity target   = EntityType.ZOMBIE.create(world);
        ZombieEntity attacker = EntityType.ZOMBIE.create(world);
        axe.getItem().postHit(axe, target, attacker);
        helper.assertTrue(axe.getDamage() == 1,
            "Axe should take 1 durability with axes_not_tools enabled (got " + axe.getDamage() + ")");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axesNotToolsDisabled(TestContext helper) {
        ETTestHelper.setFeature("axes_not_tools", false);
        ServerWorld world = helper.getWorld();
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ZombieEntity target   = EntityType.ZOMBIE.create(world);
        ZombieEntity attacker = EntityType.ZOMBIE.create(world);
        try {
            axe.getItem().postHit(axe, target, attacker);
            helper.assertTrue(axe.getDamage() == 2,
                "Axe should take 2 durability with axes_not_tools disabled (got " + axe.getDamage() + ")");
        } finally {
            ETTestHelper.setFeature("axes_not_tools", false);
        }
        helper.complete();
    }

    // noThornsBacklash
    // @Constant(intValue=2) intercepts ItemStack.damage(2, user,...): the ARMOR durability hit
    // at level 7: 0.15*7=1.05 > 1.0 -> thorns always fires

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noThornsBacklashEnabled(TestContext helper) {
        ETTestHelper.setFeature("no_thorns_backlash", true);
        ZombieEntity user     = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ZombieEntity attacker = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        // equip user with a Thorns VII chestplate so chooseEquipmentWith finds it
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.addEnchantment(Enchantments.THORNS, 7);
        user.equipStack(EquipmentSlot.CHEST, chest);
        try {
            // gameplay: EnchantmentHelper reads the Thorns level (7) off the equipped chestplate and
            // routes to ThornsEnchantment.onUserDamaged; at level 7 (0.15*7 > 1) the backlash always fires
            EnchantmentHelper.onUserDamaged(user, attacker);
            // mixin replaces damage(2,...) with damage(0,...) -> armor is undamaged
            helper.assertTrue(user.getEquippedStack(EquipmentSlot.CHEST).getDamage() == 0,
                "Thorns armor should not lose durability when no_thorns_backlash enabled");
        } finally {
            ETTestHelper.setFeature("no_thorns_backlash", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noThornsBacklashDisabled(TestContext helper) {
        ETTestHelper.setFeature("no_thorns_backlash", false);
        ZombieEntity user     = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ZombieEntity attacker = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.addEnchantment(Enchantments.THORNS, 7);
        user.equipStack(EquipmentSlot.CHEST, chest);
        try {
            // gameplay: level read from the equipped chestplate (not a hardcoded 7)
            EnchantmentHelper.onUserDamaged(user, attacker);
            // vanilla damage(2,...) -> armor loses 2 durability
            helper.assertTrue(user.getEquippedStack(EquipmentSlot.CHEST).getDamage() == 2,
                "Thorns armor should lose 2 durability when no_thorns_backlash disabled (got "
                + user.getEquippedStack(EquipmentSlot.CHEST).getDamage() + ")");
        } finally {
            ETTestHelper.setFeature("no_thorns_backlash", false);
        }
        helper.complete();
    }

    // loyalVoidTridents

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsEnabled(TestContext helper) {
        ETTestHelper.setFeature("loyal_void_tridents", true);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsDisabled(TestContext helper) {
        ETTestHelper.setFeature("loyal_void_tridents", false);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
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
    }

    // noSoulSpeedBacklash
    // @Constant(intValue=1) intercepts ItemStack.damage(1,...) inside addSoulSpeedBoostIfNeeded
    // the method has a 4% random check before damaging boots. addTemporaryModifier throws if the
    // modifier already exists, so we call removeSoulSpeedBoost() before each attempt
    // 500 calls gives 1 - 0.96^500 ~ 100% probability of triggering the damage when disabled

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashEnabled(TestContext helper) {
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
            // with mixin enabled, damage(1,...) becomes damage(0,...), so boots never lose durability
            for (int i = 0; i < 500; i++) {
                removeBoost.invoke(zombie); // clear existing modifier so addSoulSpeedBoostIfNeeded can run
                addBoost.invoke(zombie);
            }
            helper.assertTrue(zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() == 0,
                "Soul Speed boots should not take damage when no_soul_speed_backlash enabled (got "
                + zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("no_soul_speed_backlash", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashDisabled(TestContext helper) {
        // false-failure probability (no boot damage across all 500 tries when disabled): 0.96^500 ~ 1.3e-9
        ETTestHelper.setFeature("no_soul_speed_backlash", false);
        helper.setBlockState(new BlockPos(0, 1, 0), Blocks.SOUL_SAND.getDefaultState());
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.GOLDEN_BOOTS);
        boots.addEnchantment(Enchantments.SOUL_SPEED, 1);
        zombie.equipStack(EquipmentSlot.FEET, boots);
        zombie.tick(); // land on soul sand
        try {
            Method removeBoost = LivingEntity.class.getDeclaredMethod("removeSoulSpeedBoost");
            removeBoost.setAccessible(true);
            Method addBoost = LivingEntity.class.getDeclaredMethod("addSoulSpeedBoostIfNeeded");
            addBoost.setAccessible(true);
            // 500 calls -> ~100% chance of hitting the 4% boot-damage RNG at least once
            for (int i = 0; i < 500; i++) {
                removeBoost.invoke(zombie); // clear modifier so next addSoulSpeedBoostIfNeeded can run
                addBoost.invoke(zombie);
                if (zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() > 0) break;
            }
            helper.assertTrue(zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() > 0,
                "Soul Speed boots should take damage when no_soul_speed_backlash disabled (got 0 after 500 calls)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("no_soul_speed_backlash", false);
        }
        helper.complete();
    }

    // betterMending
    // vanilla mending only repairs equipped items; BetterMending scans the whole inventory

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void betterMendingEnabled(TestContext helper) {
        ETTestHelper.setFeature("better_mending", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
        mendingSword.addEnchantment(Enchantments.MENDING, 1);
        mendingSword.setDamage(100);
        // slot 20: a general inventory slot that is neither hand nor armor
        player.getInventory().setStack(20, mendingSword);

        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 50);
        ETTestHelper.repairPlayerGears(orb, player, 50);

        helper.assertTrue(player.getInventory().getStack(20).getDamage() < 100,
            "BetterMending should repair inventory item (not just equipped items)");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void betterMendingDisabled(TestContext helper) {
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
    }

    // betterMending: mainhand priority

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void betterMendingMainHandPriority(TestContext helper) {
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
    }

    // bowInfinityFix

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixEnabled(TestContext helper) {
        ETTestHelper.setFeature("bow_infinity_fix", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        player.getInventory().setStack(0, bow);
        // no arrows in inventory: vanilla would block use()
        TypedActionResult<ItemStack> result = bow.getItem().use(world, player, Hand.MAIN_HAND);
        try {
            helper.assertTrue(result.getResult().isAccepted(),
                "BowInfinityFix should allow Infinity bow to fire without arrows");
        } finally {
            ETTestHelper.setFeature("bow_infinity_fix", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixDisabled(TestContext helper) {
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixNoInfinity(TestContext helper) {
        ETTestHelper.setFeature("bow_infinity_fix", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // bow WITHOUT Infinity: mixin should not activate
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixSuppressedByMoreInfinity(TestContext helper) {
        // when more_infinity is enabled, bow_infinity_fix should be suppressed at runtime
        ETTestHelper.setFeature("bow_infinity_fix", true);
        ETTestHelper.setFeature("more_infinity", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        player.getInventory().setStack(0, bow);
        // no arrows in inventory: bow_infinity_fix would normally allow this, but more_infinity takes precedence
        TypedActionResult<ItemStack> result = bow.getItem().use(world, player, Hand.MAIN_HAND);
        try {
            helper.assertFalse(result.getResult().isAccepted(),
                "BowInfinityFix should be suppressed when more_infinity is enabled");
        } finally {
            ETTestHelper.setFeature("more_infinity", false);
            ETTestHelper.setFeature("bow_infinity_fix", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixFiresArrow(TestContext helper) {
        ETTestHelper.setFeature("bow_infinity_fix", true);
        ETTestHelper.setFeature("more_infinity", false);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // reposition the mock player (created at world ORIGIN) into the loaded test structure so
        // world.spawnEntity lands in a loaded chunk and the arrow is queryable
        BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        player.getInventory().setStack(0, bow);
        // no arrows anywhere in the inventory
        Box search = player.getBoundingBox().expand(64.0);
        int arrowsBefore = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
        try {
            // full sequence: start drawing (use) then release fully drawn (remainingUseTicks = 0)
            bow.getItem().use(world, player, Hand.MAIN_HAND);
            bow.getItem().onStoppedUsing(bow, world, player, 0);

            int arrowsAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            helper.assertTrue(arrowsAfter == arrowsBefore + 1,
                "Infinity bow with no arrows should spawn exactly one arrow on release (before=" + arrowsBefore + ", after=" + arrowsAfter + ")");
            helper.assertTrue(player.getInventory().count(Items.ARROW) == 0,
                "Firing an Infinity bow with no arrows must not add or consume inventory arrows (got " + player.getInventory().count(Items.ARROW) + ")");
            helper.assertTrue(bow.getDamage() == 1,
                "Bow should take 1 durability after firing (confirms shootAll ran; got " + bow.getDamage() + ")");
        } finally {
            ETTestHelper.setFeature("bow_infinity_fix", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixDisabledDoesNotFire(TestContext helper) {
        // with the feature OFF, releasing an Infinity bow with no arrows must NOT spawn an arrow
        // (vanilla getProjectileType stays EMPTY and onStoppedUsing bails)
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
                "Infinity bow with feature disabled must NOT fire without arrows (before=" + arrowsBefore + ", after=" + arrowsAfter + ")");
        } finally {
            ETTestHelper.setFeature("bow_infinity_fix", false);
        }
        helper.complete();
    }

    // godWeapons: all combinations

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsAllCombinations(TestContext helper) {
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsSelfReject(TestContext helper) {
        ETTestHelper.setFeature("god_weapons", true);
        helper.assertFalse(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.SHARPNESS),
            "Sharpness should NOT accept itself");
        helper.assertFalse(ETTestHelper.canAccept(Enchantments.SMITE, Enchantments.SMITE),
            "Smite should NOT accept itself");
        helper.assertFalse(ETTestHelper.canAccept(Enchantments.BANE_OF_ARTHROPODS, Enchantments.BANE_OF_ARTHROPODS),
            "Bane should NOT accept itself");
        helper.complete();
    }

    // loyalVoidTridents: edge cases

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsNoLoyalty(TestContext helper) {
        // trident WITHOUT Loyalty should NOT be rescued from void
        ETTestHelper.setFeature("loyal_void_tridents", true);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        // no Loyalty enchantment
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsStopsDescending(TestContext helper) {
        // when the mixin fires, it sets dealtDamage=true and zeros velocity at the world bottom
        // vanilla's loyalty logic then takes over and begins returning the trident upward
        // after a few ticks the trident should move UP (positive Y velocity), not keep falling
        ETTestHelper.setFeature("loyal_void_tridents", true);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 3);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        double startY = world.getBottomY() - 5.0;
        trident.setPos(abs.getX(), startY, abs.getZ());
        trident.setVelocity(0, -2.0, 0);
        world.spawnEntity(trident);
        // tick a few times to let loyalty return logic kick in
        for (int i = 0; i < 5; i++) trident.tick();
        helper.assertTrue(trident.getY() > startY,
            "Loyal trident should start returning upward after being rescued from void (Y=" + trident.getY() + ", started at " + startY + ")");
        helper.complete();
    }

    // axesNotTools: multiple hits

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axesNotToolsMultipleHits(TestContext helper) {
        ETTestHelper.setFeature("axes_not_tools", true);
        ServerWorld world = helper.getWorld();
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ZombieEntity target   = EntityType.ZOMBIE.create(world);
        ZombieEntity attacker = EntityType.ZOMBIE.create(world);
        // 3 hits should cause 3 durability (not 6)
        axe.getItem().postHit(axe, target, attacker);
        axe.getItem().postHit(axe, target, attacker);
        axe.getItem().postHit(axe, target, attacker);
        helper.assertTrue(axe.getDamage() == 3,
            "3 axe hits with axes_not_tools should cause 3 durability damage (got " + axe.getDamage() + ")");
        helper.complete();
    }

    private static final float PB_REDUCED = 3.6f; // getInflictedDamage(10, 16)

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassMagicEnabled(TestContext helper) {
        ETTestHelper.setFeature("protection_bypass_enabled", true);
        ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic");
        try {
            float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(), 10.0f);
            helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                "Magic damage should bypass protection entirely via a real hit (health delta " + delta + ", expected 10.0)");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
            ETTestHelper.setFeature("protection_bypass_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassWitherEnabled(TestContext helper) {
        ETTestHelper.setFeature("protection_bypass_enabled", true);
        ETTestHelper.setConfigValue("protection_bypass_types", "wither");
        try {
            float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().wither(), 10.0f);
            helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                "Wither damage should bypass protection entirely via a real hit (health delta " + delta + ", expected 10.0)");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
            ETTestHelper.setFeature("protection_bypass_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassDisabledStillReduces(TestContext helper) {
        ETTestHelper.setFeature("protection_bypass_enabled", false);
        ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic");
        try {
            float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(), 10.0f);
            helper.assertTrue(Math.abs(delta - PB_REDUCED) < 0.05f,
                "Magic damage with bypass disabled should be reduced by protection (health delta " + delta + ", expected ~" + PB_REDUCED + ")");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassGenericNotAffected(TestContext helper) {
        // generic is in bypasses_armor (armor points ignored) but is NOT in the configured bypass
        // list, so the enchant reduction still applies: a real hit lands the reduced 3.6, not 10
        ETTestHelper.setFeature("protection_bypass_enabled", true);
        ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic,wither,dragon_breath");
        try {
            float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().generic(), 10.0f);
            helper.assertTrue(Math.abs(delta - PB_REDUCED) < 0.05f,
                "Generic damage should still be reduced by protection (health delta " + delta + ", expected ~" + PB_REDUCED + ")");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
            ETTestHelper.setFeature("protection_bypass_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassModdedColonBranch(TestContext helper) {
        // a namespaced id ("minecraft:magic") must resolve and bypass; an unknown namespaced id
        // ("nonexistent:foo") must parse without crashing and simply never match, so damage reduces
        ETTestHelper.setFeature("protection_bypass_enabled", true);
        try {
            ETTestHelper.setConfigValue("protection_bypass_types", "minecraft:magic");
            float bypassed = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(), 10.0f);
            helper.assertTrue(Math.abs(bypassed - 10.0f) < 0.05f,
                "'minecraft:magic' should resolve and bypass protection (health delta " + bypassed + ", expected 10.0)");

            ETTestHelper.setConfigValue("protection_bypass_types", "nonexistent:foo");
            float reduced = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().magic(), 10.0f);
            helper.assertTrue(Math.abs(reduced - PB_REDUCED) < 0.05f,
                "Unknown namespaced id should not crash and should not bypass (health delta " + reduced + ", expected ~" + PB_REDUCED + ")");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
            ETTestHelper.setFeature("protection_bypass_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassDragonBreath(TestContext helper) {
        ETTestHelper.setFeature("protection_bypass_enabled", true);
        ETTestHelper.setConfigValue("protection_bypass_types", "magic,dragon_breath");
        try {
            float delta = protectedZombieDamageDelta(helper, helper.getWorld().getDamageSources().dragonBreath(), 10.0f);
            helper.assertTrue(Math.abs(delta - 10.0f) < 0.05f,
                "Dragon breath should bypass protection when listed (health delta " + delta + ", expected 10.0)");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
            ETTestHelper.setFeature("protection_bypass_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassWhitespaceTokens(TestContext helper) {
        // whitespace, malformed, and trailing-empty tokens must be skipped without discarding valid entries
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassPrecedesMoreProtection(TestContext helper) {
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.96");
        ETTestHelper.setFeature("protection_bypass_enabled", true);
        ETTestHelper.setConfigValue("protection_bypass_types", "magic");
        try {
            float bypassed = protectedZombieDamageDelta(
                helper, helper.getWorld().getDamageSources().magic(), 10.0f);
            helper.assertTrue(Math.abs(bypassed - 10.0f) < 0.05f,
                "configured damage must bypass MoreProtection as well as vanilla Protection");

            float protectedDamage = protectedZombieDamageDelta(
                helper, helper.getWorld().getDamageSources().generic(), 10.0f);
            float expected = (float)(10.0 * Math.pow(0.96, 16));
            helper.assertTrue(Math.abs(protectedDamage - expected) < 0.05f,
                "unlisted damage should retain MoreProtection scaling (got "
                    + protectedDamage + ", expected ~" + expected + ")");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
            ETTestHelper.setFeature("protection_bypass_enabled", false);
            ETTestHelper.setFeature("more_protection", false);
        }
        helper.complete();
    }

    // villagerTradeLimits

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMaxUsesEnabled(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        offer.use(); offer.use(); offer.use();
        try {
            helper.assertTrue(offer.isDisabled(),
                "Enchantment trade should be disabled after 3 uses (max_uses=3)");
        } finally {
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMaxUsesDisabled(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", false);
        ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        offer.use(); offer.use(); offer.use();
        try {
            helper.assertFalse(offer.isDisabled(),
                "Enchantment trade should NOT be disabled when feature is off (uses=3, vanilla maxUses=12)");
        } finally {
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsPerEnchantOverride(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
        ETTestHelper.setConfigValue("trade_sharpness", "2");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        offer.use(); offer.use();
        try {
            helper.assertTrue(offer.isDisabled(),
                "Sharpness trade should be disabled after 2 uses (trade_sharpness=2)");
        } finally {
            ETTestHelper.setConfigValue("trade_sharpness", "-1");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsDisabledEnchantment(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("trade_mending", "0");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        try {
            helper.assertTrue(offer.isDisabled(),
                "Mending trade should be disabled immediately when trade_mending=0");
        } finally {
            ETTestHelper.setConfigValue("trade_mending", "-1");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsRestockEnabled(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_restock", "true");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        offer.use(); offer.use();
        offer.resetUses();
        try {
            helper.assertTrue(offer.getUses() == 0,
                "Enchantment trade should restock when enchant_trade_restock=true (uses=" + offer.getUses() + ")");
        } finally {
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsRestockDisabled(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_restock", "false");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        offer.use(); offer.use();
        offer.resetUses();
        try {
            helper.assertTrue(offer.getUses() == 2,
                "Enchantment trade should NOT restock when enchant_trade_restock=false (uses=" + offer.getUses() + ")");
        } finally {
            ETTestHelper.setConfigValue("enchant_trade_restock", "true");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsNoRestockList(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_restock", "true");
        ETTestHelper.setConfigValue("enchant_trade_no_restock", "sharpness");
        ItemStack sharpBook = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer sharpOffer = new TradeOffer(new TradedItem(Items.EMERALD, 10), sharpBook, 12, 1, 0.2f);
        sharpOffer.use();
        sharpOffer.resetUses();
        ItemStack mendingBook = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1));
        TradeOffer mendingOffer = new TradeOffer(new TradedItem(Items.EMERALD, 10), mendingBook, 12, 1, 0.2f);
        mendingOffer.use();
        mendingOffer.resetUses();
        try {
            helper.assertTrue(sharpOffer.getUses() == 1,
                "Sharpness trade in no_restock list should NOT restock (uses=" + sharpOffer.getUses() + ")");
            helper.assertTrue(mendingOffer.getUses() == 0,
                "Mending trade NOT in no_restock list should restock (uses=" + mendingOffer.getUses() + ")");
        } finally {
            ETTestHelper.setConfigValue("enchant_trade_no_restock", "");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsNonEnchantedUnaffected(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_max_uses", "1");
        ETTestHelper.setConfigValue("enchant_trade_restock", "false");
        // non-enchanted trade: emeralds for wheat
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 1), new ItemStack(Items.WHEAT, 6), 12, 1, 0.2f);
        offer.use(); offer.use(); offer.use();
        offer.resetUses();
        try {
            helper.assertTrue(offer.getUses() == 0,
                "Non-enchanted trade should still restock normally (uses=" + offer.getUses() + ")");
            helper.assertFalse(offer.isDisabled(),
                "Non-enchanted trade should not be affected by enchant_trade_max_uses");
        } finally {
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setConfigValue("enchant_trade_restock", "true");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    // bowLooting: guard negatives

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowLootingIdentityGuardNegative(TestContext helper) {
        // the mixin only force-accepts LOOTING on bows/crossbows. If the `== LOOTING` identity guard
        // were dropped, EVERY enchant would become acceptable on a bow. verify that Sharpness stays rejected
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowLootingNonBowGuard(TestContext helper) {
        // feature ON, but Looting on a non-bow item must fall through to vanilla (rejected)
        ETTestHelper.setFeature("bow_looting", true);
        try {
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.DIAMOND_PICKAXE)),
                "Looting must NOT become acceptable on a pickaxe via bow_looting");
        } finally {
            ETTestHelper.setFeature("bow_looting", false);
        }
        helper.complete();
    }

    // tridentWeapons: DamageEnchantment branch + guards

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsDamageEnchantBranch(TestContext helper) {
        // the mixin's `instanceof DamageEnchantment` branch makes Sharpness/Smite/Bane acceptable on
        // a trident (broader than the FIRE_ASPECT/KNOCKBACK/LOOTING javadoc). verify that behavior
        ItemStack trident = new ItemStack(Items.TRIDENT);
        ETTestHelper.setFeature("trident_weapons", true);
        try {
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.SHARPNESS, trident), "Sharpness acceptable on trident when enabled");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.SMITE, trident),     "Smite acceptable on trident when enabled");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.BANE_OF_ARTHROPODS, trident), "Bane acceptable on trident when enabled");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        ETTestHelper.setFeature("trident_weapons", false);
        try {
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.SHARPNESS, trident), "Sharpness NOT acceptable on trident when disabled");
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.SMITE, trident),     "Smite NOT acceptable on trident when disabled");
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.BANE_OF_ARTHROPODS, trident), "Bane NOT acceptable on trident when disabled");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsNonTridentGuardAndFallthrough(TestContext helper) {
        ETTestHelper.setFeature("trident_weapons", true);
        try {
            // non-trident item: mixin bails, vanilla rejects Fire Aspect on a pickaxe
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.FIRE_ASPECT, new ItemStack(Items.DIAMOND_PICKAXE)),
                "Fire Aspect must NOT be acceptable on a pickaxe via trident_weapons");
            // on a trident, an enchant outside the allowed set (Sweeping Edge, not a DamageEnchantment)
            // falls through to vanilla, which rejects it
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.SWEEPING_EDGE, new ItemStack(Items.TRIDENT)),
                "Sweeping Edge must fall through to vanilla and be rejected on a trident");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.complete();
    }

    // weapon tweaks: cross-tweak Looting acceptability

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void weaponTweaksCrossLootingAcceptability(TestContext helper) {
        // with axe/bow/trident weapon tweaks all on, Looting becomes acceptable on all three
        // target families but still not on an unrelated pickaxe
        ETTestHelper.setFeature("axe_weapons", true);
        ETTestHelper.setFeature("bow_looting", true);
        ETTestHelper.setFeature("trident_weapons", true);
        try {
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.DIAMOND_AXE)), "Looting acceptable on axe");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.BOW)),         "Looting acceptable on bow");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.TRIDENT)),     "Looting acceptable on trident");
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.DIAMOND_PICKAXE)), "Looting NOT acceptable on pickaxe");
        } finally {
            ETTestHelper.setFeature("axe_weapons", false);
            ETTestHelper.setFeature("bow_looting", false);
            ETTestHelper.setFeature("trident_weapons", false);
        }
        helper.complete();
    }

    // axeWeapons: guard negatives + enchant-table presence

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axeWeaponsGuardNegatives(TestContext helper) {
        ETTestHelper.setFeature("axe_weapons", true);
        try {
            // non-axe: Looting stays rejected on a pickaxe
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, new ItemStack(Items.DIAMOND_PICKAXE)),
                "Looting must NOT be acceptable on a pickaxe via axe_weapons");
            // over-grant boundary: the mixin only grants Fire Aspect/Knockback/Looting, not Protection
            helper.assertFalse(ETTestHelper.isAcceptableItem(Enchantments.PROTECTION, new ItemStack(Items.DIAMOND_AXE)),
                "Protection must NOT be granted on an axe by axe_weapons");
        } finally {
            ETTestHelper.setFeature("axe_weapons", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void axeWeaponsEnchantTablePresence(TestContext helper) {
        ETTestHelper.setFeature("axe_weapons", true);
        try {
            List<EnchantmentLevelEntry> axeEntries = EnchantmentHelper.getPossibleEntries(
                helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.DIAMOND_AXE), false);
            helper.assertTrue(containsEnchant(axeEntries, Enchantments.LOOTING),     "Looting must be offered on an axe from the enchant table when axe_weapons is on");
            helper.assertTrue(containsEnchant(axeEntries, Enchantments.FIRE_ASPECT), "Fire Aspect must be offered on an axe from the enchant table when axe_weapons is on");
            helper.assertTrue(containsEnchant(axeEntries, Enchantments.KNOCKBACK),   "Knockback must be offered on an axe from the enchant table when axe_weapons is on");

            // guard: the mixin only fires for AxeItem, so an unrelated pickaxe still fails isAcceptableItem
            // (its supportedItems tag is swords-only) and is never offered these enchants
            List<EnchantmentLevelEntry> pickEntries = EnchantmentHelper.getPossibleEntries(
                helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.DIAMOND_PICKAXE), false);
            helper.assertFalse(containsEnchant(pickEntries, Enchantments.LOOTING),     "Looting must NOT be offered on a pickaxe via axe_weapons");
            helper.assertFalse(containsEnchant(pickEntries, Enchantments.FIRE_ASPECT), "Fire Aspect must NOT be offered on a pickaxe via axe_weapons");
            helper.assertFalse(containsEnchant(pickEntries, Enchantments.KNOCKBACK),   "Knockback must NOT be offered on a pickaxe via axe_weapons");
        } finally {
            ETTestHelper.setFeature("axe_weapons", false);
        }

        // feature-off mirror: with axe_weapons OFF the mixin no-ops, vanilla isAcceptableItem rejects the
        // axe (not in the swords tag), and none of the three are offered on an axe from the table
        List<EnchantmentLevelEntry> offEntries = EnchantmentHelper.getPossibleEntries(
            helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.DIAMOND_AXE), false);
        helper.assertFalse(containsEnchant(offEntries, Enchantments.LOOTING),     "Looting must be absent from axe enchant-table entries when axe_weapons is off");
        helper.assertFalse(containsEnchant(offEntries, Enchantments.FIRE_ASPECT), "Fire Aspect must be absent from axe enchant-table entries when axe_weapons is off");
        helper.assertFalse(containsEnchant(offEntries, Enchantments.KNOCKBACK),   "Knockback must be absent from axe enchant-table entries when axe_weapons is off");
        helper.complete();
    }

    // godWeapons: fall-through, Impaling, removeConflicts

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsNonDamageEnchantFallthrough(TestContext helper) {
        // the mixin only overrides canAccept when `other instanceof DamageEnchantment`. Unrelated
        // enchants must fall through to vanilla's default (compatible), unchanged
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsImpaling(TestContext helper) {
        // impaling is the 4th DamageEnchantment; it must combine with the others but not itself
        ETTestHelper.setFeature("god_weapons", true);
        try {
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.SHARPNESS, Enchantments.IMPALING), "Sharpness accepts Impaling");
            helper.assertTrue(ETTestHelper.canAccept(Enchantments.IMPALING, Enchantments.SMITE),     "Impaling accepts Smite");
            helper.assertFalse(ETTestHelper.canAccept(Enchantments.IMPALING, Enchantments.IMPALING), "Impaling must NOT accept itself");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsRemoveConflicts(TestContext helper) {
        EnchantmentLevelEntry picked = new EnchantmentLevelEntry(Enchantments.SMITE, 1);

        ETTestHelper.setFeature("god_weapons", true);
        try {
            List<EnchantmentLevelEntry> list = new ArrayList<>();
            list.add(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            EnchantmentHelper.removeConflicts(list, picked);
            helper.assertTrue(list.size() == 1,
                "Sharpness should survive removeConflicts against Smite when god_weapons on (size " + list.size() + ")");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
        }

        ETTestHelper.setFeature("god_weapons", false);
        try {
            List<EnchantmentLevelEntry> list = new ArrayList<>();
            list.add(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            EnchantmentHelper.removeConflicts(list, picked);
            helper.assertTrue(list.isEmpty(),
                "Sharpness should be removed by removeConflicts against Smite when god_weapons off (size " + list.size() + ")");
        } finally {
            ETTestHelper.setFeature("god_weapons", false);
        }
        helper.complete();
    }

    // infiniteMending: real anvil merge

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void infiniteMendingAnvilMerge(TestContext helper) {
        // real anvil: an Infinity bow + a Mending book must produce an item carrying BOTH enchants
        ETTestHelper.setFeature("infinite_mending", true);
        try {
            ItemStack bow = new ItemStack(Items.BOW);
            bow.addEnchantment(Enchantments.INFINITY, 1);
            ItemStack mendingBook = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1));
            ItemStack out = anvilCombine(helper, bow, mendingBook);
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.INFINITY, out) == 1,
                "Merged bow should keep Infinity 1 (got " + EnchantmentHelper.getLevel(Enchantments.INFINITY, out) + ")");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.MENDING, out) == 1,
                "Merged bow should gain Mending 1 (got " + EnchantmentHelper.getLevel(Enchantments.MENDING, out) + ")");
        } finally {
            ETTestHelper.setFeature("infinite_mending", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void infiniteMendingAnvilMergeDisabled(TestContext helper) {
        ETTestHelper.setFeature("infinite_mending", false);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        ItemStack mendingBook = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1));
        ItemStack out = anvilCombine(helper, bow, mendingBook);
        helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.MENDING, out) == 0,
            "Vanilla anvil must NOT merge Mending onto an Infinity bow when disabled (got "
            + EnchantmentHelper.getLevel(Enchantments.MENDING, out) + ")");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void infiniteMendingNoOverApplication(TestContext helper) {
        // the mixin only unlocks the Infinity<->Mending pair; it must not make Infinity accept itself
        // or spuriously reject other normally-compatible enchants
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
    }

    // noMendingUnbreaking: real anvil combine

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingAnvilBlocks(TestContext helper) {
        // real anvil: a Mending pickaxe + Unbreaking book must NOT coexist. Unbreaking is the only
        // transferable enchant, so it is dropped and the output carries no Unbreaking
        ETTestHelper.setFeature("no_mending_unbreaking", true);
        try {
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.addEnchantment(Enchantments.MENDING, 1);
            ItemStack unbreakingBook = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.UNBREAKING, 3));
            ItemStack out = anvilCombine(helper, pick, unbreakingBook);
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.UNBREAKING, out) == 0,
                "Unbreaking must NOT transfer onto a Mending pickaxe when no_mending_unbreaking on (got "
                + EnchantmentHelper.getLevel(Enchantments.UNBREAKING, out) + ")");
        } finally {
            ETTestHelper.setFeature("no_mending_unbreaking", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingAnvilAllowsWhenDisabled(TestContext helper) {
        ETTestHelper.setFeature("no_mending_unbreaking", false);
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.addEnchantment(Enchantments.MENDING, 1);
        ItemStack unbreakingBook = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.UNBREAKING, 3));
        ItemStack out = anvilCombine(helper, pick, unbreakingBook);
        helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.MENDING, out) == 1,
            "Vanilla anvil should keep Mending 1 (got " + EnchantmentHelper.getLevel(Enchantments.MENDING, out) + ")");
        helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.UNBREAKING, out) == 3,
            "Vanilla anvil should add Unbreaking 3 (got " + EnchantmentHelper.getLevel(Enchantments.UNBREAKING, out) + ")");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noMendingUnbreakingFallthroughUnrelated(TestContext helper) {
        // the tweak blocks only the Mending<->Unbreaking pair. An unrelated book (Efficiency) must
        // still merge onto a Mending pickaxe
        ETTestHelper.setFeature("no_mending_unbreaking", true);
        try {
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.addEnchantment(Enchantments.MENDING, 1);
            ItemStack efficiencyBook = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.EFFICIENCY, 3));
            ItemStack out = anvilCombine(helper, pick, efficiencyBook);
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.MENDING, out) == 1,
                "Mending should be preserved (got " + EnchantmentHelper.getLevel(Enchantments.MENDING, out) + ")");
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, out) == 3,
                "Efficiency should merge (unrelated pair) (got " + EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, out) + ")");
        } finally {
            ETTestHelper.setFeature("no_mending_unbreaking", false);
        }
        helper.complete();
    }

    // disableEnchantments: generation paths

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsGetPossibleEntries(TestContext helper) {
        // enchant-table generation must exclude a disabled enchant (getMaxLevel==0 +
        // isAvailableForRandomSelection==false), while a non-disabled control remains
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
        try {
            List<EnchantmentLevelEntry> entries = EnchantmentHelper.getPossibleEntries(
                helper.getWorld().getEnabledFeatures(), 30, sword, true);
            helper.assertFalse(containsEnchant(entries, Enchantments.SHARPNESS),
                "Disabled Sharpness must be absent from enchant-table entries");
            helper.assertTrue(containsEnchant(entries, Enchantments.SMITE),
                "Non-disabled Smite should remain in enchant-table entries");
        } finally {
            ETTestHelper.setConfigValue("disable_enchantments", "");
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        // feature-off mirror: Sharpness returns
        List<EnchantmentLevelEntry> mirror = EnchantmentHelper.getPossibleEntries(
            helper.getWorld().getEnabledFeatures(), 30, sword, true);
        helper.assertTrue(containsEnchant(mirror, Enchantments.SHARPNESS),
            "Sharpness should be present again once the feature is off");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsMultiEntry(TestContext helper) {
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "sharpness,mending");
        try {
            helper.assertTrue(Enchantments.SHARPNESS.getMaxLevel() == 0, "Sharpness disabled (maxLevel 0)");
            helper.assertTrue(Enchantments.MENDING.getMaxLevel() == 0,   "Mending disabled (maxLevel 0)");
            helper.assertTrue(Enchantments.SMITE.getMaxLevel() > 0,      "Smite unaffected");
        } finally {
            ETTestHelper.setConfigValue("disable_enchantments", "");
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsBookFactory(TestContext helper) {
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
        Entity anchor = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        Random random = helper.getWorld().getRandom();
        try {
            Class<?> cls = Class.forName("net.minecraft.village.TradeOffers$EnchantBookFactory");
            Constructor<?> ctor = cls.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            Object factory = ctor.newInstance(1);
            Method create = cls.getDeclaredMethod("create", Entity.class, Random.class);
            create.setAccessible(true);
            boolean sawSharpness = false;
            boolean sawControl = false;
            for (int i = 0; i < 500; i++) {
                TradeOffer offer = (TradeOffer) create.invoke(factory, anchor, random);
                var enchants = EnchantmentHelper.getEnchantments(offer.getSellItem());
                if (enchants.getLevel(Enchantments.SHARPNESS) > 0) sawSharpness = true;
                if (enchants.getLevel(Enchantments.SMITE) > 0) sawControl = true;
            }
            helper.assertFalse(sawSharpness,
                "A fresh EnchantBookFactory built after disabling Sharpness must never offer a Sharpness book");
            helper.assertTrue(sawControl,
                "A non-disabled control (Smite) should appear across 500 generated offers");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setConfigValue("disable_enchantments", "");
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsWhitespaceTrimming(TestContext helper) {
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", " sharpness , , mending ,");
        try {
            helper.assertFalse(Enchantments.SHARPNESS.isAvailableForRandomSelection(), "Trimmed 'sharpness' disabled");
            helper.assertFalse(Enchantments.MENDING.isAvailableForRandomSelection(),   "Trimmed 'mending' disabled");
            helper.assertTrue(Enchantments.SMITE.isAvailableForRandomSelection(),      "Smite still available");
        } finally {
            ETTestHelper.setConfigValue("disable_enchantments", "");
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsNamespacedInvalid(TestContext helper) {
        // the disable list matches on bare enchant PATH, so a namespaced id never matches and an
        // unknown id is a harmless no-op (no crash)
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        try {
            ETTestHelper.setConfigValue("disable_enchantments", "minecraft:sharpness");
            helper.assertTrue(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                "'minecraft:sharpness' should NOT match the bare-path list, so Sharpness stays available");

            ETTestHelper.setConfigValue("disable_enchantments", "notarealenchant");
            helper.assertTrue(Enchantments.SHARPNESS.isAvailableForRandomSelection(),
                "An unknown id should be a harmless no-op");
        } finally {
            ETTestHelper.setConfigValue("disable_enchantments", "");
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsFeatureGateOff(TestContext helper) {
        // a populated list with the master switch OFF must not disable anything
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
    }

    // xpScaling: real leveling + boundaries

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingRealLeveling(TestContext helper) {
        // drive the real consumer PlayerEntity.addExperience, which repeatedly reads the mixin-overridden
        // getNextLevelExperience. base=7, step=2: L0 needs 7 to reach L1, then 9 to reach L2
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingZeroBoundary(TestContext helper) {
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingLevelZero(TestContext helper) {
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingDefaultFallback(TestContext helper) {
        // non-numeric base falls back to the default 7 (step 2): L10 -> 7 + 2*10 = 27
        ETTestHelper.setFeature("xp_scaling", true);
        ETTestHelper.setConfigValue("xp_scaling_base", "abc");
        ETTestHelper.setConfigValue("xp_scaling_step", "2");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        player.experienceLevel = 10;
        try {
            int xp = player.getNextLevelExperience();
            helper.assertTrue(xp == 27,
                "Non-numeric base should fall back to 7 -> 7 + 2*10 = 27 (got " + xp + ")");
        } finally {
            ETTestHelper.setConfigValue("xp_scaling_base", "7");
            ETTestHelper.setConfigValue("xp_scaling_step", "2");
            ETTestHelper.setFeature("xp_scaling", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingBeyondIntRange(TestContext helper) {
        // a step beyond int range fails to parse and falls back to the default 2 (NOT a clamp):
        // l2 -> 7 + 2*2 = 11
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void xpScalingOffGateMidTier(TestContext helper) {
        // feature off -> vanilla tier-2 formula (15..29): 37 + (level-15)*5. L20 -> 62
        ETTestHelper.setFeature("xp_scaling", false);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        player.experienceLevel = 20;
        int xp = player.getNextLevelExperience();
        helper.assertTrue(xp == 62,
            "Vanilla XP at L20 should be 62 (got " + xp + ")");
        helper.complete();
    }

    // loyalVoidTridents: real void positions

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsAboveBottomNotRescued(TestContext helper) {
        // above the world bottom the mixin's `getY() <= bottomY` guard is false: no rescue, and the
        // trident keeps falling. Exercises the false-side of the threshold
        ETTestHelper.setFeature("loyal_void_tridents", true);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
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
            helper.assertTrue(trident.getY() < startY,
                "A trident above the world bottom should keep falling (Y=" + trident.getY() + ", started " + startY + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("loyal_void_tridents", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsDisabledLostToVoid(TestContext helper) {
        // feature OFF: a loyalty trident dropped below the void-kill plane (bottomY-64) is discarded
        // by vanilla on the next tick
        ETTestHelper.setFeature("loyal_void_tridents", false);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsEnabledSurvivesVoid(TestContext helper) {
        // feature ON: a loyalty trident at/below the world bottom is rescued (velocity zeroed) and
        // then climbs back toward its living owner instead of being discarded
        ETTestHelper.setFeature("loyal_void_tridents", true);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 3);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        BlockPos abs = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        double startY = world.getBottomY() - 1.0;
        trident.setPos(abs.getX(), startY, abs.getZ());
        trident.setVelocity(0, -2.0, 0);
        world.spawnEntity(trident);
        try {
            for (int i = 0; i < 10; i++) trident.tick();
            helper.assertFalse(trident.isRemoved(),
                "A rescued loyalty trident must NOT be discarded to the void");
            helper.assertTrue(trident.getY() > startY,
                "A rescued loyalty trident should climb back toward its owner (Y=" + trident.getY() + ", started " + startY + ")");
        } finally {
            ETTestHelper.setFeature("loyal_void_tridents", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsExactBoundary(TestContext helper) {
        // exact boundary Y == bottomY must trigger the rescue, guarding the `<=` comparison
        ETTestHelper.setFeature("loyal_void_tridents", true);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.LOYALTY, 1);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
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
    }

    // bowInfinityFix: short-draw bail

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixShortDrawBail(TestContext helper) {
        // releasing an Infinity bow that was barely drawn (pull < 0.1) must NOT fire, even with the
        // fix on: onStoppedUsing bails before the getProjectileType substitution matters
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
            // release immediately: remainingUseTicks == maxUseTime -> pull progress ~0 < 0.1
            bow.getItem().onStoppedUsing(bow, world, player, bow.getMaxUseTime());
            int arrowsAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            helper.assertTrue(arrowsAfter == arrowsBefore,
                "A barely-drawn Infinity bow must NOT fire (before=" + arrowsBefore + ", after=" + arrowsAfter + ")");
            helper.assertTrue(bow.getDamage() == 0,
                "A barely-drawn bow should take no durability damage (got " + bow.getDamage() + ")");
        } finally {
            ETTestHelper.setFeature("bow_infinity_fix", false);
        }
        helper.complete();
    }

    // villagerTradeLimits: most-restrictive-wins + more real paths

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMostRestrictiveWins(TestContext helper) {
        // a multi-enchant sell item takes the lowest per-enchant limit: min(sharpness=5, looting=2)=2
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
            helper.assertFalse(offer.isDisabled(),
                "After 1 use the trade should still be enabled (min limit 2)");
            offer.use();
            helper.assertTrue(offer.isDisabled(),
                "After 2 uses the most-restrictive limit (min(5,2)=2) should disable the trade");
        } finally {
            ETTestHelper.setConfigValue("trade_sharpness", "-1");
            ETTestHelper.setConfigValue("trade_looting", "-1");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMostRestrictiveZeroDisables(TestContext helper) {
        // any per-enchant limit of 0 disables the whole multi-enchant trade at 0 uses
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsGlobalZeroDisablesAll(TestContext helper) {
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
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsCapsAtVanillaMaxUses(TestContext helper) {
        // a configured limit above the vanilla maxUses never overrides vanilla: the trade disables
        // at vanilla maxUses (12), not the configured 50
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
        ETTestHelper.setConfigValue("trade_sharpness", "50");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        try {
            for (int i = 0; i < 11; i++) offer.use();
            helper.assertFalse(offer.isDisabled(),
                "After 11 uses (< vanilla maxUses 12) the trade should still be enabled");
            offer.use();
            helper.assertTrue(offer.isDisabled(),
                "After 12 uses the vanilla maxUses cap should disable the trade");
        } finally {
            ETTestHelper.setConfigValue("trade_sharpness", "-1");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsUnderLimitEnabled(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_max_uses", "3");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        offer.use(); offer.use();
        try {
            helper.assertFalse(offer.isDisabled(),
                "2 uses under a limit of 3 should stay enabled");
        } finally {
            ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsResetUsesFeatureOff(TestContext helper) {
        // with the feature OFF, the resetUses inject bails and vanilla restock runs even when
        // enchant_trade_restock=false
        ETTestHelper.setFeature("villager_trade_limits", false);
        ETTestHelper.setConfigValue("enchant_trade_restock", "false");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        offer.use(); offer.use();
        offer.resetUses();
        try {
            helper.assertTrue(offer.getUses() == 0,
                "Vanilla resetUses should run when the feature is off (uses=" + offer.getUses() + ")");
        } finally {
            ETTestHelper.setConfigValue("enchant_trade_restock", "true");
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsNoRestockListEdges(TestContext helper) {
        // messy list " mending , sharpness ,, " must trim/skip empties: mending & sharpness do not
        // restock, looting (not listed) does
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_restock", "true");
        ETTestHelper.setConfigValue("enchant_trade_no_restock", " mending , sharpness ,, ");
        TradeOffer mending = new TradeOffer(new TradedItem(Items.EMERALD, 10),
            EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.MENDING, 1)), 12, 1, 0.2f);
        TradeOffer sharpness = new TradeOffer(new TradedItem(Items.EMERALD, 10),
            EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1)), 12, 1, 0.2f);
        TradeOffer looting = new TradeOffer(new TradedItem(Items.EMERALD, 10),
            EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.LOOTING, 1)), 12, 1, 0.2f);
        mending.use(); mending.resetUses();
        sharpness.use(); sharpness.resetUses();
        looting.use(); looting.resetUses();
        try {
            helper.assertTrue(mending.getUses() == 1,   "Mending is in the no-restock list (uses=" + mending.getUses() + ")");
            helper.assertTrue(sharpness.getUses() == 1, "Sharpness is in the no-restock list (uses=" + sharpness.getUses() + ")");
            helper.assertTrue(looting.getUses() == 0,   "Looting is not listed and should restock (uses=" + looting.getUses() + ")");
        } finally {
            ETTestHelper.setConfigValue("enchant_trade_no_restock", "");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsGarbagePerEnchant(TestContext helper) {
        // negative and non-numeric per-enchant values both fall back to "use global default" (-1),
        // so with no global limit the trade never disables
        ETTestHelper.setFeature("villager_trade_limits", true);
        ETTestHelper.setConfigValue("enchant_trade_max_uses", "-1");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        try {
            ETTestHelper.setConfigValue("trade_sharpness", "-5");
            TradeOffer neg = new TradeOffer(new TradedItem(Items.EMERALD, 10), book.copy(), 12, 1, 0.2f);
            neg.use(); neg.use(); neg.use();
            helper.assertFalse(neg.isDisabled(),
                "Negative per-enchant value should fall back to global (-1) -> not disabled");

            ETTestHelper.setConfigValue("trade_sharpness", "abc");
            TradeOffer garbage = new TradeOffer(new TradedItem(Items.EMERALD, 10), book.copy(), 12, 1, 0.2f);
            garbage.use(); garbage.use(); garbage.use();
            helper.assertFalse(garbage.isDisabled(),
                "Non-numeric per-enchant value should fall back to global (-1) -> not disabled");
        } finally {
            ETTestHelper.setConfigValue("trade_sharpness", "-1");
            ETTestHelper.setFeature("villager_trade_limits", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsRealPurchasability(TestContext helper) {
        // real GUI path: MerchantInventory.updateOffers consults TradeOffer.isDisabled to decide
        // whether the output slot (2) is populated. Disabled -> empty; under-limit -> the sold book
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
    }

    // betterMending: recursive leftover-XP must not over-repair

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void betterMendingLeftoverXpNoOverRepair(TestContext helper) {
        ETTestHelper.setFeature("better_mending", true);
        // `more_mending` would rewrite getMendingRepairCost; keep it off so the vanilla 2x / /2 math holds
        ETTestHelper.setFeature("more_mending", false);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // mainhand (slot 0, priority 1): lightly damaged -> fully repaired first, leaving leftover XP
        ItemStack mainhand = new ItemStack(Items.DIAMOND_SWORD);
        mainhand.addEnchantment(Enchantments.MENDING, 1);
        mainhand.setDamage(4);
        player.getInventory().setStack(0, mainhand);
        // inventory (slot 20, priority 5): heavily damaged -> repaired on the recursive call
        ItemStack inv = new ItemStack(Items.DIAMOND_SWORD);
        inv.addEnchantment(Enchantments.MENDING, 1);
        inv.setDamage(100);
        player.getInventory().setStack(20, inv);

        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 5);
        try {
            ETTestHelper.repairPlayerGears(orb, player, 5);
            helper.assertTrue(player.getInventory().getStack(0).getDamage() == 0,
                "Mainhand item should be fully repaired first (got " + player.getInventory().getStack(0).getDamage() + ")");
            helper.assertTrue(player.getInventory().getStack(20).getDamage() == 94,
                "Recursive repair must use the leftover XP (3 -> 6 durability, damage 94), NOT the orb's "
                + "full budget (would over-repair to 90) (got " + player.getInventory().getStack(20).getDamage() + ")");
        } finally {
            ETTestHelper.setFeature("better_mending", false);
        }
        helper.complete();
    }

    // bowLooting: enchant-table reach

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowLootingEnchantTablePresence(TestContext helper) {
        ETTestHelper.setFeature("bow_looting", true);
        try {
            List<EnchantmentLevelEntry> bowEntries = EnchantmentHelper.getPossibleEntries(
                helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.BOW), false);
            helper.assertTrue(containsEnchant(bowEntries, Enchantments.LOOTING),
                "Looting must be offered on a bow from the enchant table when bow_looting is on");
            List<EnchantmentLevelEntry> crossbowEntries = EnchantmentHelper.getPossibleEntries(
                helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.CROSSBOW), false);
            helper.assertTrue(containsEnchant(crossbowEntries, Enchantments.LOOTING),
                "Looting must be offered on a crossbow from the enchant table when bow_looting is on");
            // guard: the mixin only fires for BowItem/CrossbowItem, so a pickaxe still fails vanilla
            // isAcceptableItem (swords-only supportedItems) and is never offered Looting
            List<EnchantmentLevelEntry> pickEntries = EnchantmentHelper.getPossibleEntries(
                helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.DIAMOND_PICKAXE), false);
            helper.assertFalse(containsEnchant(pickEntries, Enchantments.LOOTING),
                "Looting must NOT be offered on a pickaxe via bow_looting");
        } finally {
            ETTestHelper.setFeature("bow_looting", false);
        }
        // feature-off mirror: vanilla isAcceptableItem rejects the bow and Looting is absent
        List<EnchantmentLevelEntry> offEntries = EnchantmentHelper.getPossibleEntries(
            helper.getWorld().getEnabledFeatures(), 30, new ItemStack(Items.BOW), false);
        helper.assertFalse(containsEnchant(offEntries, Enchantments.LOOTING),
            "Looting must be absent from bow enchant-table entries when bow_looting is off");
        helper.complete();
    }

    // tridentWeapons: enchant-table reach differs by primaryItems

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tridentWeaponsEnchantTablePresence(TestContext helper) {
        ItemStack trident = new ItemStack(Items.TRIDENT);
        ETTestHelper.setFeature("trident_weapons", true);
        try {
            List<EnchantmentLevelEntry> entries = EnchantmentHelper.getPossibleEntries(
                helper.getWorld().getEnabledFeatures(), 30, trident, false);
            // `empty-primaryItems` enchants: reach the enchant table
            helper.assertTrue(containsEnchant(entries, Enchantments.FIRE_ASPECT), "Fire Aspect (empty primaryItems) must appear on a trident from the table");
            helper.assertTrue(containsEnchant(entries, Enchantments.KNOCKBACK),   "Knockback (empty primaryItems) must appear on a trident from the table");
            helper.assertTrue(containsEnchant(entries, Enchantments.LOOTING),     "Looting (empty primaryItems) must appear on a trident from the table");
            // `sword-primaryItems` enchants: blocked from the table by isPrimaryItem
            helper.assertFalse(containsEnchant(entries, Enchantments.SHARPNESS),          "Sharpness (SWORD_ENCHANTABLE primaryItems) must NOT appear on a trident from the table");
            helper.assertFalse(containsEnchant(entries, Enchantments.SMITE),              "Smite (SWORD_ENCHANTABLE primaryItems) must NOT appear on a trident from the table");
            helper.assertFalse(containsEnchant(entries, Enchantments.BANE_OF_ARTHROPODS), "Bane (SWORD_ENCHANTABLE primaryItems) must NOT appear on a trident from the table");
            // ...but the anvil path (isAcceptableItem only) DOES accept the sword-primary enchants -
            // checks the asymmetry between the anvil and the enchant table
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.SHARPNESS, trident), "Sharpness IS anvil-acceptable on a trident (isAcceptableItem relaxed)");
            helper.assertTrue(ETTestHelper.isAcceptableItem(Enchantments.LOOTING, trident),   "Looting IS anvil-acceptable on a trident (isAcceptableItem relaxed)");
        } finally {
            ETTestHelper.setFeature("trident_weapons", false);
        }
        // feature-off mirror: none of the granted enchants reach a trident's table entries
        List<EnchantmentLevelEntry> off = EnchantmentHelper.getPossibleEntries(
            helper.getWorld().getEnabledFeatures(), 30, trident, false);
        helper.assertFalse(containsEnchant(off, Enchantments.FIRE_ASPECT), "Fire Aspect absent from trident table when trident_weapons off");
        helper.assertFalse(containsEnchant(off, Enchantments.KNOCKBACK),   "Knockback absent from trident table when trident_weapons off");
        helper.assertFalse(containsEnchant(off, Enchantments.LOOTING),     "Looting absent from trident table when trident_weapons off");
        helper.complete();
    }

    // disableEnchantments: anvil application blocked

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void disableEnchantmentsAnvilBlocked(TestContext helper) {
        ETTestHelper.setFeature("disable_enchantments_enabled", true);
        ETTestHelper.setConfigValue("disable_enchantments", "sharpness");
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack sharpBook = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
            ItemStack out = anvilCombine(helper, sword, sharpBook);
            helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out) == 0,
                "Disabled Sharpness must NOT transfer onto the anvil output (clamped to maxLevel 0) (got "
                + EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out) + ")");
        } finally {
            ETTestHelper.setConfigValue("disable_enchantments", "");
            ETTestHelper.setFeature("disable_enchantments_enabled", false);
        }
        // feature-off mirror: the same combine transfers Sharpness 1 normally
        ItemStack sword2 = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack sharpBook2 = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        ItemStack out2 = anvilCombine(helper, sword2, sharpBook2);
        helper.assertTrue(EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out2) == 1,
            "With the feature off, Sharpness 1 should transfer onto the anvil output (got "
            + EnchantmentHelper.getLevel(Enchantments.SHARPNESS, out2) + ")");
        helper.complete();
    }

    // bowInfinityFix: onStoppedUsing infinity guard (real fire path)

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixNoInfinityDoesNotFireArrow(TestContext helper) {
        ETTestHelper.setFeature("bow_infinity_fix", true);
        ETTestHelper.setFeature("more_infinity", false);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack bow = new ItemStack(Items.BOW); // deliberately NO Infinity
        player.getInventory().setStack(0, bow);
        Box search = player.getBoundingBox().expand(64.0);
        int arrowsBefore = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
        try {
            bow.getItem().use(world, player, Hand.MAIN_HAND);
            bow.getItem().onStoppedUsing(bow, world, player, 0);
            int arrowsAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            helper.assertTrue(arrowsAfter == arrowsBefore,
                "A non-Infinity bow with no arrows must NOT fire even with the feature on (before="
                + arrowsBefore + ", after=" + arrowsAfter + ")");
            helper.assertTrue(bow.getDamage() == 0,
                "A non-Infinity bow that fired nothing should take no durability (got " + bow.getDamage() + ")");
        } finally {
            ETTestHelper.setFeature("bow_infinity_fix", false);
        }
        helper.complete();
    }

    // villagerTradeLimits: master switch gates both facets

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeLimitsMasterSwitchGatesBothFacets(TestContext helper) {
        ETTestHelper.setFeature("villager_trade_limits", false);
        ETTestHelper.setConfigValue("enchant_trade_max_uses", "1");
        ETTestHelper.setConfigValue("enchant_trade_restock", "false");
        ETTestHelper.setConfigValue("enchant_trade_no_restock", "sharpness");
        ItemStack book = EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(Enchantments.SHARPNESS, 1));
        TradeOffer offer = new TradeOffer(new TradedItem(Items.EMERALD, 10), book, 12, 1, 0.2f);
        try {
            offer.use(); offer.use();
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
    }

    // private helpers (test-local; ETTestHelper is read-only)

    /** runs a real anvil combine of {@code first} + {@code second} and returns the output stack */
    private static ItemStack anvilCombine(TestContext helper, ItemStack first, ItemStack second) {
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        AnvilScreenHandler handler = new AnvilScreenHandler(0, player.getInventory());
        ETTestHelper.setAnvilInputs(handler, first, second);
        handler.updateResult();
        return handler.getSlot(AnvilScreenHandler.OUTPUT_ID).getStack();
    }

    /** spawns a zombie wearing four Protection-IV diamond pieces (EPF 16), at full health */
    private static ZombieEntity spawnProtectedZombie(TestContext helper) {
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
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
            if (entry.enchantment == enchantment) return true;
        }
        return false;
    }

    /** builds a MerchantInventory pre-loaded with a single enchant-book offer and payment in slot 0 */
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
