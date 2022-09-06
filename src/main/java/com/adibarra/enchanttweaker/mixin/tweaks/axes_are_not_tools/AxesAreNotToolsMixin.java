package com.adibarra.enchanttweaker.mixin.tweaks.axes_are_not_tools;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=MiningToolItem.class, priority=1543)
public abstract class AxesAreNotToolsMixin {

	/**
	 * @author adibarra
	 * @reason Disable extra durability loss penalty for using an axe as a weapon
	 */
	@Inject(method="postHit", at=@At("HEAD"), cancellable=true)
	public void enchanttweaker$axesAreNotTools(ItemStack stack, LivingEntity target, LivingEntity attacker, CallbackInfoReturnable<Boolean> cir) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("axes_are_not_tools", true);

		if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			if(stack.getItem() instanceof AxeItem) {
				stack.damage(1, attacker, (e) -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
				cir.setReturnValue(true);
			}
		}
	}
}