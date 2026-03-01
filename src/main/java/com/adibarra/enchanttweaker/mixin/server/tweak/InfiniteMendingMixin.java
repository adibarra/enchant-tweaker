package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.InfinityEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow Infinity and Mending enchantments to co-exist.
 * @environment Server
 */
@Mixin(value=InfinityEnchantment.class)
public abstract class InfiniteMendingMixin {

    @Inject(
        method="canAccept(Lnet/minecraft/enchantment/Enchantment;)Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$infiniteMending$allowCoexist(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("InfiniteMendingMixin")) return;
        String selfP = ADUtils.getEnchantmentPath((Enchantment)(Object)this);
        String otherP = ADUtils.getEnchantmentPath(other);
        if (selfP == null || otherP == null) return;
        if ((selfP.equals("infinity") && otherP.equals("mending")) ||
            (selfP.equals("mending") && otherP.equals("infinity"))) {
            cir.setReturnValue(true);
        }
    }
}
