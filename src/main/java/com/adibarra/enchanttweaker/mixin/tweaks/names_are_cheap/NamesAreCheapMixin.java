package com.adibarra.enchanttweaker.mixin.tweaks.names_are_cheap;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AnvilScreenHandler.class, priority=1543)
public abstract class NamesAreCheapMixin extends ForgingScreenHandler {

	@Final
	@Shadow
	private Property levelCost;

	public NamesAreCheapMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
		super(type, syncId, playerInventory, context);
	}

	/**
	 * @author adibarra
	 * @reason Renaming items in anvils always costs one level
	 */
	@Inject(method="updateResult", at=@At("TAIL"))
	private void enchanttweaker$namesAreCheap(CallbackInfo ci) {
		boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("names_are_cheap", true);

		if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
			if(this.input.getStack(1).isEmpty()) {
				levelCost.set(1);
			}
		}
	}
}