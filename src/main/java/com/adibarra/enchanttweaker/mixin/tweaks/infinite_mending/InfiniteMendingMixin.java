package com.adibarra.enchanttweaker.mixin.tweaks.infinite_mending;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.InfinityEnchantment;
import net.minecraft.enchantment.MendingEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=InfinityEnchantment.class, priority=1543)
public abstract class InfiniteMendingMixin {

	/**
	 * @author adibarra
	 * @reason Allow combining infinity and mending enchantments
	 */
	@Inject(method="canAccept(Lnet/minecraft/enchantment/Enchantment;)Z", at=@At("HEAD"), cancellable=true)
	public void enchanttweaker$canAccept(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("infinite_mending", true);

		if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			if (other instanceof MendingEnchantment) {
				cir.setReturnValue(true);
			}
		}
	}
}