package com.adibarra.enchanttweaker.mixin.enchant.custom;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.ThornsEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value=ThornsEnchantment.class, priority=1543)
public abstract class NoThornsBacklashMixin {

	/**
	 * @description Disable Thorns enchant armor self-damage backlash.
	 * @environment Server
	 */
	@ModifyConstant(method="onUserDamaged(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;I)V", constant=@Constant(intValue=2))
	private int removeBacklash(int original) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("no_thorns_backlash", true)) {
			return 0;
		}
		return original;
	}
}