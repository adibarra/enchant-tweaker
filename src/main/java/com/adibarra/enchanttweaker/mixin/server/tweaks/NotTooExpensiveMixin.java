package com.adibarra.enchanttweaker.mixin.server.tweaks;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.enchanttweaker.ETUtils;
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
	@ModifyConstant(method="updateResult", constant=@Constant(intValue=40, ordinal=2))
	private int notTooExpensive(int previous) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("not_too_expensive", true)) {
			int max_cost = EnchantTweaker.getConfig().getOrDefault("nte_max_cost", Integer.MAX_VALUE);
			return ETUtils.clamp(max_cost, 0, Integer.MAX_VALUE);
		}
		return previous;
	}
}