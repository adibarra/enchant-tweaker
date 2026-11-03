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
 * @description allow multishot and piercing enchantments to co-exist
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class MultishotPiercingMixin {

    @Inject(
        method = "areCompatible(Lnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private static void enchanttweaker$multishotPiercing$allowCoexist(Holder<Enchantment> first,
        Holder<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("MultishotPiercingMixin"))
            return;
        if ((first.is(Enchantments.MULTISHOT) && second.is(Enchantments.PIERCING))
            || (first.is(Enchantments.PIERCING) && second.is(Enchantments.MULTISHOT))) {
            cir.setReturnValue(true);
        }
    }
}
