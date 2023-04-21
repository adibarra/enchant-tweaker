package com.adibarra.enchanttweaker.mixin.server.anvil;

import com.adibarra.enchanttweaker.ETUtils;
import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Customize the chance of an anvil breaking when used.
 * @note This modifies a constant in a lambda in onTakeOutput
 * @environment Server
 */
@Mixin(value= AnvilScreenHandler.class, priority=1543)
public abstract class SturdyAnvilsMixin {
	@ModifyConstant(
			method="method_24922(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
			constant=@Constant(floatValue=0.12f)
	)
	private static float sturdyAnvils(float previousBreakChance) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("sturdy_anvils", true)) {
			double anvil_damage_chance = EnchantTweaker.getConfig().getOrDefault("anvil_damage_chance", 0.06);
			return (float) ETUtils.clamp(anvil_damage_chance, 0, 1);
		}
		return previousBreakChance;
	}
}