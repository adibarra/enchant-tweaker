package com.adibarra.enchanttweaker.mixin.enchant.tweaks;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow armor to be enchanted with multiple protection enchantments.
 * @environment Server
 */
@Mixin(value=ProtectionEnchantment.class, priority=1543)
public abstract class GodArmorMixin {
	@Inject(method="canAccept", at=@At("HEAD"), cancellable=true)
	private void allowGodArmor(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("god_armor", true)) {
			if(other instanceof ProtectionEnchantment)
				cir.setReturnValue(true);
		}
	}
}