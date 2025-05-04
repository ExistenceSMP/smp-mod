package com.existencesmp.mod;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.RequiresPermissionLevel;
import net.minecraft.command.CommandSource;
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
            context.getSource().sendFeedback(() -> Text.literal("Successfully updated resource pack!"), false);
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Failed to update resource pack: " + e.getMessage()).formatted(Formatting.RED));
        }
    }
}
