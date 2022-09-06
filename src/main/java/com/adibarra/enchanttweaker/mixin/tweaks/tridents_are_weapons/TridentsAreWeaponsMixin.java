package com.adibarra.enchanttweaker.mixin.tweaks.tridents_are_weapons;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.TridentItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Enchantment.class, priority=1543)
public abstract class TridentsAreWeaponsMixin {

    /**
     * @author adibarra
     * @reason Allow some melee enchantments to be added to tridents
     */
    @Inject(method="isAcceptableItem", at=@At("HEAD"), cancellable=true)
    public void enchanttweaker$tridentsAreWeapons(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("tridents_are_weapons", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            if(stack.getItem() instanceof TridentItem) {
                //noinspection ConstantConditions
                if ((Object) this == Enchantments.FIRE_ASPECT || (Object) this == Enchantments.KNOCKBACK || (Object) this == Enchantments.LOOTING) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}