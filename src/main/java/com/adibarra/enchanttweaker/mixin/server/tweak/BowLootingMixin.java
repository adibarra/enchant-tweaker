package com.adibarra.enchanttweaker.mixin.server.tweak;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Allow bows and crossbows to be enchanted with Looting.
 * @environment Server
 */
@Mixin(value=Enchantment.class)
public abstract class BowLootingMixin {

    @Inject(
        method="isAcceptableItem(Lnet/minecraft/item/ItemStack;)Z",
        at=@At("HEAD"),
        cancellable=true)
    private void enchanttweaker$bowLooting$allowLooting(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getMixinConfig("BowLootingMixin")) return;
        boolean isBowOrCrossbow = stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
        if (!isBowOrCrossbow) return;

        Enchantment enchantment = (Enchantment) (Object) this;
        if (enchantment == Enchantments.LOOTING) {
            cir.setReturnValue(true);
        }
    }
}
