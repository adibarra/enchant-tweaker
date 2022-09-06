package com.adibarra.enchanttweaker.mixin.enchantments.custom.bows_are_magic;

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

@Mixin(value=BowItem.class, priority=1543)
public abstract class BowInfinityFixMixin {

    /**
     * @author adibarra
     * @reason Allow bows with infinity enchantment to shoot without requiring an arrow
     */
    @Inject(method="use", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$bowInfinityFix(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("bows_are_magic", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            ItemStack itemStack = user.getStackInHand(hand);
            if(EnchantmentHelper.getLevel(Enchantments.INFINITY, user.getStackInHand(hand)) > 0) {
                user.setCurrentHand(hand);
                cir.setReturnValue(TypedActionResult.consume(itemStack));
            }
        }
    }
}