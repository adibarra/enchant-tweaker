package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
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
 * @description allow bows and crossbows to be enchanted with looting
 * @environment server
 */
@Mixin(
    value = Enchantment.class)
public abstract class BowLootingMixin {

    @Inject(
        method = "canEnchant(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$bowLooting$allowLooting(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("BowLootingMixin"))
            return;
        if (enchanttweaker$bowLooting$isBowOrCrossbow(stack)
            && ADUtils.isEnchantment((Enchantment) (Object) this, Enchantments.LOOTING)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "isSupportedItem(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$bowLooting$allowLootingOnEnchant(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("BowLootingMixin"))
            return;
        if (enchanttweaker$bowLooting$isBowOrCrossbow(stack)
            && ADUtils.isEnchantment((Enchantment) (Object) this, Enchantments.LOOTING)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "isPrimaryItem(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$bowLooting$allowLootingAtEnchantingTable(ItemStack stack,
        CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("BowLootingMixin"))
            return;
        if (enchanttweaker$bowLooting$isBowOrCrossbow(stack)
            && ADUtils.isEnchantment((Enchantment) (Object) this, Enchantments.LOOTING)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean enchanttweaker$bowLooting$isBowOrCrossbow(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }
}
