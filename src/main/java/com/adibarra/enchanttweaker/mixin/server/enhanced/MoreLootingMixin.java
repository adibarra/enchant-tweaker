package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * @description Scales XP drops from mob kills based on the killer's Looting level.
 * @environment Server
 */
@Mixin(value=LivingEntity.class)
public abstract class MoreLootingMixin {

    @Shadow
    protected PlayerEntity attackingPlayer;

    @ModifyArg(
        method="dropXp()V",
        at=@At(
            value="INVOKE",
            target="Lnet/minecraft/entity/ExperienceOrbEntity;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/Vec3d;I)V"),
        index=2)
    private int enchanttweaker$moreLooting$modifyXp(int xp) {
        if (!ETMixinPlugin.getMixinConfig("MoreLootingMixin")) return xp;
        if (attackingPlayer == null) return xp;
        int lootingLevel = EnchantmentHelper.getLooting(attackingPlayer);
        if (lootingLevel <= 0) return xp;
        double multiplier = ETMixinPlugin.getConfig().getOrDefault("more_looting_multiplier", 0.5);
        return (int)(xp * (1.0 + lootingLevel * multiplier));
    }
}
