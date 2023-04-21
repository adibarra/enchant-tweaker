package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.InfinityEnchantment;
import net.minecraft.enchantment.MendingEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow Infinity and Mending enchantments to co-exist.
 * @environment Server
 */
@Mixin(value=InfinityEnchantment.class, priority=1543)
public abstract class InfiniteMendingMixin {
	@Inject(method="canAccept", at=@At("HEAD"), cancellable=true)
	private void allowInfinityMending(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("infinite_mending", true)) {
			if (other instanceof MendingEnchantment) {
				cir.setReturnValue(true);
			}
		}
	}
}