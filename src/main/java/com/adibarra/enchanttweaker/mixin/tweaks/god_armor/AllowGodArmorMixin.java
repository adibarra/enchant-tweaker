package com.adibarra.enchanttweaker.mixin.tweaks.god_armor;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ProtectionEnchantment.class, priority=1543)
public abstract class AllowGodArmorMixin {

    /**
     * @author adibarra
     * @reason Allow multiple ProtectionEnchantment types to overlap
     */
    @Inject(method="canAccept", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$allowGodArmor(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("allow_god_armor", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled && other instanceof ProtectionEnchantment) {
            cir.setReturnValue(true);
        }
    }
}