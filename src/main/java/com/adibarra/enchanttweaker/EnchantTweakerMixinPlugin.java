package com.adibarra.enchanttweaker;

import com.adibarra.enchanttweaker.utils.Utils;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.include.com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnchantTweakerMixinPlugin implements IMixinConfigPlugin {

    private static int num_mixins = 0;

    private static final Map<String, Utils.Conflict> CONFLICTS = ImmutableMap.of(
        "CrossbowInfinityFixMixin", new Utils.Conflict(() -> true, "WIP Beta")
    );

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        num_mixins++;
        return !CONFLICTS.getOrDefault(
            mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1),
            new Utils.Conflict(() -> false, "No conflict")
        ).condition().getAsBoolean();
    }

    public static int getNumMixins() {
        return num_mixins;
    }

    public static Map<String, Utils.Conflict> getConflicts() {
        return CONFLICTS;
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