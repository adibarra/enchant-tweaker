package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description scale protection damage multiplicatively
 * @environment server
 */
@Mixin(
    value = LivingEntity.class)
public abstract class MoreProtectionMixin {

    @WrapOperation(
        method = "modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/DamageUtil;getInflictedDamage(FF)F"))
    private float enchanttweaker$moreProtection$multiplicative(float damage, float epf, Operation<Float> original) {
        if (!ETMixinPlugin.getMixinConfig("MoreProtectionMixin")) {
            return original.call(damage, epf);
        }
        double base = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_protection_base", 0.96), 0.0, 1.0);
        return (float) (damage * Math.pow(base, epf));
    }
}
