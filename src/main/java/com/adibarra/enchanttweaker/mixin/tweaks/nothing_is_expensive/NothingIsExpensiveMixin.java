package com.adibarra.enchanttweaker.mixin.tweaks.nothing_is_expensive;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=AnvilScreen.class, priority=1543)
public abstract class NothingIsExpensiveMixin {

	/**
	 * @author adibarra
	 * @reason Anvils no longer say "Too Expensive!"
	 */
	@ModifyConstant(method="drawForeground", constant=@Constant(intValue=40))
	private int enchanttweaker$nothingIsExpensive(int previous) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("nothing_is_expensive", true);
		int this_is_too_expensive = EnchantTweaker.getConfig().getOrDefault("nothing_is_expensive", Integer.MAX_VALUE);

		if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			return this_is_too_expensive;
		}
		return previous;
	}
}