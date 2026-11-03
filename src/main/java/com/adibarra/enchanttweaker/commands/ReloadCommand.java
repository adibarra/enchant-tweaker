package com.adibarra.enchanttweaker.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import com.adibarra.enchanttweaker.ETCommands;
import com.adibarra.enchanttweaker.ETMixinPlugin;

public class ReloadCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        ETMixinPlugin.reloadConfig();

        // push the freshly reloaded config to connected clients
        MinecraftServer server = context.getSource().getServer();
        if (server != null) {
            ETCommands.broadcastConfigSync(server);
        }

        CommandFeedback.feedback(context.getSource(),
            Component.literal("Config Reloaded!").withStyle(ChatFormatting.AQUA));
        return Command.SINGLE_SUCCESS;
    }
}
