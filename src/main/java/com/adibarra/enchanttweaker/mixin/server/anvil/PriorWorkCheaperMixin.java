package com.adibarra.enchanttweaker.mixin.server.anvil;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADMath;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Enchanting/repairing cost is cheaper with prior work.
 * @note Not ModifyConstant because we want to work with doubles then cast to int.
 * @environment Server
 */
@Mixin(value=AnvilScreenHandler.class)
public abstract class PriorWorkCheaperMixin {

    @Inject(
        method="getNextCost(I)I",
        at=@At("HEAD"),
        cancellable=true)
    private static void enchanttweaker$priorWorkCheaper$modifyRepairCost(int cost, CallbackInfoReturnable<Integer> cir) {
        double coefficient = ETMixinPlugin.getConfig().getOrDefault("pw_cost_multiplier", 2.0);
        double newCost = ADMath.clamp(coefficient, 0, Double.MAX_VALUE) * cost + 1;
        cir.setReturnValue((int) Math.round(newCost));
    }
}
