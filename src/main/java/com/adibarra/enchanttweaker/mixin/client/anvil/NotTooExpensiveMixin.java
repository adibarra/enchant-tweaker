package com.adibarra.enchanttweaker.mixin.client.anvil;

import com.adibarra.enchanttweaker.AnvilCost;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description anvils no longer say "Too Expensive!"
 * @environment Client
 */
@Environment(EnvType.CLIENT)
@Mixin(value=AnvilScreen.class)
public abstract class NotTooExpensiveMixin {

    @ModifyConstant(
        method="drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V",
        constant=@Constant(intValue=40, ordinal=0))
    private int notTooExpensiveClient(int orig) {
        return AnvilCost.tooExpensiveThreshold(orig);
    }
}
