package com.adibarra.enchanttweaker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.adibarra.utils.ADConfig;

public final class ETMixinPlugin implements IMixinConfigPlugin {

    // replaced during config updates, volatile keeps game-thread readers current
    private static volatile int numMixins = 0;
    private static volatile boolean MOD_ENABLED = false;
    private static volatile ADConfig CONFIG;
    private static final Logger LOGGER = LoggerFactory.getLogger(EnchantTweaker.MOD_NAME);
    private static final Map<String, String> KEYS = new HashMap<>();
    private static final Map<String, CompatEntry> COMPAT = new HashMap<>();
    private static final Map<String, Boolean> FEATURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> CAPMOD_CACHE = new ConcurrentHashMap<>();
    // client-only cosmetic keys stay local
    private static final Set<String> CLIENT_LOCAL_KEYS = Set.of("roman_numerals", "shiny_name");
    private static volatile Map<String, String> LOCAL_CONFIG_BACKUP;

    private record CompatEntry(boolean shouldApply, String reason, BooleanSupplier condition, Runnable callback) {
    }

    static {
        // map each mixin's simple class name to its config key
        KEYS.put("CheapNamesMixin", "cheap_names");
        KEYS.put("NotTooExpensiveMixin", "not_too_expensive");
        KEYS.put("PriorWorkCheaperMixin", "prior_work_cheaper");
        KEYS.put("PriorWorkFreeMixin", "prior_work_free");
        KEYS.put("SturdyAnvilsMixin", "sturdy_anvils");

        KEYS.put("MoreBindingMixin", "more_binding");
        KEYS.put("MoreBlastProtectionMixin", "more_blast_protection");
        KEYS.put("MoreChannelingMixin", "more_channeling");
        KEYS.put("MoreFireProtectionMixin", "more_fire_protection");
        KEYS.put("MoreFlameMixin", "more_flame");
        KEYS.put("MoreFlameRangedWeaponMixin", "more_flame");
        KEYS.put("MoreInfinityMixin", "more_infinity");
        KEYS.put("MoreLootingMixin", "more_looting");
        KEYS.put("MoreMendingMixin", "more_mending");
        KEYS.put("MoreMultishotMixin", "more_multishot");
        KEYS.put("MoreProtectionMixin", "more_protection");

        KEYS.put("GrindstoneDisenchantMixin", "grindstone_disenchant");
        KEYS.put("GrindstoneOutputSlotMixin", "grindstone_disenchant");

        KEYS.put("AxesNotToolsMixin", "axes_not_tools");
        KEYS.put("AxeWeaponsMixin", "axe_weapons");
        KEYS.put("BetterMendingMixin", "better_mending");
        KEYS.put("BowInfinityFixMixin", "bow_infinity_fix");
        KEYS.put("BowLootingMixin", "bow_looting");
        KEYS.put("DisableEnchantmentsMixin", "disable_enchantments_enabled");
        KEYS.put("DisableEnchantmentsLootMixin", "disable_enchantments_enabled");
        KEYS.put("GodArmorMixin", "god_armor");
        KEYS.put("GodWeaponsMixin", "god_weapons");
        KEYS.put("InfiniteMendingMixin", "infinite_mending");
        KEYS.put("LoyalVoidTridentsMixin", "loyal_void_tridents");
        KEYS.put("MultishotPiercingMixin", "multishot_piercing");
        KEYS.put("NoMendingUnbreakingMixin", "no_mending_unbreaking");
        KEYS.put("NoSoulSpeedBacklashMixin", "no_soul_speed_backlash");
        KEYS.put("NoThornsBacklashMixin", "no_thorns_backlash");
        KEYS.put("ProtectionBypassMixin", "protection_bypass_enabled");
        KEYS.put("RomanNumeralMixin", "roman_numerals");
        KEYS.put("ShinyNameMixin", "shiny_name");
        KEYS.put("TridentWeaponsMixin", "trident_weapons");
        KEYS.put("VillagerTradeLimitsMixin", "villager_trade_limits");
        KEYS.put("XpScalingMixin", "xp_scaling");

        KEYS.put("CapmodMixin", "capmod_enabled");

        // apply compatibility overrides without changing saved config
        COMPAT.put("AxesNotToolsMixin",
            new CompatEntry(false, "Mod 'AxesAreWeapons' detected",
                () -> FabricLoader.getInstance().isModLoaded("axesareweapons"),
                () -> CONFIG.setAll(Map.of(getMixinKey("AxesNotToolsMixin"), Boolean.FALSE.toString()))));

        COMPAT.put("NotTooExpensiveMixin",
            new CompatEntry(false, "Mod 'Fabrication' detected",
                () -> FabricLoader.getInstance().isModLoaded("fabrication"),
                () -> CONFIG.setAll(Map.of(getMixinKey("NotTooExpensiveMixin"), Boolean.FALSE.toString()))));
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
        if (!MOD_ENABLED)
            return false;

        String mixinName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        CompatEntry compatEntry = COMPAT.get(mixinName);

        if (compatEntry != null && compatEntry.condition().getAsBoolean()) {
            String state = compatEntry.shouldApply() ? "enabled" : "disabled";
            LOGGER.info(EnchantTweaker.PREFIX + "[COMPAT] {} {}. Reason: {}", mixinName, state, compatEntry.reason());
            compatEntry.callback().run();
            return compatEntry.shouldApply();
        }

        return true;
    }

