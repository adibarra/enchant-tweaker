package com.adibarra.enchanttweaker.mixin.server.enhanced;

import java.util.concurrent.ThreadLocalRandom;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description lets infinity bows sometimes preserve arrows
 * @environment server
 */
@Mixin(
    value = RangedWeaponItem.class)
public abstract class MoreInfinityMixin {

    @ModifyExpressionValue(
        method = "getProjectile(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;Z)Lnet/minecraft/item/ItemStack;",
        at = @At(
            ordinal = 0,
            value = "INVOKE",
            target = "Lnet/minecraft/item/RangedWeaponItem;isInfinity(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Z)Z"))
    private static boolean enchanttweaker$moreInfinity$freeArrow(boolean orig, @Local(
        argsOnly = true,
        ordinal = 0) ItemStack weaponStack,
        @Local(
            argsOnly = true,
            ordinal = 0) LivingEntity shooter) {
        if (!ETMixinPlugin.getMixinConfig("MoreInfinityMixin"))
            return orig;
        if (!orig)
            return false; // already lacks infinity, so leave unchanged
        // creative shots must remain free
        if (shooter != null && shooter.isInCreativeMode())
            return true;
        int infinityLevel = EnchantmentHelper.getLevel(Enchantments.INFINITY, weaponStack);
        double pct = ETMixinPlugin.getConfig().getOrDefault("more_infinity_pct", 0.03);
        return ADUtils.infinityPreservesArrow(infinityLevel, pct, ThreadLocalRandom.current().nextFloat());
    }
}
