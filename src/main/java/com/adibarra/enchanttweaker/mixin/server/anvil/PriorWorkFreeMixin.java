package com.adibarra.enchanttweaker.mixin.server.anvil;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @description Enchanting/repairing cost does not increase with prior work.
 * @environment Server
 */
@Mixin(value=ItemStack.class, priority=1543)
public abstract class PriorWorkFreeMixin {

    @Inject(
        method="getRepairCost()I",
        at=@At(value="HEAD"),
        cancellable=true)
    private void priorWorkFree(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }
}
