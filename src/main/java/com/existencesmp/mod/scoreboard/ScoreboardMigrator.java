package com.existencesmp.mod.scoreboard;

import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;

import java.util.List;

public class ScoreboardMigrator {
    private static final List<String> SUM_SCORES = List.of(
            "exi_playtime_h", "exi_deaths", "exi_elytra_km", "mpp_afk_adv", "mpp_sleep_adv", "exi_warden_kill", "exi_warden_count"
    );

    public static boolean shouldSum(ScoreboardObjective objective) {
        return SUM_SCORES.contains(objective.getName()) || objective.getName().startsWith("ts_");
    }

    public static void migrate(ServerScoreboard scoreboard, ScoreboardMigration migration) {
        migration.override.forEach(objective -> {
            ReadableScoreboardScore oldValue = scoreboard.getScore(migration.oldHolder, objective);
            assert oldValue != null;
            scoreboard.getOrCreateScore(migration.newHolder, objective, true).setScore(oldValue.getScore());
        });
        migration.create.forEach(objective -> {
            ReadableScoreboardScore oldValue = scoreboard.getScore(migration.oldHolder, objective);
            assert oldValue != null;
            scoreboard.getOrCreateScore(migration.newHolder, objective, true).setScore(oldValue.getScore());
        });
        migration.sum.forEach(objective -> {
            ReadableScoreboardScore oldValue = scoreboard.getScore(migration.oldHolder, objective);
            ReadableScoreboardScore newValue = scoreboard.getScore(migration.newHolder, objective);
            if (newValue != null) {
                scoreboard.getOrCreateScore(migration.newHolder, objective, true).setScore(oldValue.getScore() + newValue.getScore());
            } else {
                scoreboard.getOrCreateScore(migration.newHolder, objective, true).setScore(oldValue.getScore());
            }
        });
        migration.deleteAfter.forEach(objective -> {
            scoreboard.removeScore(migration.oldHolder, objective);
        });
    }
}
