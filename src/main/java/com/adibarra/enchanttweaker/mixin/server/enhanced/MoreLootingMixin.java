package com.adibarra.enchanttweaker.mixin.server.enhanced;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.adibarra.enchanttweaker.ETMixinPlugin;

/**
 * @description scales experience drops from kills using looting
 * @environment server
 */
@Mixin(
    value = LivingEntity.class)
public abstract class MoreLootingMixin {

    @Shadow
    protected Player lastHurtByPlayer;

    @ModifyArg(
        method = "dropExperience()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
        index = 2)
    private int enchanttweaker$moreLooting$modifyXp(int xp) {
        if (!ETMixinPlugin.getMixinConfig("MoreLootingMixin"))
            return xp;
        if (lastHurtByPlayer == null)
            return xp;
        int lootingLevel = EnchantmentHelper.getMobLooting(lastHurtByPlayer);
        if (lootingLevel <= 0)
            return xp;
        double multiplier = Math.max(0.0, ETMixinPlugin.getConfig().getOrDefault("more_looting_multiplier", 0.5));
        return (int) (xp * (1.0 + lootingLevel * multiplier));
    }
}
