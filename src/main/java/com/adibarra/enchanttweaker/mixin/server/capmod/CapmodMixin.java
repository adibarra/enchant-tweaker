package com.adibarra.enchanttweaker.mixin.server.capmod;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADMath;
import com.adibarra.utils.ADUtils;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Modify enchantment level cap.
 * @environment Server
 */
@Mixin(value=Enchantment.class)
public abstract class CapmodMixin {

    @Inject(method="getMaxLevel()I", at=@At("RETURN"), cancellable=true)
    private void enchanttweaker$capmod$modifyMaxLevel(CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("CapmodMixin")) return;
        String key = ADUtils.getEnchantmentPath((Enchantment)(Object)this);
        if (key == null) return;
        int orig = cir.getReturnValue();
        int lvlCap = ETMixinPlugin.getConfig().getOrDefault(key, orig);
        if (lvlCap < 0) return;
        cir.setReturnValue(ADMath.clamp(lvlCap, 0, 255));
    }
}
