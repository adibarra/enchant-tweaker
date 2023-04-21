package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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