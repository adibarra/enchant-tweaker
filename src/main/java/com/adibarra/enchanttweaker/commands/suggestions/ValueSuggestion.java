package com.adibarra.enchanttweaker.commands.suggestions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.enchantment.Enchantment;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;

/** tab-completion for the `/et config ` value argument */
public final class ValueSuggestion {

    private ValueSuggestion() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    public static final SuggestionProvider<CommandSourceStack> PROVIDER = (CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder) -> {
        String key = StringArgumentType.getString(context, "key").toLowerCase(Locale.ROOT);
        RegistryAccess registryManager = context.getSource().registryAccess();
        return buildSuggestions(builder, key, candidatesFor(key, registryManager));
    };

    /**
     * returns raw candidates independent of the typed prefix
     */
    public static List<String> candidatesFor(String key, RegistryAccess registryManager) {
        if (key == null)
            return List.of();
        key = key.toLowerCase(Locale.ROOT);
        if (ETConfigSchema.isReserved(key))
            return List.of();

        ETConfigSchema.ValueType type = ETConfigSchema.typeOf(key);
        if (type == null)
            return List.of();

        return switch (type) {
            case BOOLEAN -> List.of("true", "false");
            case INTEGER, DECIMAL -> currentAndDefault(key);
            case LIST -> switch (key) {
                case "disable_enchantments", "enchant_trade_no_restock" -> enchantmentPaths(registryManager);
                case "protection_bypass_types" -> damageTypePaths(registryManager);
                default -> currentAndDefault(key);
            };
        };
    }

    private static List<String> currentAndDefault(String key) {
        List<String> out = new ArrayList<>();
        String current = ETMixinPlugin.getConfig().getOrDefault(key, null);
        if (current != null && !current.isEmpty())
            out.add(current);
        String def = ETConfigSchema.defaultOf(key);
        if (def != null && !def.isEmpty() && !out.contains(def))
            out.add(def);
        return out;
    }

    private static List<String> enchantmentPaths(RegistryAccess registryManager) {
        if (registryManager == null)
            return List.of();
        Registry<Enchantment> registry = registryManager.registryOrThrow(Registries.ENCHANTMENT);
        return registry.keySet().stream()
            .map(id -> ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace()) ? id.getPath() : id.toString())
            .sorted().toList();
    }

    private static List<String> damageTypePaths(RegistryAccess registryManager) {
        if (registryManager == null)
            return List.of();
        Registry<DamageType> registry = registryManager.registryOrThrow(Registries.DAMAGE_TYPE);
        return registry.keySet().stream()
            .map(id -> ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace()) ? id.getPath() : id.toString())
            .sorted().toList();
    }

    private static CompletableFuture<Suggestions> buildSuggestions(SuggestionsBuilder builder, String key,
        List<String> candidates) {
        if (candidates.isEmpty())
            return Suggestions.empty();

        String remaining = builder.getRemaining();
        String prefix = "";
        String query = remaining;

        // complete only the trailing comma-separated segment
        if (ETConfigSchema.typeOf(key) == ETConfigSchema.ValueType.LIST) {
            int comma = remaining.lastIndexOf(',');
            if (comma >= 0) {
                int segmentStart = comma + 1;
                while (segmentStart < remaining.length() && Character.isWhitespace(remaining.charAt(segmentStart)))
                    segmentStart++;
                prefix = remaining.substring(0, segmentStart);
                query = remaining.substring(segmentStart);
            }
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerQuery)) {
                builder.suggest(prefix + candidate);
            }
        }
        return builder.buildFuture();
    }
}
