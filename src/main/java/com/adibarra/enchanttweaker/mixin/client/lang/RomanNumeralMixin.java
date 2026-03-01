package com.adibarra.enchanttweaker.mixin.client.lang;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.client.resource.language.TranslationStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Dynamically generate Roman numeral translations for enchantment levels above X.
 * @environment Client
 */
@Mixin(TranslationStorage.class)
public abstract class RomanNumeralMixin {

    @Unique
    private static final String LEVEL_PREFIX = "enchantment.level.";

    @Unique
    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    @Unique
    private static final String[] ROMAN_SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    @Inject(method="get(Ljava/lang/String;)Ljava/lang/String;", at=@At("HEAD"), cancellable=true)
    private void enchanttweaker$romanNumeral$get(String key, CallbackInfoReturnable<String> cir) {
        if (!ETMixinPlugin.getMixinConfig("RomanNumeralMixin")) return;
        if (!key.startsWith(LEVEL_PREFIX)) return;
        try {
            int level = Integer.parseInt(key.substring(LEVEL_PREFIX.length()));
            if (level == 0) {
                cir.setReturnValue("0");
            } else if (level > 10) {
                cir.setReturnValue(enchanttweaker$toRoman(level));
            }
        } catch (NumberFormatException ignored) {}
    }

    @Inject(method="hasTranslation(Ljava/lang/String;)Z", at=@At("HEAD"), cancellable=true)
    private void enchanttweaker$romanNumeral$hasTranslation(String key, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("RomanNumeralMixin")) return;
        if (!key.startsWith(LEVEL_PREFIX)) return;
        try {
            int level = Integer.parseInt(key.substring(LEVEL_PREFIX.length()));
            if (level == 0 || level > 10) {
                cir.setReturnValue(true);
            }
        } catch (NumberFormatException ignored) {}
    }

    @Unique
    private static String enchanttweaker$toRoman(int num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUES.length; i++) {
            while (num >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_SYMBOLS[i]);
                num -= ROMAN_VALUES[i];
            }
        }
        return sb.toString();
    }
}
