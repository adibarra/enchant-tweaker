package com.adibarra.enchanttweaker.mixin.server.enhanced;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description scale blast protection knockback multiplicatively
 * @environment server
 */
@Mixin(
    value = ProtectionEnchantment.class)
public abstract class MoreBlastProtectionMixin {

    @Inject(
        method = "getExplosionKnockbackAfterDampener(Lnet/minecraft/world/entity/LivingEntity;D)D",
        at = @At("HEAD"),
        cancellable = true)
    private static void enchanttweaker$moreBlastProtection$multiplicative(LivingEntity entity, double knockback,
        CallbackInfoReturnable<Double> cir) {
        if (!ETMixinPlugin.getMixinConfig("MoreBlastProtectionMixin"))
            return;
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.BLAST_PROTECTION, entity);
        if (level > 0) {
            double base = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_blast_protection_base", 0.85), 0.0,
                1.0);
            cir.setReturnValue(knockback * Math.pow(base, level));
        }
    }
}
