package com.adibarra.enchanttweaker.mixin.server.grindstone;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/** allows plain books in grindstone input slots */
@Mixin(
    targets = {"net.minecraft.world.inventory.GrindstoneMenu$2", "net.minecraft.world.inventory.GrindstoneMenu$3"})
public abstract class GrindstoneInputSlotMixin extends Slot {
    protected GrindstoneInputSlotMixin(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (ETMixinPlugin.getConfig().getOrDefault("grindstone_disenchant", false) && stack.is(Items.BOOK))
            return 1;
        return super.getMaxStackSize(stack);
    }

    @Inject(
        method = "canInsert(Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$grindstoneDisenchant$canInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getConfig().getOrDefault("grindstone_disenchant", false))
            return;

        if (stack.is(Items.BOOK)) {
            cir.setReturnValue(true);
        }
    }
}
