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
 * @description replace additive fire protection duration formula with
 *              multiplicative scaling vanilla: duration -= floor(duration *
 *              level * 0.15) modded: duration * 0.85^level
 * @environment Server
 */
@Mixin(
    value = ProtectionEnchantment.class)
public abstract class MoreFireProtectionMixin {

    @Inject(
        method = "transformFireDuration(Lnet/minecraft/entity/LivingEntity;I)I",
        at = @At("HEAD"),
        cancellable = true)
    private static void enchanttweaker$moreFireProtection$multiplicative(LivingEntity entity, int duration,
        CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("MoreFireProtectionMixin"))
            return;
        int level = EnchantmentHelper.getEquipmentLevel(Enchantments.FIRE_PROTECTION, entity);
        if (level > 0) {
            double base = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_fire_protection_base", 0.85), 0.0,
                1.0);
            cir.setReturnValue((int) (duration * Math.pow(base, level)));
        }
    }
}
