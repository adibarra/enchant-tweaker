package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
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
 * @description allow axes to be enchanted with fire aspect, knockback, and
 *              looting
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class AxeWeaponsMixin {

    @Unique
    private boolean enchanttweaker$axeWeapons$isAxeWeaponEnchantment() {
        Enchantment enchantment = (Enchantment) (Object) this;
        return ADUtils.isEnchantment(enchantment, Enchantments.FIRE_ASPECT)
            || ADUtils.isEnchantment(enchantment, Enchantments.KNOCKBACK)
            || ADUtils.isEnchantment(enchantment, Enchantments.LOOTING);
    }

    @Inject(
        method = "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$axeWeapons$allowFireAspectKnockbackLooting(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("AxeWeaponsMixin"))
            return;
        if (stack.getItem() instanceof AxeItem && enchanttweaker$axeWeapons$isAxeWeaponEnchantment()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "isSupportedItem(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$axeWeapons$allowFireAspectKnockbackLootingOnEnchant(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("AxeWeaponsMixin"))
            return;
        if (stack.getItem() instanceof AxeItem && enchanttweaker$axeWeapons$isAxeWeaponEnchantment()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "isPrimaryItem(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$axeWeapons$allowFireAspectKnockbackLootingAtEnchantingTable(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("AxeWeaponsMixin"))
            return;
        if (stack.getItem() instanceof AxeItem && enchanttweaker$axeWeapons$isAxeWeaponEnchantment()) {
            cir.setReturnValue(true);
        }
    }
}
