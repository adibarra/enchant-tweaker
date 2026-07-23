package com.adibarra.enchanttweaker.mixin.server.grindstone;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/** allows plain books in grindstone input slots */
@Mixin(
    targets = {"net.minecraft.screen.GrindstoneScreenHandler$2", "net.minecraft.screen.GrindstoneScreenHandler$3"})
public abstract class GrindstoneInputSlotMixin extends Slot {
    protected GrindstoneInputSlotMixin(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public int getMaxItemCount(ItemStack stack) {
        if (ETMixinPlugin.getConfig().getOrDefault("grindstone_disenchant", false) && stack.isOf(Items.BOOK))
            return 1;
        return super.getMaxItemCount(stack);
    }

    @Inject(
        method = "canInsert(Lnet/minecraft/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$grindstoneDisenchant$canInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getConfig().getOrDefault("grindstone_disenchant", false))
            return;

        if (stack.isOf(Items.BOOK)) {
            cir.setReturnValue(true);
        }
    }
}
