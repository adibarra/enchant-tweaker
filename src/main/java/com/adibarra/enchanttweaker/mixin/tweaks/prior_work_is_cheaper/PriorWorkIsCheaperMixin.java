package com.adibarra.enchanttweaker.mixin.tweaks.prior_work_is_cheaper;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=AnvilScreenHandler.class, priority=1543)
public abstract class PriorWorkIsCheaperMixin {

	/**
	 * @author adibarra
	 * @reason Substitute vanilla prior work penalty multiplier for custom coefficient
	 */
	@Inject(method="getNextCost(I)I", at=@At("HEAD"), cancellable=true)
	private static void enchanttweaker$priorWorkIsCheaper(int cost, CallbackInfoReturnable<Integer> cir) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("prior_work_is_cheaper", true);
		double prior_work_coefficient = EnchantTweaker.getConfig().getOrDefault("prior_work_coefficient", 1.66);

		if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			cir.setReturnValue((int) Math.ceil(prior_work_coefficient * cost + 1));
		}
	}
}