package com.adibarra.enchanttweaker.mixin.server.anvil;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.Utils;
import net.minecraft.screen.*;
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
		int maxCost = EnchantTweaker.getConfig().getOrDefault("nte_max_cost", orig);
		return Utils.clamp(maxCost, 0, Integer.MAX_VALUE);
	}
}