package com.adibarra.enchanttweaker;

import com.adibarra.enchanttweaker.commands.*;
import com.adibarra.utils.ADBrigadier;
import com.adibarra.utils.ADUtils;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ETCommands {

    public static final String BASE_CMD = "et";
    private static final List<ADBrigadier.Command> COMMANDS = new ArrayList<>();

    static {
        COMMANDS.add(
            new ADBrigadier.Command("Reloads the config file.", () ->
                CommandManager.literal("reload")
                    .executes(new ReloadCommand())
                    .build())
        );

        COMMANDS.add(
            new ADBrigadier.Command("Get a list of all config keys.", () ->
                CommandManager.literal("keys")
                    .executes(new KeysCommand())
                    .build())
        );

        COMMANDS.add(
            new ADBrigadier.Command("Get the value for a config key.", () ->
                CommandManager.literal("get")
                    .then(CommandManager.argument("key", StringArgumentType.word())
                        .suggests(GetCommand.KEY_SUGGESTIONS)
                        .executes(new GetCommand()))
                    .build())
        );

        COMMANDS.add(
            new ADBrigadier.Command("Set the value for a config key.", () ->
                CommandManager.literal("set")
                    .then(CommandManager.argument("key", StringArgumentType.word())
                        .suggests(SetCommand.KEY_SUGGESTIONS)
                        .then(CommandManager.argument("value", StringArgumentType.word())
                            .executes(new SetCommand())))
                    .build())
        );

        COMMANDS.sort(Comparator.comparing(a -> a.node().get().getLiteral()));
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess, environment) -> {
            // Build base node
            CommandNode<ServerCommandSource> baseNode = CommandManager.literal(BASE_CMD)
                .requires(source -> source.hasPermissionLevel(2))
                .executes(new BaseCommand())
                .build();

            // Register subcommands to base node
            for (ADBrigadier.Command command : COMMANDS) {
                baseNode.addChild(command.node().get());
            }

            // Build and register help node
            baseNode.addChild(
                CommandManager.literal("help")
                    .executes(new HelpCommand())
                    .build());

            // Register base nodes to root
            commandDispatcher.getRoot().addChild(baseNode);
            commandDispatcher.getRoot().addChild(ADBrigadier.buildAlias("enchanttweaker", baseNode));
        });
    }

    public static void registerEventListeners() {
        // Listen for server reload event (i.e. /reload)
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, manager) -> {

            // Reload config file
            ETMixinPlugin.reloadConfig();
            ADUtils.broadcastOps(server, ADText.joinText(new Text[] {
                Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN),
                Text.literal("Config Reloaded!")
            }));
        });
    }

    public static List<ADBrigadier.Command> getCommands() {
        return COMMANDS;
    }
}
