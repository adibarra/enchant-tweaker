package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description scales experience drops from kills using looting
 * @environment server
 */
@Mixin(
    value = LivingEntity.class)
public abstract class MoreLootingMixin {

    @ModifyArg(
        method = "dropExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
        index = 2)
    private int enchanttweaker$moreLooting$modifyXp(int xp, @Local(
        argsOnly = true,
        ordinal = 0) Entity killer) {
        if (!ETMixinPlugin.getMixinConfig("MoreLootingMixin"))
            return xp;
        if (!(killer instanceof Player player))
            return xp;
        int lootingLevel = ADUtils.getEnchantmentLevel(player.getMainHandItem(), Enchantments.LOOTING);
        if (lootingLevel <= 0)
            return xp;
        double multiplier = Math.max(0.0, ETMixinPlugin.getConfig().getOrDefault("more_looting_multiplier", 0.5));
        return (int) (xp * (1.0 + lootingLevel * multiplier));
    }
}
