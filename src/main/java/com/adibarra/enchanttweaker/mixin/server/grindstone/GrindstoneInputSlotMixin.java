package com.adibarra.enchanttweaker.mixin.server.grindstone;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** allows plain books in grindstone input slots */
@Mixin(targets = {
    "net.minecraft.screen.GrindstoneScreenHandler$2",
    "net.minecraft.screen.GrindstoneScreenHandler$3"
})
public abstract class GrindstoneInputSlotMixin {

    @Inject(method = "canInsert(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void enchanttweaker$grindstoneDisenchant$canInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!ETMixinPlugin.getConfig().getOrDefault("grindstone_disenchant", false)) return;

        if (stack.isOf(Items.BOOK)) {
            cir.setReturnValue(true);
        }
    }
}
