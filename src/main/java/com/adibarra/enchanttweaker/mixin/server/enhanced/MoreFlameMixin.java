package com.adibarra.enchanttweaker.mixin.server.enhanced;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.FlameLevelAccess;

/**
 * @description scales flame burn time by enchantment level adds two seconds per
 *              level above one
 * @environment server
 */
@Mixin(
    value = AbstractArrow.class)
public abstract class MoreFlameMixin implements FlameLevelAccess {

    @Unique
    private static final String FLAME_LEVEL_NBT_KEY = "EnchantTweakerFlameLevel";

    @Unique
    private int enchanttweaker$flameLevel = 0;

    @Inject(
        method = "setEnchantmentEffectsFromEntity(Lnet/minecraft/world/entity/LivingEntity;F)V",
        at = @At("HEAD"))
    private void enchanttweaker$moreFlame$captureMobWeaponLevel(LivingEntity shooter, float damageModifier,
        CallbackInfo ci) {
        enchanttweaker$setFlameLevel(EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAME, shooter));
    }

    @Override
    @Unique
    public int enchanttweaker$getFlameLevel() {
        return enchanttweaker$flameLevel;
    }

    @Override
    @Unique
    public void enchanttweaker$setFlameLevel(int level) {
        enchanttweaker$flameLevel = Math.max(0, level);
    }

    @Inject(
        method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V",
        at = @At("TAIL"))
    private void enchanttweaker$moreFlame$writeLevel(CompoundTag nbt, CallbackInfo ci) {
        if (enchanttweaker$flameLevel > 0) {
            nbt.putInt(FLAME_LEVEL_NBT_KEY, enchanttweaker$flameLevel);
        } else {
            nbt.remove(FLAME_LEVEL_NBT_KEY);
        }
    }

    @Inject(
        method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V",
        at = @At("TAIL"))
    private void enchanttweaker$moreFlame$readLevel(CompoundTag nbt, CallbackInfo ci) {
        enchanttweaker$setFlameLevel(nbt.getInt(FLAME_LEVEL_NBT_KEY));
    }

    @ModifyConstant(
        method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
        slice = @Slice(
            from = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/Entity;getRemainingFireTicks()I"),
            to = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(I)V")),
        constant = @Constant(
            intValue = 5,
            ordinal = 0))
    private int enchanttweaker$moreFlame$modifyBurnTime(int orig) {
        if (!ETMixinPlugin.getMixinConfig("MoreFlameMixin"))
            return orig;
        int perLevel = ETMixinPlugin.getConfig().getOrDefault("more_flame_per_level", 2);
        long extra = (long) Math.max(0, perLevel) * Math.max(0L, (long) enchanttweaker$flameLevel - 1);
        long seconds = (long) orig + extra;
        return (int) Math.clamp(seconds, orig, Integer.MAX_VALUE / 20);
    }
}
