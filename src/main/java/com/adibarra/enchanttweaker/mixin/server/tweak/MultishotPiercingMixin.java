package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.enchantment.ArrowPiercingEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.MultiShotEnchantment;
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
    value = {MultiShotEnchantment.class, ArrowPiercingEnchantment.class})
public abstract class MultishotPiercingMixin {

    @Inject(
        method = "checkCompatibility(Lnet/minecraft/world/item/enchantment/Enchantment;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$multishotPiercing$allowCoexist(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("MultishotPiercingMixin"))
            return;
        String selfId = BuiltInRegistries.ENCHANTMENT.getResourceKey((Enchantment) (Object) this)
            .map(key -> key.location().toString()).orElse(null);
        String otherId = BuiltInRegistries.ENCHANTMENT.getResourceKey(other).map(key -> key.location().toString())
            .orElse(null);
        if (selfId == null || otherId == null)
            return;
        if ((selfId.equals("minecraft:multishot") && otherId.equals("minecraft:piercing"))
            || (selfId.equals("minecraft:piercing") && otherId.equals("minecraft:multishot"))) {
            cir.setReturnValue(true);
        }
    }
}
