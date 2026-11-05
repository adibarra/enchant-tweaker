package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description scale fire protection duration multiplicatively
 * @environment server
 */
@Mixin(
    value = LivingEntity.class)
public abstract class MoreFireProtectionMixin {

    @WrapOperation(
        method = "igniteForTicks(I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double enchanttweaker$moreFireProtection$multiplicative(LivingEntity entity, Holder<Attribute> attribute,
        Operation<Double> original) {
        if (!ETMixinPlugin.getMixinConfig("MoreFireProtectionMixin"))
            return original.call(entity, attribute);
        int level = ADUtils.getHighestArmorEnchantmentLevel(entity, Enchantments.FIRE_PROTECTION);
        if (level <= 0)
            return original.call(entity, attribute);
        double base = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_fire_protection_base", 0.85), 0.0, 1.0);
        return Math.pow(base, level);
    }
}
