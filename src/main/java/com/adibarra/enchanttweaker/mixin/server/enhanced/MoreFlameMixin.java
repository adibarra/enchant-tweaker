package com.adibarra.enchanttweaker.mixin.server.enhanced;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.FlameLevelAccess;
import com.adibarra.utils.ADUtils;

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
        method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
        at = @At("HEAD"))
    private void enchanttweaker$moreFlame$captureWeaponLevel(EntityHitResult result, CallbackInfo ci) {
        enchanttweaker$setFlameLevel(
            ADUtils.getEnchantmentLevel(((AbstractArrow) (Object) this).getWeaponItem(), Enchantments.FLAME));
    }

    @Inject(
        method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V",
        at = @At("TAIL"))
    private void enchanttweaker$moreFlame$writeLevel(ValueOutput output, CallbackInfo ci) {
        if (enchanttweaker$flameLevel > 0) {
            output.putInt(FLAME_LEVEL_NBT_KEY, enchanttweaker$flameLevel);
        }
    }

    @Inject(
        method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V",
        at = @At("TAIL"))
    private void enchanttweaker$moreFlame$readLevel(ValueInput input, CallbackInfo ci) {
        enchanttweaker$setFlameLevel(input.getIntOr(FLAME_LEVEL_NBT_KEY, 0));
    }

    @ModifyConstant(
        method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V",
        constant = @Constant(
            floatValue = 5.0f,
            ordinal = 0))
    private float enchanttweaker$moreFlame$modifyBurnTime(float orig) {
        if (!ETMixinPlugin.getMixinConfig("MoreFlameMixin"))
            return orig;
        int perLevel = ETMixinPlugin.getConfig().getOrDefault("more_flame_per_level", 2);
        return orig + (float) Math.max(0, perLevel) * Math.max(0, enchanttweaker$flameLevel - 1);
    }
}
