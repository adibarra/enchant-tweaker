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
}
