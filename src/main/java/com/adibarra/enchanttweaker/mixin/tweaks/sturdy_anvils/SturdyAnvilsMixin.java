package com.adibarra.enchanttweaker.mixin.tweaks.sturdy_anvils;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value=AnvilScreenHandler.class, priority=1543)
public abstract class SturdyAnvilsMixin {

	/**
	 * @author adibarra
	 * @reason Anvils don't break as often (modify const in lambda in onTakeOutput)
	 */
	@ModifyConstant(method= "method_24922(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V", constant=@Constant(floatValue=0.12f))
	private static float enchanttweaker$sturdyAnvils(float previousBreakChance) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("anvils_last_longer", true);
		double anvil_damage_chance = EnchantTweaker.getConfig().getOrDefault("anvil_damage_chance", 0.06);

		if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			return (float) anvil_damage_chance;
		}
		return previousBreakChance;
	}
}