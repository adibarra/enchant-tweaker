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
 * @description Replace additive fire protection duration formula with multiplicative scaling.
 * Vanilla: duration -= floor(duration * level * 0.15)
 * Modded:  duration * 0.85^level
 * @environment Server
 */
@Mixin(value=ProtectionEnchantment.class)
public abstract class MoreFireProtectionMixin {

    @Unique
    private static int enchanttweaker$originalFireDuration;

    @Inject(
        method="transformFireDuration(Lnet/minecraft/entity/LivingEntity;I)I",
        at=@At("HEAD"))
    private static void enchanttweaker$moreFireProtection$capture(LivingEntity entity, int duration, CallbackInfoReturnable<Integer> cir) {
        enchanttweaker$originalFireDuration = duration;
    }

    @ModifyReturnValue(
        method="transformFireDuration(Lnet/minecraft/entity/LivingEntity;I)I",
        at=@At("RETURN"))
    private static int enchanttweaker$moreFireProtection$multiplicative(int result, LivingEntity entity) {
        if (!ETMixinPlugin.getMixinConfig("MoreFireProtectionMixin")) return result;
        int level = EnchantmentHelper.getEquipmentLevel(Enchantments.FIRE_PROTECTION, entity);
        if (level > 0) {
            double base = ETMixinPlugin.getConfig().getOrDefault("more_fire_protection_base", 0.85);
            return (int)(enchanttweaker$originalFireDuration * Math.pow(base, level));
        }
        return result;
    }
}
