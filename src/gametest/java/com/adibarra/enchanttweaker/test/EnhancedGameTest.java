package com.adibarra.enchanttweaker.test;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.entity.LightningEntity;
import net.minecraft.world.GameMode;

import net.minecraft.entity.ExperienceOrbEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class EnhancedGameTest implements FabricGameTest {

    // ─── MoreMultishot ───────────────────────────────────────────────
    // Multishot II with MoreMultishot: level*2+1 = 5 projectiles loaded vs vanilla 3.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMultishotEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class, ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 5,
                "Multishot II should load 5 projectiles with more_multishot enabled (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setEnchantCap("multishot", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMultishotDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_multishot", false);
        ETTestHelper.setEnchantCap("multishot", 2);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class, ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 3,
                "Multishot (vanilla) should load 3 projectiles when more_multishot disabled (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("more_multishot", true);
            ETTestHelper.setEnchantCap("multishot", -1);
        }
        helper.complete();
    }

    // ─── MoreFlame ───────────────────────────────────────────────────
    // Flame II: burn time = 2*(2-1)+5 = 7 seconds (140 ticks) vs vanilla 5s (100 ticks).

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFlameEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_flame", true);
        ETTestHelper.setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);

        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);

        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // Flame II: setOnFireFor(7) = 140 ticks; vanilla Flame I = 100 ticks
            helper.assertTrue(target.getFireTicks() > 100,
                "Flame II should burn target > 5s (got " + target.getFireTicks() + " ticks)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setEnchantCap("flame", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFlameDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_flame", false);
        ETTestHelper.setEnchantCap("flame", 2);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 2);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);

        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);

        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // Flame II with mixin disabled: vanilla setOnFireFor(5) = 100 ticks (not 140)
            helper.assertTrue(target.getFireTicks() == 100,
                "Flame II with more_flame disabled should burn target 100 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("more_flame", true);
            ETTestHelper.setEnchantCap("flame", -1);
        }
        helper.complete();
    }

    // ─── MoreMending ─────────────────────────────────────────────────
    // Vanilla: getMendingRepairCost(amount) = amount / 2 = 50 for amount=100.
    // MoreMending (mendingLevel=0 default): round(100 * clamp(0.6, 0.1, 0.6)) = 60.
    // Tests getMendingRepairCost directly to avoid infinite-loop risk in repairPlayerGears.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMendingEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_mending", true);
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.entity.ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost", int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());
            int cost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            // MoreMending formula (mendingLevel=0 default): round(100 * clamp(0.6, 0.1, 0.6)) = 60
            helper.assertTrue(cost == 60,
                "MoreMending getMendingRepairCost(100) with mendingLevel=0 should be 60 (got " + cost + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMendingDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_mending", false);
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.entity.ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost", int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());
            int cost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            // Vanilla 1.20.3: amount / 2 = 50
            helper.assertTrue(cost == 50,
                "Vanilla getMendingRepairCost(100) should return 50 (got " + cost + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("more_mending", true);
        }
        helper.complete();
    }

    // ─── MoreInfinity ────────────────────────────────────────────────
    // At Infinity level 34: threshold = clamp(1 - 0.03*34, 0, 1) = 0.
    // random.nextFloat() is always in [0,1), so random > 0 → always true → always free arrow.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreInfinityEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("infinity", 34);
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 34);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            // getProjectile is protected static — access via reflection
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class, LivingEntity.class, boolean.class);
            m.setAccessible(true);
            ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
            // Level 34: threshold = clamp(1 - 0.03*34, 0, 1) = 0 → random > 0 always → always intangible
            helper.assertTrue(proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                "Level-34 Infinity should always give intangible projectile (threshold=0)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("more_infinity", false);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("infinity", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreInfinityDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_infinity", false);
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class, LivingEntity.class, boolean.class);
            m.setAccessible(true);
            // Vanilla Infinity I: always intangible — mixin must not interfere
            ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
            helper.assertTrue(proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                "Vanilla Infinity I should give intangible projectile when more_infinity disabled");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("more_infinity", true);
        }
        helper.complete();
    }

    // ─── MoreBinding ─────────────────────────────────────────────────
    // Level 10 binding: keep chance = 1 - clamp(1.1 - 0.1*10, 0.1, 1.0) = 90%.
    // P(zero keeps in 50 attempts) = 0.1^50 ≈ 10^-50 → effectively certain to see at least one keep.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreBindingEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 10);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        boolean keptAtLeastOnce = false;
        for (int i = 0; i < 50; i++) {
            ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
            helmet.addEnchantment(Enchantments.BINDING_CURSE, 10);
            player.getInventory().armor.set(3, helmet); // slot 3 = head
            player.getInventory().dropAll();
            if (!player.getInventory().armor.get(3).isEmpty()) {
                keptAtLeastOnce = true;
                player.getInventory().armor.set(3, ItemStack.EMPTY);
                break;
            }
        }
        try {
            helper.assertTrue(keptAtLeastOnce,
                "MoreBinding should keep Binding Curse X armor at least once in 50 attempts (90% per attempt)");
        } finally {
            ETTestHelper.setFeature("more_binding", false);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("binding_curse", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreBindingDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_binding", false);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 10);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.addEnchantment(Enchantments.BINDING_CURSE, 10);
        player.getInventory().armor.set(3, helmet);
        player.getInventory().dropAll();
        try {
            helper.assertTrue(player.getInventory().armor.get(3).isEmpty(),
                "Vanilla dropAll should always drop Binding Curse armor when more_binding disabled");
        } finally {
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("binding_curse", -1);
        }
        helper.complete();
    }

    // ─── MoreChanneling ──────────────────────────────────────────────
    // MoreChannelingMixin makes Channeling II work in rain (not just thunderstorm).
    // The mixin replaces the isThundering() return: orig || (channelingII && isRaining()).

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreChannelingEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 2);
        ServerWorld world = helper.getWorld();
        // Rain but no thunder — vanilla would skip channeling, mixin should fire
        world.setWeather(0, 100000, true, false);
        ETTestHelper.forceRainGradient(world, 1.0f); // isRaining() checks rainGradient, not the flag; gradient updates lazily per tick
        // Place the target high in the sky to satisfy hasDirectSkyLight check
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
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            Box searchBox = new Box(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10,
                                    absBase.getX() + 10, skyY + 20, absBase.getZ() + 10);
            List<LightningEntity> lightnings = world.getEntitiesByType(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(!lightnings.isEmpty(),
                "MoreChanneling II should summon lightning in rain (no lightning spawned)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            world.setWeather(100000, 0, false, false);
            ETTestHelper.setFeature("more_channeling", false);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("channeling", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreChannelingDisabled(TestContext helper) {
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
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            Box searchBox = new Box(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10,
                                    absBase.getX() + 10, skyY + 20, absBase.getZ() + 10);
            List<LightningEntity> lightnings = world.getEntitiesByType(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(lightnings.isEmpty(),
                "Channeling II in rain (no thunder) without mixin should NOT summon lightning");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            world.setWeather(100000, 0, false, false);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("channeling", -1);
        }
        helper.complete();
    }

    // ─── MoreChanneling: Level I in rain ──────────────────────────────
    // Channeling I in rain should NOT summon lightning even with mixin enabled.
    // The mixin only activates for level >= 2.

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreChannelingLevel1RainNoLightning(TestContext helper) {
        ETTestHelper.setFeature("more_channeling", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("channeling", 1);
        ServerWorld world = helper.getWorld();
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
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(trident, new EntityHitResult(target));
            Box searchBox = new Box(absBase.getX() - 10, skyY - 10, absBase.getZ() - 10,
                                    absBase.getX() + 10, skyY + 20, absBase.getZ() + 10);
            List<LightningEntity> lightnings = world.getEntitiesByType(EntityType.LIGHTNING_BOLT, searchBox, e -> true);
            helper.assertTrue(lightnings.isEmpty(),
                "Channeling I in rain should NOT summon lightning even with more_channeling enabled");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            world.setWeather(100000, 0, false, false);
            ETTestHelper.setFeature("more_channeling", false);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("channeling", -1);
        }
        helper.complete();
    }

    // ─── MoreMultishot: level edge cases ──────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMultishotLevel1Vanilla(TestContext helper) {
        // Level 1 with mixin enabled: 1*2+1 = 3 = same as vanilla
        ETTestHelper.setFeature("more_multishot", true);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 1);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class, ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 3,
                "Multishot I with more_multishot should still load 3 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMultishotLevel3(TestContext helper) {
        // Level 3: 3*2+1 = 7 projectiles
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 3);
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 3);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class, ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 7,
                "Multishot III should load 7 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("multishot", -1);
        }
        helper.complete();
    }

    // ─── MoreFlame: level 1 = vanilla ─────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFlameLevel1Vanilla(TestContext helper) {
        // Level 1 with mixin enabled: 2*(1-1)+5 = 5s = 100 ticks = vanilla
        ETTestHelper.setFeature("more_flame", true);
        ServerWorld world = helper.getWorld();
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack flameBow = new ItemStack(Items.BOW);
        flameBow.addEnchantment(Enchantments.FLAME, 1);
        shooter.equipStack(EquipmentSlot.MAINHAND, flameBow);
        BlockPos spawnPos = helper.getAbsolutePos(new BlockPos(0, 3, 0));
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            helper.assertTrue(target.getFireTicks() == 100,
                "Flame I with more_flame enabled should still burn 100 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.complete();
    }

    // ─── MoreMending: high level clamp ────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMendingHighLevel(TestContext helper) {
        // Mending level 10: round(100 * clamp(0.6 - 0.05*10, 0.1, 0.6)) = round(100 * 0.1) = 10
        // Must disable better_mending so vanilla repairPlayerGears path runs (captureMendingLevel fires)
        ETTestHelper.setFeature("more_mending", true);
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
        net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 100);
        try {
            // repairPlayerGears triggers captureMendingLevel → getMendingRepairCost with level 10
            int remaining = ETTestHelper.repairPlayerGears(orb, player, 100);
            // At level 10: clamp(0.6-0.5, 0.1, 0.6) = 0.1 → repair cost = round(repairAmount * 0.1)
            // Less XP consumed per repair point → more remaining than vanilla (50)
            helper.assertTrue(remaining > 50,
                "MoreMending level 10 should consume less XP per repair (remaining=" + remaining + ", vanilla would be ~50)");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("better_mending", true);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("mending", -1);
        }
        helper.complete();
    }

    // ─── MoreInfinity: level 1 consumes arrows ────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreInfinityLevel1Consumes(TestContext helper) {
        // Level 1: threshold = clamp(1 - 0.03*1, 0, 1) = 0.97
        // ~97% chance to consume arrow (not intangible). Over 100 tries, at least one should consume.
        // P(all 100 are free) = 0.03^100 ≈ 10^-152 → impossible
        ETTestHelper.setFeature("more_infinity", true);
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        ItemStack arrow = new ItemStack(Items.ARROW);
        boolean consumedAtLeastOnce = false;
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class, LivingEntity.class, boolean.class);
            m.setAccessible(true);
            for (int i = 0; i < 100; i++) {
                ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
                if (!proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE)) {
                    consumedAtLeastOnce = true;
                    break;
                }
            }
            helper.assertTrue(consumedAtLeastOnce,
                "MoreInfinity level 1 should consume arrow most of the time (3% free, not 100%)");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("more_infinity", false);
        }
        helper.complete();
    }

    // ─── MoreInfinity: custom rate ─────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreInfinityCustomRateHigh(TestContext helper) {
        // more_infinity_pct=1.0, level 1: threshold = clamp(1 - 1.0*1, 0, 1) = 0 → always free
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setConfigValue("more_infinity_pct", "1.0");
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 1);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class, LivingEntity.class, boolean.class);
            m.setAccessible(true);
            for (int i = 0; i < 50; i++) {
                ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
                helper.assertTrue(proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                    "more_infinity_pct=1.0 level 1 should always give free arrow (iteration " + i + ")");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setConfigValue("more_infinity_pct", "0.03");
            ETTestHelper.setFeature("more_infinity", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreInfinityCustomRateZero(TestContext helper) {
        // more_infinity_pct=0.0, level 34: threshold = clamp(1 - 0*34, 0, 1) = 1.0 → never free
        ETTestHelper.setFeature("more_infinity", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("infinity", 34);
        ETTestHelper.setConfigValue("more_infinity_pct", "0.0");
        ZombieEntity shooter = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.INFINITY, 34);
        ItemStack arrow = new ItemStack(Items.ARROW);
        try {
            Method m = RangedWeaponItem.class.getDeclaredMethod("getProjectile", ItemStack.class, ItemStack.class, LivingEntity.class, boolean.class);
            m.setAccessible(true);
            for (int i = 0; i < 100; i++) {
                ItemStack proj = (ItemStack) m.invoke(null, bow, arrow, shooter, false);
                helper.assertTrue(!proj.contains(DataComponentTypes.INTANGIBLE_PROJECTILE),
                    "more_infinity_pct=0.0 should never give free arrow (iteration " + i + ")");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setConfigValue("more_infinity_pct", "0.03");
            ETTestHelper.setFeature("more_infinity", false);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("infinity", -1);
        }
        helper.complete();
    }

    // ─── MoreProtection ─────────────────────────────────────────────────
    // Vanilla EPF formula: damage * (1 - clamp(epf, 0, 20) / 25)
    // Multiplicative:      damage * 0.96^epf
    // 4x Protection IV = EPF 16. Damage 20:
    //   Vanilla:        20 * (1 - 16/25) = 20 * 0.36 = 7.2
    //   Multiplicative: 20 * 0.96^16     ≈ 10.397

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreProtectionEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_protection", true);
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
        float expected = (float)(20.0 * Math.pow(0.96, 16));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.01f,
                "MoreProtection should give ~" + expected + " damage (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("more_protection", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreProtectionDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_protection", false);
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
        float expected = net.minecraft.entity.DamageUtil.getInflictedDamage(20.0f, 16.0f);
        helper.assertTrue(Math.abs(result - expected) < 0.01f,
            "Vanilla protection should give ~" + expected + " damage (got " + result + ")");
        helper.complete();
    }

    // ─── MoreFireProtection ─────────────────────────────────────────────
    // Fire Protection IV: level = 4, duration = 200.
    //   Vanilla:        200 - floor(200 * 4 * 0.15) = 200 - 120 = 80
    //   Multiplicative: (int)(200 * 0.85^4) = 104

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_fire_protection", true);
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
        int expected = (int)(200 * Math.pow(0.85, 4));
        try {
            helper.assertTrue(result == expected,
                "MoreFireProtection should give duration " + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.setFeature("more_fire_protection", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_fire_protection", false);
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
        // Vanilla: 200 - floor(200 * 4 * 0.15) = 80
        int expected = 80;
        helper.assertTrue(result == expected,
            "Vanilla fire protection should give duration " + expected + " (got " + result + ")");
        helper.complete();
    }

    // ─── MoreBlastProtection ────────────────────────────────────────────
    // Blast Protection IV: level = 4, knockback = 1.0.
    //   Vanilla:        1.0 * clamp(1 - 4*0.15, 0, 1) = 0.4
    //   Multiplicative: 1.0 * 0.85^4 ≈ 0.522

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_blast_protection", true);
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
            ETTestHelper.setFeature("more_blast_protection", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_blast_protection", false);
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.BLAST_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        double result = net.minecraft.enchantment.ProtectionEnchantment.transformExplosionKnockback(entity, 1.0);
        // Vanilla: 1.0 * clamp(1 - 4*0.15, 0, 1) = 0.4
        double expected = 0.4;
        helper.assertTrue(Math.abs(result - expected) < 0.001,
            "Vanilla blast protection should give knockback ~" + expected + " (got " + result + ")");
        helper.complete();
    }

    // ─── MoreBinding: level 1 = always drops (vanilla) ────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreBindingLevel1AlwaysDrops(TestContext helper) {
        // Level 1: clamp(1.0 + 0.1 - 0.1*1, 0.0, 1.0) = 1.0 → random > 1.0 is never true → always drops
        ETTestHelper.setFeature("more_binding", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("binding_curse", 1);
        PlayerEntity player = helper.createMockPlayer(GameMode.SURVIVAL);
        try {
            for (int i = 0; i < 50; i++) {
                ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
                helmet.addEnchantment(Enchantments.BINDING_CURSE, 1);
                player.getInventory().armor.set(3, helmet);
                player.getInventory().dropAll();
                helper.assertTrue(player.getInventory().armor.get(3).isEmpty(),
                    "MoreBinding level 1 should always drop armor (iteration " + i + ")");
            }
        } finally {
            ETTestHelper.setFeature("more_binding", false);
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("binding_curse", -1);
        }
        helper.complete();
    }

    // ─── Configurable values ──────────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreProtectionCustomBase(TestContext helper) {
        // Custom base 0.90 instead of default 0.96
        ETTestHelper.setFeature("more_protection", true);
        ETTestHelper.setConfigValue("more_protection_base", "0.90");
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
        float expected = (float)(20.0 * Math.pow(0.90, 16));
        try {
            helper.assertTrue(Math.abs(result - expected) < 0.01f,
                "MoreProtection with base=0.90 should give ~" + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.setConfigValue("more_protection_base", "0.96");
            ETTestHelper.setFeature("more_protection", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFireProtectionCustomBase(TestContext helper) {
        ETTestHelper.setFeature("more_fire_protection", true);
        ETTestHelper.setConfigValue("more_fire_protection_base", "0.70");
        ZombieEntity entity = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
        entity.equipStack(EquipmentSlot.FEET, boots);
        int result = net.minecraft.enchantment.ProtectionEnchantment.transformFireDuration(entity, 200);
        int expected = (int)(200 * Math.pow(0.70, 4));
        try {
            helper.assertTrue(result == expected,
                "MoreFireProtection with base=0.70 should give duration " + expected + " (got " + result + ")");
        } finally {
            ETTestHelper.setConfigValue("more_fire_protection_base", "0.85");
            ETTestHelper.setFeature("more_fire_protection", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreBlastProtectionCustomBase(TestContext helper) {
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
            ETTestHelper.setConfigValue("more_blast_protection_base", "0.85");
            ETTestHelper.setFeature("more_blast_protection", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMendingCustomStepAndFloor(TestContext helper) {
        // Custom step=0.1, floor=0.05. At mendingLevel=0: clamp(0.6 - 0.1*0, 0.05, 0.6) = 0.6
        // Same as vanilla-equivalent at level 0. Test the formula reads config.
        ETTestHelper.setFeature("more_mending", true);
        ETTestHelper.setConfigValue("more_mending_step", "0.1");
        ETTestHelper.setConfigValue("more_mending_floor", "0.05");
        ServerWorld world = helper.getWorld();
        BlockPos pos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
        net.minecraft.entity.ExperienceOrbEntity orb = new net.minecraft.entity.ExperienceOrbEntity(world, pos.getX(), pos.getY(), pos.getZ(), 0);
        try {
            Method getMRC = net.minecraft.entity.ExperienceOrbEntity.class.getDeclaredMethod("getMendingRepairCost", int.class);
            getMRC.setAccessible(true);
            boolean isStatic = java.lang.reflect.Modifier.isStatic(getMRC.getModifiers());
            int cost = (int) getMRC.invoke(isStatic ? null : orb, 100);
            // mendingLevel=0: clamp(0.6 - 0.1*0, 0.05, 0.6) = 0.6 → round(100*0.6) = 60
            helper.assertTrue(cost == 60,
                "MoreMending custom step/floor at level 0 should be 60 (got " + cost + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setConfigValue("more_mending_step", "0.05");
            ETTestHelper.setConfigValue("more_mending_floor", "0.1");
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreMultishotCustomPerLevel(TestContext helper) {
        // Custom per_level=3: level 2 → 2*3+1 = 7 projectiles
        ETTestHelper.setFeature("more_multishot", true);
        ETTestHelper.setCapmod(true);
        ETTestHelper.setEnchantCap("multishot", 2);
        ETTestHelper.setConfigValue("more_multishot_per_level", "3");
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        crossbow.addEnchantment(Enchantments.MULTISHOT, 2);
        player.getInventory().insertStack(new ItemStack(Items.ARROW, 64));
        try {
            Method loadProjectiles = CrossbowItem.class.getDeclaredMethod("loadProjectiles", LivingEntity.class, ItemStack.class);
            loadProjectiles.setAccessible(true);
            loadProjectiles.invoke(null, player, crossbow);
            var charged = crossbow.get(DataComponentTypes.CHARGED_PROJECTILES);
            List<ItemStack> projectiles = charged != null ? charged.getProjectiles() : List.of();
            helper.assertTrue(projectiles.size() == 7,
                "Multishot II with per_level=3 should load 7 projectiles (got " + projectiles.size() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setConfigValue("more_multishot_per_level", "2");
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("multishot", -1);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreFlameCustomPerLevel(TestContext helper) {
        // Custom per_level=5: Flame II → 5*(2-1)+5 = 10s = 200 ticks
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
        ArrowEntity arrow = new ArrowEntity(world, (double) spawnPos.getX(), (double) spawnPos.getY(), (double) spawnPos.getZ(), new ItemStack(Items.ARROW));
        arrow.setOwner(shooter);
        arrow.setOnFireFor(10);
        world.spawnEntity(arrow);
        ZombieEntity target = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(2, 2, 0));
        try {
            Method onEntityHit = PersistentProjectileEntity.class.getDeclaredMethod("onEntityHit", EntityHitResult.class);
            onEntityHit.setAccessible(true);
            onEntityHit.invoke(arrow, new EntityHitResult(target));
            // 5*(2-1)+5 = 10s = 200 ticks
            helper.assertTrue(target.getFireTicks() == 200,
                "Flame II with per_level=5 should burn 200 ticks (got " + target.getFireTicks() + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setConfigValue("more_flame_per_level", "2");
            ETTestHelper.setCapmod(false);
            ETTestHelper.setEnchantCap("flame", -1);
        }
        helper.complete();
    }

    // ─── More Looting (XP Boost) ─────────────────────────────────────

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreLootingEnabled(TestContext helper) {
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 3);
        player.equipStack(EquipmentSlot.MAINHAND, sword);

        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            // Set attackingPlayer and playerHitTimer so dropXp fires
            Field attackingPlayerField = LivingEntity.class.getDeclaredField("attackingPlayer");
            attackingPlayerField.setAccessible(true);
            attackingPlayerField.set(zombie, player);
            Field playerHitTimerField = LivingEntity.class.getDeclaredField("playerHitTimer");
            playerHitTimerField.setAccessible(true);
            playerHitTimerField.setInt(zombie, 100);

            // Get base XP
            int baseXp = zombie.getXpToDrop();
            if (baseXp <= 0) {
                // Force a known XP value via experiencePoints field on MobEntity
                Field xpField = net.minecraft.entity.mob.MobEntity.class.getDeclaredField("experiencePoints");
                xpField.setAccessible(true);
                xpField.setInt(zombie, 10);
                baseXp = 10;
            }

            // Clear existing XP orbs
            BlockPos absPos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            Box searchBox = new Box(absPos).expand(10);
            world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, e -> true).forEach(e -> e.discard());

            // Call dropXp
            Method dropXp = LivingEntity.class.getDeclaredMethod("dropXp");
            dropXp.setAccessible(true);
            dropXp.invoke(zombie);

            // Sum XP orbs
            List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, e -> true);
            int totalXp = orbs.stream().mapToInt(ExperienceOrbEntity::getExperienceAmount).sum();

            // Looting 3 with multiplier 0.5: xp * (1 + 3 * 0.5) = xp * 2.5
            int expectedXp = (int)(baseXp * 2.5);
            helper.assertTrue(totalXp == expectedXp,
                "MoreLooting should boost XP: expected " + expectedXp + " (got " + totalXp + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setFeature("more_looting", false);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreLootingDisabled(TestContext helper) {
        ETTestHelper.setFeature("more_looting", false);
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 3);
        player.equipStack(EquipmentSlot.MAINHAND, sword);

        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            Field attackingPlayerField = LivingEntity.class.getDeclaredField("attackingPlayer");
            attackingPlayerField.setAccessible(true);
            attackingPlayerField.set(zombie, player);
            Field playerHitTimerField = LivingEntity.class.getDeclaredField("playerHitTimer");
            playerHitTimerField.setAccessible(true);
            playerHitTimerField.setInt(zombie, 100);

            Field xpField = net.minecraft.entity.mob.MobEntity.class.getDeclaredField("experiencePoints");
            xpField.setAccessible(true);
            xpField.setInt(zombie, 10);

            BlockPos absPos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            Box searchBox = new Box(absPos).expand(10);
            world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, e -> true).forEach(e -> e.discard());

            Method dropXp = LivingEntity.class.getDeclaredMethod("dropXp");
            dropXp.setAccessible(true);
            dropXp.invoke(zombie);

            List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, e -> true);
            int totalXp = orbs.stream().mapToInt(ExperienceOrbEntity::getExperienceAmount).sum();

            // Feature disabled: XP should be unmodified (10)
            helper.assertTrue(totalXp == 10,
                "MoreLooting disabled should not boost XP: expected 10 (got " + totalXp + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        helper.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void moreLootingCustomMultiplier(TestContext helper) {
        ETTestHelper.setFeature("more_looting", true);
        ETTestHelper.setConfigValue("more_looting_multiplier", "1.0");
        ServerWorld world = helper.getWorld();
        ServerPlayerEntity player = helper.createMockCreativeServerPlayerInWorld();
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 2);
        player.equipStack(EquipmentSlot.MAINHAND, sword);

        ZombieEntity zombie = helper.spawnMob(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        try {
            Field attackingPlayerField = LivingEntity.class.getDeclaredField("attackingPlayer");
            attackingPlayerField.setAccessible(true);
            attackingPlayerField.set(zombie, player);
            Field playerHitTimerField = LivingEntity.class.getDeclaredField("playerHitTimer");
            playerHitTimerField.setAccessible(true);
            playerHitTimerField.setInt(zombie, 100);

            Field xpField = net.minecraft.entity.mob.MobEntity.class.getDeclaredField("experiencePoints");
            xpField.setAccessible(true);
            xpField.setInt(zombie, 10);

            BlockPos absPos = helper.getAbsolutePos(new BlockPos(0, 2, 0));
            Box searchBox = new Box(absPos).expand(10);
            world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, e -> true).forEach(e -> e.discard());

            Method dropXp = LivingEntity.class.getDeclaredMethod("dropXp");
            dropXp.setAccessible(true);
            dropXp.invoke(zombie);

            List<ExperienceOrbEntity> orbs = world.getEntitiesByClass(ExperienceOrbEntity.class, searchBox, e -> true);
            int totalXp = orbs.stream().mapToInt(ExperienceOrbEntity::getExperienceAmount).sum();

            // Looting 2 with multiplier 1.0: 10 * (1 + 2 * 1.0) = 30
            helper.assertTrue(totalXp == 30,
                "MoreLooting with multiplier=1.0 and Looting II should give 30 XP (got " + totalXp + ")");
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            ETTestHelper.setConfigValue("more_looting_multiplier", "0.5");
            ETTestHelper.setFeature("more_looting", false);
        }
        helper.complete();
    }
}
