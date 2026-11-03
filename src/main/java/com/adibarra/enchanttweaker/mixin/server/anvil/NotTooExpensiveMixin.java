package com.adibarra.enchanttweaker.mixin.server.anvil;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.adibarra.enchanttweaker.AnvilCost;

/**
 * @description anvils no longer say "Too Expensive!"
 * @environment server
 */
@Mixin(
    value = AnvilMenu.class)
public abstract class NotTooExpensiveMixin {

    @ModifyConstant(
        method = "createResult()V",
        constant = @Constant(
            ordinal = 2,
            intValue = 40))
    private int enchanttweaker$notTooExpensive$modifyTooExpensive(int orig) {
        return AnvilCost.tooExpensiveThreshold(orig);
    }
}
