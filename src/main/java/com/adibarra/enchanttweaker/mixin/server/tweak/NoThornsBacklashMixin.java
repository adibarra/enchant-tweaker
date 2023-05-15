package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.enchantment.ThornsEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Disable Thorns enchant armor self-damage backlash.
 * @environment Server
 */
@Mixin(value=ThornsEnchantment.class, priority=1543)
public abstract class NoThornsBacklashMixin {

    @SuppressWarnings("SameReturnValue")
    @ModifyConstant(
        method="onUserDamaged(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;I)V",
        constant=@Constant(intValue=2))
    private int enchanttweaker$noThornsBacklash$noBacklash(int orig) {
        return 0;
    }
}
