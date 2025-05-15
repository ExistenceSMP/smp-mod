package com.existencesmp.mod;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;

import java.util.ArrayList;
import java.util.List;

public class ScoreboardMigrator {
    static class ScoreboardMigration {
        public ScoreHolder oldHolder;
        public ScoreHolder newHolder;
        public List<ScoreboardObjective> override = new ArrayList<>();
        public List<ScoreboardObjective> create = new ArrayList<>();
        public List<ScoreboardObjective> ignore = new ArrayList<>();
        public List<ScoreboardObjective> sum = new ArrayList<>();
        public List<ScoreboardObjective> deleteAfter = new ArrayList<>();

        public ScoreboardMigration(ServerScoreboard scoreboard, ScoreHolder oldHolder, ScoreHolder newHolder) {
            this.oldHolder = oldHolder;
            this.newHolder = newHolder;
            Object2IntMap<ScoreboardObjective> oldObjectives = scoreboard.getScoreHolderObjectives(oldHolder);
            Object2IntMap<ScoreboardObjective> newObjectives = scoreboard.getScoreHolderObjectives(newHolder);
            oldObjectives.forEach((objective, score) -> {
                if (newObjectives.containsKey(objective) && !shouldSum(objective)) {
                    override.add(objective);
                } else if (newObjectives.containsKey(objective)) {
                    sum.add(objective);
                } else {
                    create.add(objective);
                }
                deleteAfter.add(objective);
            });
            newObjectives.forEach((objective, score) -> {
                if (!oldObjectives.containsKey(objective)) {
                    ignore.add(objective);
                }
            });
        }
    }

    private static final List<String> SUM_SCORES = List.of(
            "exi_playtime_h", "exi_deaths", "exi_elytra_km", "mpp_afk_adv", "mpp_sleep_adv", "exi_warden_kill", "exi_warden_count"
    );

    private static boolean shouldSum(ScoreboardObjective objective) {
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
