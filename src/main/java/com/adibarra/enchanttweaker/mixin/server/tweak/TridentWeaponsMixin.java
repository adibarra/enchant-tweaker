package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow tridents to be enchanted with fire aspect, knockback, and looting.
 * @environment Server
 */
@Mixin(value=Enchantment.class, priority=1543)
public abstract class TridentWeaponsMixin {
	@Inject(method="isAcceptableItem", at=@At("HEAD"), cancellable=true)
	private void tridentWeapons(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("trident_weapons", true)) {
			if(stack.getItem() instanceof TridentItem) {
				Enchantment enchantment = (Enchantment) (Object) this;
				if(enchantment == Enchantments.FIRE_ASPECT || enchantment == Enchantments.KNOCKBACK || enchantment == Enchantments.LOOTING) {
					cir.setReturnValue(true);
				}
			}
		}
	}
}