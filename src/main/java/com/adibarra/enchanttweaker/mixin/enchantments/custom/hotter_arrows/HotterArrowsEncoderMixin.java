package com.adibarra.enchanttweaker.mixin.enchantments.custom.hotter_arrows;

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
public abstract class HotterArrowsEncoderMixin {

	private int enchanttweaker$flameLevel;

	@Inject(method="onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V", at=@At("HEAD"))
	private void enchanttweaker$localCaptureFlameLevel(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo ci) {
		enchanttweaker$flameLevel = EnchantmentHelper.getLevel(Enchantments.FLAME, stack);
	}

	/**
	 * @author adibarra
	 * @reason Changes flame enchantment behavior depending on level
	 * @return Multiplies default burn time by flame level for later use
	 */
	@ModifyConstant(method="onStoppedUsing(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)V", constant=@Constant(intValue=100))
	private int enchanttweaker$hotterArrowsBowEncoder(int original) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("hotter_arrows", true);

		if (EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			return enchanttweaker$flameLevel * original;
		}
		return original;
	}
}
