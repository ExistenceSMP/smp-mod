package com.existencesmp.mod.command;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import net.minecraft.server.command.ServerCommandSource;

@Command("afk")
public class AFKCommand {
    @Command()
    public void afk(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getPlayer() != null)
            EntityPlayerMPFake.createShadow(context.getSource().getServer(), context.getSource().getPlayer());
    }
}
