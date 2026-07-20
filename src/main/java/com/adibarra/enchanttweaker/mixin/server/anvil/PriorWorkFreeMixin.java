package com.adibarra.enchanttweaker.mixin.server.anvil;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description enchanting/repairing cost does not increase with prior work
 * @environment Server
 */
@Mixin(
    value = AnvilScreenHandler.class)
public abstract class PriorWorkFreeMixin {

    @Shadow
    @Final
    private Property levelCost;

    @Inject(
        method = "updateResult()V",
        at = @At("RETURN"))
    private void enchanttweaker$priorWorkFree$removeRepairCostPenalty(CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("PriorWorkFreeMixin"))
            return;
        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;
        // preserve CheapNames' one-level rename cost when both mixins run
        if (ETMixinPlugin.getMixinConfig("CheapNamesMixin")
            && self.getSlot(AnvilScreenHandler.INPUT_2_ID).getStack().isEmpty()) {
            return;
        }
        int penalty = self.getSlot(AnvilScreenHandler.INPUT_1_ID).getStack()
            .getOrDefault(DataComponentTypes.REPAIR_COST, 0)
            + self.getSlot(AnvilScreenHandler.INPUT_2_ID).getStack().getOrDefault(DataComponentTypes.REPAIR_COST, 0);
        if (penalty <= 0)
            return;
        levelCost.set(Math.max(0, levelCost.get() - penalty));
    }
}
