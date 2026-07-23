package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.adibarra.enchanttweaker.AnvilRepairHandler;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADConfig;
import com.adibarra.utils.ADText;

/** reports active settings, conflicts, and config details */
public class DiagnoseCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        ADConfig config = ETMixinPlugin.getConfig();
        List<Text> msg = new ArrayList<>();

        boolean modEnabledNow = config.getOrDefault("mod_enabled", false);
        Map<String, String> activeCompat = ETMixinPlugin.getActiveCompatOverrides();

        header(msg, "Mod Status");
        row(msg, "mod enabled", ADText.colorValue(String.valueOf(modEnabledNow)));
        row(msg, "applied mixins",
            Text.literal(String.valueOf(ETMixinPlugin.getNumMixins())).formatted(Formatting.AQUA));

        header(msg, "Desynced Features");
        Set<String> seen = new HashSet<>();
        boolean anyDesynced = false;
        for (Map.Entry<String, String> entry : ETMixinPlugin.getMixinKeys().entrySet()) {
            String mixinName = entry.getKey();
            String configKey = entry.getValue();
            if (!seen.add(configKey))
                continue; // shared keys (e.g. grindstone) reported once
            boolean want = config.getOrDefault(configKey, false);
            boolean applied = ETMixinPlugin.getMixinConfig(mixinName);
            if (want != applied) {
                anyDesynced = true;
                String reason;
                if (!want)
                    reason = "feature remains active while configured false";
                else if (!modEnabledNow)
                    reason = "mod_enabled is currently false";
                else if (activeCompat.containsKey(configKey))
                    reason = "COMPAT override: " + activeCompat.get(configKey);
                else
                    reason = "unknown";
                msg.add(Text.literal("\n  "));
                msg.add(Text.literal(configKey).formatted(Formatting.RED));
                msg.add(Text.literal(" - " + reason).formatted(Formatting.GRAY));
            }
        }
        if (!anyDesynced)
            msg.add(Text.literal("\n  (none)").formatted(Formatting.DARK_GRAY));

        header(msg, "Active COMPAT Overrides");
        if (activeCompat.isEmpty()) {
            msg.add(Text.literal("\n  (none)").formatted(Formatting.DARK_GRAY));
        } else {
            for (Map.Entry<String, String> entry : activeCompat.entrySet()) {
                msg.add(Text.literal("\n  "));
                msg.add(Text.literal(entry.getKey()).formatted(Formatting.GOLD));
                msg.add(Text.literal(" -> " + entry.getValue()).formatted(Formatting.GRAY));
            }
        }

        header(msg, "Anvil Repair");
        row(msg, "anvil_repair", ADText.colorValue(String.valueOf(config.getOrDefault("anvil_repair", false))));
        int repairCost = config.getOrDefault("anvil_repair_ingot_cost", 9);
        String costNote;
        if (repairCost <= 0) {
            costNote = " (repair disabled)";
        } else if (repairCost % 9 == 0) {
            int blocks = repairCost / 9;
            costNote = " (payable with " + blocks + " iron block" + (blocks == 1 ? "" : "s") + ")";
        } else {
            costNote = " (not block-payable)";
        }
        row(msg, "cost", Text.literal(repairCost + " ingots" + costNote).formatted(Formatting.BLUE));
        row(msg, "handler registered", ADText.colorValue(String.valueOf(AnvilRepairHandler.isRegistered())));

        header(msg, "Config File");
        row(msg, "path", Text.literal(config.getConfigPath()).formatted(Formatting.AQUA));
        row(msg, "keys", Text.literal(String.valueOf(config.getKeys().size())).formatted(Formatting.AQUA));
        row(msg, "config_version", ADText.colorValue(config.getOrDefault("config_version", "?")));

        CommandFeedback.feedback(context.getSource(), msg);
        return Command.SINGLE_SUCCESS;
    }

    private static void header(List<Text> msg, String title) {
        msg.add(
            Text.literal("\n").append(Text.literal("== " + title + " ==").formatted(Formatting.AQUA, Formatting.BOLD)));
    }

    private static void row(List<Text> msg, String label, Text value) {
        msg.add(Text.literal("\n  " + label + ": ").formatted(Formatting.GRAY));
        msg.add(value);
    }
}
