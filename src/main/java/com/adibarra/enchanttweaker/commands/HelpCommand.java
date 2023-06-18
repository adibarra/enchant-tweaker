package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADBrigadier;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements Command<ServerCommandSource> {

    // VERSION CHANGES:
    // 1.19+: sendFeedback(
    // 1.20+: sendFeedback(() ->
    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        List<Text> msg = new ArrayList<>();

        msg.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
        for (ADBrigadier.Command command : ETCommands.getCommands()) {
            msg.add(Text.literal("\n"));
            msg.add(ADText.buildCmdLink(ETCommands.BASE_CMD, command.node().get().getLiteral()));
            msg.add(Text.literal(" - " + command.description()));
        }

        msg.add(Text.literal("\n"));
        msg.add(ADText.buildCmdLink(ETCommands.BASE_CMD, "help"));
        msg.add(Text.literal(" - Shows this help message."));

        context.getSource().sendFeedback(() -> ADText.joinText(msg), false);
        return Command.SINGLE_SUCCESS;
    }
}
