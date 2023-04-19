package com.adibarra.enchanttweaker.mixin.enchant.custom;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO: This is a work in progress. As such, it is flagged as a conflict in EnchantTweakerMixinPlugin.

/**
 * @description Lets crossbows with Infinity enchant shoot without arrows.
 * @environment Server
 */
@Mixin(value=CrossbowItem.class, priority=1543)
public abstract class CrossbowInfinityFixMixin {

    @ModifyExpressionValue(method="use", at=@At(value="INVOKE", target="Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private static boolean crossbowInfinityFix1(boolean original) {
        return original && !EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true);
    }

    @ModifyExpressionValue(method="loadProjectiles", at=@At(value="INVOKE", target="Lnet/minecraft/item/ItemStack;isEmpty()Z"))
    private static boolean crossbowInfinityFix2(boolean original) {
        EnchantTweaker.getLogger().info(
                "crossbowInfinityFix2: Orig: " + original + ", Config: "
                + EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true) + ", Result: "
                + !(original && !EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true))
        );
        return !(original && !EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true));
    }

    @ModifyVariable(method="loadProjectile", at=@At(value="HEAD"), ordinal=1, argsOnly=true)
    private static boolean crossbowInfinityFix3(boolean creative) {
        EnchantTweaker.getLogger().info(
                "crossbowInfinityFix3: Orig: " + creative + ", Config: "
                + EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true) + ", Result: "
                + (creative || EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true))
        );
        return creative || EnchantTweaker.getConfig().getOrDefault("bow_infinity_fix", true);
    }
}