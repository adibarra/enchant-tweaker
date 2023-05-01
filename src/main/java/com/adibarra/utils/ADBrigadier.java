package com.adibarra.utils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

@SuppressWarnings("unused")
public class ADBrigadier {
    /**
     * Builds an alias for a command node.
     * @param alias the alias to build
     * @param destNode the destination node to forward to
     * @return the built alias node
     */
    public static <S> LiteralCommandNode<S> buildAlias(String alias, CommandNode<S> destNode) {
        LiteralArgumentBuilder<S> aliasNode = LiteralArgumentBuilder.<S>literal(alias)
            .requires(destNode.getRequirement())
            .forward(destNode.getRedirect(), destNode.getRedirectModifier(), destNode.isFork())
            .executes(destNode.getCommand());
        for (CommandNode<S> child : destNode.getChildren()) aliasNode.then(child);
        return aliasNode.build();
    }
}
