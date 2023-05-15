package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.InfinityEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow Infinity and Mending enchantments to co-exist.
 * @environment Server
 */
@Mixin(value=InfinityEnchantment.class, priority=1543)
public abstract class InfiniteMendingMixin {

    @Inject(
        method="canAccept(Lnet/minecraft/enchantment/Enchantment;)Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$infiniteMending$allowCoexist(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (other == Enchantments.MENDING) {
            cir.setReturnValue(true);
        }
    }
}
