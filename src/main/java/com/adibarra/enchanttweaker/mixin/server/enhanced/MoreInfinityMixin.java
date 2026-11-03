package com.adibarra.enchanttweaker.mixin.server.enhanced;

import java.util.concurrent.ThreadLocalRandom;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description lets infinity bows sometimes preserve arrows
 * @environment server
 */
@Mixin(
    value = ProjectileWeaponItem.class)
public abstract class MoreInfinityMixin {

    @ModifyExpressionValue(
        method = "useAmmo(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Z)Lnet/minecraft/world/item/ItemStack;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processAmmoUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I"))
    private static int enchanttweaker$moreInfinity$freeArrow(int orig, @Local(
        argsOnly = true,
        ordinal = 0) ItemStack weaponStack) {
        if (!ETMixinPlugin.getMixinConfig("MoreInfinityMixin"))
            return orig;
        if (orig == 0)
            return orig; // already preserved, so leave unchanged
        int infinityLevel = ADUtils.getEnchantmentLevel(weaponStack, Enchantments.INFINITY);
        if (infinityLevel <= 0)
            return orig;
        double pct = ETMixinPlugin.getConfig().getOrDefault("more_infinity_pct", 0.03);
        return ADUtils.infinityPreservesArrow(infinityLevel, pct, ThreadLocalRandom.current().nextFloat()) ? 0 : orig;
    }
}
