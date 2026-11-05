package com.adibarra.enchanttweaker.mixin.server.anvil;

import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description customize the chance of an anvil breaking when used
 * @environment server
 */
@Mixin(
    value = AnvilMenu.class)
public abstract class SturdyAnvilsMixin {

    @ModifyConstant(
        method = "lambda$onTake$0(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
        constant = @Constant(
            floatValue = 0.12f,
            ordinal = 0))
    private static float enchanttweaker$sturdyAnvils$modifyDamageChance(float orig) {
        if (!ETMixinPlugin.getMixinConfig("SturdyAnvilsMixin"))
            return orig;
        float anvilDamageChance = ETMixinPlugin.getConfig().getOrDefault("anvil_damage_chance", orig);
        return Math.clamp(anvilDamageChance, 0f, 1f);
    }
}
