package com.adibarra.enchanttweaker.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TweakGameTest implements FabricGameTest {

    // ─── GodArmor ────────────────────────────────────────────────────

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
            ETTestHelper.setFeature("god_armor", true);
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

    // ─── GodWeapons ──────────────────────────────────────────────────

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
            ETTestHelper.setFeature("god_weapons", true);
        }
        helper.complete();
    }

    // ─── InfiniteMending ─────────────────────────────────────────────

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
            ETTestHelper.setFeature("infinite_mending", true);
        }
        helper.complete();
    }

    // ─── MultishotPiercing ───────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void multishotPiercingEnabled(TestContext helper) {
        ETTestHelper.setFeature("multishot_piercing", true);
        helper.assertTrue(ETTestHelper.canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
            "Multishot should accept Piercing when enabled");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void multishotPiercingDisabled(TestContext helper) {
        ETTestHelper.setFeature("multishot_piercing", false);
        try {
            helper.assertTrue(!ETTestHelper.canAccept(Enchantments.MULTISHOT, Enchantments.PIERCING),
                "Multishot should not accept Piercing when disabled");
        } finally {
            ETTestHelper.setFeature("multishot_piercing", true);
        }
        helper.complete();
    }

    // ─── TridentWeapons ──────────────────────────────────────────────

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
            ETTestHelper.setFeature("trident_weapons", true);
        }
        helper.complete();
    }

    // ─── AxeWeapons ──────────────────────────────────────────────────

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
            ETTestHelper.setFeature("axe_weapons", true);
        }
        helper.complete();
    }

    // ─── AxesNotTools ────────────────────────────────────────────────

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
            ETTestHelper.setFeature("axes_not_tools", true);
        }
        helper.complete();
    }

    // ─── NoThornsBacklash ────────────────────────────────────────────
    // @Constant(intValue=2) intercepts ItemStack.damage(2, user, …) — the ARMOR durability hit.
    // At level 7: 0.15*7=1.05 > 1.0 → thorns always fires.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noThornsBacklashEnabled(TestContext helper) {
        ETTestHelper.setFeature("no_thorns_backlash", true);
        ZombieEntity user     = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ZombieEntity attacker = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(1, 2, 0));
        // Equip user with a Thorns VII chestplate so chooseEquipmentWith finds it
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        chest.addEnchantment(Enchantments.THORNS, 7);
        user.equipStack(EquipmentSlot.CHEST, chest);
        Enchantments.THORNS.onUserDamaged(user, attacker, 7);
        // Mixin replaces damage(2,…) with damage(0,…) → armor is undamaged
        helper.assertTrue(user.getEquippedStack(EquipmentSlot.CHEST).getDamage() == 0,
            "Thorns armor should not lose durability when no_thorns_backlash enabled");
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
            Enchantments.THORNS.onUserDamaged(user, attacker, 7);
            // Vanilla damage(2,…) → armor loses 2 durability
            helper.assertTrue(user.getEquippedStack(EquipmentSlot.CHEST).getDamage() == 2,
                "Thorns armor should lose 2 durability when no_thorns_backlash disabled (got "
                + user.getEquippedStack(EquipmentSlot.CHEST).getDamage() + ")");
        } finally {
            ETTestHelper.setFeature("no_thorns_backlash", true);
        }
        helper.complete();
    }

    // ─── LoyalVoidTridents ───────────────────────────────────────────

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
            ETTestHelper.setFeature("loyal_void_tridents", true);
        }
        helper.complete();
    }

    // ─── NoSoulSpeedBacklash ─────────────────────────────────────────
    // @Constant(intValue=1) intercepts ItemStack.damage(1, …) inside addSoulSpeedBoostIfNeeded.
    // The method has a 4% random check before damaging boots. addTemporaryModifier throws if the
    // modifier already exists, so we call removeSoulSpeedBoost() before each attempt.
    // 500 calls gives 1 - 0.96^500 ≈ 100% probability of triggering the damage when disabled.

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
            // With mixin enabled, damage(1,…) → damage(0,…) — boots never lose durability
            for (int i = 0; i < 500; i++) {
                removeBoost.invoke(zombie); // clear existing modifier so addSoulSpeedBoostIfNeeded can run
                addBoost.invoke(zombie);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.assertTrue(zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() == 0,
            "Soul Speed boots should not take damage when no_soul_speed_backlash enabled (got "
            + zombie.getEquippedStack(EquipmentSlot.FEET).getDamage() + ")");
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void noSoulSpeedBacklashDisabled(TestContext helper) {
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
            // 500 calls → ~100% chance of hitting the 4% boot-damage RNG at least once
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
            ETTestHelper.setFeature("no_soul_speed_backlash", true);
        }
        helper.complete();
    }

    // ─── BetterMending ───────────────────────────────────────────────
    // Vanilla mending only repairs equipped items; BetterMending scans the whole inventory.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void betterMendingEnabled(TestContext helper) {
        ETTestHelper.setFeature("better_mending", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
        mendingSword.addEnchantment(Enchantments.MENDING, 1);
        mendingSword.setDamage(100);
        // Slot 20: a general inventory slot that is neither hand nor armor
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
            ETTestHelper.setFeature("better_mending", true);
        }
        helper.complete();
    }
}
