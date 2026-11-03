package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.DamageEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description allow tridents to be enchanted with fire aspect, knockback, and
 *              looting
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class TridentWeaponsMixin {

    @Inject(
        method = "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$tridentWeapons$allowFireAspectKnockbackLooting(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("TridentWeaponsMixin"))
            return;
        boolean isTrident = stack.getItem() instanceof TridentItem;
        if (!isTrident)
            return;

        Enchantment enchantment = (Enchantment) (Object) this;
        if (enchantment instanceof DamageEnchantment || enchantment == Enchantments.FIRE_ASPECT
            || enchantment == Enchantments.KNOCKBACK || enchantment == Enchantments.LOOTING) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "isPrimaryItem(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$tridentWeapons$allowDamageEnchantmentsAtEnchantingTable(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("TridentWeaponsMixin"))
            return;
        if (stack.getItem() instanceof TridentItem && (Enchantment) (Object) this instanceof DamageEnchantment) {
            cir.setReturnValue(true);
        }
    }
}
