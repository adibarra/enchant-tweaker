package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.EnchantTweaker;
import com.magistermaks.simpleconfig.SimpleConfig;
import com.mojang.brigadier.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.*;

public final class EnchantTweakerCommands {
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("enchanttweaker")
			.then(literal("check"))
					.then(argument("enchantment", string()))
							.executes(ctx -> checkEnchantCap(ctx.getSource(), getString(ctx, "enchantment")))
			.requires(source -> source.hasPermissionLevel(2))
					.then(literal("reload")
							.executes(ctx -> reload(ctx.getSource()))));
	}

	public static int checkEnchantCap(ServerCommandSource source, String enchantmentName) {
		if (source.getPlayer() != null)
			source.getPlayer().sendMessage(Text.of("Not implemented yet."));
		else
			source.getServer().sendMessage(Text.of("Not implemented yet."));
		return Command.SINGLE_SUCCESS;
	}

	@SuppressWarnings("SameReturnValue")
	public static int reload(ServerCommandSource source) {
		EnchantTweaker.config = SimpleConfig
				.of("enchant-tweaker")
				.provider(EnchantTweaker::getDefaultConfig)
				.request();

		if (source.getPlayer() != null)
			source.getPlayer().sendMessage(Text.of("Enchant Tweaker: Config reloaded!"));
		else
			source.getServer().sendMessage(Text.of("Enchant Tweaker: Config reloaded!"));
		return Command.SINGLE_SUCCESS;
	}
}