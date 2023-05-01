package com.adibarra.enchanttweaker.mixin.server.anvil;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADMath;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Customize the chance of an anvil breaking when used.
 * @note This modifies a constant in a lambda in onTakeOutput
 * @environment Server
 */
@Mixin(value=AnvilScreenHandler.class, priority=1543)
public abstract class SturdyAnvilsMixin {

	@ModifyConstant(
		method="method_24922(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
		constant=@Constant(floatValue=0.12f))
	private static float sturdyAnvils(float orig) {
		double anvilDamageChance = ETMixinPlugin.getConfig().getOrDefault("anvil_damage_chance", orig);
		return ADMath.clamp((float) anvilDamageChance, 0f, 1f);
	}
}
