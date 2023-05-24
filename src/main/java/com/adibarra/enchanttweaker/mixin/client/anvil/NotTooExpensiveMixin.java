package com.adibarra.enchanttweaker.mixin.client.anvil;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADMath;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * @description Anvils no longer say "Too Expensive!"
 * @environment Client
 */
@Environment(EnvType.CLIENT)
@Mixin(value=AnvilScreen.class)
public abstract class NotTooExpensiveMixin {

    @ModifyConstant(
        method="drawForeground(Lnet/minecraft/client/util/math/MatrixStack;II)V",
        constant=@Constant(intValue=40))
    private int notTooExpensiveClient(int orig) {
        int maxCost = ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", orig);
        return ADMath.clamp(maxCost, 0, Integer.MAX_VALUE);
    }
}
