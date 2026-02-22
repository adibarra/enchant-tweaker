package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Disable Soul Speed enchant armor self-damage backlash.
 * @environment Server
 */
@Mixin(value=LivingEntity.class)
public abstract class NoSoulSpeedBacklashMixin {

    @SuppressWarnings("SameReturnValue")
    @ModifyConstant(
        method="addSoulSpeedBoostIfNeeded()V",
        constant=@Constant(intValue=1))
    private int enchanttweaker$noSoulSpeedBacklash$noBacklash(int orig) {
        if (!ETMixinPlugin.getMixinConfig("NoSoulSpeedBacklashMixin")) return orig;
        return 0;
    }
}
