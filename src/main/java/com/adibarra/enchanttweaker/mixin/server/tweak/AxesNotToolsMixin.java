package com.adibarra.enchanttweaker.mixin.server.tweak;

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

	@ModifyConstant(
		method="postHit(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/LivingEntity;)Z",
		constant=@Constant(intValue=2))
	private int modifySelfDamage(int orig, ItemStack stack) {
		boolean isAxe = stack.getItem() instanceof AxeItem;
		return isAxe ? 1 : orig;
	}
}