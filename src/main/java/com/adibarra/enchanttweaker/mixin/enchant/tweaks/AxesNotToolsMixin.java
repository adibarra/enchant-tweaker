package com.adibarra.enchanttweaker.mixin.enchant.tweaks;

import com.adibarra.enchanttweaker.EnchantTweaker;
		import net.minecraft.entity.EquipmentSlot;
		import net.minecraft.entity.LivingEntity;
		import net.minecraft.item.AxeItem;
		import net.minecraft.item.ItemStack;
		import net.minecraft.item.MiningToolItem;
		import org.spongepowered.asm.mixin.Mixin;
		import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=MiningToolItem.class, priority=1543)
public abstract class AxesNotToolsMixin {

	/**
	 * @description Remove extra self-damage from axes when used as a weapon.
	 * @environment Server
	 */
	@ModifyConstant(method="postHit", constant=@Constant(intValue=2))
	public int modifySelfDamage(int original) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("axes_not_tools", true)) {
			return 1;
		}
		return original;
	}
}