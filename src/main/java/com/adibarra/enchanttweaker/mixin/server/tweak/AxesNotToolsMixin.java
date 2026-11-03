package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description remove extra self-damage from axes when used as a weapon
 * @environment server
 */
@Mixin(
    value = DiggerItem.class)
public abstract class AxesNotToolsMixin {

    @ModifyConstant(
        method = "hurtEnemy(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
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
