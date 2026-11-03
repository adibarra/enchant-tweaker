package com.adibarra.enchanttweaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.adibarra.enchanttweaker.commands.*;
import com.adibarra.enchanttweaker.commands.suggestions.ValueSuggestion;
import com.adibarra.enchanttweaker.network.ConfigSyncPayload;
import com.adibarra.utils.ADBrigadier;
import com.adibarra.utils.ADText;
import com.adibarra.utils.ADUtils;

public class ETCommands {

    public static final String BASE_CMD = "et";
    private static final List<ADBrigadier.Command> COMMANDS = new ArrayList<>();
    private static final List<ADBrigadier.Command> COMMANDS_VIEW = Collections.unmodifiableList(COMMANDS);

    static {
        COMMANDS.add(new ADBrigadier.Command("Reloads the config file.",
            () -> Commands.literal("reload").executes(new ReloadCommand()).build()));

        COMMANDS.add(new ADBrigadier.Command("Check, set, list, or reset config keys.",
            () -> Commands.literal("config").executes(new ConfigCommand())
                // literal subcommands are matched before the <key> word argument
                .then(Commands.literal("list").executes(new ListCommand())
                    .then(Commands.argument("category", StringArgumentType.word())
                        .suggests(ListCommand.CATEGORY_SUGGESTIONS).executes(new ListCommand())
                        .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(new ListCommand()))))
                .then(Commands.literal("reset").executes(new ResetCommand())
                    .then(Commands.argument("key", StringArgumentType.word()).suggests(ResetCommand.KEY_SUGGESTIONS)
                        .executes(new ResetCommand())))
                .then(Commands.argument("key", StringArgumentType.word()).suggests(GetCommand.KEY_SUGGESTIONS)
                    .executes(new GetCommand())
                    // `greedyString` (not word) so comma-separated list values like
                    // "magic,wither" are captured verbatim; it must remain the last argument
                    .then(Commands.argument("value", StringArgumentType.greedyString())
                        .suggests(ValueSuggestion.PROVIDER).executes(new SetCommand())))
                .build()));

        COMMANDS.add(new ADBrigadier.Command("Reports live runtime diagnostics.",
            () -> Commands.literal("diagnose").executes(new DiagnoseCommand()).build()));

        COMMANDS.sort(Comparator.comparing(a -> a.node().get().getLiteral()));
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess, environment) -> {
            CommandNode<CommandSourceStack> baseNode = Commands.literal(BASE_CMD)
                .requires(source -> source.hasPermission(2)).executes(new BaseCommand()).build();

            for (ADBrigadier.Command command : COMMANDS) {
                baseNode.addChild(command.node().get());
            }

            baseNode.addChild(Commands.literal("help").executes(new HelpCommand()).build());

            commandDispatcher.getRoot().addChild(baseNode);
            commandDispatcher.getRoot().addChild(ADBrigadier.buildAlias("enchanttweaker", baseNode));
        });
    }

    public static void registerEventListeners() {
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, manager) -> {
            ETMixinPlugin.reloadConfig();

            List<Component> msg = new ArrayList<>();
            msg.add(Component.literal(EnchantTweaker.PREFIX).withStyle(ChatFormatting.GREEN));
            msg.add(Component.literal("Config Reloaded!").withStyle(ChatFormatting.AQUA));

            ADUtils.broadcastOps(server, ADText.joinTextMutable(msg));
            broadcastConfigSync(server);
        });
    }

    public static void broadcastConfigSync(MinecraftServer server) {
        ConfigSyncPayload payload = new ConfigSyncPayload(ETMixinPlugin.getConfigMap());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, ConfigSyncPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static List<ADBrigadier.Command> getCommands() {
        return COMMANDS_VIEW;
    }
}
