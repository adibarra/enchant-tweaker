package com.adibarra.enchanttweaker;

import com.adibarra.utils.ADBrigadier;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ETCommands {

    private record Command(String description, Supplier<LiteralCommandNode<ServerCommandSource>> node) { }

    private static final List<Command> COMMANDS = new ArrayList<>();

    static {
        COMMANDS.add(
            new Command("Reloads the config file.", () ->
                CommandManager.literal("reload")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        ETMixinPlugin.reloadConfig();
                        Text[] out = new Text[] {
                            Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN),
                            Text.literal("Config Reloaded!")
                        };
                        context.getSource().sendFeedback(ADText.joinText(out), false);
                        return 1;
                    }).build())
        );

        COMMANDS.add(
            new Command("Shows this help message.", () ->
                CommandManager.literal("help")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        List<Text> out = new ArrayList<>();
                        out.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
                        for (Command command : COMMANDS) {
                            out.add(Text.literal("\n")
                                .append(ADText.joinText(new Text[] {
                                    ADText.buildCmdLink("/et " + command.node.get().getLiteral()),
                                    Text.literal(" - " + command.description)
                                })));
                        }
                        context.getSource().sendFeedback(ADText.joinText(out), false);
                        return 1;
                    }).build())
        );
  }

	public static void registerCommands() {
        // Build base nodes
        CommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess, environment) -> {
            CommandNode<ServerCommandSource> baseNode = CommandManager.literal("et")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> {
                    Text[] out = new Text[] {
                        Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN),
                        Text.literal("Try running ").formatted(Formatting.GRAY),
                        ADText.buildCmdLink("/et help"),
                        Text.literal(" for more information.").formatted(Formatting.GRAY)
                    };
                    context.getSource().sendFeedback(ADText.joinText(out), false);
                    return 1;
                }).build();

            // Register subcommands to base node
            for (Command command : COMMANDS) {
                baseNode.addChild(command.node.get());
            }

            // Register base nodes to root
            commandDispatcher.getRoot().addChild(baseNode);
            commandDispatcher.getRoot().addChild(ADBrigadier.buildAlias("enchanttweaker", baseNode));
        });
	}
}
