package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.MultishotEnchantment;
import net.minecraft.enchantment.PiercingEnchantment;
import net.minecraft.registry.Registries;
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
    value = {MultishotEnchantment.class, PiercingEnchantment.class})
public abstract class MultishotPiercingMixin {

    @Inject(
        method = "canAccept(Lnet/minecraft/enchantment/Enchantment;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$multishotPiercing$allowCoexist(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("MultishotPiercingMixin"))
            return;
        String selfId = Registries.ENCHANTMENT.getKey((Enchantment) (Object) this).map(key -> key.getValue().toString())
            .orElse(null);
        String otherId = Registries.ENCHANTMENT.getKey(other).map(key -> key.getValue().toString()).orElse(null);
        if (selfId == null || otherId == null)
            return;
        if ((selfId.equals("minecraft:multishot") && otherId.equals("minecraft:piercing"))
            || (selfId.equals("minecraft:piercing") && otherId.equals("minecraft:multishot"))) {
            cir.setReturnValue(true);
        }
    }
}
