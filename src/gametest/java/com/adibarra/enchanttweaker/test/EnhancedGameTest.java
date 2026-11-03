package com.adibarra.enchanttweaker.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import com.adibarra.enchanttweaker.FlameLevelAccess;
import com.adibarra.enchanttweaker.MendingLevelAccess;
import com.adibarra.utils.ADUtils;

public class EnhancedGameTest implements FabricGameTest {

    // more multishot level two loads five projectiles
    // vanilla loads three projectiles

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 2);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getItems() : List.of();
            helper.assertTrue(projectiles.size() == 5,
                "Multishot II should load 5 projectiles with more_multishot enabled (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "multishot");
        ETTestHelper.setFeature("more_multishot", false);
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 2);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getItems() : List.of();
            helper.assertTrue(projectiles.size() == 3,
                "Multishot (vanilla) should load 3 projectiles when more_multishot disabled (got " + projectiles.size()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotPracticalCap(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "300");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 1);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getItems() : List.of();
            helper.assertTrue(projectiles.size() == 256,
                "MoreMultishot should cap practical projectile counts (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotProjectileCountBounds(GameTestHelper helper) {
        try {
            Method projectileCount = ProjectileWeaponItem.class
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
        helper.succeed();
    }

    // more flame level two burns for 140 ticks

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setEnchantCap("flame", 2);
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 2);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, flameBow);

        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        arrow.setEnchantmentEffectsFromEntity(shooter, 0.0f);

        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // flame two sets the target on fire for 140 ticks
            helper.assertTrue(target.getRemainingFireTicks() == 140,
                "Flame II should burn target exactly 140 ticks (got " + target.getRemainingFireTicks() + " ticks)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "flame");
        ETTestHelper.setFeature("more_flame", false);
        ETTestHelper.setEnchantCap("flame", 2);
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 2);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, flameBow);

        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        arrow.setEnchantmentEffectsFromEntity(shooter, 0.0f);

        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // flame II with mixin disabled: vanilla setOnFireFor(5) = 100 ticks
            helper.assertTrue(target.getRemainingFireTicks() == 100,
                "Flame II with more_flame disabled should burn target 100 ticks (got " + target.getRemainingFireTicks()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlamePlayerShotKeepsWeaponLevel(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setEnchantCap("flame", 3);
        ServerLevel world = helper.getLevel();
        ServerPlayer shooter = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.FLAME, 3);
        try {
            Method createArrow = ProjectileWeaponItem.class.getDeclaredMethod("createArrowEntity", Level.class,
                LivingEntity.class, ItemStack.class, ItemStack.class, boolean.class);
            createArrow.setAccessible(true);
            Projectile projectile = (Projectile) createArrow.invoke(bow.getItem(), world, shooter, bow,
                new ItemStack(Items.ARROW), false);
            helper.assertTrue(projectile instanceof Arrow, "bow projectile should be an ArrowEntity");
            helper.assertTrue(((FlameLevelAccess) projectile).enchanttweaker$getFlameLevel() == 3,
                "projectile should capture Flame III from the fired weapon");

            Arrow arrow = (Arrow) projectile;
            arrow.igniteForSeconds(10);
            world.addFreshEntity(arrow);
            Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 180,
                "player-fired Flame III arrow should burn for 180 ticks after weapon state is unavailable (got "
                    + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameLevelSurvivesNbtRoundTrip(GameTestHelper helper) {
        ServerLevel world = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow original = new Arrow(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.ARROW));
        ((FlameLevelAccess) original).enchanttweaker$setFlameLevel(7);
        CompoundTag nbt = new CompoundTag();
        original.saveWithoutId(nbt);

        Arrow restored = new Arrow(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Items.ARROW));
        restored.load(nbt);
        helper.assertTrue(((FlameLevelAccess) restored).enchanttweaker$getFlameLevel() == 7,
            "serialized projectile should restore captured Flame level 7");
        helper.succeed();
    }

    // more mending costs 60 without mending
    // level one costs 55
    // direct invocation avoids recursive gear repair

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "more_mending_step",
            "more_mending_floor");
        try {
            ETTestHelper.setFeature("more_mending", true);
            ETTestHelper.setConfigValue("more_mending_step", "0.05");
            ETTestHelper.setConfigValue("more_mending_floor", "0.1");
            ServerLevel world = helper.getLevel();
            BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
            net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(world,
                pos.getX(), pos.getY(), pos.getZ(), 0);
            Method getMRC = net.minecraft.world.entity.ExperienceOrb.class.getDeclaredMethod("getMendingRepairCost",
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending");
        ETTestHelper.setFeature("more_mending", false);
        ServerLevel world = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(world, pos.getX(),
            pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.world.entity.ExperienceOrb.class.getDeclaredMethod("getMendingRepairCost",
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
        helper.succeed();
    }

    // moreInfinity
    // infinity level 34 always preserves arrows

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct",
            "capmod_enabled", "infinity");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.03");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("infinity", 34);
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.INFINITY, 34);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            // `getProjectile` is protected static, access via reflection
            Method m = ProjectileWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
            // level 34 produces an intangible projectile
            helper.assertTrue(proj.has(DataComponents.INTANGIBLE_PROJECTILE),
                "Level-34 Infinity should always give intangible projectile (threshold=0)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity");
        ETTestHelper.setFeature("more_infinity", false);
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.INFINITY, 1);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            Method m = ProjectileWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            // vanilla Infinity I: always intangible, mixin must not interfere
            ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
            helper.assertTrue(proj.has(DataComponents.INTANGIBLE_PROJECTILE),
                "Vanilla Infinity I should give intangible projectile when more_infinity disabled");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void randomChanceThresholdBoundaries(GameTestHelper helper) {
        helper.assertTrue(ADUtils.bindingKeepsItem(5, 0.125, 0.5f),
            "Binding roll exactly at the keep threshold should keep the item");
        helper.assertFalse(ADUtils.bindingKeepsItem(5, 0.125, Math.nextDown(0.5f)),
            "Binding roll immediately below the keep threshold should drop the item");
        helper.assertTrue(ADUtils.infinityPreservesArrow(2, 0.25, 0.5f),
            "Infinity roll exactly at the preserve threshold should preserve the arrow");
        helper.assertFalse(ADUtils.infinityPreservesArrow(2, 0.25, Math.nextDown(0.5f)),
            "Infinity roll immediately below the preserve threshold should consume the arrow");
        helper.succeed();
    }

    // moreBinding
    // a high-level item is retained without random trials

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBindingEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.enchant(Enchantments.BINDING_CURSE, 5);
        player.getInventory().armor.set(3, helmet);
        try {
            player.getInventory().dropAll();
            helper.assertFalse(player.getInventory().armor.get(3).isEmpty(),
                "MoreBinding should retain Binding Curse V armor with a zero drop threshold");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBindingDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "capmod_enabled",
            "binding_curse");
        ETTestHelper.setFeature("more_binding", false);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 10);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.enchant(Enchantments.BINDING_CURSE, 10);
        player.getInventory().armor.set(3, helmet);
        try {
            player.getInventory().dropAll();
            helper.assertTrue(player.getInventory().armor.get(3).isEmpty(),
                "Vanilla dropAll should always drop Binding Curse armor when more_binding disabled");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // more channeling enables channeling two during rain
    // the mixin accepts thunderstorms or level-two rain

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreChannelingEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 2);
        ServerLevel world = helper.getLevel();
        float[] originalRainGradients = ETTestHelper.snapshotRainGradient(world);
        // rain without thunder should still enable channeling two
        world.setWeatherParameters(0, 100000, true, false);
        ETTestHelper.forceRainGradient(world, 1.0f); // israining checks the rain gradient
                                                     // the gradient updates lazily each tick
        // place the target high in the sky to satisfy hasDirectSkyLight
        BlockPos absBase = helper.absolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getMaxBuildHeight() - 20.0;
        Zombie target = EntityType.ZOMBIE.create(world);
        target.setPosRaw(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.addFreshEntity(target);
        Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.enchant(Enchantments.CHANNELING, 2);
        ThrownTrident trident = new ThrownTrident(world, owner, tridentStack);
        trident.setPosRaw(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.addFreshEntity(trident);
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            AABB searchBox = new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10,
                skyY + 20, absBase.getZ() + 10);
            List<LightningBolt> lightnings = world.getEntities(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(!lightnings.isEmpty(),
                "MoreChanneling II should summon lightning in rain (no lightning spawned)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world.getEntities(EntityType.LIGHTNING_BOLT, new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10,
                absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true).forEach(Entity::discard);
            world.setWeatherParameters(100000, 0, false, false);
            ETTestHelper.restoreRainGradient(world, originalRainGradients);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreChannelingDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", false);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 2);
        ServerLevel world = helper.getLevel();
        world.setWeatherParameters(0, 100000, true, false); // rain, no thunder
        BlockPos absBase = helper.absolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getMaxBuildHeight() - 20.0;
        Zombie target = EntityType.ZOMBIE.create(world);
        target.setPosRaw(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.addFreshEntity(target);
        Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.enchant(Enchantments.CHANNELING, 2);
        ThrownTrident trident = new ThrownTrident(world, owner, tridentStack);
        trident.setPosRaw(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.addFreshEntity(trident);
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            AABB searchBox = new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10,
                skyY + 20, absBase.getZ() + 10);
            List<LightningBolt> lightnings = world.getEntities(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(lightnings.isEmpty(),
                "Channeling II in rain (no thunder) without mixin should NOT summon lightning");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world.getEntities(EntityType.LIGHTNING_BOLT, new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10,
                absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true).forEach(Entity::discard);
            world.setWeatherParameters(100000, 0, false, false);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // channeling one does not summon lightning during rain
    // the mixin requires level two

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreChannelingLevel1RainNoLightning(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 1);
        ServerLevel world = helper.getLevel();
        float[] originalRainGradients = ETTestHelper.snapshotRainGradient(world);
        world.setWeatherParameters(0, 100000, true, false);
        ETTestHelper.forceRainGradient(world, 1.0f);
        BlockPos absBase = helper.absolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getMaxBuildHeight() - 20.0;
        Zombie target = EntityType.ZOMBIE.create(world);
        target.setPosRaw(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.addFreshEntity(target);
        Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.enchant(Enchantments.CHANNELING, 1);
        ThrownTrident trident = new ThrownTrident(world, owner, tridentStack);
        trident.setPosRaw(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.addFreshEntity(trident);
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            AABB searchBox = new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10,
                skyY + 20, absBase.getZ() + 10);
            List<LightningBolt> lightnings = world.getEntities(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(lightnings.isEmpty(),
                "Channeling I in rain should NOT summon lightning even with more_channeling enabled");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world.getEntities(EntityType.LIGHTNING_BOLT, new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10,
                absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true).forEach(Entity::discard);
            world.setWeatherParameters(100000, 0, false, false);
            ETTestHelper.restoreRainGradient(world, originalRainGradients);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // moreMultishot: level edge cases

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotLevel1Vanilla(GameTestHelper helper) {
        // level one still loads three projectiles
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 1);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getItems() : List.of();
            helper.assertTrue(projectiles.size() == 3,
                "Multishot I with more_multishot should still load 3 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotLevel3(GameTestHelper helper) {
        // level 3: 3*2+1 = 7 projectiles
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "capmod_enabled", "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 3);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 3);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getItems() : List.of();
            helper.assertTrue(projectiles.size() == 7,
                "Multishot III should load 7 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // moreFlame: level 1 = vanilla

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameLevel1Vanilla(GameTestHelper helper) {
        // level one burns targets for 100 ticks
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 1);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        arrow.setEnchantmentEffectsFromEntity(shooter, 0.0f);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 100,
                "Flame I with more_flame enabled should still burn 100 ticks (got " + target.getRemainingFireTicks()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // moreMending: high level clamp

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingHighLevel(GameTestHelper helper) {
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
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingSword.enchant(Enchantments.MENDING, 10);
            mendingSword.setDamageValue(200);
            player.getInventory().setItem(0, mendingSword); // mainhand
            BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
            net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(world,
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingBetterMendingCombined(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        try {
            ETTestHelper.setFeature("more_mending", true);
            ETTestHelper.setFeature("better_mending", true);
            ETTestHelper.setConfigValue("more_mending_step", "0.05");
            ETTestHelper.setConfigValue("more_mending_floor", "0.1");
            ETTestHelper.setCapmod(true);
            ETTestHelper.setEnchantCap("mending", 5);
            ServerLevel world = helper.getLevel();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack mendingSword = new ItemStack(Items.DIAMOND_SWORD);
            mendingSword.enchant(Enchantments.MENDING, 5);
            mendingSword.setDamageValue(40);
            player.getInventory().setItem(0, mendingSword); // mainhand: BetterMending's first pick
            BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
            net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(world,
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingFloorClampNoCrash(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "more_mending_step",
            "more_mending_floor");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setConfigValue("more_mending_step", "0.05");
        ETTestHelper.setConfigValue("more_mending_floor", "0.9"); // > 0.6 -> would crash pre-fix
        ServerLevel world = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(world, pos.getX(),
            pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.world.entity.ExperienceOrb.class.getDeclaredMethod("getMendingRepairCost",
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
        helper.succeed();
    }

    // moreInfinity: level 1 consumes arrows

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityLevel1Consumes(GameTestHelper helper) {
        // zero rate makes level one consumption deterministic
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.0");
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.INFINITY, 1);
        try {
            Method m = ProjectileWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            ItemStack projectile = (ItemStack) m.invoke(null, bow, new ItemStack(Items.ARROW), shooter, false);
            helper.assertFalse(projectile.has(DataComponents.INTANGIBLE_PROJECTILE),
                "MoreInfinity level 1 with zero free-shot rate must consume the arrow");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // moreInfinity: custom rate

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityCustomRateHigh(GameTestHelper helper) {
        // rate one always preserves level-one arrows
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "1.0");
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.INFINITY, 1);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            Method m = ProjectileWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            for (int i = 0; i < 50; i++) {
                ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
                helper.assertTrue(proj.has(DataComponents.INTANGIBLE_PROJECTILE),
                    "more_infinity_pct=1.0 level 1 should always give free arrow (iteration " + i + ")");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityCustomRateZero(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct",
            "capmod_enabled", "infinity");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("infinity", 34);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.0");
        ServerLevel world = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.INFINITY, 34);
        player.getInventory().setItem(0, bow);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            int before = player.getInventory().countItem(Items.ARROW);
            final int shots = 5;
            for (int i = 0; i < shots; i++) {
                bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
                bow.getItem().releaseUsing(bow, world, player, 0);
            }
            int after = player.getInventory().countItem(Items.ARROW);
            helper.assertTrue(after == before - shots,
                "more_infinity_pct=0.0 must consume one arrow per shot: expected " + (before - shots) + " (got " + after
                    + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // protection uses multiplicative damage reduction
    // protection four on four pieces has epf 16
    // damage 20 becomes approximately 10.397

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreProtectionEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.96");
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
        net.minecraft.world.damagesource.DamageSource source = helper.getLevel().damageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = (float) (20.0 * Math.pow(0.96, 16));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.01f,
                "MoreProtection should give ~" + expected + " damage (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreProtectionDisabled(GameTestHelper helper) {
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
        net.minecraft.world.damagesource.DamageSource source = helper.getLevel().damageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = net.minecraft.world.damagesource.CombatRules.getDamageAfterMagicAbsorb(20.0f, 16.0f);
        helper.assertTrue(Math.abs(result - expected) < 0.01f,
            "Vanilla protection should give ~" + expected + " damage (got " + result + ")");
        helper.succeed();
    }

    // fire protection four reduces 200 ticks to 104
    // vanilla instead reduces it to 80

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFireProtectionEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.85");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.FIRE_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        int result = net.minecraft.world.item.enchantment.ProtectionEnchantment.getFireAfterDampener(entity, 200);
        int expected = (int) (200 * Math.pow(0.85, 4));
        try {
            helper.assertTrue(result == expected,
                "MoreFireProtection should give duration " + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFireProtectionDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection");
        ETTestHelper.setFeature("more_fire_protection", false);
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.FIRE_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        int result = net.minecraft.world.item.enchantment.ProtectionEnchantment.getFireAfterDampener(entity, 200);
        try {
            helper.assertTrue(result == 80, "Vanilla fire protection should give duration 80 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // blast protection four multiplies knockback by 0.85^4
    // vanilla knockback is 0.4

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBlastProtectionEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "0.85");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.BLAST_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        double result = net.minecraft.world.item.enchantment.ProtectionEnchantment
            .getExplosionKnockbackAfterDampener(entity, 1.0);
        double expected = Math.pow(0.85, 4);
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.001,
                "MoreBlastProtection should give knockback ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBlastProtectionDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection");
        ETTestHelper.setFeature("more_blast_protection", false);
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.BLAST_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        double result = net.minecraft.world.item.enchantment.ProtectionEnchantment
            .getExplosionKnockbackAfterDampener(entity, 1.0);
        try {
            helper.assertTrue(Math.abs(result - 0.4) < 0.001,
                "Vanilla blast protection should give knockback ~0.4 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // moreBinding: level one with a full drop threshold

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBindingLevel1AlwaysDrops(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 1);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.enchant(Enchantments.BINDING_CURSE, 1);
            player.getInventory().armor.set(3, helmet);
            player.getInventory().dropAll();
            helper.assertTrue(player.getInventory().armor.get(3).isEmpty(),
                "MoreBinding level 1 should drop armor at the full threshold");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // configurable values

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreProtectionCustomBase(GameTestHelper helper) {
        // custom base 0.90 instead of default 0.96
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.90");
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
        net.minecraft.world.damagesource.DamageSource source = helper.getLevel().damageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = (float) (20.0 * Math.pow(0.90, 16));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.01f,
                "MoreProtection with base=0.90 should give ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFireProtectionCustomBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.70");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.FIRE_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        int result = net.minecraft.world.item.enchantment.ProtectionEnchantment.getFireAfterDampener(entity, 200);
        int expected = (int) (200 * Math.pow(0.70, 4));
        try {
            helper.assertTrue(result == expected,
                "MoreFireProtection with base=0.70 should give duration " + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBlastProtectionCustomBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "0.70");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.BLAST_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        double result = net.minecraft.world.item.enchantment.ProtectionEnchantment
            .getExplosionKnockbackAfterDampener(entity, 1.0);
        double expected = Math.pow(0.70, 4);
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.001,
                "MoreBlastProtection with base=0.70 should give knockback ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingCustomStepAndFloor(GameTestHelper helper) {
        // custom step and floor retain the level-zero factor
        // the repair cost remains 60
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "more_mending_step",
            "more_mending_floor");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setConfigValue("more_mending_step", "0.1");
        ETTestHelper.setConfigValue("more_mending_floor", "0.05");
        ServerLevel world = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(world, pos.getX(),
            pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.world.entity.ExperienceOrb.class.getDeclaredMethod("getMendingRepairCost",
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotCustomPerLevel(GameTestHelper helper) {
        // custom per_level=3: level 2 -> 2*3+1 = 7 projectiles
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "capmod_enabled", "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 2);
        ETTestHelper.setConfigValue("more_multishot_per_level", "3");
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 2);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getItems() : List.of();
            helper.assertTrue(projectiles.size() == 7,
                "Multishot II with per_level=3 should load 7 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameCustomPerLevel(GameTestHelper helper) {
        // flame two with per-level five burns 200 ticks
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level",
            "capmod_enabled", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("flame", 2);
        ETTestHelper.setConfigValue("more_flame_per_level", "5");
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 2);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        arrow.setEnchantmentEffectsFromEntity(shooter, 0.0f);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 200,
                "Flame II with per_level=5 should burn 200 ticks (got " + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // more Looting (XP Boost)

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreLootingEnabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.LOOTING, 3);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            int expected = expectedLootingXp(base, 3, 0.5);
            helper.assertTrue(total == expected,
                "MoreLooting (Looting III, x0.5) should boost 10 XP to " + expected + " (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreLootingDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting");
        ETTestHelper.setFeature("more_looting", false);
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.LOOTING, 3);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            helper.assertTrue(total == base,
                "MoreLooting disabled should not boost XP: expected " + base + " (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreLootingCustomMultiplier(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "1.0");
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.LOOTING, 2);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 10;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            int expected = expectedLootingXp(base, 2, 1.0);
            helper.assertTrue(total == expected,
                "MoreLooting with multiplier=1.0 and Looting II should give " + expected + " XP (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }
    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameLevel0Vanilla(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        shooter.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        arrow.setEnchantmentEffectsFromEntity(shooter, 0.0f);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 100,
                "Non-Flame fire arrow (flameLevel 0) should burn vanilla 100 ticks (got "
                    + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameNegativePerLevelVanilla(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level",
            "capmod_enabled", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "-3");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("flame", 2);
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 2);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        arrow.setEnchantmentEffectsFromEntity(shooter, 0.0f);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 100,
                "Flame II with negative per_level should clamp to vanilla 100 ticks (got "
                    + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreLootingNegativeMultiplier(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "-1.0");
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.LOOTING, 3);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreProtectionNegativeBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "-0.5");
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
        net.minecraft.world.damagesource.DamageSource source = helper.getLevel().damageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        try {
            helper.assertFalse(Float.isNaN(result), "Negative more_protection_base must not produce NaN damage");
            helper.assertTrue(result == 0.0f,
                "Negative more_protection_base should clamp to 0 -> 0 damage (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotNegativePerLevel(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "-2");
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 2);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getItems() : List.of();
            helper.assertTrue(projectiles.size() == 1,
                "Multishot II with negative per_level should load exactly 1 projectile (got " + projectiles.size()
                    + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }
    private static void forceThunderGradient(ServerLevel world, float gradient) {
        try {
            for (String name : new String[]{"thunderGradient", "thunderGradientPrev", "rainGradient",
                    "rainGradientPrev"}) {
                Field f = Level.class.getDeclaredField(name);
                f.setAccessible(true);
                f.setFloat(world, gradient);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("forceThunderGradient reflection failed", e);
        }
    }

    /** equips enchanted diamond armor at the requested level */
    private static void equipArmorSet(net.minecraft.world.entity.LivingEntity entity,
        net.minecraft.world.item.enchantment.Enchantment ench, int level) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            ItemStack armor = new ItemStack(switch (slot) {
                case HEAD -> Items.DIAMOND_HELMET;
                case CHEST -> Items.DIAMOND_CHESTPLATE;
                case LEGS -> Items.DIAMOND_LEGGINGS;
                case FEET -> Items.DIAMOND_BOOTS;
                default -> Items.DIAMOND_HELMET;
            });
            armor.enchant(ench, level);
            entity.setItemSlot(slot, armor);
        }
    }

    private static int dropAndSumXp(GameTestHelper helper, ServerLevel world, Zombie zombie, ServerPlayer killer,
        int baseXp) {
        try {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                zombie.setItemSlot(slot, ItemStack.EMPTY); // empty slot -> no random getXpToDrop() bonus
            }
            zombie.setBaby(false); // baby zombies multiply experiencePoints by 2.5 inside getXpToDrop()
            Field attackingPlayerField = LivingEntity.class.getDeclaredField("attackingPlayer");
            attackingPlayerField.setAccessible(true);
            attackingPlayerField.set(zombie, killer);
            Field playerHitTimerField = LivingEntity.class.getDeclaredField("playerHitTimer");
            playerHitTimerField.setAccessible(true);
            playerHitTimerField.setInt(zombie, 100);
            Field xpField = net.minecraft.world.entity.Mob.class.getDeclaredField("experiencePoints");
            xpField.setAccessible(true);
            xpField.setInt(zombie, baseXp);

            // remove nearby experience orbs before dropping new ones
            AABB searchBox = zombie.getBoundingBox().inflate(3.0);
            world.getEntitiesOfClass(ExperienceOrb.class, searchBox, e -> true).forEach(e -> e.discard());

            Method dropXp = LivingEntity.class.getDeclaredMethod("dropXp");
            dropXp.setAccessible(true);
            dropXp.invoke(zombie);

            return world.getEntitiesOfClass(ExperienceOrb.class, searchBox, e -> true).stream()
                .mapToInt(ExperienceOrb::getValue).sum();
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
        template = EMPTY_STRUCTURE)
    public void moreBindingAfterRespawnRestore(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0"); // level five always keeps bound items
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        try {
            ServerPlayer old = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            old.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.enchant(Enchantments.BINDING_CURSE, 5);
            old.getInventory().armor.set(3, helmet); // slot 3 = head

            old.getInventory().dropAll();
            // the mixin keeps the helmet in slot three
            // it also stashes a copy for respawn
            helper.assertFalse(old.getInventory().armor.get(3).isEmpty(),
                "MoreBinding should retain the Binding-5 helmet in slot 3 after dropAll");

            ServerPlayer respawned = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);

            ItemStack restored = respawned.getInventory().armor.get(3);
            helper.assertTrue(
                restored.is(Items.DIAMOND_HELMET)
                    && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BINDING_CURSE, restored) == 5,
                "AFTER_RESPAWN should restore the Binding-5 helmet to slot 3 (got " + restored + ")");

            // a second invocation finds no remaining stash entry
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);
            helper.assertTrue(respawned.getInventory().armor.get(3).isEmpty(),
                "A second AFTER_RESPAWN should restore nothing (stash entry already consumed)");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBindingDisconnectClearsPendingArmor(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        try {
            ServerPlayer old = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.enchant(Enchantments.BINDING_CURSE, 5);
            old.getInventory().armor.set(3, helmet);
            old.getInventory().dropAll();
            helper.assertFalse(old.getInventory().armor.get(3).isEmpty(),
                "death path should stage the bound helmet before disconnect");

            ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(old.connection,
                helper.getLevel().getServer());
            ServerPlayer respawned = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);
            helper.assertTrue(respawned.getInventory().armor.get(3).isEmpty(),
                "disconnect should clear armor abandoned before respawn");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreChannelingClearWeatherNoLightning(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 2);
        ServerLevel world = helper.getLevel();
        float[] originalRainGradients = ETTestHelper.snapshotRainGradient(world);
        world.setWeatherParameters(100000, 0, false, false); // clear
        ETTestHelper.forceRainGradient(world, 0.0f);
        BlockPos absBase = helper.absolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getMaxBuildHeight() - 20.0;
        Zombie target = EntityType.ZOMBIE.create(world);
        target.setPosRaw(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.addFreshEntity(target);
        Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.enchant(Enchantments.CHANNELING, 2);
        ThrownTrident trident = new ThrownTrident(world, owner, tridentStack);
        trident.setPosRaw(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.addFreshEntity(trident);
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            AABB searchBox = new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10,
                skyY + 20, absBase.getZ() + 10);
            List<LightningBolt> lightnings = world.getEntities(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(lightnings.isEmpty(),
                "MoreChanneling II in CLEAR weather must NOT summon lightning (isRaining false-branch)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world.getEntities(EntityType.LIGHTNING_BOLT, new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10,
                absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true).forEach(Entity::discard);
            world.setWeatherParameters(100000, 0, false, false);
            ETTestHelper.restoreRainGradient(world, originalRainGradients);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameEndToEndRealShot(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setEnchantCap("flame", 2);
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 2);
        player.getInventory().setItem(0, flameBow);
        player.getInventory().add(new ItemStack(Items.ARROW, 8));
        AABB search = player.getBoundingBox().inflate(64.0);
        try {
            helper.assertTrue(EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, player) == 2,
                "Player should read Flame II from the mainhand bow");
            int before = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            flameBow.getItem().use(world, player, InteractionHand.MAIN_HAND);
            flameBow.getItem().releaseUsing(flameBow, world, player, 0);
            List<AbstractArrow> arrows = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true);
            helper.assertTrue(arrows.size() == before + 1,
                "Flame bow should fire exactly one arrow (before=" + before + ", after=" + arrows.size() + ")");
            AbstractArrow arrow = arrows.get(arrows.size() - 1);
            helper.assertTrue(arrow.isOnFire(), "Arrow from a Flame bow should spawn on fire");
            helper.assertTrue(arrow.getOwner() != null && arrow.getOwner().getUUID().equals(player.getUUID()),
                "Fired arrow's owner should be the shooting player");

            Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 2, 1));
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 140,
                "Flame II shot should burn the target 7s = 140 ticks (got " + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityCreativeNoConsume(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct",
            "capmod_enabled", "infinity");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.03");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("infinity", 5);
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        AABB search = player.getBoundingBox().inflate(64.0);
        try {
            // creative shots spawn arrows without consuming inventory arrows
            ItemStack plainBow = new ItemStack(Items.BOW);
            player.getInventory().setItem(0, plainBow);
            player.getInventory().add(new ItemStack(Items.ARROW, 64));
            int arrowsInvBefore = player.getInventory().countItem(Items.ARROW);
            int entBefore = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            plainBow.getItem().use(world, player, InteractionHand.MAIN_HAND);
            plainBow.getItem().releaseUsing(plainBow, world, player, 0);
            int entAfter = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            helper.assertTrue(entAfter == entBefore + 1,
                "Creative shot should spawn one arrow (before=" + entBefore + ", after=" + entAfter + ")");
            helper.assertTrue(player.getInventory().countItem(Items.ARROW) == arrowsInvBefore,
                "Creative shot must not consume inventory arrows (before=" + arrowsInvBefore + ", after="
                    + player.getInventory().countItem(Items.ARROW) + ")");

            // infinity shots in creative also preserve inventory arrows
            ItemStack infBow = new ItemStack(Items.BOW);
            infBow.enchant(Enchantments.INFINITY, 5);
            player.getInventory().setItem(0, infBow);
            int base = player.getInventory().countItem(Items.ARROW);
            for (int i = 0; i < 20; i++) {
                infBow.getItem().use(world, player, InteractionHand.MAIN_HAND);
                infBow.getItem().releaseUsing(infBow, world, player, 0);
                helper.assertTrue(player.getInventory().countItem(Items.ARROW) == base,
                    "Creative Infinity shots must never consume arrows (iteration " + i + ", got "
                        + player.getInventory().countItem(Items.ARROW) + ")");
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreProtectionUncappedEpf(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.96");
        try {
            net.minecraft.world.damagesource.DamageSource source = helper.getLevel().damageSources().generic();

            Zombie e32 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
            equipArmorSet(e32, Enchantments.PROTECTION, 8); // 4 * 8 = `EPF` 32
            float r32 = ETTestHelper.modifyAppliedDamage(e32, source, 20.0f);
            float exp32 = (float) (20.0 * Math.pow(0.96, 32));
            helper.assertTrue(Math.abs(r32 - exp32) < 0.05f, "EPF 32 should give ~" + exp32 + " (got " + r32 + ")");

            Zombie e40 = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
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
        helper.succeed();
    }

    // thunderstorms retain vanilla channeling behavior at every level
    // the mixin does not suppress this path

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreChannelingThunderstormLightning(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_channeling", "capmod_enabled",
            "channeling");
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 1);
        ServerLevel world = helper.getLevel();
        float[] originalRainGradients = ETTestHelper.snapshotRainGradient(world);
        world.setWeatherParameters(0, 100000, true, true); // thunderstorm
        forceThunderGradient(world, 1.0f);
        BlockPos absBase = helper.absolutePos(new BlockPos(0, 0, 0));
        double skyY = world.getMaxBuildHeight() - 20.0;
        Zombie target = EntityType.ZOMBIE.create(world);
        target.setPosRaw(absBase.getX() + 0.5, skyY, absBase.getZ() + 0.5);
        world.addFreshEntity(target);
        Zombie owner = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        ItemStack tridentStack = new ItemStack(Items.TRIDENT);
        tridentStack.enchant(Enchantments.CHANNELING, 1);
        ThrownTrident trident = new ThrownTrident(world, owner, tridentStack);
        trident.setPosRaw(absBase.getX() + 0.5, skyY + 2.0, absBase.getZ() + 0.5);
        world.addFreshEntity(trident);
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            AABB searchBox = new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10, absBase.getX() + 10,
                skyY + 20, absBase.getZ() + 10);
            List<LightningBolt> lightnings = world.getEntities(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(!lightnings.isEmpty(),
                "Channeling I in a real thunderstorm should summon lightning (orig short-circuit)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            target.discard();
            trident.discard();
            world.getEntities(EntityType.LIGHTNING_BOLT, new AABB(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10,
                absBase.getX() + 10, skyY + 20, absBase.getZ() + 10), e -> true).forEach(Entity::discard);
            world.setWeatherParameters(100000, 0, false, false);
            forceThunderGradient(world, 0.0f);
            ETTestHelper.restoreRainGradient(world, originalRainGradients);
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // uncapped flame three burns targets for 180 ticks

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameUncappedLevel3(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level",
            "capmod_enabled", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("flame", 3);
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 3);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        arrow.setEnchantmentEffectsFromEntity(shooter, 0.0f);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 180,
                "Flame III should burn 9s = 180 ticks (got " + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // real survival shots test free and consumed arrows
    // rate one preserves arrows and zero consumes them

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityRealPathFreeConsume(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ServerLevel world = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.INFINITY, 1);
        player.getInventory().setItem(0, bow);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        AABB search = player.getBoundingBox().inflate(64.0);
        try {
            // free: pct=1.0 -> threshold 0 -> always free
            ETTestHelper.setConfigValue("more_infinity_pct", "1.0");
            int freeBefore = player.getInventory().countItem(Items.ARROW);
            int entBefore = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
            bow.getItem().releaseUsing(bow, world, player, 0);
            int entAfter = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            helper.assertTrue(entAfter == entBefore + 1,
                "FREE shot should spawn one arrow (before=" + entBefore + ", after=" + entAfter + ")");
            helper.assertTrue(player.getInventory().countItem(Items.ARROW) == freeBefore,
                "FREE shot (pct=1.0) must not consume an arrow (before=" + freeBefore + ", after="
                    + player.getInventory().countItem(Items.ARROW) + ")");

            // consume: pct=0.0 -> threshold 1 -> always consume
            ETTestHelper.setConfigValue("more_infinity_pct", "0.0");
            int consumeBefore = player.getInventory().countItem(Items.ARROW);
            int entBefore2 = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            bow.getItem().use(world, player, InteractionHand.MAIN_HAND);
            bow.getItem().releaseUsing(bow, world, player, 0);
            int entAfter2 = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            helper.assertTrue(entAfter2 == entBefore2 + 1,
                "CONSUME shot should spawn one arrow (before=" + entBefore2 + ", after=" + entAfter2 + ")");
            helper.assertTrue(player.getInventory().countItem(Items.ARROW) == consumeBefore - 1,
                "CONSUME shot (pct=0.0) must consume exactly one arrow (before=" + consumeBefore + ", after="
                    + player.getInventory().countItem(Items.ARROW) + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingLevelScalingRealPath(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setFeature("better_mending", false);
        ETTestHelper.setConfigValue("more_mending_step", "0.05");
        ETTestHelper.setConfigValue("more_mending_floor", "0.1");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 3);
        ServerLevel world = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        try {
            int[] levels = {1, 2, 3};
            int[] expected = {45, 50, 55};
            for (int idx = 0; idx < levels.length; idx++) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.enchant(Enchantments.MENDING, levels[idx]);
                sword.setDamageValue(100);
                player.getInventory().setItem(0, sword);
                ExperienceOrb orb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), 100);
                int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
                helper.assertTrue(remaining == expected[idx], "MoreMending level " + levels[idx] + " should leave "
                    + expected[idx] + " XP (got " + remaining + ")");
                helper.assertTrue(sword.getDamageValue() == 0, "MoreMending level " + levels[idx]
                    + " should fully repair the sword (damage=" + sword.getDamageValue() + ")");
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // negative mending floors clamp to zero
    // level twelve repairs items without experience cost

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingNegativeFloorFreeRepair(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setFeature("better_mending", false);
        ETTestHelper.setConfigValue("more_mending_step", "0.05");
        ETTestHelper.setConfigValue("more_mending_floor", "-0.5");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 12);
        ServerLevel world = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.MENDING, 12);
        sword.setDamageValue(100);
        player.getInventory().setItem(0, sword);
        ExperienceOrb orb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), 100);
        try {
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
            helper.assertTrue(remaining == 100,
                "Negative floor clamps to 0 -> factor 0 -> free repair, all 100 XP kept (got " + remaining + ")");
            helper.assertTrue(sword.getDamageValue() == 0,
                "Free repair should fully mend the sword (damage=" + sword.getDamageValue() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // zero mending step keeps the factor at 0.6
    // the repair leaves 40 experience

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingZeroStepNoScaling(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setFeature("better_mending", false);
        ETTestHelper.setConfigValue("more_mending_step", "0.0");
        ETTestHelper.setConfigValue("more_mending_floor", "0.1");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 5);
        ServerLevel world = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.MENDING, 5);
        sword.setDamageValue(100);
        player.getInventory().setItem(0, sword);
        ExperienceOrb orb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), 100);
        try {
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
            helper.assertTrue(remaining == 40,
                "step=0 pins factor at 0.6 -> cost 60 -> remaining 40 (got " + remaining + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMendingMultiItemRecursion(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_mending", "better_mending",
            "more_mending_step", "more_mending_floor", "capmod_enabled", "mending");
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setFeature("better_mending", false);
        ETTestHelper.setConfigValue("more_mending_step", "0.05");
        ETTestHelper.setConfigValue("more_mending_floor", "0.1");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("mending", 5);
        ServerLevel world = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.enchant(Enchantments.MENDING, 1);
        helmet.setDamageValue(20);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.MENDING, 5);
        sword.setDamageValue(40);
        player.getInventory().armor.set(3, helmet); // head
        player.getInventory().setItem(0, sword); // mainhand
        ExperienceOrb orb = new ExperienceOrb(world, pos.getX(), pos.getY(), pos.getZ(), 1000);
        try {
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 1000);
            helper.assertTrue(remaining == 975,
                "Per-item capture: cost 11 (L1 helmet) + 14 (L5 sword) -> remaining 975 (got " + remaining + ")");
            helper.assertTrue(helmet.getDamageValue() == 0 && sword.getDamageValue() == 0,
                "Both Mending items should be fully repaired (helmet=" + helmet.getDamageValue() + ", sword="
                    + sword.getDamageValue() + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // count arrow entities from real crossbow charging and firing

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotRealFireEntities(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "capmod_enabled", "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 2);
        player.getInventory().setItem(0, crossbow);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        AABB search = player.getBoundingBox().inflate(64.0);
        try {
            int before = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            crossbow.getItem().use(world, player, InteractionHand.MAIN_HAND); // begin charge
            crossbow.getItem().releaseUsing(crossbow, world, player, 0); // finish charge (loads 5)
            crossbow.getItem().use(world, player, InteractionHand.MAIN_HAND); // fire all charged
            int spawned = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size() - before;
            helper.assertTrue(spawned == 5,
                "Multishot II (per_level=2) should fire 5 arrow entities (got " + spawned + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotRealFireDisabled(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "capmod_enabled",
            "multishot");
        ETTestHelper.setFeature("more_multishot", false);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 2);
        player.getInventory().setItem(0, crossbow);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        AABB search = player.getBoundingBox().inflate(64.0);
        try {
            int before = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size();
            crossbow.getItem().use(world, player, InteractionHand.MAIN_HAND);
            crossbow.getItem().releaseUsing(crossbow, world, player, 0);
            crossbow.getItem().use(world, player, InteractionHand.MAIN_HAND);
            int spawned = world.getEntitiesOfClass(AbstractArrow.class, search, e -> true).size() - before;
            helper.assertTrue(spawned == 3,
                "Vanilla Multishot should fire 3 arrow entities when more_multishot disabled (got " + spawned + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // god armor stacks protection types for epf 48

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreProtectionGodArmorEpf48(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "god_armor",
            "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setFeature("god_armor", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.96");
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
            armor.enchant(Enchantments.BLAST_PROTECTION, 4);
            entity.setItemSlot(slot, armor);
        }
        net.minecraft.world.damagesource.DamageSource source = helper.getLevel().damageSources()
            .explosion((net.minecraft.world.level.Explosion) null);
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameHugePerLevelClamped(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "200000000");
        ETTestHelper.setEnchantCap("flame", 2);
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 2);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        arrow.setEnchantmentEffectsFromEntity(shooter, 0.0f);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 2_147_483_640,
                "Huge more_flame_per_level should clamp to the largest safe tick count (got "
                    + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // non-burning arrows do not apply flame effects

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameArrowNotOnFire(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level", "flame");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ETTestHelper.setEnchantCap("flame", 2);
        ServerLevel world = helper.getLevel();
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.enchant(Enchantments.FLAME, 2);
        shooter.setItemSlot(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        // deliberately not on fire
        world.addFreshEntity(arrow);
        ((FlameLevelAccess) arrow).enchanttweaker$setFlameLevel(2);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        // new mobs may begin with negative fire ticks
        // compare the initial value rather than assuming zero
        int fireTicksBefore = target.getRemainingFireTicks();
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == fireTicksBefore && target.getRemainingFireTicks() <= 0,
                "A non-burning arrow must not set the target on fire regardless of Flame level (before "
                    + fireTicksBefore + ", after " + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // ownerless flaming arrows use vanilla burn duration

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFlameNullOwner(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_flame", "more_flame_per_level");
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setConfigValue("more_flame_per_level", "2");
        ServerLevel world = helper.getLevel();
        BlockPos spawnPos = helper.absolutePos(new BlockPos(0, 3, 0));
        Arrow arrow = new Arrow(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(),
            new ItemStack(Items.ARROW));
        // no owner set
        arrow.igniteForSeconds(10);
        world.addFreshEntity(arrow);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = AbstractArrow.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getRemainingFireTicks() == 100,
                "An on-fire arrow with a null owner (flameLevel 0) should burn vanilla 100 ticks (got "
                    + target.getRemainingFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // fire protection tests clamps fallbacks and levels

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFireProtectionLevel0(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.85");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS)); // no fire protection
        try {
            int result = net.minecraft.world.item.enchantment.ProtectionEnchantment.getFireAfterDampener(entity, 200);
            helper.assertTrue(result == 200,
                "Fire protection level 0 should fall through to vanilla (200, got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFireProtectionNegativeBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "-0.5");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.FIRE_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        try {
            int result = net.minecraft.world.item.enchantment.ProtectionEnchantment.getFireAfterDampener(entity, 200);
            helper.assertTrue(result == 0, "Negative base clamps to 0 -> duration 0 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFireProtectionUpperBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "5.0");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.FIRE_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        try {
            int result = net.minecraft.world.item.enchantment.ProtectionEnchantment.getFireAfterDampener(entity, 200);
            helper.assertTrue(result == 200, "base > 1 clamps to 1 -> duration unchanged at 200 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFireProtectionNonNumericBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "abc");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.FIRE_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        try {
            int result = net.minecraft.world.item.enchantment.ProtectionEnchantment.getFireAfterDampener(entity, 200);
            int expected = (int) (200 * Math.pow(0.85, 4)); // falls back to default 0.85 -> 104
            helper.assertTrue(result == expected,
                "Non-numeric base falls back to 0.85 -> " + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreFireProtectionLevelScaling(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_fire_protection",
            "more_fire_protection_base");
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.85");
        int[] levels = {1, 2, 3};
        int[] expected = {170, 144, 122}; // (int)(200 * 0.85^level)
        try {
            for (int i = 0; i < levels.length; i++) {
                Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(2 * i, 2, 0));
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                boots.enchant(Enchantments.FIRE_PROTECTION, levels[i]);
                entity.setItemSlot(EquipmentSlot.FEET, boots);
                int result = net.minecraft.world.item.enchantment.ProtectionEnchantment.getFireAfterDampener(entity,
                    200);
                helper.assertTrue(result == expected[i],
                    "Fire Protection " + levels[i] + " should give " + expected[i] + " (got " + result + ")");
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // blast protection tests clamps capmod and fallbacks

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBlastProtectionUpperClamp(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "5.0");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.BLAST_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        try {
            double result = net.minecraft.world.item.enchantment.ProtectionEnchantment
                .getExplosionKnockbackAfterDampener(entity, 1.0);
            helper.assertTrue(Math.abs(result - 1.0) < 0.001,
                "base > 1 clamps to 1 -> knockback unchanged ~1.0 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBlastProtectionLowerClamp(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.BLAST_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        try {
            for (String base : new String[]{"0.0", "-1"}) {
                ETTestHelper.setConfigValue("more_blast_protection_base", base);
                double result = net.minecraft.world.item.enchantment.ProtectionEnchantment
                    .getExplosionKnockbackAfterDampener(entity, 1.0);
                helper.assertFalse(Double.isNaN(result), "base=" + base + " must not produce NaN knockback");
                helper.assertTrue(Math.abs(result - 0.0) < 0.001,
                    "base <= 0 clamps to 0 -> knockback ~0.0 (base=" + base + ", got " + result + ")");
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBlastProtectionCapmod8(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base", "capmod_enabled", "blast_protection");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "0.85");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("blast_protection", 8);
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.BLAST_PROTECTION, 8);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        try {
            double result = net.minecraft.world.item.enchantment.ProtectionEnchantment
                .getExplosionKnockbackAfterDampener(entity, 1.0);
            double expected = Math.pow(0.85, 8);
            helper.assertTrue(result > 0.0, "Multiplicative blast protection never reaches 0 (got " + result + ")");
            helper.assertTrue(Math.abs(result - expected) < 0.001,
                "Blast Protection 8 should give ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBlastProtectionLevel0(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "0.85");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        entity.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS)); // no blast protection
        try {
            double result = net.minecraft.world.item.enchantment.ProtectionEnchantment
                .getExplosionKnockbackAfterDampener(entity, 1.0);
            helper.assertTrue(Math.abs(result - 1.0) < 0.001,
                "Blast protection level 0 falls through to vanilla -> knockback 1.0 (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBlastProtectionMalformedBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_blast_protection",
            "more_blast_protection_base");
        ETTestHelper.setFeature("more_blast_protection", true);
        ETTestHelper.setConfigValue("more_blast_protection_base", "not-a-number");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(Enchantments.BLAST_PROTECTION, 4);
        entity.setItemSlot(EquipmentSlot.FEET, boots);
        try {
            double result = net.minecraft.world.item.enchantment.ProtectionEnchantment
                .getExplosionKnockbackAfterDampener(entity, 1.0);
            double expected = Math.pow(0.85, 4); // falls back to default 0.85
            helper.assertTrue(Math.abs(result - expected) < 0.001,
                "Malformed base falls back to 0.85 -> ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // protection tests upper clamps and malformed bases

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreProtectionUpperClampBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        try {
            for (String base : new String[]{"5.0", "1.0"}) {
                ETTestHelper.setConfigValue("more_protection_base", base);
                Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
                equipArmorSet(entity, Enchantments.PROTECTION, 4); // `EPF` 16
                net.minecraft.world.damagesource.DamageSource source = helper.getLevel().damageSources().generic();
                float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
                helper.assertTrue(Math.abs(result - 20.0f) < 0.01f,
                    "base >= 1 clamps to 1 -> damage unchanged at 20.0 (base=" + base + ", got " + result + ")");
                entity.discard();
            }
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreProtectionMalformedBase(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_protection", "more_protection_base");
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "abc");
        Zombie entity = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        equipArmorSet(entity, Enchantments.PROTECTION, 4); // `EPF` 16
        net.minecraft.world.damagesource.DamageSource source = helper.getLevel().damageSources().generic();
        float result = ETTestHelper.modifyAppliedDamage(entity, source, 20.0f);
        float expected = (float) (20.0 * Math.pow(0.96, 16));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.05f,
                "Malformed base falls back to 0.96 -> ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // looting tests zero levels capmod and truncation

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreLootingZeroLevelGuard(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); // no Looting
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreLootingCapmodLevel5(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier",
            "capmod_enabled", "looting");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("looting", 5);
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.LOOTING, 5);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreLootingTruncation(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_looting", "more_looting_multiplier");
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ServerLevel world = helper.getLevel();
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(Enchantments.LOOTING, 1);
        player.setItemSlot(EquipmentSlot.MAINHAND, sword);
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            int base = 3;
            int total = dropAndSumXp(helper, world, zombie, player, base);
            int expected = expectedLootingXp(base, 1, 0.5);
            helper.assertTrue(total == expected && expected == 4,
                "Looting 1 x0.5 on 3 XP should truncate the boosted total to 4 (got " + total + ")");
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    // additional infinity edge cases

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityNonArrowProjectileConsumed(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "1.0"); // a plain arrow would always be free here
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0)); // non-creative
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.INFINITY, 1);
        try {
            Method m = ProjectileWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            // plain arrows always preserve at rate one
            ItemStack plain = (ItemStack) m.invoke(null, bow, new ItemStack(Items.ARROW), shooter, false);
            helper.assertTrue(plain.has(DataComponents.INTANGIBLE_PROJECTILE),
                "Plain arrow at pct=1.0 should be free (intangible) - the contrast case");
            // spectral arrows fail vanilla's plain-arrow infinity gate
            ItemStack spectral = (ItemStack) m.invoke(null, bow, new ItemStack(Items.SPECTRAL_ARROW), shooter, false);
            helper.assertFalse(spectral.has(DataComponents.INTANGIBLE_PROJECTILE),
                "Spectral arrow must never be armed free by MoreInfinity (vanilla Items.ARROW gate)");
            // tipped arrow: same gate
            ItemStack tipped = (ItemStack) m.invoke(null, bow, new ItemStack(Items.TIPPED_ARROW), shooter, false);
            helper.assertFalse(tipped.has(DataComponents.INTANGIBLE_PROJECTILE),
                "Tipped arrow must never be armed free by MoreInfinity (vanilla Items.ARROW gate)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreInfinityNegativePctAlwaysConsumes(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_infinity", "more_infinity_pct");
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "-1.0");
        Zombie shooter = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0)); // non-creative
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.INFINITY, 1);
        try {
            Method m = ProjectileWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class,
                LivingEntity.class, boolean.class);
            m.setAccessible(true);
            // negative rates clamp the threshold to one
            // every arrow is consumed
            for (int i = 0; i < 20; i++) {
                ItemStack proj = (ItemStack) m.invoke(null, bow, new ItemStack(Items.ARROW), shooter, false);
                helper.assertFalse(proj.has(DataComponents.INTANGIBLE_PROJECTILE),
                    "Negative more_infinity_pct must always consume the arrow (iteration " + i + ")");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreBindingMultipleItemsRestoredToCorrectSlots(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_binding", "more_binding_step",
            "capmod_enabled", "binding_curse");
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setConfigValue("more_binding_step", "1.0"); // level five always keeps bound items
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 5);
        try {
            ServerPlayer old = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
            BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
            old.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.enchant(Enchantments.BINDING_CURSE, 5);
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            boots.enchant(Enchantments.BINDING_CURSE, 5);
            old.getInventory().armor.set(3, helmet); // slot 3 = head
            old.getInventory().armor.set(0, boots); // slot 0 = feet

            old.getInventory().dropAll();
            helper.assertTrue(
                old.getInventory().armor.get(3).is(Items.DIAMOND_HELMET)
                    && old.getInventory().armor.get(0).is(Items.DIAMOND_BOOTS),
                "MoreBinding should retain BOTH bound pieces after dropAll (helmet@3, boots@0)");

            ServerPlayer respawned = ETTestHelper.createServerPlayer(helper, GameType.SURVIVAL);
            respawned.getInventory().armor.set(3, ItemStack.EMPTY);
            respawned.getInventory().armor.set(0, ItemStack.EMPTY);
            ServerPlayerEvents.AFTER_RESPAWN.invoker().afterRespawn(old, respawned, false);

            ItemStack head = respawned.getInventory().armor.get(3);
            ItemStack feet = respawned.getInventory().armor.get(0);
            helper.assertTrue(
                head.is(Items.DIAMOND_HELMET)
                    && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BINDING_CURSE, head) == 5,
                "AFTER_RESPAWN should restore the helmet to head slot 3 (got " + head + ")");
            helper.assertTrue(
                feet.is(Items.DIAMOND_BOOTS)
                    && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BINDING_CURSE, feet) == 5,
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
        helper.succeed();
    }

    @GameTest(
        template = EMPTY_STRUCTURE)
    public void moreMultishotCapmodHighLevel(GameTestHelper helper) {
        Map<String, String> originalConfig = ETTestHelper.snapshotConfig("more_multishot", "more_multishot_per_level",
            "capmod_enabled", "multishot");
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setConfigValue("more_multishot_per_level", "2");
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 10);
        ServerPlayer player = ETTestHelper.createServerPlayer(helper, GameType.CREATIVE);
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.enchant(Enchantments.MULTISHOT, 10);
        player.getInventory().add(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class,
                ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getItems() : List.of();
            helper.assertTrue(projectiles.size() == 21,
                "Multishot 10 (capmod) with per_level=2 should load 21 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.restoreConfig(originalConfig);
        }
        helper.succeed();
    }

}
