package com.adibarra.enchanttweaker.mixin.tweaks.axes_are_weapons;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.*;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Enchantment.class, priority=1543)
public abstract class AxeWeaponEnchantsMixin {

    /**
     * @author adibarra
     * @reason Allow some melee enchantments to be added to axes
     */
    @Inject(method="isAcceptableItem", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$axeWeaponEnchants(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("axes_are_weapons", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            if(stack.getItem() instanceof AxeItem) {
                //noinspection ConstantConditions
                if ((Object) this == Enchantments.FIRE_ASPECT || (Object) this == Enchantments.KNOCKBACK || (Object) this == Enchantments.LOOTING) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}