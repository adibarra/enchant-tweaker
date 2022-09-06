package com.adibarra.enchanttweaker.mixin.tweaks.prior_work_goes_unpaid;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ItemStack.class, priority=1543)
public abstract class PriorWorkGoesUnpaidMixin {

	/**
	 * @author adibarra
	 * @reason Disable prior work penalty
	 */
	@Inject(method="getRepairCost()I", at=@At(value="HEAD"), cancellable=true)
	private void enchanttweaker$priorWorkGoesUnpaid(CallbackInfoReturnable<Integer> cir) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("prior_work_goes_unpaid", false);

		if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			cir.setReturnValue(0);
		}
	}
}