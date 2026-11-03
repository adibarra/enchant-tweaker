package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description scale blast protection knockback multiplicatively
 * @environment server
 */
@Mixin(
    value = ServerExplosion.class)
public abstract class MoreBlastProtectionMixin {

    @Unique
    private static int enchanttweaker$moreBlastProtection$totalLevel(LivingEntity entity) {
        int total = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST,
                EquipmentSlot.HEAD}) {
            total += ADUtils.getEnchantmentLevel(entity.getItemBySlot(slot), Enchantments.BLAST_PROTECTION);
        }
        return total;
    }

    @WrapOperation(
        method = "hurtEntities()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double enchanttweaker$moreBlastProtection$multiplicative(LivingEntity entity, Holder<Attribute> attribute,
        Operation<Double> original) {
        if (!ETMixinPlugin.getMixinConfig("MoreBlastProtectionMixin"))
            return original.call(entity, attribute);
        int level = enchanttweaker$moreBlastProtection$totalLevel(entity);
        if (level <= 0)
            return original.call(entity, attribute);
        double base = Math.clamp(ETMixinPlugin.getConfig().getOrDefault("more_blast_protection_base", 0.85), 0.0, 1.0);
        return 1.0 - Math.pow(base, level);
    }
}
