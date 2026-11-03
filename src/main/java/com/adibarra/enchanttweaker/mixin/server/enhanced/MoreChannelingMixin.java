package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.WeatherCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description enables channeling during rain at level 2
 * @environment server
 */
@Mixin(
    value = WeatherCheck.class)
public abstract class MoreChannelingMixin {

    @ModifyExpressionValue(
        method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;isThundering()Z"))
    private boolean enchanttweaker$moreChanneling$modifyWeatherCheck(boolean orig, @Local(
        argsOnly = true) LootContext context) {
        if (!ETMixinPlugin.getMixinConfig("MoreChannelingMixin"))
            return orig;
        if (orig)
            return true;
        Integer level = context.getOptionalParameter(LootContextParams.ENCHANTMENT_LEVEL);
        return level != null && level > 1 && context.getLevel().isRaining();
    }
}
