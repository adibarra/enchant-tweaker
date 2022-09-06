package com.adibarra.enchanttweaker.mixin.enchantments.vanilla;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.VanishingCurseEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=VanishingCurseEnchantment.class, priority=1543)
public abstract class VanishingCurseMixin {

    /**
     * @author adibarra
     * @reason Modify enchantment level cap
     */
    @Inject(method="getMaxLevel()I", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$getMaxLevel(CallbackInfoReturnable<Integer> cir) {
        int level_cap = EnchantTweaker.getConfig().getOrDefault("curse_of_vanishing", -1);

        if(EnchantTweaker.MOD_ENABLED && level_cap != -1) {
            cir.setReturnValue(level_cap);
        }
    }
}