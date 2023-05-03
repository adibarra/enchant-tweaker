package com.adibarra.enchanttweaker.commands;

import com.adibarra.enchanttweaker.ETMixinPlugin;
import com.adibarra.enchanttweaker.EnchantTweaker;
import com.adibarra.utils.ADText;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class KeysCommand implements Command<ServerCommandSource>  {
    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        List<Text> msg = new ArrayList<>();

        msg.add(Text.literal(EnchantTweaker.PREFIX).formatted(Formatting.GREEN));
        msg.add(Text.literal("List of all config keys:\n"));

        for (String key : ETMixinPlugin.getConfig().getKeys())
            msg.add(Text.literal(key + ", ").formatted(Formatting.GRAY));

        context.getSource().sendFeedback(ADText.joinText(msg), false);
        return Command.SINGLE_SUCCESS;
    }
}
