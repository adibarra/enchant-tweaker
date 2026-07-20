package com.adibarra.enchanttweaker;

import com.adibarra.enchanttweaker.commands.*;
import com.adibarra.enchanttweaker.commands.suggestions.ValueSuggestion;
import com.adibarra.enchanttweaker.network.ConfigSyncPayload;
import com.adibarra.utils.ADBrigadier;
import com.adibarra.utils.ADUtils;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
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
            new ADBrigadier.Command("Check, set, list, or reset config keys.", () ->
                CommandManager.literal("config")
                    .executes(new ConfigCommand())
                    // literal subcommands are matched before the <key> word argument
                    .then(CommandManager.literal("list")
                        .executes(new ListCommand())
                        .then(CommandManager.argument("category", StringArgumentType.word())
                            .suggests(ListCommand.CATEGORY_SUGGESTIONS)
                                .executes(new ListCommand())
                            .then(CommandManager.argument("page", IntegerArgumentType.integer(1))
                                .executes(new ListCommand()))))
                    .then(CommandManager.literal("reset")
                        .executes(new ResetCommand())
                        .then(CommandManager.argument("key", StringArgumentType.word())
                            .suggests(ResetCommand.KEY_SUGGESTIONS)
                                .executes(new ResetCommand())))
                    .then(CommandManager.argument("key", StringArgumentType.word())
                        .suggests(GetCommand.KEY_SUGGESTIONS)
                            .executes(new GetCommand())
                        // `greedyString` (not word) so comma-separated list values like
                        // "magic,wither" are captured verbatim; it must remain the last argument
                        .then(CommandManager.argument("value", StringArgumentType.greedyString())
                            .suggests(ValueSuggestion.PROVIDER)
                            .executes(new SetCommand())))
                    .build())
        );

        COMMANDS.add(
            new ADBrigadier.Command("Reports live runtime diagnostics.", () ->
                CommandManager.literal("diagnose")
                    .executes(new DiagnoseCommand())
                    .build())
        );

        COMMANDS.sort(Comparator.comparing(a -> a.node().get().getLiteral()));
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess, environment) -> {
            CommandNode<ServerCommandSource> baseNode = CommandManager.literal(BASE_CMD)
                .requires(source -> source.hasPermissionLevel(2))
                .executes(new BaseCommand())
                .build();

            for (ADBrigadier.Command command : COMMANDS) {
                baseNode.addChild(command.node().get());
            }

            baseNode.addChild(
                CommandManager.literal("help")
                    .executes(new HelpCommand())
                    .build());

            commandDispatcher.getRoot().addChild(baseNode);
            commandDispatcher.getRoot().addChild(ADBrigadier.buildAlias("enchanttweaker", baseNode));
        });
    }

    public static void registerEventListeners() {
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, manager) -> {

            ETMixinPlugin.reloadConfig();

            List<Text> msg = new ArrayList<>();
            msg.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
            msg.add(Text.literal("Config Reloaded!").formatted(Formatting.AQUA));

            ADUtils.broadcastOps(server, ADText.joinTextMutable(msg));
            broadcastConfigSync(server);
        });
    }

    public static void broadcastConfigSync(MinecraftServer server) {
        ConfigSyncPayload payload = new ConfigSyncPayload(ETMixinPlugin.getConfigMap());
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, ConfigSyncPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static List<ADBrigadier.Command> getCommands() {
        return COMMANDS;
    }
}
