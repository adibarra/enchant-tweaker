package com.adibarra.enchanttweaker.mixin.enchantments.vanilla;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.LuckEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=LuckEnchantment.class, priority=1543)
public abstract class LuckMixin {

    /**
     * @author adibarra
     * @reason Modify enchantment level cap
     */
    @Inject(method="getMaxLevel()I", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$getMaxLevel(CallbackInfoReturnable<Integer> cir) {
        int looting_level_cap = EnchantTweaker.getConfig().getOrDefault("looting", -1);
        int fortune_level_cap = EnchantTweaker.getConfig().getOrDefault("fortune", -1);
        int luck_of_the_sea_level_cap = EnchantTweaker.getConfig().getOrDefault("luck_of_the_sea", -1);

        if(EnchantTweaker.MOD_ENABLED) {
            if (((Object)this) == Enchantments.LOOTING) {
                if (looting_level_cap != -1) {
                    cir.setReturnValue(looting_level_cap);
                }
            } else if (((Object)this) == Enchantments.FORTUNE) {
                if (fortune_level_cap != -1) {
                    cir.setReturnValue(fortune_level_cap);
                }
            } else if (((Object)this) == Enchantments.LUCK_OF_THE_SEA) {
                if (luck_of_the_sea_level_cap != -1) {
                    cir.setReturnValue(luck_of_the_sea_level_cap);
                }
            }
        }
    }
}