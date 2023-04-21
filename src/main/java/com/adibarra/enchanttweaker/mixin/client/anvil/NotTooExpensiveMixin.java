package com.adibarra.enchanttweaker.mixin.client.anvil;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.enchanttweaker.ETUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Anvils no longer say "Too Expensive!"
 * @environment Client
 */
@Environment(EnvType.CLIENT)
@Mixin(value=AnvilScreen.class, priority=1543)
public abstract class NotTooExpensiveMixin {
	@ModifyConstant(method="drawForeground", constant=@Constant(intValue=40))
	private int notTooExpensiveClient(int previous) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("not_too_expensive", true)) {
			int max_cost = EnchantTweaker.getConfig().getOrDefault("nte_max_cost", Integer.MAX_VALUE);
			return ETUtils.clamp(max_cost, 0, Integer.MAX_VALUE);
		}
		return previous;
	}
}