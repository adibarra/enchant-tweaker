package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description remove extra self-damage from axes when used as a weapon
 * @environment server
 */
@Mixin(
    value = ItemStack.class)
public abstract class AxesNotToolsMixin {

    @ModifyExpressionValue(
        method = "postHurtEnemy(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/Weapon;itemDamagePerAttack()I"))
    private int enchanttweaker$axesNotTools$modifySelfDamage(int orig) {
        if (!ETMixinPlugin.getMixinConfig("AxesNotToolsMixin"))
            return orig;
        ItemStack stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof AxeItem))
            return orig;
        return orig == 2 ? 1 : orig;
    }
}