    public static synchronized void reloadConfig() {
        if (CONFIG != null)
            LOGGER.info(EnchantTweaker.PREFIX + "Reloading config...");

        String internalDefaultConfigPath = "assets/" + EnchantTweaker.MOD_ID + "/enchant-tweaker.properties";
        ADConfig reloaded = new ADConfig(EnchantTweaker.MOD_NAME, "enchant-tweaker.properties",
            internalDefaultConfigPath);
        if (reloaded.isLoaded() || CONFIG == null) {
            CONFIG = reloaded;
        } else {
            LOGGER.warn(EnchantTweaker.PREFIX + "Config reload failed; retaining the active configuration.");
        }
        FEATURE_CACHE.clear();
        CAPMOD_CACHE.clear();
    }

    public static String getMixinKey(String mixinName) {
        String key = KEYS.getOrDefault(mixinName, null);
        if (key == null)
            LOGGER.error(EnchantTweaker.PREFIX + "Unknown mixin name: {}", mixinName);
        return key;
    }

    public static synchronized boolean getMixinConfig(String mixinName) {
        if (CONFIG == null || mixinName == null)
            return false;
        return FEATURE_CACHE.computeIfAbsent(mixinName, k -> {
            if (!CONFIG.getOrDefault("mod_enabled", false))
                return false;
            // reject mixins without config keys
            String key = getMixinKey(k);
            if (key == null)
                return false;
            return CONFIG.getOrDefault(key, false);
        });
    }

    public static synchronized int getCapmodLevel(String enchantmentPath, int vanillaLevel) {
        if (enchantmentPath == null || CONFIG == null)
            return vanillaLevel;
        int cap = CAPMOD_CACHE.computeIfAbsent(enchantmentPath, key -> CONFIG.getOrDefault(key, -1));
        if (cap < 0)
            return vanillaLevel;
        return Math.clamp(cap, 0, 255);
    }

    public static int getNumMixins() {
        return numMixins;
    }

    /** returns a copy of the mixin name to config key map */
    public static Map<String, String> getMixinKeys() {
        return Map.copyOf(KEYS);
    }

    /** returns client-local config keys excluded from server sync */
    public static Set<String> getClientLocalKeys() {
        return Set.copyOf(CLIENT_LOCAL_KEYS);
    }

    /** returns config keys currently overridden by a conflicting mod */
    public static Map<String, String> getActiveCompatOverrides() {
        Map<String, String> active = new HashMap<>();
        for (Map.Entry<String, CompatEntry> entry : COMPAT.entrySet()) {
            CompatEntry compat = entry.getValue();
            if (compat.condition().getAsBoolean()) {
                String key = KEYS.get(entry.getKey());
                if (key != null)
                    active.put(key, compat.reason());
            }
        }
        return active;
    }

    public static ADConfig getConfig() {
        return CONFIG;
    }

    public static synchronized void clearCaches() {
        FEATURE_CACHE.clear();
        CAPMOD_CACHE.clear();
    }

    public static Map<String, String> getConfigMap() {
        Map<String, String> map = new HashMap<>();
        if (CONFIG != null) {
            for (Map.Entry<String, String> entry : CONFIG.getEntries()) {
                // preserve client-only visual settings during server sync
                if (CLIENT_LOCAL_KEYS.contains(entry.getKey()))
                    continue;
                map.put(entry.getKey(), entry.getValue());
            }
        }
        // clear previously synced state when the server disables
        map.put("mod_enabled", Boolean.toString(CONFIG != null && CONFIG.getOrDefault("mod_enabled", false)));
        return map;
    }

    public static synchronized void captureLocalConfig() {
        if (CONFIG == null)
            return;
        Map<String, String> snapshot = new HashMap<>();
        for (Map.Entry<String, String> entry : CONFIG.getEntries()) {
            snapshot.put(entry.getKey(), entry.getValue());
        }
        LOCAL_CONFIG_BACKUP = Map.copyOf(snapshot);
    }

    public static synchronized void syncConfigFrom(Map<String, String> data) {
        if (CONFIG == null || data == null)
            return;
        if (LOCAL_CONFIG_BACKUP == null)
            captureLocalConfig();
        Map<String, String> synced = new HashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!CLIENT_LOCAL_KEYS.contains(entry.getKey()))
                synced.put(entry.getKey(), entry.getValue());
        }
        CONFIG.setAll(synced);
        FEATURE_CACHE.clear();
        CAPMOD_CACHE.clear();
    }

    public static synchronized void restoreLocalConfig() {
        if (CONFIG != null && LOCAL_CONFIG_BACKUP != null)
            CONFIG.setAll(LOCAL_CONFIG_BACKUP);
        LOCAL_CONFIG_BACKUP = null;
        reloadConfig();
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
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
