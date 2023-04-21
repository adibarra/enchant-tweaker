package com.adibarra.enchanttweaker.mixin.server.tweaks;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Remove extra self-damage from axes when used as a weapon.
 * @environment Server
 */
@Mixin(value=MiningToolItem.class, priority=1543)
public abstract class AxesNotToolsMixin {
	@ModifyConstant(method="postHit", constant=@Constant(intValue=2))
	private int modifySelfDamage(int original, ItemStack stack) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("axes_not_tools", true)) {
			if(stack.getItem() instanceof AxeItem) {
				return 1;
			}
		}
		return original;
	}
}