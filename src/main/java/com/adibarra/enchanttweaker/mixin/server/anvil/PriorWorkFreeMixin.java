package com.adibarra.enchanttweaker.mixin.server.anvil;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description enchanting/repairing cost does not increase with prior work
 * @environment server
 */
@Mixin(
    value = AnvilMenu.class)
public abstract class PriorWorkFreeMixin {

    @ModifyConstant(
        method = "createResult()V",
        constant = @Constant(
            longValue = 0L,
            ordinal = 0),
        require = 1)
    private long enchanttweaker$priorWorkFree$removeRepairCostPenalty(long cost) {
        if (!ETMixinPlugin.getMixinConfig("PriorWorkFreeMixin"))
            return cost;
        AnvilMenu self = (AnvilMenu) (Object) this;
        return -(long) self.getSlot(AnvilMenu.INPUT_SLOT).getItem().getOrDefault(DataComponents.REPAIR_COST, 0)
            - self.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem().getOrDefault(DataComponents.REPAIR_COST, 0);
    }
}
