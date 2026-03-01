package com.adibarra.enchanttweaker.mixin.server.enhanced;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description Scales the Binding Curse enchantment to have a chance of not dropping the item on death.
 * @environment Server
 */
@Mixin(value=PlayerInventory.class)
public abstract class MoreBindingMixin {

    @Unique
    private static final Random RAND = new Random();

    @Unique
    private static final Map<UUID, Map<Integer, ItemStack>> BOUND_ARMOR = new ConcurrentHashMap<>();
    static {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            Map<Integer, ItemStack> armorMap = BOUND_ARMOR.remove(oldPlayer.getUuid());
            if (armorMap == null) return;
            for (Map.Entry<Integer, ItemStack> entry : armorMap.entrySet()) {
                newPlayer.getInventory().armor.set(entry.getKey(), entry.getValue());
            }
        });
    }

    @Shadow @Final
    public DefaultedList<ItemStack> armor;

    @Shadow @Final
    public PlayerEntity player;

    @ModifyExpressionValue(
        method="dropAll()V",
        at=@At(
            ordinal=0,
            value="INVOKE",
            target="Lnet/minecraft/item/ItemStack;isEmpty()Z"))
        private boolean enchanttweaker$moreBinding$modifyDropAll(boolean orig, @Local ItemStack stack) {
        if (!ETMixinPlugin.getMixinConfig("MoreBindingMixin")) return orig;
        if (orig) return true;
        if (!armor.contains(stack)) return false;

        int bindingLevel = EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, stack);
        if (RAND.nextFloat() > Math.clamp(1.1 - 0.1 * bindingLevel, 0.1, 1.0)) {
            BOUND_ARMOR.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
                       .put(armor.indexOf(stack), stack.copy());
            return true;
        }
        return false;
    }
}
