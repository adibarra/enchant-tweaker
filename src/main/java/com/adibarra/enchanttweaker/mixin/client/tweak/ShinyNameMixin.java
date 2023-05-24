package com.adibarra.enchanttweaker.mixin.client.tweak;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * @description Makes the enchantment name yellow when at max level.
 * Also adds a 'charged' effect. Ignores curse enchantments.
 * @note Uses the client's mod config for max enchantment levels.
 * @environment Client
 */
@Environment(EnvType.CLIENT)
@Mixin(value=Enchantment.class)
public abstract class ShinyNameMixin {

    private static final Random RAND = new Random();

    @Shadow
    public abstract int getMaxLevel();

    @Shadow
    public abstract boolean isCursed();

    @Inject(
        method="getName(I)Lnet/minecraft/text/Text;",
        at=@At("TAIL"))
    private void getName(int level, CallbackInfoReturnable<Text> cir, @Local MutableText mutableText) {
        if (level < this.getMaxLevel()) return;

        if (!this.isCursed()) {
            mutableText.formatted(Formatting.YELLOW);
        }

        if (RAND.nextFloat() < 0.005f) {
            mutableText.formatted(Formatting.OBFUSCATED);
        }
    }
}
