package com.adibarra.enchanttweaker;

/** Shared anvil cost decisions used by the server handler and client display. */
public final class AnvilCost {

    private AnvilCost() { }

    public static int tooExpensiveThreshold(int vanillaThreshold) {
        if (!ETMixinPlugin.getMixinConfig("NotTooExpensiveMixin")) return vanillaThreshold;
        int configured = ETMixinPlugin.getConfig().getOrDefault("nte_max_cost", vanillaThreshold);
        return Math.clamp(configured, 0, Integer.MAX_VALUE);
    }
}
