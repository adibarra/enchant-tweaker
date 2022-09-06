package com.adibarra.enchanttweaker.mixin.tweaks.whats_the_max_level_again;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Random;

@SuppressWarnings({})
@Mixin(Enchantment.class)
public abstract class WhatsTheMaxLevelAgainMixin {

	@Shadow
	public abstract int getMaxLevel();

	/**
	 * @author adibarra
	 * @reason Show tooltip of a maxed out enchantments in yellow with a "charged" effect
	 */
	@Inject(method="getName", at=@At(value="TAIL"), locals=LocalCapture.CAPTURE_FAILSOFT)
	private void enchanttweaker$whatsTheMaxLevelAgain(int level, CallbackInfoReturnable<Text> cir, MutableText mutableText) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("whats_the_max_level_again", true);

		if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			if(level >= this.getMaxLevel()) {
				//noinspection ConstantConditions
				if(((Object) this) != Enchantments.BINDING_CURSE && ((Object) this) != Enchantments.VANISHING_CURSE) {
					mutableText.formatted(Formatting.YELLOW);

					if (new Random().nextFloat() < 0.005f)
						mutableText.formatted(Formatting.OBFUSCATED);
				}
			}
		}
	}
}