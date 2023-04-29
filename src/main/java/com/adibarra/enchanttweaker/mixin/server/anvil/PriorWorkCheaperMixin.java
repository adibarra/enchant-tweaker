package com.adibarra.enchanttweaker.mixin.server.anvil;

import com.adibarra.utils.Utils;
import com.adibarra.enchanttweaker.EnchantTweaker;
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
@Mixin(value= AnvilScreenHandler.class, priority=1543)
public abstract class PriorWorkCheaperMixin {

	@Inject(
		method="getNextCost(I)I",
		at=@At(value="HEAD"),
		cancellable=true)
	private static void priorWorkCheaper(int cost, CallbackInfoReturnable<Integer> cir) {
		double coefficient = EnchantTweaker.getConfig().getOrDefault("pw_cost_multiplier", 1.66);
		double newCost = Utils.clamp(coefficient, 0, Double.MAX_VALUE) * cost + 1;
		cir.setReturnValue((int) Math.round(newCost));
	}
}