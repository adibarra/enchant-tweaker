package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description make mending and unbreaking enchantments mutually exclusive
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class NoMendingUnbreakingMixin {

    @Inject(
        method = "canAccept(Lnet/minecraft/enchantment/Enchantment;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$noMendingUnbreaking$blockCoexist(Enchantment other,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("NoMendingUnbreakingMixin"))
            return;
        String selfId = Registries.ENCHANTMENT.getKey((Enchantment) (Object) this).map(key -> key.getValue().toString())
            .orElse(null);
        String otherId = Registries.ENCHANTMENT.getKey(other).map(key -> key.getValue().toString()).orElse(null);
        if (selfId == null || otherId == null)
            return;
        if ((selfId.equals("minecraft:mending") && otherId.equals("minecraft:unbreaking"))
            || (selfId.equals("minecraft:unbreaking") && otherId.equals("minecraft:mending"))) {
            cir.setReturnValue(false);
        }
    }
}
