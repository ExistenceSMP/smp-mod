package com.existencesmp.mod;

import com.mojang.brigadier.context.CommandContext;
import de.maxhenkel.admiral.annotations.Command;
import de.maxhenkel.admiral.annotations.Name;
import de.maxhenkel.admiral.annotations.RequiresPermission;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

@Command("smp")
public class SMPCommand {


    @Command({"resource-pack", "update"})
    @RequiresPermission("smp.resourcePack.update")
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

    @Command({"scoreboard", "count"})
    @RequiresPermission("smp.scoreboard.count")
    public void scoreboardCount(CommandContext<ServerCommandSource> context, @Name("username") String username) {
        ServerScoreboard scoreboard = context.getSource().getServer().getScoreboard();
        ScoreHolder holder = ScoreHolder.fromName(username);
        int scoreCount = scoreboard.getScoreHolderObjectives(holder).size();
        context.getSource().sendFeedback(() -> Text.literal(username + " has " + scoreCount + " scores"), false);
    }

    @Command({"scoreboard", "compare"})
    @RequiresPermission("smp.scoreboard.compare")
    public void scoreboardCompare(CommandContext<ServerCommandSource> context, @Name("oldUsername") String oldUsername, @Name("newUsername") String newUsername) {
        ServerScoreboard scoreboard = context.getSource().getServer().getScoreboard();
        ScoreHolder oldHolder = ScoreHolder.fromName(oldUsername);
        ScoreHolder newHolder = ScoreHolder.fromName(newUsername);
        ScoreboardMigrator.ScoreboardMigration migration = new ScoreboardMigrator.ScoreboardMigration(scoreboard, oldHolder, newHolder);
        List<String> overrideNames = migration.override.stream().map(ScoreboardObjective::getName).toList();
        List<String> createNames = migration.create.stream().map(ScoreboardObjective::getName).toList();
        List<String> ignoreNames = migration.ignore.stream().map(ScoreboardObjective::getName).toList();
        List<String> sumNames = migration.sum.stream().map(ScoreboardObjective::getName).toList();
        ServerCommandSource source = context.getSource();
        if (!overrideNames.isEmpty())
            source.sendFeedback(() -> Text.literal("The following scoreboard objectives will be overridden for " + newUsername + " using " + oldUsername + "'s values: " + String.join(", ", overrideNames)), false);
        if (!createNames.isEmpty())
            source.sendFeedback(() -> Text.literal("The following scoreboard objectives will be created for " + newUsername + " using " + oldUsername + "'s values: " + String.join(", ", createNames)), false);
        if (!ignoreNames.isEmpty())
            source.sendFeedback(() -> Text.literal("The following scoreboard objectives will be ignored for " + newUsername + ": " + String.join(", ", ignoreNames)), false);
        if (!sumNames.isEmpty())
            source.sendFeedback(() -> Text.literal("The following scoreboard objectives will be summed and given to " + newUsername + ": " + String.join(", ", sumNames)), false);
    }

    @Command({"scoreboard", "migrate"})
    @RequiresPermission("smp.scoreboard.migrate")
    public void scoreboardMigrate(CommandContext<ServerCommandSource> context, @Name("oldUsername") String oldUsername, @Name("newUsername") String newUsername) {
        ServerScoreboard scoreboard = context.getSource().getServer().getScoreboard();
        ScoreHolder oldHolder = ScoreHolder.fromName(oldUsername);
        ScoreHolder newHolder = ScoreHolder.fromName(newUsername);
        ScoreboardMigrator.ScoreboardMigration migration = new ScoreboardMigrator.ScoreboardMigration(scoreboard, oldHolder, newHolder);
        List<String> overrideNames = migration.override.stream().map(ScoreboardObjective::getName).toList();
        List<String> createNames = migration.create.stream().map(ScoreboardObjective::getName).toList();
        List<String> ignoreNames = migration.ignore.stream().map(ScoreboardObjective::getName).toList();
        List<String> sumNames = migration.sum.stream().map(ScoreboardObjective::getName).toList();
        ServerCommandSource source = context.getSource();
        ScoreboardMigrator.migrate(scoreboard, migration);
        if (!overrideNames.isEmpty())
            source.sendFeedback(() -> Text.literal("The following scoreboard objectives have been overridden for " + newUsername + " using " + oldUsername + "'s values: " + String.join(", ", overrideNames)), false);
        if (!createNames.isEmpty())
            source.sendFeedback(() -> Text.literal("The following scoreboard objectives have been created for " + newUsername + " using " + oldUsername + "'s values: " + String.join(", ", createNames)), false);
        if (!ignoreNames.isEmpty())
            source.sendFeedback(() -> Text.literal("The following scoreboard objectives have been ignored for " + newUsername + ": " + String.join(", ", ignoreNames)), false);
        if (!sumNames.isEmpty())
            source.sendFeedback(() -> Text.literal("The following scoreboard objectives have been summed and given to " + newUsername + ": " + String.join(", ", sumNames)), false);
    }
}
