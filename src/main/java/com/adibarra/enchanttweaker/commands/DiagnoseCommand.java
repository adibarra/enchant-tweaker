package com.adibarra.enchanttweaker.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import com.adibarra.enchanttweaker.AnvilRepairHandler;
import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.utils.ADConfig;
import com.adibarra.utils.ADText;

/** reports active settings, conflicts, and config details */
public class DiagnoseCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        ADConfig config = ETMixinPlugin.getConfig();
        List<Component> msg = new ArrayList<>();

        boolean modEnabledNow = config.getOrDefault("mod_enabled", false);
        Map<String, String> activeCompat = ETMixinPlugin.getActiveCompatOverrides();

        header(msg, "Mod Status");
        row(msg, "mod enabled", ADText.colorValue(String.valueOf(modEnabledNow)));
        row(msg, "applied mixins",
            Component.literal(String.valueOf(ETMixinPlugin.getNumMixins())).withStyle(ChatFormatting.AQUA));

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
                msg.add(Component.literal("\n  "));
                msg.add(Component.literal(configKey).withStyle(ChatFormatting.RED));
                msg.add(Component.literal(" - " + reason).withStyle(ChatFormatting.GRAY));
            }
        }
        if (!anyDesynced)
            msg.add(Component.literal("\n  (none)").withStyle(ChatFormatting.DARK_GRAY));

        header(msg, "Active COMPAT Overrides");
        if (activeCompat.isEmpty()) {
            msg.add(Component.literal("\n  (none)").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (Map.Entry<String, String> entry : activeCompat.entrySet()) {
                msg.add(Component.literal("\n  "));
                msg.add(Component.literal(entry.getKey()).withStyle(ChatFormatting.GOLD));
                msg.add(Component.literal(" -> " + entry.getValue()).withStyle(ChatFormatting.GRAY));
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
        row(msg, "cost", Component.literal(repairCost + " ingots" + costNote).withStyle(ChatFormatting.BLUE));
        row(msg, "handler registered", ADText.colorValue(String.valueOf(AnvilRepairHandler.isRegistered())));

        header(msg, "Config File");
        row(msg, "path", Component.literal(config.getConfigPath()).withStyle(ChatFormatting.AQUA));
        row(msg, "keys", Component.literal(String.valueOf(config.getKeys().size())).withStyle(ChatFormatting.AQUA));
        row(msg, "config_version", ADText.colorValue(config.getOrDefault("config_version", "?")));

        CommandFeedback.feedback(context.getSource(), msg);
        return Command.SINGLE_SUCCESS;
    }

    private static void header(List<Component> msg, String title) {
        msg.add(Component.literal("\n")
            .append(Component.literal("== " + title + " ==").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
    }

    private static void row(List<Component> msg, String label, Component value) {
        msg.add(Component.literal("\n  " + label + ": ").withStyle(ChatFormatting.GRAY));
        msg.add(value);
    }
}
