package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Replace additive blast protection knockback formula with multiplicative scaling.
 * Vanilla: knockback *= clamp(1 - level * 0.15, 0, 1)
 * Modded:  knockback * 0.85^level
 * @environment Server
 */
@Mixin(value=ProtectionEnchantment.class)
public abstract class MoreBlastProtectionMixin {

    @Unique
    private static double enchanttweaker$originalBlastKnockback;

    @Inject(
        method="transformExplosionKnockback(Lnet/minecraft/entity/LivingEntity;D)D",
        at=@At("HEAD"))
    private static void enchanttweaker$moreBlastProtection$capture(LivingEntity entity, double knockback, CallbackInfoReturnable<Double> cir) {
        enchanttweaker$originalBlastKnockback = knockback;
    }

    @ModifyReturnValue(
        method="transformExplosionKnockback(Lnet/minecraft/entity/LivingEntity;D)D",
        at=@At("RETURN"))
    private static double enchanttweaker$moreBlastProtection$multiplicative(double result, LivingEntity entity) {
        if (!ETMixinPlugin.getMixinConfig("MoreBlastProtectionMixin")) return result;
        int level = EnchantmentHelper.getEquipmentLevel(Enchantments.BLAST_PROTECTION, entity);
        if (level > 0) {
            return enchanttweaker$originalBlastKnockback * Math.pow(0.85, level);
        }
        return result;
    }
}
