package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.utils.ADMath;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.item.BowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Random;

/**
 * @description Lets bows with Infinity enchant have a chance at shooting
 * without consuming an arrow. Overrides BowInfinityFix.
 * @environment Server
 */
@Mixin(value=BowItem.class)
public abstract class MoreInfinityMixin {

    @Unique
    private static final Random RAND = new Random();

    @ModifyExpressionValue(
        method="onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V",
        at=@At(
            ordinal=0,
            value="INVOKE",
            target="Lnet/minecraft/enchantment/EnchantmentHelper;getLevel(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I"))
    private int enchanttweaker$moreInfinity$freeArrow(int infinityLevel) {
        if (RAND.nextFloat() > ADMath.clamp(1.0 - 0.03 * infinityLevel, 0, 1.0)) {
            return infinityLevel;
        }
        return 0;
    }

}
