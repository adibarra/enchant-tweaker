package com.adibarra.enchanttweaker.mixin.client.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADShiny;
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

import java.util.concurrent.ThreadLocalRandom;

/**
 * @description makes the enchantment name yellow when at max level
 * also adds a 'charged' effect. Ignores curse enchantments
 * @note uses the client's mod config for max enchantment levels
 * @environment Client
 */
@Environment(EnvType.CLIENT)
@Mixin(value=Enchantment.class)
public abstract class ShinyNameMixin {

    @Shadow
    public abstract int getMaxLevel();

    @Shadow
    public abstract boolean isCursed();

    @Inject(
        method="getName(I)Lnet/minecraft/text/Text;",
        at=@At("TAIL"))
    private void getName(int level, CallbackInfoReturnable<Text> cir, @Local MutableText mutableText) {
        if (!ETMixinPlugin.getMixinConfig("ShinyNameMixin")) return;
        if (level < this.getMaxLevel()) return;

        if (ADShiny.shouldColorGold(level, this.getMaxLevel(), this.isCursed())) {
            mutableText.formatted(Formatting.YELLOW);
        }

        if (ThreadLocalRandom.current().nextFloat() < 0.005f) {
            mutableText.formatted(Formatting.OBFUSCATED);
        }
    }
}
