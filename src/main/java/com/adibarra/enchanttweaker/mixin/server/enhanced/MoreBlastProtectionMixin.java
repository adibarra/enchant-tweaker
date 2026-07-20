package com.adibarra.enchanttweaker.mixin.server.enhanced;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description replace additive blast protection knockback formula with
 *              multiplicative scaling vanilla: knockback *= clamp(1 - level *
 *              0.15, 0, 1) modded: knockback * 0.85^level
 * @environment Server
 */
@Mixin(
    value = ProtectionEnchantment.class)
public abstract class MoreBlastProtectionMixin {

    @Inject(
        method = "transformExplosionKnockback(Lnet/minecraft/entity/LivingEntity;D)D",
        at = @At("HEAD"),
        cancellable = true)
    private static void enchanttweaker$moreBlastProtection$multiplicative(LivingEntity entity, double knockback,
        CallbackInfoReturnable<Double> cir) {
        if (!ETMixinPlugin.getMixinConfig("MoreBlastProtectionMixin"))
            return;
        int level = EnchantmentHelper.getEquipmentLevel(Enchantments.BLAST_PROTECTION, entity);
        if (level > 0) {
            double base = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_blast_protection_base", 0.85), 0.0,
                1.0);
            cir.setReturnValue(knockback * Math.pow(base, level));
        }
    }
}
