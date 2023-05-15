package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow axes to be enchanted with fire aspect, knockback, and looting.
 * @environment Server
 */
@Mixin(value=Enchantment.class, priority=1543)
public abstract class AxeWeaponsMixin {

    @Inject(
        method="isAcceptableItem(Lnet/minecraft/item/ItemStack;)Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$axeWeapons$allowSwordEnchants(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        boolean isAxe = stack.getItem() instanceof AxeItem;
        if (!isAxe) return;

        Enchantment enchantment = (Enchantment) (Object) this;
        if (enchantment == Enchantments.FIRE_ASPECT || enchantment == Enchantments.KNOCKBACK || enchantment == Enchantments.LOOTING) {
            cir.setReturnValue(true);
        }
    }
}
