package com.adibarra.enchanttweaker.mixin.server.anvil;

import com.adibarra.enchanttweaker.ETUtils;
import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Enchanting/repairing cost is cheaper with prior work.
 * @environment Server
 */
@Mixin(value= AnvilScreenHandler.class, priority=1543)
public abstract class PriorWorkCheaperMixin {
	@Inject(method="getNextCost", at=@At(value="HEAD"), cancellable=true)
	private static void priorWorkCheaper(int cost, CallbackInfoReturnable<Integer> cir) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("prior_work_cheaper", false)) {
			double prior_work_coefficient = EnchantTweaker.getConfig().getOrDefault("pw_cost_multiplier", 1.66);
			cir.setReturnValue((int) Math.round(ETUtils.clamp(prior_work_coefficient, 0, Double.MAX_VALUE) * cost + 1));
		}
	}
}