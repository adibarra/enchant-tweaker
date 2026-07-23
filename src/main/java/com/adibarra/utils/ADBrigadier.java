package com.adibarra.utils;

import java.util.function.Supplier;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.ServerCommandSource;

@SuppressWarnings("unused")
public class ADBrigadier {

    private ADBrigadier() {
        throw new IllegalStateException("Utility class. Do not instantiate.");
    }

    /**
     * a record representing a command
     *
     * @param description
     *            the command description
     * @param node
     *            the command node
     */
    public record Command(String description, Supplier<LiteralCommandNode<ServerCommandSource>> node) {
    }

    /**
     * builds an alias for a command node
     *
     * @param alias
     *            the alias
     * @param destNode
     *            the destination node
     * @return the built alias node
     */
    public static <S> LiteralCommandNode<S> buildAlias(String alias, CommandNode<S> destNode) {
        LiteralArgumentBuilder<S> aliasNode = LiteralArgumentBuilder.<S>literal(alias)
            .requires(destNode.getRequirement()).executes(destNode.getCommand());

        if (destNode.getRedirect() == null)
            for (CommandNode<S> child : destNode.getChildren())
                aliasNode.then(copyNode(child));

        if (destNode.getRedirect() != null || destNode.getRedirectModifier() != null || destNode.isFork())
            aliasNode.forward(destNode.getRedirect(), destNode.getRedirectModifier(), destNode.isFork());

        return aliasNode.build();
    }

    private static <S> CommandNode<S> copyNode(CommandNode<S> sourceNode) {
        ArgumentBuilder<S, ?> copy = sourceNode.createBuilder();
        if (sourceNode.getRedirect() == null)
            for (CommandNode<S> child : sourceNode.getChildren())
                copy.then(copyNode(child));
        return copy.build();
    }
}
