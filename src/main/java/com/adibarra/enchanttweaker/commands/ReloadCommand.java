package com.adibarra.enchanttweaker.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.ETMixinPlugin;

public class ReloadCommand implements Command<ServerCommandSource> {

    @Override
    public int run(CommandContext<ServerCommandSource> context) {
        ETMixinPlugin.reloadConfig();

        // push the freshly reloaded config to connected clients
        MinecraftServer server = context.getSource().getServer();
        if (server != null) {
            ETCommands.broadcastConfigSync(server);
        }

        CommandFeedback.feedback(context.getSource(), Text.literal("Config Reloaded!").formatted(Formatting.AQUA));
        return Command.SINGLE_SUCCESS;
    }
}
