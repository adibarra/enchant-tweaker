package com.adibarra.enchanttweaker.mixin.client.anvil;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.Utils;
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
	@ModifyConstant(
		method="drawForeground(Lnet/minecraft/client/util/math/MatrixStack;II)V",
		constant=@Constant(intValue=40))
	private int notTooExpensiveClient(int orig) {
		int max_cost = EnchantTweaker.getConfig().getOrDefault("nte_max_cost", orig);
		return Utils.clamp(max_cost, 0, Integer.MAX_VALUE);
	}
}