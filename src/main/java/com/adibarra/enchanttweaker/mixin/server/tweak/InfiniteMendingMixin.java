package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description allow infinity and mending enchantments to co-exist
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class InfiniteMendingMixin {

    @Inject(
        method = "areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private static void enchanttweaker$infiniteMending$allowCoexist(Holder<Enchantment> first,
        Holder<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("InfiniteMendingMixin"))
            return;
        if ((first.is(Enchantments.INFINITY) && second.is(Enchantments.MENDING))
            || (first.is(Enchantments.MENDING) && second.is(Enchantments.INFINITY))) {
            cir.setReturnValue(true);
        }
    }
}
