package com.adibarra.enchanttweaker.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
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

    // ─── NoMendingUnbreaking ──────────────────────────────────────────

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

    // ─── BetterMending: mainhand priority ─────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void betterMendingMainHandPriority(TestContext helper) {
        ETTestHelper.setFeature("better_mending", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // Damaged mending sword in mainhand (slot 0)
        ItemStack mainhandSword = new ItemStack(Items.DIAMOND_SWORD);
        mainhandSword.addEnchantment(Enchantments.MENDING, 1);
        mainhandSword.setDamage(100);
        player.getInventory().setStack(0, mainhandSword);
        // Damaged mending sword in inventory (slot 20)
        ItemStack inventorySword = new ItemStack(Items.DIAMOND_SWORD);
        inventorySword.addEnchantment(Enchantments.MENDING, 1);
        inventorySword.setDamage(100);
        player.getInventory().setStack(20, inventorySword);

        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        // Small amount of XP so it only repairs one item
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 5);
        ETTestHelper.repairPlayerGears(orb, player, 5);

        helper.assertTrue(player.getInventory().getStack(0).getDamage() < 100,
            "BetterMending should prioritize mainhand over inventory");
        helper.assertTrue(player.getInventory().getStack(20).getDamage() == 100,
            "BetterMending should NOT repair inventory item when mainhand needs repair");
        helper.complete();
    }

    // ─── BowInfinityFix ──────────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixEnabled(TestContext helper) {
        ETTestHelper.setFeature("bow_infinity_fix", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        player.getInventory().setStack(0, bow);
        // No arrows in inventory — vanilla would block use()
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
            ETTestHelper.setFeature("bow_infinity_fix", true);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void bowInfinityFixNoInfinity(TestContext helper) {
        ETTestHelper.setFeature("bow_infinity_fix", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        // Bow WITHOUT Infinity — mixin should not activate
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
        // When more_infinity is enabled, bow_infinity_fix should be suppressed at runtime
        ETTestHelper.setFeature("bow_infinity_fix", true);
        ETTestHelper.setFeature("more_infinity", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        player.getInventory().setStack(0, bow);
        // No arrows in inventory — bow_infinity_fix would normally allow this, but more_infinity takes precedence
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

    // ─── GodWeapons: all combinations ─────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void godWeaponsAllCombinations(TestContext helper) {
        ETTestHelper.setFeature("god_weapons", true);
        // Test all 3 pairs: Smite↔Bane, Sharpness↔Bane, Sharpness↔Smite
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

    // ─── LoyalVoidTridents: edge cases ────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void loyalVoidTridentsNoLoyalty(TestContext helper) {
        // Trident WITHOUT Loyalty should NOT be rescued from void
        ETTestHelper.setFeature("loyal_void_tridents", true);
        ServerWorld world = helper.getWorld();
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        // No Loyalty enchantment
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
        // When the mixin fires, it sets dealtDamage=true and zeros velocity at the world bottom.
        // Vanilla's loyalty logic then takes over and begins returning the trident upward.
        // After a few ticks the trident should move UP (positive Y velocity), not keep falling.
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
        // Tick a few times to let loyalty return logic kick in
        for (int i = 0; i < 5; i++) trident.tick();
        helper.assertTrue(trident.getY() > startY,
            "Loyal trident should start returning upward after being rescued from void (Y=" + trident.getY() + ", started at " + startY + ")");
        helper.complete();
    }

    // ─── AxesNotTools: multiple hits ──────────────────────────────────

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

    // ─── ProtectionBypass ──────────────────────────────────────────────
    // When enabled, specific damage types bypass Protection enchantment entirely.
    // Magic damage (20) with Protection IV x4 (EPF 16):
    //   Vanilla:  20 * (1 - 16/25) = 7.2  (reduced)
    //   Bypassed: 20.0                     (full damage)

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassMagicEnabled(TestContext helper) {
        ETTestHelper.setFeature("protection_bypass_enabled", true);
        ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic");
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
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().magic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        try {
            helper.assertTrue(Math.abs(result - 20.0f) < 0.01f,
                "Magic damage should bypass protection entirely (got " + result + ", expected 20.0)");
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
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().wither();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        try {
            helper.assertTrue(Math.abs(result - 20.0f) < 0.01f,
                "Wither damage should bypass protection entirely (got " + result + ", expected 20.0)");
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
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().magic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float vanillaExpected = net.minecraft.entity.DamageUtil.getInflictedDamage(20.0f, 16.0f);
        try {
            helper.assertTrue(Math.abs(result - vanillaExpected) < 0.01f,
                "Magic damage with bypass disabled should be reduced by protection (got " + result + ", expected ~" + vanillaExpected + ")");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void protectionBypassGenericNotAffected(TestContext helper) {
        // Generic damage should never bypass, even with bypass types configured
        ETTestHelper.setFeature("protection_bypass_enabled", true);
        ETTestHelper.setConfigValue("protection_bypass_types", "magic,indirect_magic,wither,dragon_breath");
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
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float vanillaExpected = net.minecraft.entity.DamageUtil.getInflictedDamage(20.0f, 16.0f);
        try {
            helper.assertTrue(Math.abs(result - vanillaExpected) < 0.01f,
                "Generic damage should still be reduced by protection (got " + result + ", expected ~" + vanillaExpected + ")");
        } finally {
            ETTestHelper.setConfigValue("protection_bypass_types", "");
            ETTestHelper.setFeature("protection_bypass_enabled", false);
        }
        helper.complete();
    }

    // ─── VillagerTradeLimits ────────────────────────────────────────

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
        // Non-enchanted trade: emeralds for wheat
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
}
