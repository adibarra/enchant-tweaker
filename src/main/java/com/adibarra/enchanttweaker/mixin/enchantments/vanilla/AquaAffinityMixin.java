package com.adibarra.enchanttweaker.mixin.enchantments.vanilla;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.EfficiencyEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=EfficiencyEnchantment.class, priority=1543)
public abstract class AquaAffinityMixin {

    /**
     * @author adibarra
     * @reason Modify enchantment level cap
     */
    @Inject(method="getMaxLevel()I", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$getMaxLevel(CallbackInfoReturnable<Integer> cir) {
        int level_cap = EnchantTweaker.getConfig().getOrDefault("aqua_affinity", -1);

        if(EnchantTweaker.MOD_ENABLED && level_cap != -1) {
            cir.setReturnValue(level_cap);
        }
    }
}