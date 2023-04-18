package com.adibarra.enchanttweaker.mixin.enchant.custom;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=BowItem.class, priority=1543)
public abstract class MoreFlameEncoderMixin {

	private int flameLevel = 1;

	/**
	 * @description Capture the flame level of the bow for later use.
	 * @environment Server
	 */
	@Inject(method="onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V", at=@At("HEAD"))
	private void captureFlameLevel(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
		flameLevel = EnchantmentHelper.getLevel(Enchantments.FLAME, stack);
	}

	/**
	 * @description Use the flame level to change the burn time of the flame enchantment.
	 * @environment Server
	 */
	@ModifyConstant(method="onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V", constant=@Constant(intValue=100))
	private int encoder(int original) {
		if (EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("more_flame", true)) {
			return flameLevel * original;
		}
		return original;
	}
}
