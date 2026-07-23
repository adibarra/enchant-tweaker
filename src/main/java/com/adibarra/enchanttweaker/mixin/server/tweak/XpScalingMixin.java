package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description replace vanilla's tiered xp-per-level formula with a
 *              configurable linear formula
 * @environment server
 */
@Mixin(
    value = PlayerEntity.class)
public abstract class XpScalingMixin {

    @Shadow
    public int experienceLevel;

    @Inject(
        method = "getNextLevelExperience()I",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$xpScaling$linearize(CallbackInfoReturnable<Integer> cir) {
        if (!ETMixinPlugin.getMixinConfig("XpScalingMixin"))
            return;
        int base = ETMixinPlugin.getConfig().getOrDefault("xp_scaling_base", 7);
        int step = ETMixinPlugin.getConfig().getOrDefault("xp_scaling_step", 2);
        // long math avoids int overflow
        // clamp the result to the valid range
        cir.setReturnValue((int) Math.clamp((long) base + (long) step * experienceLevel, 1, Integer.MAX_VALUE));
    }
}
