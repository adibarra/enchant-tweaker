package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.ProtectionEnchantment;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow armor to be enchanted with multiple protection enchantments.
 * @environment Server
 */
@Mixin(value=ProtectionEnchantment.class)
public abstract class GodArmorMixin {

    @Shadow @Final
    public ProtectionEnchantment.Type protectionType;

    @Inject(
        method="canAccept(Lnet/minecraft/enchantment/Enchantment;)Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$godArmor$allowAllProtectionEnchants(Enchantment other, CallbackInfoReturnable<Boolean> cir) {
        if (other instanceof ProtectionEnchantment protectionEnchantment) {
            cir.setReturnValue(this.protectionType != protectionEnchantment.protectionType);
        }
    }
}
