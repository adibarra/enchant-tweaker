package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class SetCommand implements Command<ServerCommandSource> {

    // VERSION CHANGES:
    // 1.16+: sendFeedback(Text)
    // 1.20+: sendFeedback(() -> Text)
    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        String key = StringArgumentType.getString(context, "key").toLowerCase();
        String value = boolString(StringArgumentType.getString(context, "value").toLowerCase());
        List<Text> msg = new ArrayList<>();

        msg.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
        if (ETMixinPlugin.getConfig().set(key, value)) {
            msg.add(Text.literal("Key '").formatted(Formatting.GRAY));
            msg.add(Text.literal(key));
            msg.add(Text.literal("' set to '").formatted(Formatting.GRAY));
            msg.add(ADText.colorValue(value));
            msg.add(Text.literal("'.").formatted(Formatting.GRAY));
            if (boolString(value).equals("true") || boolString(value).equals("false")) {
                String isServer = context.getSource().getServer().isDedicated() ? "server " : "";
                msg.add(Text.literal("\nThis change requires a " + isServer + "restart to take effect.").formatted(Formatting.LIGHT_PURPLE));
            }
            context.getSource().sendFeedback(() -> ADText.joinText(msg), false);
            return Command.SINGLE_SUCCESS;
        }

        msg.add(Text.literal("Key '").formatted(Formatting.GRAY));
        msg.add(Text.literal(key).formatted(Formatting.RED));
        msg.add(Text.literal("' does not exist.").formatted(Formatting.GRAY));
        context.getSource().sendError(ADText.joinText(msg));
        return 0;
    }

    private static String boolString(String value) {
        if (ADText.TRUE_VALUES.contains(value)) return "true";
        if (ADText.FALSE_VALUES.contains(value)) return "false";
        return value;
    }
}
