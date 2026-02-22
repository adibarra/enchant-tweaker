package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.enchantment.ThornsEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Disable Thorns enchant armor self-damage backlash.
 * @environment Server
 */
@Mixin(value=ThornsEnchantment.class)
public abstract class NoThornsBacklashMixin {

    @SuppressWarnings("SameReturnValue")
    @ModifyConstant(
        method="onUserDamaged(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;I)V",
        constant=@Constant(intValue=2))
    private int enchanttweaker$noThornsBacklash$noBacklash(int orig) {
        if (!ETMixinPlugin.getMixinConfig("NoThornsBacklashMixin")) return orig;
        return 0;
    }
}
