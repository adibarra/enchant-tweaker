package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description allow tridents to be enchanted with fire aspect, knockback, and
 *              looting
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class TridentWeaponsMixin {

    @Unique
    private boolean enchanttweaker$tridentWeapons$isTridentWeaponEnchantment() {
        Enchantment enchantment = (Enchantment) (Object) this;
        return ADUtils.isEnchantment(enchantment, Enchantments.SHARPNESS)
            || ADUtils.isEnchantment(enchantment, Enchantments.SMITE)
            || ADUtils.isEnchantment(enchantment, Enchantments.BANE_OF_ARTHROPODS)
            || ADUtils.isEnchantment(enchantment, Enchantments.FIRE_ASPECT)
            || ADUtils.isEnchantment(enchantment, Enchantments.KNOCKBACK)
            || ADUtils.isEnchantment(enchantment, Enchantments.LOOTING);
    }

    @Inject(
        method = "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$tridentWeapons$allowFireAspectKnockbackLooting(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("TridentWeaponsMixin"))
            return;
        if (stack.getItem() instanceof TridentItem && enchanttweaker$tridentWeapons$isTridentWeaponEnchantment()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "isSupportedItem(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$tridentWeapons$allowFireAspectKnockbackLootingOnEnchant(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("TridentWeaponsMixin"))
            return;
        if (stack.getItem() instanceof TridentItem && enchanttweaker$tridentWeapons$isTridentWeaponEnchantment()) {
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
        if (stack.getItem() instanceof TridentItem && enchanttweaker$tridentWeapons$isTridentWeaponEnchantment()) {
            cir.setReturnValue(true);
        }
    }
}
