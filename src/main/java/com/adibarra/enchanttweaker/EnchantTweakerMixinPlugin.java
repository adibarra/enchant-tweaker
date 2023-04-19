package com.adibarra.enchanttweaker;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnchantTweakerMixinPlugin implements IMixinConfigPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger("EnchantTweaker");
    private static final Map<String, Supplier<Boolean>> CONFLICTS = ImmutableMap.of(
        "com.adibarra.enchanttweaker.mixin.enchant.custom.CrossbowInfinityFixMixin", () -> true // TODO: WIP
    );

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (CONFLICTS.getOrDefault(mixinClassName, () -> false).get()) {
            LOGGER.info("[Compat] Disabling Mixin: " + mixinClassName.substring(
                    mixinClassName.lastIndexOf('.') + 1));
            return false;
        }
        return true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}