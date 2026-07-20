package com.adibarra.enchanttweaker.commands.suggestions;

import com.adibarra.enchanttweaker.ETConfigSchema;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** tab-completion for the `/et config ` value argument */
public final class ValueSuggestion {

    private ValueSuggestion() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    public static final SuggestionProvider<ServerCommandSource> PROVIDER =
        (CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) -> {
            String key = StringArgumentType.getString(context, "key").toLowerCase();
            DynamicRegistryManager registryManager = context.getSource().getRegistryManager();
            return buildSuggestions(builder, key, candidatesFor(key, registryManager));
        };

    /** the raw candidate values for a key's value argument, independent of any typed prefix */
    public static List<String> candidatesFor(String key, DynamicRegistryManager registryManager) {
        if (key == null) return List.of();
        key = key.toLowerCase();
        if (ETConfigSchema.isReserved(key)) return List.of();

        ETConfigSchema.ValueType type = ETConfigSchema.typeOf(key);
        if (type == null) return List.of();

        return switch (type) {
            case BOOLEAN -> List.of("true", "false");
            case INTEGER, DECIMAL -> currentAndDefault(key);
            case LIST -> switch (key) {
                case "disable_enchantments", "enchant_trade_no_restock" -> enchantmentPaths();
                case "protection_bypass_types" -> damageTypePaths(registryManager);
                default -> currentAndDefault(key);
            };
        };
    }

    private static List<String> currentAndDefault(String key) {
        List<String> out = new ArrayList<>();
        String current = ETMixinPlugin.getConfig().getOrDefault(key, null);
        if (current != null && !current.isEmpty()) out.add(current);
        String def = ETConfigSchema.defaultOf(key);
        if (def != null && !def.isEmpty() && !out.contains(def)) out.add(def);
        return out;
    }

    private static List<String> enchantmentPaths() {
        return Registries.ENCHANTMENT.getIds().stream()
            .map(Identifier::getPath)
            .sorted()
            .toList();
    }

    private static List<String> damageTypePaths(DynamicRegistryManager registryManager) {
        if (registryManager == null) return List.of();
        Registry<DamageType> registry = registryManager.get(RegistryKeys.DAMAGE_TYPE);
        return registry.getIds().stream()
            .map(Identifier::getPath)
            .sorted()
            .toList();
    }

    private static CompletableFuture<Suggestions> buildSuggestions(SuggestionsBuilder builder, String key, List<String> candidates) {
        if (candidates.isEmpty()) return Suggestions.empty();

        String remaining = builder.getRemaining();
        String prefix = "";
        String query = remaining;

        // list keys complete one comma-separated segment at a time: keep everything up to and
        // including the last comma as a fixed prefix and complete only the trailing segment
        if (ETConfigSchema.typeOf(key) == ETConfigSchema.ValueType.LIST) {
            int comma = remaining.lastIndexOf(',');
            if (comma >= 0) {
                prefix = remaining.substring(0, comma + 1);
                query = remaining.substring(comma + 1);
            }
        }

        String lowerQuery = query.toLowerCase();
        for (String candidate : candidates) {
            if (candidate.toLowerCase().startsWith(lowerQuery)) {
                builder.suggest(prefix + candidate);
            }
        }
        return builder.buildFuture();
    }
}
