package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description lets bows with Infinity enchant shoot without arrows
 * @environment Server
 */
@Mixin(
    value = BowItem.class)
public abstract class BowInfinityFixMixin {

    @ModifyExpressionValue(
        method = "use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;",
        at = @At(
            ordinal = 0,
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private boolean enchanttweaker$bowInfinityFix$fireNoArrow(boolean orig, @Local ItemStack stack) {
        if (!ETMixinPlugin.getMixinConfig("BowInfinityFixMixin"))
            return orig;
        if (ETMixinPlugin.getMixinConfig("MoreInfinityMixin"))
            return orig;
        if (EnchantmentHelper.getLevel(Enchantments.INFINITY, stack) > 0) {
            return false;
        }
        return orig;
    }

    /** the draw-gate fix above only allows `use()` to start drawing the bow */
    @ModifyExpressionValue(
        method = "onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getProjectileType(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack enchanttweaker$bowInfinityFix$shootNoArrow(ItemStack orig, @Local(
        argsOnly = true,
        ordinal = 0) ItemStack stack) {
        if (!ETMixinPlugin.getMixinConfig("BowInfinityFixMixin"))
            return orig;
        if (ETMixinPlugin.getMixinConfig("MoreInfinityMixin"))
            return orig;
        if (orig.isEmpty() && EnchantmentHelper.getLevel(Enchantments.INFINITY, stack) > 0) {
            return new ItemStack(Items.ARROW);
        }
        return orig;
    }
}
