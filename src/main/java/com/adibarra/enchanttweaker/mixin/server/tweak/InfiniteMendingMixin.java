package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.world.item.enchantment.ArrowInfiniteEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description allow infinity and mending enchantments to co-exist
 * @environment server
 */
@Mixin(
    value = ArrowInfiniteEnchantment.class)
public abstract class InfiniteMendingMixin {

    @Inject(
        method = "checkCompatibility(Lnet/minecraft/world/item/enchantment/Enchantment;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$infiniteMending$allowCoexist(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("InfiniteMendingMixin"))
            return;
        String selfP = ADUtils.getEnchantmentPath((Enchantment) (Object) this);
        String otherP = ADUtils.getEnchantmentPath(other);
        if (selfP == null || otherP == null)
            return;
        if ((selfP.equals("infinity") && otherP.equals("mending"))
            || (selfP.equals("mending") && otherP.equals("infinity"))) {
            cir.setReturnValue(true);
        }
    }
}
