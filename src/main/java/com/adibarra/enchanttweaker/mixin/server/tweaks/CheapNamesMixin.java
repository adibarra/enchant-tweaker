package com.adibarra.enchanttweaker.mixin.server.tweaks;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @description Renaming items in anvils costs 1 level.
 * @environment Server
 */
@Mixin(value=AnvilScreenHandler.class, priority=1543)
public abstract class CheapNamesMixin extends ForgingScreenHandler {

	@Shadow @Final
	private Property levelCost;

	@SuppressWarnings("unused")
	private CheapNamesMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(type, syncId, playerInventory, context);
	}

	@Inject(method="updateResult", at=@At("TAIL"))
	private void freeNames(CallbackInfo ci) {
		if(EnchantTweaker.isEnabled() && EnchantTweaker.getConfig().getOrDefault("cheap_names", true)) {
			if(this.input.getStack(1).isEmpty()) {
				levelCost.set(1);
			}
		}
	}
}