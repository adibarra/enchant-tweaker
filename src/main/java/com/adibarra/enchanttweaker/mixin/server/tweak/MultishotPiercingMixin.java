package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.MultishotEnchantment;
import net.minecraft.enchantment.PiercingEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow Multishot and Piercing enchantments to co-exist.
 * @environment Server
 */
@Mixin(value={
    MultishotEnchantment.class, PiercingEnchantment.class
}, priority=1543)
public abstract class MultishotPiercingMixin {

    @Inject(
        method="canAccept(Lnet/minecraft/enchantment/Enchantment;)Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$multishotPiercing$allowCoexist(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (other == Enchantments.MULTISHOT || other == Enchantments.PIERCING) {
            cir.setReturnValue(true);
        }
    }
}
