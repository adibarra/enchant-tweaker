package com.adibarra.enchanttweaker.mixin.client.lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADRoman;

/**
 * @description generate roman numeral translations above level ten
 * @environment client
 */
@Environment(EnvType.CLIENT)
@Mixin(TranslationStorage.class)
public abstract class RomanNumeralMixin {

    @Inject(
        method = "get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$romanNumeral$get(String key, String fallback, CallbackInfoReturnable<String> cir) {
        if (!ETMixinPlugin.getMixinConfig("RomanNumeralMixin"))
            return;
        String override = ADRoman.levelKeyOverride(key);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(
        method = "hasTranslation(Ljava/lang/String;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$romanNumeral$hasTranslation(String key, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("RomanNumeralMixin"))
            return;
        if (ADRoman.levelKeyOverride(key) != null) {
            cir.setReturnValue(true);
        }
    }
}
