package com.existencesmp.mod.scoreboard;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;

import java.util.ArrayList;
import java.util.List;

import static com.existencesmp.mod.scoreboard.ScoreboardMigrator.shouldSum;

public class ScoreboardMigration {
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