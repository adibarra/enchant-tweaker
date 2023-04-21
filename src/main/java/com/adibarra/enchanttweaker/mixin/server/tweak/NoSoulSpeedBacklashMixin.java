package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value=LivingEntity.class, priority=1543)
public abstract class NoSoulSpeedBacklashMixin {

	/**
	 * @description Disable Soul Speed enchant armor self-damage backlash.
	 * @environment Server
	 */
	@ModifyConstant(method="addSoulSpeedBoostIfNeeded()V", constant=@Constant(intValue=1))
	private int removeBacklash(int original) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("no_soul_speed_backlash", true)) {
			return 0;
		}
		return original;
	}
}