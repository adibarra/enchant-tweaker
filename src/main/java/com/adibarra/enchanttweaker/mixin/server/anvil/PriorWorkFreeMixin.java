package com.adibarra.enchanttweaker.mixin.server.anvil;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.screen.AnvilScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description enchanting/repairing cost does not increase with prior work
 * @environment server
 */
@Mixin(
    value = AnvilScreenHandler.class)
public abstract class PriorWorkFreeMixin {

    @ModifyConstant(
        method = "updateResult()V",
        constant = @Constant(
            longValue = 0L,
            ordinal = 0),
        require = 1)
    private long enchanttweaker$priorWorkFree$removeRepairCostPenalty(long cost) {
        if (!ETMixinPlugin.getMixinConfig("PriorWorkFreeMixin"))
            return cost;
        AnvilScreenHandler self = (AnvilScreenHandler) (Object) this;
        return -(long) self.getSlot(AnvilScreenHandler.INPUT_1_ID).getStack()
            .getOrDefault(DataComponentTypes.REPAIR_COST, 0)
            - self.getSlot(AnvilScreenHandler.INPUT_2_ID).getStack().getOrDefault(DataComponentTypes.REPAIR_COST, 0);
    }
}
