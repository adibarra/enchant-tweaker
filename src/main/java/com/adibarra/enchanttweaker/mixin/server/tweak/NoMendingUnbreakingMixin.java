package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Make Mending and Unbreaking enchantments mutually exclusive.
 * @environment Server
 */
@Mixin(value=Enchantment.class)
public abstract class NoMendingUnbreakingMixin {

    @Inject(
        method="canAccept(Lnet/minecraft/enchantment/Enchantment;)Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$noMendingUnbreaking$blockCoexist(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("NoMendingUnbreakingMixin")) return;
        String selfP = ADUtils.getEnchantmentPath((Enchantment)(Object)this);
        String otherP = ADUtils.getEnchantmentPath(other);
        if (selfP == null || otherP == null) return;
        if ((selfP.equals("mending") && otherP.equals("unbreaking")) ||
            (selfP.equals("unbreaking") && otherP.equals("mending"))) {
            cir.setReturnValue(false);
        }
    }
}
