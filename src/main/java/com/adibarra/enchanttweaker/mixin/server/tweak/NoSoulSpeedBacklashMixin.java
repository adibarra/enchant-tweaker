package com.adibarra.enchanttweaker.mixin.server.tweak;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.effects.ChangeItemDamage;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADUtils;

/**
 * @description disable soul speed enchant armor self-damage backlash
 * @environment server
 */
@Mixin(
    value = ChangeItemDamage.class)
public abstract class NoSoulSpeedBacklashMixin {

    @Inject(
        method = "apply(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void enchanttweaker$noSoulSpeedBacklash$noBacklash(ServerLevel level, int enchantmentLevel,
        EnchantedItemInUse item, Entity entity, Vec3 pos, CallbackInfo ci) {
        if (!ETMixinPlugin.getMixinConfig("NoSoulSpeedBacklashMixin"))
            return;
        if (item.inSlot() != EquipmentSlot.FEET)
            return;
        ChangeItemDamage effect = (ChangeItemDamage) (Object) this;
        if (effect.amount().calculate(enchantmentLevel) != 1.0f)
            return;
        if (ADUtils.getEnchantmentLevel(item.itemStack(), Enchantments.SOUL_SPEED) <= 0)
            return;
        ci.cancel();
    }
}
