package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @description Lets bows with Infinity enchant shoot without arrows.
 * @environment Server
 */
@Mixin(value=BowItem.class, priority=1543)
public abstract class BowInfinityFixMixin {

    @ModifyExpressionValue(
        method="use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private boolean enchanttweaker$bowInfinityFix$fireNoArrow(boolean orig, @Local ItemStack stack) {
        if (EnchantmentHelper.getLevel(Enchantments.INFINITY, stack) > 0) {
            return false;
        }
        return orig;
    }
}
