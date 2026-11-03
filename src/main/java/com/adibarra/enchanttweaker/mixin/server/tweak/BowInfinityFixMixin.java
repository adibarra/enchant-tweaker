package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description lets bows with infinity enchant shoot without arrows
 * @environment server
 */
@Mixin(
    value = BowItem.class)
public abstract class BowInfinityFixMixin {

    @ModifyExpressionValue(
        method = "use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;",
        at = @At(
            ordinal = 0,
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"))
    private boolean enchanttweaker$bowInfinityFix$fireNoArrow(boolean orig, @Local ItemStack stack) {
        if (!ETMixinPlugin.getMixinConfig("BowInfinityFixMixin"))
            return orig;
        if (ETMixinPlugin.getMixinConfig("MoreInfinityMixin"))
            return orig;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY, stack) > 0) {
            return false;
        }
        return orig;
    }

    /** the draw-gate fix above only allows `use()` to start drawing the bow */
    @ModifyExpressionValue(
        method = "releaseUsing(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getProjectile(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack enchanttweaker$bowInfinityFix$shootNoArrow(ItemStack orig, @Local(
        argsOnly = true,
        ordinal = 0) ItemStack stack) {
        if (!ETMixinPlugin.getMixinConfig("BowInfinityFixMixin"))
            return orig;
        if (ETMixinPlugin.getMixinConfig("MoreInfinityMixin"))
            return orig;
        if (orig.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY, stack) > 0) {
            return new ItemStack(Items.ARROW);
        }
        return orig;
    }
}
