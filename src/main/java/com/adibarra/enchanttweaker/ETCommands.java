package com.adibarra.enchanttweaker;

import com.adibarra.utils.ADBrigadier;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
            new Command("Get a list of all config keys.", () ->
                CommandManager.literal("keys")
                    .executes(context -> {
                        List<Text> out = new ArrayList<>();
                        out.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
                        out.add(Text.literal("List of all config keys:\n"));
                        for (String key : ETMixinPlugin.getConfig().getKeys())
                            out.add(Text.literal(key + ", ").formatted(Formatting.GRAY));
                        context.getSource().sendFeedback(ADText.joinText(out), false);
                        return 1;
                    }).build())
        );

        COMMANDS.add(
            new Command("Get the value for a config key.", () ->
                CommandManager.literal("get")
                    .then(CommandManager.argument("key", StringArgumentType.word())
                        .executes((context -> {
                            String key = StringArgumentType.getString(context, "key").toLowerCase();
                            String value = ETMixinPlugin.getConfig().getOrDefault(key, null);
                            List<Text> out = new ArrayList<>();
                            out.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
                            if (value != null) {
                                out.add(Text.literal("Key '").formatted(Formatting.GRAY));
                                out.add(Text.literal(key));
                                out.add(Text.literal("' is set to '").formatted(Formatting.GRAY));
                                out.add(ADText.colorValue(value.toLowerCase()));
                                out.add(Text.literal("'.").formatted(Formatting.GRAY));
                            } else {
                                out.add(Text.literal("Key '").formatted(Formatting.GRAY));
                                out.add(Text.literal(key).formatted(Formatting.RED));
                                out.add(Text.literal("' does not exist.").formatted(Formatting.GRAY));
                            }
                            context.getSource().sendFeedback(ADText.joinText(out), false);
                            return 1;
                        }))).build())
        );

        // TODO: Set command should also save to config file
        // COMMANDS.add(
        //     new Command("Set the value for a config key.", () ->
        //         CommandManager.literal("set")
        //             .then(CommandManager.argument("key", StringArgumentType.word())
        //                 .then(CommandManager.argument("value", StringArgumentType.word())
        //                     .executes(context -> {
        //                         String key = StringArgumentType.getString(context, "key").toLowerCase();
        //                         String value = boolString(StringArgumentType.getString(context, "value").toLowerCase());
        //                         List<Text> out = new ArrayList<>();
        //                         out.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
        //                         if (ETMixinPlugin.getConfig().set(key, value)) {
        //                             out.add(Text.literal("Key '").formatted(Formatting.GRAY));
        //                             out.add(Text.literal(key));
        //                             out.add(Text.literal("' set to '").formatted(Formatting.GRAY));
        //                             out.add(ADText.colorValue(value));
        //                             out.add(Text.literal("'.").formatted(Formatting.GRAY));
        //                         }
        //                         else {
        //                             out.add(Text.literal("Key '").formatted(Formatting.GRAY));
        //                             out.add(Text.literal(key).formatted(Formatting.RED));
        //                             out.add(Text.literal("' does not exist.").formatted(Formatting.GRAY));
        //                         }
        //                         context.getSource().sendFeedback(ADText.joinText(out), false);
        //                         return 1;
        //                     }))).build())
        // );

        COMMANDS.sort(Comparator.comparing(a -> a.node.get().getLiteral()));
    }

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess, environment) -> {
            // Build base node
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

            // Build and register help node
            baseNode.addChild(
                CommandManager.literal("help")
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
                        out.add(Text.literal("\n")
                            .append(ADText.joinText(new Text[] {
                                ADText.buildCmdLink("/et help"),
                                Text.literal(" - Shows this help message.")
                            })));
                        context.getSource().sendFeedback(ADText.joinText(out), false);
                        return 1;
                    }).build());

            // Register base nodes to root
            commandDispatcher.getRoot().addChild(baseNode);
            commandDispatcher.getRoot().addChild(ADBrigadier.buildAlias("enchanttweaker", baseNode));
        });
    }

    public static void registerEventListeners() {
        // Listen for server reload event (i.e. /reload)
        ServerLifecycleEvents.START_DATA_PACK_RELOAD.register((server, manager) -> ETMixinPlugin.reloadConfig());
    }

    public static String boolString(String value) {
        if (Arrays.asList("true", "t", "yes", "on", "enable", "enabled").contains(value)) return "true";
        if (Arrays.asList("false", "f", "no", "off", "disable", "disabled").contains(value)) return "false";
        return value;
    }
}
