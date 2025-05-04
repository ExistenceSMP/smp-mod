package com.existencesmp.mod;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermissionLevel;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@Command("smp")
public class SMPCommand {
    @Command({"resource-pack", "update"})
    @RequiresPermissionLevel(4)
    public void resourcePackUpdate(CommandContext<ServerCommandSource> context) {
        try {
            SMPMod.downloadResourcePack(context.getSource().getServer(), true);
            context.getSource().getServer().getPlayerManager().broadcast(
                    Text.literal("An ").formatted(Formatting.GRAY)
                            .append(Text.literal("Existence Community Resource Pack").formatted(Formatting.DARK_RED))
                            .append(Text.literal(" update is available! Quit and re-join for it to take effect.").formatted(Formatting.GRAY)),
                    false);
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Failed to update resource pack: " + e.getMessage()).formatted(Formatting.RED));
        }
    }
}
