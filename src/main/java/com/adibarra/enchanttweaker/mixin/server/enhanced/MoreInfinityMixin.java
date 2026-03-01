package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @description Lets bows with Infinity enchant have a chance at shooting
 * without consuming an arrow. Overrides BowInfinityFix.
 * @environment Server
 */
@Mixin(value=RangedWeaponItem.class)
public abstract class MoreInfinityMixin {

    @ModifyExpressionValue(
        method="getProjectile(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;Z)Lnet/minecraft/item/ItemStack;",
        at=@At(
            ordinal=0,
            value="INVOKE",
            target="Lnet/minecraft/item/RangedWeaponItem;isInfinity(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;Z)Z"))
    private static boolean enchanttweaker$moreInfinity$freeArrow(boolean orig, @Local(argsOnly=true, ordinal=0) ItemStack weaponStack) {
        if (!ETMixinPlugin.getMixinConfig("MoreInfinityMixin")) return orig;
        if (!orig) return false; // already no infinity, don't change
        int infinityLevel = EnchantmentHelper.getLevel(Enchantments.INFINITY, weaponStack);
        if (ThreadLocalRandom.current().nextFloat() > Math.clamp(1.0 - 0.03 * infinityLevel, 0, 1.0)) {
            return true; // keep infinity: free arrow
        }
        return false; // lose infinity: consume arrow
    }

}
