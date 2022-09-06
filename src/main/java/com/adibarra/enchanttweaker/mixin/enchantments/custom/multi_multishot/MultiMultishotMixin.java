package com.adibarra.enchanttweaker.mixin.enchantments.custom.multi_multishot;

import com.adibarra.enchanttweaker.EnchantTweaker;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static net.minecraft.item.CrossbowItem.*;

@Mixin(value=CrossbowItem.class, priority=1543)
public abstract class MultiMultishotMixin {

    private static int enchanttweaker$multishotLevel;

    @Inject(method="loadProjectiles(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)Z", at=@At("HEAD"))
    private static void enchanttweaker$captureLocalsProjectile(LivingEntity shooter, ItemStack projectile, CallbackInfoReturnable<Boolean> cir) {
        enchanttweaker$multishotLevel = EnchantmentHelper.getLevel(Enchantments.MULTISHOT, projectile);
    }

    /**
     * @author adibarra
     * @reason Allow the multishot enchantment to scale properly
     */
    @ModifyConstant(method="loadProjectiles(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)Z", constant=@Constant(intValue=3))
    private static int enchanttweaker$soulSpeedDoesntDamageArmor(int original) {
        boolean tweakEnabled = EnchantTweaker.getConfig().getOrDefault("multi_multishot", true);

        if(EnchantTweaker.MOD_ENABLED && tweakEnabled) {
            return enchanttweaker$multishotLevel * 2 + 1;
        }
        return original;
    }

    //TODO: Find alternative for this yucky overwrite
    /**
     * @author adibarra
     * @reason Allow the multishot enchantment to shoot properly when scaled
     */
    @Overwrite
    public static void shootAll(World world, LivingEntity entity, Hand hand, ItemStack stack, float speed, float divergence) {
        List<ItemStack> list = getProjectiles(stack);
        float[] fs = getSoundPitches(entity.getRandom());

        float range = Math.max(10.0F, list.size()*0.2F);

        for(int i = 0; i < list.size(); ++i) {
            ItemStack itemStack = list.get(i);
            boolean bl = entity instanceof PlayerEntity && ((PlayerEntity)entity).getAbilities().creativeMode;
            if (!itemStack.isEmpty()) {

                if (i == 0) {
                    shoot(world, entity, hand, stack, itemStack, fs[i], bl, speed, divergence, 0.0F);
                } else if (i % 2 != 0) {
                    shoot(world, entity, hand, stack, itemStack, fs[1], bl, speed, divergence, -range * (i/(float)list.size()/2F));
                } else {
                    shoot(world, entity, hand, stack, itemStack, fs[2], bl, speed, divergence, range * (i/(float)list.size()/2F));
                }
            }
        }

        postShoot(world, entity, stack);
    }
}