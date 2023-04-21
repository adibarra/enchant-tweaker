package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow weapons to be enchanted with multiple damage enchantments.
 * @environment Server
 */
@Mixin(value=DamageEnchantment.class, priority=1543)
public abstract class GodWeaponsMixin {
	@Inject(method="canAccept", at=@At("HEAD"), cancellable=true)
	private void allowGodWeapons(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("god_weapons", true)) {
			if(other instanceof DamageEnchantment)
				cir.setReturnValue(true);
		}
	}
}