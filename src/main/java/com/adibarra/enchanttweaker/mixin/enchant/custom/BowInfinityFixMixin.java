package com.adibarra.enchanttweaker.mixin.enchant.custom;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Lets bows with Infinity enchant shoot without arrows.
 * @environment Server
 */
@Mixin(value=BowItem.class, priority=1543)
public abstract class BowInfinityFixMixin {

    @Inject(method="use", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$bowInfinityFix(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true)) {
            ItemStack bow = user.getStackInHand(hand);
            if(EnchantmentHelper.getLevel(Enchantments.INFINITY, bow) > 0) {
                user.setCurrentHand(hand);
                cir.setReturnValue(TypedActionResult.consume(bow));
            }
        }
    }
}