package com.adibarra.enchanttweaker.mixin.server.anvil;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADMath;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Anvils no longer say "Too Expensive!"
 * @environment Server
 */
@Mixin(value=AnvilScreenHandler.class, priority=1543)
public abstract class NotTooExpensiveMixin {

    @ModifyConstant(
        method="updateResult()V",
        constant=@Constant(intValue=40, ordinal=2))
    private int notTooExpensive(int orig) {
        int maxCost = ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", orig);
        return ADMath.clamp(maxCost, 0, Integer.MAX_VALUE);
    }
}
