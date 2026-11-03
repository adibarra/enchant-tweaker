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
 * @description scale fire protection duration multiplicatively
 * @environment server
 */
@Mixin(
    value = ProtectionEnchantment.class)
public abstract class MoreFireProtectionMixin {

    @Inject(
        method = "getFireAfterDampener(Lnet/minecraft/world/entity/LivingEntity;I)I",
        at = @At("HEAD"),
        cancellable = true)
    private static void enchanttweaker$moreFireProtection$multiplicative(LivingEntity entity, int duration,
        CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("MoreFireProtectionMixin"))
            return;
        int level = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, entity);
        if (level > 0) {
            double base = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_fire_protection_base", 0.85), 0.0,
                1.0);
            cir.setReturnValue((int) (duration * Math.pow(base, level)));
        }
    }
}
