package com.adibarra.enchanttweaker.mixin.client.tweak;

import java.util.concurrent.ThreadLocalRandom;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADShiny;

/**
 * @description show max-level enchantments with a charged yellow name
 * @note uses the client's mod config for max enchantment levels
 * @environment client
 */
@Environment(EnvType.CLIENT)
@Mixin(
    value = Enchantment.class)
public abstract class ShinyNameMixin {

    @Shadow
    public abstract int getMaxLevel();

    @Shadow
    public abstract boolean isCurse();

    @Inject(
        method = "getFullname(I)Lnet/minecraft/network/chat/Component;",
        at = @At("TAIL"))
    private void getName(int level, CallbackInfoReturnable<Component> cir, @Local MutableComponent mutableText) {
        if (!ETMixinPlugin.getMixinConfig("ShinyNameMixin"))
            return;
        ADShiny.applyNameStyle(mutableText, level, this.getMaxLevel(), this.isCurse(),
            ThreadLocalRandom.current().nextFloat());
    }
}
