package com.adibarra.enchanttweaker.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import com.adibarra.enchanttweaker.FlameLevelAccess;
import com.adibarra.enchanttweaker.MendingLevelAccess;
import com.adibarra.utils.ADUtils;

public class EnhancedGameTest implements FabricGameTest {

    // more multishot level two loads five projectiles
    // vanilla loads three projectiles

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 5,
                "Multishot II should load 5 projectiles with more_multishot enabled (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "multishot");
        ETTestHelper.setFeature("more_multishot", false);
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 3,
                "Multishot (vanilla) should load 3 projectiles when more_multishot disabled (got " + projectiles.size()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotPracticalCap(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "300");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 1);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 256,
                "MoreMultishot should cap practical projectile counts (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotProjectileCountBounds(TestContext helper) {
        try {
            Method projectileCount = RangedWeaponItem.class
                .getDeclaredMethod("enchanttweaker$moreMultishot$projectileCount", int.class, int.class);
            projectileCount.setAccessible(true);
            int lowerBound = (int) projectileCount.invoke(null, -1, 2);
            int upperBoundary = (int) projectileCount.invoke(null, 255, 1);
            int upperClamp = (int) projectileCount.invoke(null, 256, 1);
            int overflowingProduct = (int) projectileCount.invoke(null, Integer.MAX_VALUE, Integer.MAX_VALUE);
            helper.assertTrue(lowerBound == 1,
                "projectile count below the valid range should clamp to 1 (got " + lowerBound + ")");
            helper.assertTrue(upperBoundary == 256,
                "projectile count at the upper boundary should remain 256 (got " + upperBoundary + ")");
            helper.assertTrue(upperClamp == 256,
                "projectile count above the valid range should clamp to 256 (got " + upperClamp + ")");
            helper.assertTrue(overflowingProduct == 256,
                "overflowing int product should be computed safely and clamp to 256 (got " + overflowingProduct + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.complete();
    }

    // more flame level two burns for 140 ticks

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);

        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        arrow.applyEnchantmentEffects(shooter, 0.0f);

        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // flame two sets the target on fire for 140 ticks
            helper.assertTrue(target.getFireTicks() == 140,
                "Flame II should burn target exactly 140 ticks (got " + target.getFireTicks() + " ticks)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "flame");
        ETTestHelper.setFeature("more_flame", false);
        ETTestHelper.setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);

        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        arrow.applyEnchantmentEffects(shooter, 0.0f);

        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // flame II with mixin disabled: vanilla setOnFireFor(5) = 100 ticks
            helper.assertTrue(target.getFireTicks() == 100,
                "Flame II with more_flame disabled should burn target 100 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlamePlayerShotKeepsWeaponLevel(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setEnchantCap("flame", 3);
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity shooter = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.FLAME, 3);
        try {
            Method createArrow = RangedWeaponItem.class.getDeclaredMethod("createArrowEntity", World.class,
                LivingEntity.class, ItemStack.class, ItemStack.class, boolean.class);
            createArrow.setAccessible(true);
            ProjectileEntity projectile = (ProjectileEntity) createArrow.invoke(bow.getItem(), world, shooter, bow,
                new ItemStack(Items.ARROW), false);
            helper.assertTrue(projectile instanceof ArrowEntity, "bow projectile should be an ArrowEntity");
            helper.assertTrue(((FlameLevelAccess) projectile).enchanttweaker$getFlameLevel() == 3,
                "projectile should capture Flame III from the fired weapon");

            ArrowEntity arrow = (ArrowEntity) projectile;
            arrow.setOnFireFor(10);
            world.spawnEntity(arrow);
            ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 180,
                "player-fired Flame III arrow should burn for 180 ticks after weapon state is unavailable (got "
                    + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameLevelSurvivesNbtRoundTrip(TestContext helper) {
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity original = new ArrowEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.ARROW));
        ((FlameLevelAccess) original).enchanttweaker$setFlameLevel(7);
        NbtCompound nbt = new NbtCompound();
        original.writeNbt(nbt);

        ArrowEntity restored = new ArrowEntity(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.ARROW));
        restored.readNbt(nbt);
        helper.assertTrue(((FlameLevelAccess) restored).enchanttweaker$getFlameLevel() == 7,
            "serialized projectile should restore captured Flame level 7");
        helper.complete();
    }

    // more mending costs 60 without mending
    // level one costs 55
    // direct invocation avoids recursive gear repair

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "more_mending_step",
            "more_mending_floor");
        try {
            ETTestHelper.setFeature("more_mending", true);
            ETTestHelper.setConfigValue("more_mending_step", "0.05");
            ETTestHelper.setConfigValue("more_mending_floor", "0.1");
            ServerWorld world = helper.getWorld();
            BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world,
                pos.getX(), pos.getY(), pos.getZ(), 0);
            Method getMRC = net.minecraft.entity.ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost",
                int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());

            int baseCost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            helper.assertTrue(baseCost == 60,
                "MoreMending getMendingRepairCost(100) with no Mending level should be 60 (got " + baseCost + ")");

            ((MendingLevelAccess) orb).enchanttweaker$setMendingLevel(1);
            int levelOneCost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            helper.assertTrue(levelOneCost == 55,
                "MoreMending level 1 should reduce the repair cost to 55 (got " + levelOneCost + ")");

            ((MendingLevelAccess) orb).enchanttweaker$setMendingLevel(6);
            int integerBoundaryCost = (int) getMRC.invoke(isStatic ? null : orb, 10);
            helper.assertTrue(integerBoundaryCost == 3,
                "MoreMending level 6 should preserve the exact integer boundary (got " + integerBoundaryCost + ")");

            ((MendingLevelAccess) orb).enchanttweaker$setMendingLevel(10);
            int floorCost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            helper.assertTrue(floorCost == 10,
                "MoreMending level 10 should clamp the repair cost to the configured floor of 10 (got " + floorCost
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending");
        ETTestHelper.setFeature("more_mending", false);
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world, pos.getX(),
            pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.entity.ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost",
                int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());
            int cost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            // vanilla 1.20.3: amount / 2 = 50
            helper.assertTrue(cost == 50, "Vanilla getMendingRepairCost(100) should return 50 (got " + cost + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // moreInfinity
    // infinity level 34 always preserves arrows

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct",
            "capmod_enabled", "infinity");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.03");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("infinity", 34);
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 34);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            // `getProjectile` is protected static, access via reflection
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
            // level 34 produces an intangible projectile
            helper.assertTrue(proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                "Level-34 Infinity should always give intangible projectile (threshold=0)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity");
        ETTestHelper.setFeature("more_infinity", false);
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            // vanilla Infinity I: always intangible, mixin must not interfere
            ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
            helper.assertTrue(proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                "Vanilla Infinity I should give intangible projectile when more_infinity disabled");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void randomChanceThresholdBoundaries(TestContext helper) {
        helper.assertTrue(ADUtils.bindingKeepsItem(5, 0.125, 0.5f),
            "Binding roll exactly at the keep threshold should keep the item");
        helper.assertFalse(ADUtils.bindingKeepsItem(5, 0.125, Math.nextDown(0.5f)),
            "Binding roll immediately below the keep threshold should drop the item");
        helper.assertTrue(ADUtils.infinityPreservesArrow(2, 0.25, 0.5f),
            "Infinity roll exactly at the preserve threshold should preserve the arrow");
        helper.assertFalse(ADUtils.infinityPreservesArrow(2, 0.25, Math.nextDown(0.5f)),
            "Infinity roll immediately below the preserve threshold should consume the arrow");
        helper.complete();
    }

    // moreBinding
    // a high-level item is retained without random trials

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBindingEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.addEnchantment(Enchantments.BINDING_CURSE, 5);
        player.getInventory().armor.set(3, helmet);
        try {
            player.getInventory().dropAll();
            helper.assertFalse(player.getInventory().armor.get(3).isEmpty(),
                "MoreBinding should retain Binding Curse V armor with a zero drop threshold");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBindingDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "capmod_enabled",
            "binding_curse");
        ETTestHelper.setFeature("more_binding", false);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 10);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.addEnchantment(Enchantments.BINDING_CURSE, 10);
        player.getInventory().armor.set(3, helmet);
        try {
            player.getInventory().dropAll();
            helper.assertTrue(player.getInventory().armor.get(3).isEmpty(),
                "Vanilla dropAll should always drop Binding Curse armor when more_binding disabled");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // more channeling enables channeling two during rain
    // the mixin accepts thunderstorms or level-two rain

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreChannelingEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 2);
        ServerWorld world = helper.getWorld();
        float[] originalRainGradients = ETTestHelper.snapshotRainGradient(world);
        // rain without thunder should still enable channeling two
        world.setWeather(0, 100000, true, false);
        ETTestHelper.forceRainGradient(world, 1.0f); // israining checks the rain gradient
                                                     // the gradient updates lazily each tick
        // place the target high in the sky to satisfy hasDirectSkyLight
        BlockPos absBase = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getTopY() - 20.0;
        ZombieEntity target = EntityType.ZOMBIE.create(world);
        target.setPos(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.spawnEntity(target);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.CHANNELING, 2);
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        trident.setPos(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.spawnEntity(trident);
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            Box searchBox = new Box(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10, skyY + 20,
                absBase.getZ() + 10);
            List<LightningEntity> lightnings = world.getEntitiesByType(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(!lightnings.isEmpty(),
                "MoreChanneling II should summon lightning in rain (no lightning spawned)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world
                .getEntitiesByType(EntityType.LIGHTNING_BOLT, new Box(absBase.getX() - 10, skyY - 10,
                    absBase.getZ() - 10, absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true)
                .forEach(Entity::discard);
            world.setWeather(100000, 0, false, false);
            ETTestHelper.restoreRainGradient(world, originalRainGradients);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreChannelingDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", false);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 2);
        ServerWorld world = helper.getWorld();
        world.setWeather(0, 100000, true, false); // rain, no thunder
        BlockPos absBase = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getTopY() - 20.0;
        ZombieEntity target = EntityType.ZOMBIE.create(world);
        target.setPos(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.spawnEntity(target);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.CHANNELING, 2);
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        trident.setPos(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.spawnEntity(trident);
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            Box searchBox = new Box(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10, skyY + 20,
                absBase.getZ() + 10);
            List<LightningEntity> lightnings = world.getEntitiesByType(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(lightnings.isEmpty(),
                "Channeling II in rain (no thunder) without mixin should NOT summon lightning");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world
                .getEntitiesByType(EntityType.LIGHTNING_BOLT, new Box(absBase.getX() - 10, skyY - 10,
                    absBase.getZ() - 10, absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true)
                .forEach(Entity::discard);
            world.setWeather(100000, 0, false, false);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // channeling one does not summon lightning during rain
    // the mixin requires level two

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreChannelingLevel1RainNoLightning(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 1);
        ServerWorld world = helper.getWorld();
        float[] originalRainGradients = ETTestHelper.snapshotRainGradient(world);
        world.setWeather(0, 100000, true, false);
        ETTestHelper.forceRainGradient(world, 1.0f);
        BlockPos absBase = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getTopY() - 20.0;
        ZombieEntity target = EntityType.ZOMBIE.create(world);
        target.setPos(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.spawnEntity(target);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.CHANNELING, 1);
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        trident.setPos(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.spawnEntity(trident);
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            Box searchBox = new Box(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10, skyY + 20,
                absBase.getZ() + 10);
            List<LightningEntity> lightnings = world.getEntitiesByType(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(lightnings.isEmpty(),
                "Channeling I in rain should NOT summon lightning even with more_channeling enabled");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world
                .getEntitiesByType(EntityType.LIGHTNING_BOLT, new Box(absBase.getX() - 10, skyY - 10,
                    absBase.getZ() - 10, absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true)
                .forEach(Entity::discard);
            world.setWeather(100000, 0, false, false);
            ETTestHelper.restoreRainGradient(world, originalRainGradients);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // moreMultishot: level edge cases

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotLevel1Vanilla(TestContext helper) {
        // level one still loads three projectiles
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 1);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 3,
                "Multishot I with more_multishot should still load 3 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotLevel3(TestContext helper) {
        // level 3: 3*2+1 = 7 projectiles
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "capmod_enabled", "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 3);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 3);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 7,
                "Multishot III should load 7 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // moreFlame: level 1 = vanilla

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameLevel1Vanilla(TestContext helper) {
        // level one burns targets for 100 ticks
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 1);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        arrow.applyEnchantmentEffects(shooter, 0.0f);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 100,
                "Flame I with more_flame enabled should still burn 100 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // moreMending: high level clamp

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingHighLevel(TestContext helper) {
        // level ten clamps repair cost to ten
        // disable better mending for vanilla gear repair
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "more_mending_step",
            "more_mending_floor", "better_mending", "capmod_enabled", "mending");
        try {
            ETTestHelper.setFeature("more_mending", true);
            ETTestHelper.setConfigValue("more_mending_step", "0.05");
            ETTestHelper.setConfigValue("more_mending_floor", "0.1");
            ETTestHelper.setFeature("better_mending", false);
            ETTestHelper.setCapmod(true);
            ETTestHelper.setEnchantCap("mending", 10);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingSword.addEnchantment(Enchantments.MENDING, 10);
            mendingSword.setDamage(200);
            player.getInventory().setStack(0, mendingSword); // mainhand
            BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world,
                pos.getX(), pos.getY(), pos.getZ(), 100);
            // gear repair captures the item's mending level
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
            helper.assertTrue(remaining == 80,
                "MoreMending level 10 should leave exactly 80 XP (remaining=" + remaining + "; vanilla would be 0)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingBetterMendingCombined(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        try {
            ETTestHelper.setFeature("more_mending", true);
            ETTestHelper.setFeature("better_mending", true);
            ETTestHelper.setConfigValue("more_mending_step", "0.05");
            ETTestHelper.setConfigValue("more_mending_floor", "0.1");
            ETTestHelper.setCapmod(true);
            ETTestHelper.setEnchantCap("mending", 5);
            ServerWorld world = helper.getWorld();
            PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
            ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingSword.addEnchantment(Enchantments.MENDING, 5);
            mendingSword.setDamage(40);
            player.getInventory().setStack(0, mendingSword); // mainhand: BetterMending's first pick
            BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world,
                pos.getX(), pos.getY(), pos.getZ(), 100);
            // better mending must capture level five before costing
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
            // level five costs 14 and leaves 86 experience
            // level zero would leave 76 experience
            helper.assertTrue(remaining == 86,
                "Combined MoreMending+BetterMending should use the item's actual Mending level (remaining=" + remaining
                    + ", expected 86; the old level-0 bug would give 76)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingFloorClampNoCrash(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "more_mending_step",
            "more_mending_floor");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setConfigValue("more_mending_step", "0.05");
        ETTestHelper.setConfigValue("more_mending_floor", "0.9"); // > 0.6 -> would crash pre-fix
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world, pos.getX(),
            pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.entity.ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost",
                int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());
            // this configuration previously threw an illegal argument exception
            int cost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            // the floor clamps to 0.6
            // 100 experience then costs 60
            helper.assertTrue(cost == 60,
                "MoreMending with floor=0.9 (>0.6) should clamp the floor to 0.6 and return 60 without crashing (got "
                    + cost + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // moreInfinity: level 1 consumes arrows

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityLevel1Consumes(TestContext helper) {
        // zero rate makes level one consumption deterministic
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.0");
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            ItemStack projectile = (ItemStack) m.invoke(null, bow, new ItemStack(Items.ARROW), shooter, false);
            helper.assertFalse(projectile.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                "MoreInfinity level 1 with zero free-shot rate must consume the arrow");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // moreInfinity: custom rate

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityCustomRateHigh(TestContext helper) {
        // rate one always preserves level-one arrows
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "1.0");
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            for (int i = 0; i < 50; i++) {
                ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
                helper.assertTrue(proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                    "more_infinity_pct=1.0 level 1 should always give free arrow (iteration " + i + ")");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityCustomRateZero(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct",
            "capmod_enabled", "infinity");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("infinity", 34);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.0");
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 34);
        player.getInventory().setStack(0, bow);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            int before = player.getInventory().count(Items.ARROW);
            final int shots = 5;
            for (int i = 0; i < shots; i++) {
                bow.getItem().use(world, player, Hand.MAIN_HAND);
                bow.getItem().onStoppedUsing(bow, world, player, 0);
            }
            int after = player.getInventory().count(Items.ARROW);
            helper.assertTrue(after == before - shots,
                "more_infinity_pct=0.0 must consume one arrow per shot: expected " + (before - shots) + " (got " + after
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // protection uses multiplicative damage reduction
    // protection four on four pieces has epf 16
    // damage 20 becomes approximately 10.397

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreProtectionEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.96");
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
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = (float) (20.0 * Math.pow(0.96, 16));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.01f,
                "MoreProtection should give ~" + expected + " damage (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreProtectionDisabled(TestContext helper) {
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
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = net.minecraft.entity.DamageUtil.getInflictedDamage(20.0f, 16.0f);
        helper.assertTrue(Math.abs(result - expected) < 0.01f,
            "Vanilla protection should give ~" + expected + " damage (got " + result + ")");
        helper.complete();
    }

    // fire protection four reduces 200 ticks to 104
    // vanilla instead reduces it to 80

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.85");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
        int expected = (int) (200 * Math.pow(0.85, 4));
        try {
            helper.assertTrue(result == expected,
                "MoreFireProtection should give duration " + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection");
        ETTestHelper.setFeature("more_fire_protection", false);
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
        try {
            helper.assertTrue(result == 80, "Vanilla fire protection should give duration 80 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // blast protection four multiplies knockback by 0.85^4
    // vanilla knockback is 0.4

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "0.85");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity, 1.0);
        double expected = Math.pow(0.85, 4);
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.001,
                "MoreBlastProtection should give knockback ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection");
        ETTestHelper.setFeature("more_blast_protection", false);
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity, 1.0);
        try {
            helper.assertTrue(Math.abs(result - 0.4) < 0.001,
                "Vanilla blast protection should give knockback ~0.4 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // moreBinding: level one with a full drop threshold

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBindingLevel1AlwaysDrops(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 1);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.addEnchantment(Enchantments.BINDING_CURSE, 1);
            player.getInventory().armor.set(3, helmet);
            player.getInventory().dropAll();
            helper.assertTrue(player.getInventory().armor.get(3).isEmpty(),
                "MoreBinding level 1 should drop armor at the full threshold");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // configurable values

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreProtectionCustomBase(TestContext helper) {
        // custom base 0.90 instead of default 0.96
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.90");
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
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = (float) (20.0 * Math.pow(0.90, 16));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.01f,
                "MoreProtection with base=0.90 should give ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionCustomBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.70");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
        int expected = (int) (200 * Math.pow(0.70, 4));
        try {
            helper.assertTrue(result == expected,
                "MoreFireProtection with base=0.70 should give duration " + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionCustomBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "0.70");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity, 1.0);
        double expected = Math.pow(0.70, 4);
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.001,
                "MoreBlastProtection with base=0.70 should give knockback ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingCustomStepAndFloor(TestContext helper) {
        // custom step and floor retain the level-zero factor
        // the repair cost remains 60
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "more_mending_step",
            "more_mending_floor");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setConfigValue("more_mending_step", "0.1");
        ETTestHelper.setConfigValue("more_mending_floor", "0.05");
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world, pos.getX(),
            pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.entity.ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost",
                int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());
            int cost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            // level zero uses a 0.6 factor for 60 cost
            helper.assertTrue(cost == 60, "MoreMending custom step/floor at level 0 should be 60 (got " + cost + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotCustomPerLevel(TestContext helper) {
        // custom per_level=3: level 2 -> 2*3+1 = 7 projectiles
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "capmod_enabled", "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 2);
        ETTestHelper.setConfigValue("more_multishot_per_level", "3");
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 7,
                "Multishot II with per_level=3 should load 7 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameCustomPerLevel(TestContext helper) {
        // flame two with per-level five burns 200 ticks
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level",
            "capmod_enabled", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("flame", 2);
        ETTestHelper.setConfigValue("more_flame_per_level", "5");
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        arrow.applyEnchantmentEffects(shooter, 0.0f);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 200,
                "Flame II with per_level=5 should burn 200 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // more Looting (XP Boost)

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreLootingEnabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 3);
        player.equipStack(EquipmentSlot.MAINHAND, sword);
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            int expected = expectedLootingXp(base, 3, 0.5);
            helper.assertTrue(total == expected,
                "MoreLooting (Looting III, x0.5) should boost 10 XP to " + expected + " (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreLootingDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting");
        ETTestHelper.setFeature("more_looting", false);
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 3);
        player.equipStack(EquipmentSlot.MAINHAND, sword);
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            helper.assertTrue(total == base,
                "MoreLooting disabled should not boost XP: expected " + base + " (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreLootingCustomMultiplier(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "1.0");
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 2);
        player.equipStack(EquipmentSlot.MAINHAND, sword);
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            int expected = expectedLootingXp(base, 2, 1.0);
            helper.assertTrue(total == expected,
                "MoreLooting with multiplier=1.0 and Looting II should give " + expected + " XP (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }
    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameLevel0Vanilla(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        shooter.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        arrow.applyEnchantmentEffects(shooter, 0.0f);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 100,
                "Non-Flame fire arrow (flameLevel 0) should burn vanilla 100 ticks (got " + target.getFireTicks()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameNegativePerLevelVanilla(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level",
            "capmod_enabled", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "-3");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        arrow.applyEnchantmentEffects(shooter, 0.0f);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 100,
                "Flame II with negative per_level should clamp to vanilla 100 ticks (got " + target.getFireTicks()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreLootingNegativeMultiplier(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "-1.0");
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 3);
        player.equipStack(EquipmentSlot.MAINHAND, sword);
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            int expected = expectedLootingXp(base, 3, -1.0);
            helper.assertTrue(total == expected && expected == base,
                "Negative more_looting_multiplier should clamp to 0 -> XP unchanged at " + base + " (got " + total
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreProtectionNegativeBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "-0.5");
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
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        try {
            helper.assertFalse(Float.isNaN(result), "Negative more_protection_base must not produce NaN damage");
            helper.assertTrue(result == 0.0f,
                "Negative more_protection_base should clamp to 0 -> 0 damage (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotNegativePerLevel(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "-2");
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 1,
                "Multishot II with negative per_level should load exactly 1 projectile (got " + projectiles.size()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }
    private static void forceThunderGradient(ServerWorld world, float gradient) {
        try {
            for (String name : new String[]{"thunderGradient", "thunderGradientPrev", "rainGradient",
                    "rainGradientPrev"}) {
                Field f = World.class.getDeclaredField(name);
                f.setAccessible(true);
                f.setFloat(world, gradient);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("forceThunderGradient reflection failed", e);
        }
    }

    /** equips enchanted diamond armor at the requested level */
    private static void equipArmorSet(net.minecraft.entity.LivingEntity entity,
        net.minecraft.enchantment.Enchantment ench, int level) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            ItemStack armor = new ItemStack(switch (slot) {
                case HEAD -> Items.DIAMOND_HELMET;
                case CHEST -> Items.DIAMOND_CHESTPLATE;
                case LEGS -> Items.DIAMOND_LEGGINGS;
                case FEET -> Items.DIAMOND_BOOTS;
                default -> Items.DIAMOND_HELMET;
            });
            armor.addEnchantment(ench, level);
            entity.equipStack(slot, armor);
        }
    }

    private static int dropAndSumXp(TestContext helper, ServerWorld world, ZombieEntity zombie,
        ServerPlayerEntity killer, int baseXp) {
        try {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                zombie.equipStack(slot, ItemStack.EMPTY); // empty slot -> no random getXpToDrop() bonus
            }
            zombie.setBaby(false); // baby zombies multiply experiencePoints by 2.5 inside getXpToDrop()
            Field attackingPlayerField = LivingEntity.class.getDeclaredField("attackingPlayer");
            attackingPlayerField.setAccessible(true);
            attackingPlayerField.set(zombie, killer);
            Field playerHitTimerField = LivingEntity.class.getDeclaredField("playerHitTimer");
            playerHitTimerField.setAccessible(true);
            playerHitTimerField.setInt(zombie, 100);
            Field xpField = net.minecraft.entity.mob.MobEntity.class.getDeclaredField("experiencePoints");
            xpField.setAccessible(true);
            xpField.setInt(zombie, baseXp);

            // remove nearby experience orbs before dropping new ones
            Box searchBox = zombie.getBoundingBox().expand(3.0);
            world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, e -> true).forEach(e -> e.discard());

            Method dropXp = LivingEntity.class.getDeclaredMethod("dropXp");
            dropXp.setAccessible(true);
            dropXp.invoke(zombie);

            return world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, e -> true).stream()
                .mapToInt(ExperienceOrbEntity::getExperienceAmount).sum();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static int expectedLootingXp(int base, int lootingLevel, double multiplier) {
        if (lootingLevel <= 0)
            return base;
        double m = Math.max(0.0, multiplier);
        return (int) (base * (1.0 + lootingLevel * m));
    }

    // gameplay drivers for player-observable outcomes

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBindingAfterRespawnRestore(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0"); // level five always keeps bound items
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        try {
            ServerPlayerEntity old = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
            BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
            old.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.addEnchantment(Enchantments.BINDING_CURSE, 5);
            old.getInventory().armor.set(3, helmet); // slot 3 = head

            old.getInventory().dropAll();
            // the mixin keeps the helmet in slot three
            // it also stashes a copy for respawn
            helper.assertFalse(old.getInventory().armor.get(3).isEmpty(),
                "MoreBinding should retain the Binding-5 helmet in slot 3 after dropAll");

            ServerPlayerEntity respawned = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);

            ItemStack restored = respawned.getInventory().armor.get(3);
            helper.assertTrue(
                restored.isOf(Items.DIAMOND_HELMET)
                    && EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, restored) == 5,
                "AFTER_RESPAWN should restore the Binding-5 helmet to slot 3 (got " + restored + ")");

            // a second invocation finds no remaining stash entry
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);
            helper.assertTrue(respawned.getInventory().armor.get(3).isEmpty(),
                "A second AFTER_RESPAWN should restore nothing (stash entry already consumed)");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBindingDisconnectClearsPendingArmor(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        try {
            ServerPlayerEntity old = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.addEnchantment(Enchantments.BINDING_CURSE, 5);
            old.getInventory().armor.set(3, helmet);
            old.getInventory().dropAll();
            helper.assertFalse(old.getInventory().armor.get(3).isEmpty(),
                "death path should stage the bound helmet before disconnect");

            ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(old.networkHandler,
                helper.getWorld().getServer());
            ServerPlayerEntity respawned = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);
            helper.assertTrue(respawned.getInventory().armor.get(3).isEmpty(),
                "disconnect should clear armor abandoned before respawn");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreChannelingClearWeatherNoLightning(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 2);
        ServerWorld world = helper.getWorld();
        float[] originalRainGradients = ETTestHelper.snapshotRainGradient(world);
        world.setWeather(100000, 0, false, false); // clear
        ETTestHelper.forceRainGradient(world, 0.0f);
        BlockPos absBase = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getTopY() - 20.0;
        ZombieEntity target = EntityType.ZOMBIE.create(world);
        target.setPos(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.spawnEntity(target);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.CHANNELING, 2);
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        trident.setPos(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.spawnEntity(trident);
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            Box searchBox = new Box(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10, skyY + 20,
                absBase.getZ() + 10);
            List<LightningEntity> lightnings = world.getEntitiesByType(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(lightnings.isEmpty(),
                "MoreChanneling II in CLEAR weather must NOT summon lightning (isRaining false-branch)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world
                .getEntitiesByType(EntityType.LIGHTNING_BOLT, new Box(absBase.getX() - 10, skyY - 10,
                    absBase.getZ() - 10, absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true)
                .forEach(Entity::discard);
            world.setWeather(100000, 0, false, false);
            ETTestHelper.restoreRainGradient(world, originalRainGradients);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameEndToEndRealShot(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        player.getInventory().setStack(0, flameBow);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 8));
        Box search = player.getBoundingBox().expand(64.0);
        try {
            helper.assertTrue(EnchantmentHelper.getEquipmentLevel(Enchantments.FLAME, player) == 2,
                "Player should read Flame II from the mainhand bow");
            int before = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            flameBow.getItem().use(world, player, Hand.MAIN_HAND);
            flameBow.getItem().onStoppedUsing(flameBow, world, player, 0);
            List<PersistentProjectileEntity> arrows = world.getEntitiesByClass(PersistentProjectileEntity.class, search,
                e -> true);
            helper.assertTrue(arrows.size() == before + 1,
                "Flame bow should fire exactly one arrow (before=" + before + ", after=" + arrows.size() + ")");
            PersistentProjectileEntity arrow = arrows.get(arrows.size() - 1);
            helper.assertTrue(arrow.isOnFire(), "Arrow from a Flame bow should spawn on fire");
            helper.assertTrue(arrow.getOwner() != null && arrow.getOwner().getUuid().equals(player.getUuid()),
                "Fired arrow's owner should be the shooting player");

            ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 140,
                "Flame II shot should burn the target 7s = 140 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityCreativeNoConsume(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct",
            "capmod_enabled", "infinity");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.03");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("infinity", 5);
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        Box search = player.getBoundingBox().expand(64.0);
        try {
            // creative shots spawn arrows without consuming inventory arrows
            ItemStack plainBow = new ItemStack(Items.BOW);
            player.getInventory().setStack(0, plainBow);
            player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
            int arrowsInvBefore = player.getInventory().count(Items.ARROW);
            int entBefore = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            plainBow.getItem().use(world, player, Hand.MAIN_HAND);
            plainBow.getItem().onStoppedUsing(plainBow, world, player, 0);
            int entAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            helper.assertTrue(entAfter == entBefore + 1,
                "Creative shot should spawn one arrow (before=" + entBefore + ", after=" + entAfter + ")");
            helper.assertTrue(player.getInventory().count(Items.ARROW) == arrowsInvBefore,
                "Creative shot must not consume inventory arrows (before=" + arrowsInvBefore + ", after="
                    + player.getInventory().count(Items.ARROW) + ")");

            // infinity shots in creative also preserve inventory arrows
            ItemStack infBow = new ItemStack(Items.BOW);
            infBow.addEnchantment(Enchantments.INFINITY, 5);
            player.getInventory().setStack(0, infBow);
            int base = player.getInventory().count(Items.ARROW);
            for (int i = 0; i < 20; i++) {
                infBow.getItem().use(world, player, Hand.MAIN_HAND);
                infBow.getItem().onStoppedUsing(infBow, world, player, 0);
                helper.assertTrue(player.getInventory().count(Items.ARROW) == base,
                    "Creative Infinity shots must never consume arrows (iteration " + i + ", got "
                        + player.getInventory().count(Items.ARROW) + ")");
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreProtectionUncappedEpf(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.96");
        try {
            net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().generic();

            ZombieEntity e32 = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            equipArmorSet(e32, Enchantments.PROTECTION, 8); // 4 * 8 = `EPF` 32
            float r32 = ETTestHelper.modifyAppliedDamage(e32, source, 20.0f);
            float exp32 = (float) (20.0 * Math.pow(0.96, 32));
            helper.assertTrue(Math.abs(r32 - exp32) < 0.05f, "EPF 32 should give ~" + exp32 + " (got " + r32 + ")");

            ZombieEntity e40 = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            equipArmorSet(e40, Enchantments.PROTECTION, 10); // 4 * 10 = `EPF` 40
            float r40 = ETTestHelper.modifyAppliedDamage(e40, source, 20.0f);
            float exp40 = (float) (20.0 * Math.pow(0.96, 40));
            helper.assertTrue(Math.abs(r40 - exp40) < 0.05f, "EPF 40 should give ~" + exp40 + " (got " + r40 + ")");
            helper.assertTrue(r40 < r32,
                "EPF 40 must deal strictly less damage than EPF 32 (scaling past the vanilla 20 cap): r40=" + r40
                    + ", r32=" + r32);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // thunderstorms retain vanilla channeling behavior at every level
    // the mixin does not suppress this path

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreChannelingThunderstormLightning(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 1);
        ServerWorld world = helper.getWorld();
        float[] originalRainGradients = ETTestHelper.snapshotRainGradient(world);
        world.setWeather(0, 100000, true, true); // thunderstorm
        forceThunderGradient(world, 1.0f);
        BlockPos absBase = helper.getAbsolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getTopY() - 20.0;
        ZombieEntity target = EntityType.ZOMBIE.create(world);
        target.setPos(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.spawnEntity(target);
        ZombieEntity owner = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.addEnchantment(Enchantments.CHANNELING, 1);
        TridentEntity trident = new TridentEntity(world, owner, tridentStack);
        trident.setPos(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.spawnEntity(trident);
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            Box searchBox = new Box(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10, skyY + 20,
                absBase.getZ() + 10);
            List<LightningEntity> lightnings = world.getEntitiesByType(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(!lightnings.isEmpty(),
                "Channeling I in a real thunderstorm should summon lightning (orig short-circuit)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world
                .getEntitiesByType(EntityType.LIGHTNING_BOLT, new Box(absBase.getX() - 10, skyY - 10,
                    absBase.getZ() - 10, absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true)
                .forEach(Entity::discard);
            world.setWeather(100000, 0, false, false);
            forceThunderGradient(world, 0.0f);
            ETTestHelper.restoreRainGradient(world, originalRainGradients);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // uncapped flame three burns targets for 180 ticks

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameUncappedLevel3(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level",
            "capmod_enabled", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("flame", 3);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 3);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        arrow.applyEnchantmentEffects(shooter, 0.0f);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 180,
                "Flame III should burn 9s = 180 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // real survival shots test free and consumed arrows
    // rate one preserves arrows and zero consumes them

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityRealPathFreeConsume(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        player.getInventory().setStack(0, bow);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        Box search = player.getBoundingBox().expand(64.0);
        try {
            // free: pct=1.0 -> threshold 0 -> always free
            ETTestHelper.setConfigValue("more_infinity_pct", "1.0");
            int freeBefore = player.getInventory().count(Items.ARROW);
            int entBefore = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            bow.getItem().use(world, player, Hand.MAIN_HAND);
            bow.getItem().onStoppedUsing(bow, world, player, 0);
            int entAfter = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            helper.assertTrue(entAfter == entBefore + 1,
                "FREE shot should spawn one arrow (before=" + entBefore + ", after=" + entAfter + ")");
            helper.assertTrue(player.getInventory().count(Items.ARROW) == freeBefore,
                "FREE shot (pct=1.0) must not consume an arrow (before=" + freeBefore + ", after="
                    + player.getInventory().count(Items.ARROW) + ")");

            // consume: pct=0.0 -> threshold 1 -> always consume
            ETTestHelper.setConfigValue("more_infinity_pct", "0.0");
            int consumeBefore = player.getInventory().count(Items.ARROW);
            int entBefore2 = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            bow.getItem().use(world, player, Hand.MAIN_HAND);
            bow.getItem().onStoppedUsing(bow, world, player, 0);
            int entAfter2 = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            helper.assertTrue(entAfter2 == entBefore2 + 1,
                "CONSUME shot should spawn one arrow (before=" + entBefore2 + ", after=" + entAfter2 + ")");
            helper.assertTrue(player.getInventory().count(Items.ARROW) == consumeBefore - 1,
                "CONSUME shot (pct=0.0) must consume exactly one arrow (before=" + consumeBefore + ", after="
                    + player.getInventory().count(Items.ARROW) + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingLevelScalingRealPath(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setFeature("better_mending", false);
        ETTestHelper.setConfigValue("more_mending_step", "0.05");
        ETTestHelper.setConfigValue("more_mending_floor", "0.1");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 3);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        try {
            int[] levels = {1, 2, 3};
            int[] expected = {45, 50, 55};
            for (int idx = 0; idx < levels.length; idx++) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.addEnchantment(Enchantments.MENDING, levels[idx]);
                sword.setDamage(100);
                player.getInventory().setStack(0, sword);
                ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 100);
                int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
                helper.assertTrue(remaining == expected[idx], "MoreMending level " + levels[idx] + " should leave "
                    + expected[idx] + " XP (got " + remaining + ")");
                helper.assertTrue(sword.getDamage() == 0, "MoreMending level " + levels[idx]
                    + " should fully repair the sword (damage=" + sword.getDamage() + ")");
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // negative mending floors clamp to zero
    // level twelve repairs items without experience cost

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingNegativeFloorFreeRepair(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setFeature("better_mending", false);
        ETTestHelper.setConfigValue("more_mending_step", "0.05");
        ETTestHelper.setConfigValue("more_mending_floor", "-0.5");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 12);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.MENDING, 12);
        sword.setDamage(100);
        player.getInventory().setStack(0, sword);
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 100);
        try {
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
            helper.assertTrue(remaining == 100,
                "Negative floor clamps to 0 -> factor 0 -> free repair, all 100 XP kept (got " + remaining + ")");
            helper.assertTrue(sword.getDamage() == 0,
                "Free repair should fully mend the sword (damage=" + sword.getDamage() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // zero mending step keeps the factor at 0.6
    // the repair leaves 40 experience

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingZeroStepNoScaling(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setFeature("better_mending", false);
        ETTestHelper.setConfigValue("more_mending_step", "0.0");
        ETTestHelper.setConfigValue("more_mending_floor", "0.1");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 5);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.MENDING, 5);
        sword.setDamage(100);
        player.getInventory().setStack(0, sword);
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 100);
        try {
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
            helper.assertTrue(remaining == 40,
                "step=0 pins factor at 0.6 -> cost 60 -> remaining 40 (got " + remaining + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMendingMultiItemRecursion(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setFeature("better_mending", false);
        ETTestHelper.setConfigValue("more_mending_step", "0.05");
        ETTestHelper.setConfigValue("more_mending_floor", "0.1");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 5);
        ServerWorld world = helper.getWorld();
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.addEnchantment(Enchantments.MENDING, 1);
        helmet.setDamage(20);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.MENDING, 5);
        sword.setDamage(40);
        player.getInventory().armor.set(3, helmet); // head
        player.getInventory().setStack(0, sword); // mainhand
        ExperienceOrbEntity orb = new ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 1000);
        try {
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 1000);
            helper.assertTrue(remaining == 975,
                "Per-item capture: cost 11 (L1 helmet) + 14 (L5 sword) -> remaining 975 (got " + remaining + ")");
            helper.assertTrue(helmet.getDamage() == 0 && sword.getDamage() == 0,
                "Both Mending items should be fully repaired (helmet=" + helmet.getDamage() + ", sword="
                    + sword.getDamage() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // count arrow entities from real crossbow charging and firing

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotRealFireEntities(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "capmod_enabled", "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().setStack(0, crossbow);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        Box search = player.getBoundingBox().expand(64.0);
        try {
            int before = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            crossbow.getItem().use(world, player, Hand.MAIN_HAND); // begin charge
            crossbow.getItem().onStoppedUsing(crossbow, world, player, 0); // finish charge (loads 5)
            crossbow.getItem().use(world, player, Hand.MAIN_HAND); // fire all charged
            int spawned = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size() - before;
            helper.assertTrue(spawned == 5,
                "Multishot II (per_level=2) should fire 5 arrow entities (got " + spawned + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotRealFireDisabled(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "capmod_enabled",
            "multishot");
        ETTestHelper.setFeature("more_multishot", false);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        player.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().setStack(0, crossbow);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        Box search = player.getBoundingBox().expand(64.0);
        try {
            int before = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size();
            crossbow.getItem().use(world, player, Hand.MAIN_HAND);
            crossbow.getItem().onStoppedUsing(crossbow, world, player, 0);
            crossbow.getItem().use(world, player, Hand.MAIN_HAND);
            int spawned = world.getEntitiesByClass(PersistentProjectileEntity.class, search, e -> true).size() - before;
            helper.assertTrue(spawned == 3,
                "Vanilla Multishot should fire 3 arrow entities when more_multishot disabled (got " + spawned + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // god armor stacks protection types for epf 48

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreProtectionGodArmorEpf48(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "god_armor",
            "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setFeature("god_armor", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.96");
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
            armor.addEnchantment(Enchantments.BLAST_PROTECTION, 4);
            entity.equipStack(slot, armor);
        }
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources()
            .explosion((net.minecraft.world.explosion.Explosion) null);
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = (float) (20.0 * Math.pow(0.96, 48));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.05f,
                "EPF 48 (God Armor type-stacking) should give ~" + expected + " (got " + result + ")");
            helper.assertTrue(Math.abs(result - 4.0f) > 0.5f,
                "Multiplicative EPF 48 must differ from vanilla's 20-EPF-clamped 4.0 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameHugePerLevelClamped(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "200000000");
        ETTestHelper.setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        arrow.applyEnchantmentEffects(shooter, 0.0f);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 2_147_483_640,
                "Huge more_flame_per_level should clamp to the largest safe tick count (got " + target.getFireTicks()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // non-burning arrows do not apply flame effects

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameArrowNotOnFire(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        // deliberately not on fire
        world.spawnEntity(arrow);
        ((FlameLevelAccess) arrow).enchanttweaker$setFlameLevel(2);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        // new mobs may begin with negative fire ticks
        // compare the initial value rather than assuming zero
        int fireTicksBefore = target.getFireTicks();
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == fireTicksBefore && target.getFireTicks() <= 0,
                "A non-burning arrow must not set the target on fire regardless of Flame level (before "
                    + fireTicksBefore + ", after " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // ownerless flaming arrows use vanilla burn duration

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFlameNullOwner(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ServerWorld world = helper.getWorld();
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(),
            (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        // no owner set
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit",
                EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 100,
                "An on-fire arrow with a null owner (flameLevel 0) should burn vanilla 100 ticks (got "
                    + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // fire protection tests clamps fallbacks and levels

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionLevel0(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.85");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        entity.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS)); // no fire protection
        try {
            int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
            helper.assertTrue(result == 200,
                "Fire protection level 0 should fall through to vanilla (200, got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionNegativeBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "-0.5");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        try {
            int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
            helper.assertTrue(result == 0, "Negative base clamps to 0 -> duration 0 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionUpperBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "5.0");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        try {
            int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
            helper.assertTrue(result == 200, "base > 1 clamps to 1 -> duration unchanged at 200 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionNonNumericBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "abc");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        try {
            int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
            int expected = (int) (200 * Math.pow(0.85, 4)); // falls back to default 0.85 -> 104
            helper.assertTrue(result == expected,
                "Non-numeric base falls back to 0.85 -> " + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionLevelScaling(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.85");
        int[] levels = {1, 2, 3};
        int[] expected = {170, 144, 122}; // (int)(200 * 0.85^level)
        try {
            for (int i = 0; i < levels.length; i++) {
                ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2 * i, 2, 0));
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                boots.addEnchantment(Enchantments.FIRE_PROTECTION, levels[i]);
                entity.equipStack(EquipmentSlot.FEET, boots);
                int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
                helper.assertTrue(result == expected[i],
                    "Fire Protection " + levels[i] + " should give " + expected[i] + " (got " + result + ")");
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // blast protection tests clamps capmod and fallbacks

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionUpperClamp(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "5.0");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        try {
            double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity, 1.0);
            helper.assertTrue(Math.abs(result - 1.0) < 0.001,
                "base > 1 clamps to 1 -> knockback unchanged ~1.0 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionLowerClamp(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        try {
            for (String base : new String[]{"0.0", "-1"}) {
                ETTestHelper.setConfigValue("more_blast_protection_base", base);
                double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity,
                    1.0);
                helper.assertFalse(Double.isNaN(result), "base=" + base + " must not produce NaN knockback");
                helper.assertTrue(Math.abs(result - 0.0) < 0.001,
                    "base <= 0 clamps to 0 -> knockback ~0.0 (base=" + base + ", got " + result + ")");
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionCapmod8(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base", "capmod_enabled", "blast_protection");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "0.85");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("blast_protection", 8);
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 8);
        entity.equipStack(EquipmentSlot.FEET, boots);
        try {
            double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity, 1.0);
            double expected = Math.pow(0.85, 8);
            helper.assertTrue(result > 0.0, "Multiplicative blast protection never reaches 0 (got " + result + ")");
            helper.assertTrue(Math.abs(result - expected) < 0.001,
                "Blast Protection 8 should give ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionLevel0(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "0.85");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        entity.equipStack(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS)); // no blast protection
        try {
            double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity, 1.0);
            helper.assertTrue(Math.abs(result - 1.0) < 0.001,
                "Blast protection level 0 falls through to vanilla -> knockback 1.0 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionMalformedBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "not-a-number");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        try {
            double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity, 1.0);
            double expected = Math.pow(0.85, 4); // falls back to default 0.85
            helper.assertTrue(Math.abs(result - expected) < 0.001,
                "Malformed base falls back to 0.85 -> ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // protection tests upper clamps and malformed bases

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreProtectionUpperClampBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        try {
            for (String base : new String[]{"5.0", "1.0"}) {
                ETTestHelper.setConfigValue("more_protection_base", base);
                ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
                equipArmorSet(entity, Enchantments.PROTECTION, 4); // `EPF` 16
                net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().generic();
                float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
                helper.assertTrue(Math.abs(result - 20.0f) < 0.01f,
                    "base >= 1 clamps to 1 -> damage unchanged at 20.0 (base=" + base + ", got " + result + ")");
                entity.discard();
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreProtectionMalformedBase(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "abc");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        equipArmorSet(entity, Enchantments.PROTECTION, 4); // `EPF` 16
        net.minecraft.entity.damage.DamageSource source = helper.getWorld().getDamageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = (float) (20.0 * Math.pow(0.96, 16));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.05f,
                "Malformed base falls back to 0.96 -> ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // looting tests zero levels capmod and truncation

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreLootingZeroLevelGuard(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); // no Looting
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            // zero looting level leaves experience unchanged
            int expected = expectedLootingXp(base, 0, 0.5);
            helper.assertTrue(total == expected && expected == base,
                "lootingLevel<=0 guard should leave XP unchanged at " + base + " (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreLootingCapmodLevel5(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier",
            "capmod_enabled", "looting");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("looting", 5);
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 5);
        player.equipStack(EquipmentSlot.MAINHAND, sword);
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            // looting five with multiplier 0.5 yields 35 experience
            int expected = expectedLootingXp(base, 5, 0.5);
            helper.assertTrue(total == expected,
                "Looting 5 (capmod) with multiplier 0.5 should give " + expected + " XP (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreLootingTruncation(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 1);
        player.equipStack(EquipmentSlot.MAINHAND, sword);
        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 3;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            int expected = expectedLootingXp(base, 1, 0.5);
            helper.assertTrue(total == expected && expected == 4,
                "Looting 1 x0.5 on 3 XP should truncate the boosted total to 4 (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    // additional infinity edge cases

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityNonArrowProjectileConsumed(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "1.0"); // a plain arrow would always be free here
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0)); // non-creative
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            // plain arrows always preserve at rate one
            ItemStack plain = (ItemStack) m.invoke(null, bow, new ItemStack(Items.ARROW), shooter, false);
            helper.assertTrue(plain.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                "Plain arrow at pct=1.0 should be free (intangible) - the contrast case");
            // spectral arrows fail vanilla's plain-arrow infinity gate
            ItemStack spectral = (ItemStack) m.invoke(null, bow, new ItemStack(Items.SPECTRAL_ARROW), shooter, false);
            helper.assertFalse(spectral.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                "Spectral arrow must never be armed free by MoreInfinity (vanilla Items.ARROW gate)");
            // tipped arrow: same gate
            ItemStack tipped = (ItemStack) m.invoke(null, bow, new ItemStack(Items.TIPPED_ARROW), shooter, false);
            helper.assertFalse(tipped.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                "Tipped arrow must never be armed free by MoreInfinity (vanilla Items.ARROW gate)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreInfinityNegativePctAlwaysConsumes(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "-1.0");
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0)); // non-creative
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            // negative rates clamp the threshold to one
            // every arrow is consumed
            for (int i = 0; i < 20; i++) {
                ItemStack proj = (ItemStack) m.invoke(null, bow, new ItemStack(Items.ARROW), shooter, false);
                helper.assertFalse(proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                    "Negative more_infinity_pct must always consume the arrow (iteration " + i + ")");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreBindingMultipleItemsRestoredToCorrectSlots(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0"); // level five always keeps bound items
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        try {
            ServerPlayerEntity old = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
            BlockPos pos = helper.getAbsolutePos(new BlockPos(1, 2, 1));
            old.setPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.addEnchantment(Enchantments.BINDING_CURSE, 5);
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            boots.addEnchantment(Enchantments.BINDING_CURSE, 5);
            old.getInventory().armor.set(3, helmet); // slot 3 = head
            old.getInventory().armor.set(0, boots); // slot 0 = feet

            old.getInventory().dropAll();
            helper.assertTrue(
                old.getInventory().armor.get(3).isOf(Items.DIAMOND_HELMET)
                    && old.getInventory().armor.get(0).isOf(Items.DIAMOND_BOOTS),
                "MoreBinding should retain BOTH bound pieces after dropAll (helmet@3, boots@0)");

            ServerPlayerEntity respawned = ETTestHelper.createServerPlayer(helper, GameMode.SURVIVAL);
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            respawned.getInventory().armor.set(0, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);

            ItemStack head = respawned.getInventory().armor.get(3);
            ItemStack feet = respawned.getInventory().armor.get(0);
            helper.assertTrue(
                head.isOf(Items.DIAMOND_HELMET) && EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, head) == 5,
                "AFTER_RESPAWN should restore the helmet to head slot 3 (got " + head + ")");
            helper.assertTrue(
                feet.isOf(Items.DIAMOND_BOOTS) && EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, feet) == 5,
                "AFTER_RESPAWN should restore the boots to feet slot 0 (got " + feet + ")");

            // a second respawn finds no remaining stash entry
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            respawned.getInventory().armor.set(0, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);
            helper.assertTrue(
                respawned.getInventory().armor.get(3).isEmpty() && respawned.getInventory().armor.get(0).isEmpty(),
                "A second AFTER_RESPAWN should restore nothing (stash entry already consumed)");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

    @GameTest(
        templateName = EMPTY_STRUCTURE)
    public void moreMultishotCapmodHighLevel(TestContext helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "capmod_enabled", "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 10);
        ServerPlayerEntity player = ETTestHelper.createServerPlayer(helper, GameMode.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 10);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 21,
                "Multishot 10 (capmod) with per_level=2 should load 21 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.complete();
    }

}
