package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description remove extra self-damage from axes when used as a weapon
 * @environment server
 */
@Mixin(
    value = MiningToolItem.class)
public abstract class AxesNotToolsMixin {

    @ModifyConstant(
        method = "postHit(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/LivingEntity;)Z",
        constant = @Constant(
            intValue = 2,
            ordinal = 0))
    private int enchanttweaker$axesNotTools$modifySelfDamage(int orig, ItemStack stack) {
        if (!ETMixinPlugin.getMixinConfig("AxesNotToolsMixin"))
            return orig;
        boolean isAxe = stack.getItem() instanceof AxeItem;
        return isAxe ? 1 : orig;
    }
}
