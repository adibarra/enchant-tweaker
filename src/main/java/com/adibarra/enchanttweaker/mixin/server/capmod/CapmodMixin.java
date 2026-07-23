package com.adibarra.enchanttweaker.mixin.server.capmod;

import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description modify enchantment level cap
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class CapmodMixin {

    @Inject(
        method = "getMaxLevel()I",
        at = @At("RETURN"),
        cancellable = true)
    private void enchanttweaker$capmod$modifyMaxLevel(CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("CapmodMixin"))
            return;
        String key = ADUtils.getEnchantmentPath((Enchantment) (Object) this);
        if (key == null)
            return;
        cir.setReturnValue(ETMixinPlugin.getCapmodLevel(key, cir.getReturnValue()));
    }
}
