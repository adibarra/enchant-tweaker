package com.adibarra.enchanttweaker.mixin.client.tweaks;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantment;
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

/**
 * @description Makes the enchantment name yellow when at max level.
 * Also adds a 'charged' effect. Ignores curse enchantments.
 * @note Uses the client's mod config for max enchantment levels.
 * @environment Client
 */
@Mixin(value=Enchantment.class, priority=1543)
public abstract class ShinyNameMixin {

	@Shadow
	public abstract int getMaxLevel();

	@Shadow
	public abstract boolean isCursed();

	@Inject(method="getName", at=@At(value="TAIL"), locals=LocalCapture.CAPTURE_FAILSOFT)
	private void getName(int level, CallbackInfoReturnable<Text> cir, MutableText mutableText) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("shiny_name", true)) {
			if(level >= this.getMaxLevel() && !this.isCursed()) {
				mutableText.formatted(Formatting.YELLOW);
				if (new Random().nextFloat() < 0.005f)
					mutableText.formatted(Formatting.OBFUSCATED);
			}
		}
	}
}
