package com.adibarra.enchanttweaker.mixin.enchantments.custom.thorns_dont_damage_armor;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.ThornsEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=ThornsEnchantment.class, priority=1543)
public abstract class ThornsDontDamageArmorMixin {

    /**
     * @author adibarra
     * @reason Disable damage thorns does to own armor
     */
    @ModifyConstant(method="onUserDamaged(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;I)V", constant=@Constant(intValue=2))
    private int enchanttweaker$thornsDontDamageArmor(int original) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("thorns_dont_damage_armor", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            return 0;
        }
        return original;
    }
}