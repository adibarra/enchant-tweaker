package com.adibarra.enchanttweaker;

import com.adibarra.enchanttweaker.utils.Utils;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.List;

import static com.adibarra.enchanttweaker.EnchantTweaker.reloadConfig;

public class Commands {

	private static final String[] ALIASES = new String[] { "enchanttweaker", "et" };

	private record Command(String name, String description, String usage, String permission, String[] aliases, String[] subcommands) { }

	private static final List<LiteralCommandNode<ServerCommandSource>> COMMAND_NODES = List.of(
		CommandManager.literal("reload")
			.requires(source -> source.hasPermissionLevel(2))
			.executes(context -> {
				reloadConfig();
				context.getSource().sendFeedback(
					Utils.joinText(new Text[] {
						Text.literal("[Enchant Tweaker]").formatted(Formatting.GREEN),
						Text.literal(" Config Reloaded!")
					}), false);
				return 1;
			}).build()
	);

	public static void registerCommands() {
		CommandRegistrationCallback.EVENT.register((commandDispatcher, registryAccess, environment) -> {

			// Register all commands to all aliases
			for (String alias : ALIASES) {
				for (LiteralCommandNode<ServerCommandSource> node : COMMAND_NODES) {
					commandDispatcher.register(CommandManager.literal(alias).then(node));
				}

				// Base command should show help message
				commandDispatcher.register(
					CommandManager.literal(alias)
						.requires(source -> source.hasPermissionLevel(2))
						.executes(context -> {
							context.getSource().sendFeedback(
								Utils.joinText(new Text[] {
									Text.literal("[Enchant Tweaker] ").formatted(Formatting.GREEN),
									Text.literal("Try running ").formatted(Formatting.GRAY),
									Text.literal("/et help ")
										.setStyle(Style.EMPTY
											.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/et help"))
											.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to run /et help")))
									),
									Text.literal("for more information.").formatted(Formatting.GRAY)
								}), false);
							return 1;
						})
				);

				// Generate and register Help command
				commandDispatcher.register(
					CommandManager.literal(alias)
						.then(CommandManager.literal("help")
							.requires(source -> source.hasPermissionLevel(2))
							.executes(context -> {
								context.getSource().sendFeedback(
									Utils.joinText(new Text[] {
										Text.literal("[Enchant Tweaker]\n").formatted(Formatting.GREEN),
										Text.literal("/et reload ")
											.setStyle(Style.EMPTY
												.withColor(Formatting.AQUA)
												.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/et reload"))
												.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to run /et reload")))
											),
										Text.literal("- Reloads the config file.\n"),
										Text.literal("/et help ")
											.setStyle(Style.EMPTY
												.withColor(Formatting.AQUA)
												.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/et help"))
												.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Click to run /et help")))
											),
										Text.literal("- Shows this message."),
									}), false);
								return 1;
							}))
				);
			}
		});
	}
}
