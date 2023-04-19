package com.adibarra.enchanttweaker.mixin.enchant.custom;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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

    @ModifyExpressionValue(method="use", at=@At(value="INVOKE", target="Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private boolean bowInfinityFix(boolean original) {
        return original && !EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true);
    }
}