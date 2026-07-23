package com.adibarra.enchanttweaker.mixin.server.anvil;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description renaming items in anvils costs 1 level
 * @environment server
 */
@Mixin(
    value = AnvilScreenHandler.class)
public abstract class CheapNamesMixin extends ForgingScreenHandler {

    @Shadow
    @Final
    private Property levelCost;

    @SuppressWarnings("unused")
    private CheapNamesMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
        ScreenHandlerContext context) {
        super(type, syncId, playerInventory, context);
    }

    @Inject(
        method = "updateResult()V",
        at = @At("TAIL"))
    private void enchanttweaker$cheapNames$modifyLevelCost(CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("CheapNamesMixin"))
            return;
        if (this.input.getStack(1).isEmpty() && !this.output.getStack(0).isEmpty()) {
            levelCost.set(1);
        }
    }
}
