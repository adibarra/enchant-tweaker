package com.adibarra.enchanttweaker;

import com.adibarra.utils.ADConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

public final class ETMixinPlugin implements IMixinConfigPlugin {

    private static int numMixins = 0;
    private static boolean MOD_ENABLED = false;
    private static ADConfig CONFIG;
    private static final Logger LOGGER = LoggerFactory.getLogger(EnchantTweaker.MOD_NAME);
    private static final Map<String, String> KEYS = new HashMap<>();
    private static final Map<String, CompatEntry> COMPAT = new HashMap<>();

    private record CompatEntry(boolean shouldApply, String reason, BooleanSupplier condition, Runnable callback) { }

    static {
        KEYS.put("CheapNamesMixin",           "cheap_names");
        KEYS.put("NotTooExpensiveMixin",      "not_too_expensive");
        KEYS.put("PriorWorkCheaperMixin",     "prior_work_cheaper");
        KEYS.put("PriorWorkFreeMixin",        "prior_work_free");
        KEYS.put("SturdyAnvilsMixin",         "sturdy_anvils");

        KEYS.put("MoreBindingMixin",          "more_binding");
        KEYS.put("MoreChannelingMixin",       "more_channeling");
        KEYS.put("MoreFlameMixin",            "more_flame");
        KEYS.put("MoreInfinityMixin",         "more_infinity");
        KEYS.put("MoreMendingMixin",          "more_mending");
        KEYS.put("MoreMultishotMixin",        "more_multishot");

        KEYS.put("AxesNotToolsMixin",         "axes_not_tools");
        KEYS.put("AxeWeaponsMixin",           "axe_weapons");
        KEYS.put("BetterMendingMixin",        "better_mending");
        KEYS.put("BowInfinityFixMixin",       "bow_infinity_fix");
        KEYS.put("GodArmorMixin",             "god_armor");
        KEYS.put("GodWeaponsMixin",           "god_weapons");
        KEYS.put("InfiniteMendingMixin",      "infinite_mending");
        KEYS.put("LoyalVoidTridentsMixin",    "loyal_void_tridents");
        KEYS.put("MultishotPiercingMixin",    "multishot_piercing");
        KEYS.put("NoSoulSpeedBacklashMixin",  "no_soul_speed_backlash");
        KEYS.put("NoThornsBacklashMixin",     "no_thorns_backlash");
        KEYS.put("ShinyNameMixin",            "shiny_name");
        KEYS.put("TridentWeaponsMixin",       "trident_weapons");

        KEYS.put("DamageEnchantMixin",        "capmod_enabled");
        KEYS.put("GenericEnchantMixin",       "capmod_enabled");
        KEYS.put("LuckEnchantMixin",          "capmod_enabled");
        KEYS.put("ProtectionEnchantMixin",    "capmod_enabled");
        KEYS.put("SpecialEnchantMixin",       "capmod_enabled");

        COMPAT.put(
            "NotTooExpensiveMixin",
            new CompatEntry(
                false,
                "Mod 'Fabrication' detected",
                () -> FabricLoader.getInstance().isModLoaded("fabrication"),
                () -> CONFIG.set(getMixinKey("NotTooExpensiveMixin"), Boolean.FALSE.toString()))
        );

        COMPAT.put(
            "BowInfinityFixMixin",
            new CompatEntry(
                false,
                "MoreInfinityMixin is enabled and takes precedence",
                () -> getMixinConfig("MoreInfinityMixin"),
                () -> CONFIG.set(getMixinKey("BowInfinityFixMixin"), Boolean.FALSE.toString()))
        );
    }

    @Override
    public void onLoad(String mixinPackage) {
        reloadConfig();
        MOD_ENABLED = CONFIG.getOrDefault("mod_enabled", false);
        LOGGER.info(EnchantTweaker.PREFIX + "Mod {}",
            MOD_ENABLED ? "enabled! Enabling mixins..." : "disabled! No mixins will be applied.");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!MOD_ENABLED) return false;

        String mixinName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        CompatEntry compatEntry = COMPAT.get(mixinName);

        // if there is a compat entry, and it activates, override config
        if (compatEntry != null && compatEntry.condition().getAsBoolean()) {
            String state = compatEntry.shouldApply() ? "enabled" : "disabled";
            LOGGER.info(EnchantTweaker.PREFIX + "[COMPAT] {} {}. Reason: {}", mixinName, state, compatEntry.reason());
            compatEntry.callback().run();
            return compatEntry.shouldApply();
        }

        // assume no compatibility entries, use config
        return getMixinConfig(mixinName);
    }

    public static void reloadConfig() {
        if (CONFIG != null) LOGGER.info(EnchantTweaker.PREFIX + "Reloading config...");
        CONFIG = new ADConfig(EnchantTweaker.MOD_NAME,
            "assets/" + EnchantTweaker.MOD_ID + "/enchant-tweaker.properties");
    }

    public static String getMixinKey(String mixinName) {
        String key = KEYS.getOrDefault(mixinName, null);
        if (key == null) LOGGER.error(EnchantTweaker.PREFIX + "Unknown mixin name: {}", mixinName);
        return key;
    }

    public static boolean getMixinConfig(String mixinName) {
        return CONFIG.getOrDefault(getMixinKey(mixinName), false);
    }

    public static int getNumMixins() {
        return numMixins;
    }

    public static ADConfig getConfig() {
        return CONFIG;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        numMixins++;
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
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
