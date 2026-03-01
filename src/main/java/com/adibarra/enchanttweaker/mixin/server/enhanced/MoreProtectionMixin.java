package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @description Replace additive EPF protection formula with multiplicative scaling.
 * Vanilla: damage * (1 - clamp(epf, 0, 20) / 25)
 * Modded:  damage * 0.96^epf
 * @environment Server
 */
@Mixin(value=LivingEntity.class)
public abstract class MoreProtectionMixin {

    @WrapOperation(
        method="modifyAppliedDamage(Lnet/minecraft/entity/damage/DamageSource;F)F",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/entity/DamageUtil;getInflictedDamage(FF)F"))
    private float enchanttweaker$moreProtection$multiplicative(float damage, float epf, Operation<Float> original) {
        if (!ETMixinPlugin.getMixinConfig("MoreProtectionMixin")) {
            return original.call(damage, epf);
        }
        double base = ETMixinPlugin.getConfig().getOrDefault("more_protection_base", 0.96);
        return (float)(damage * Math.pow(base, epf));
    }
}
