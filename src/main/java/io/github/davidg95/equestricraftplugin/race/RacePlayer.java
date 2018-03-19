/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Score;

/**
 *
 * @author David
 */
public class RacePlayer {

    private final Player player;
    private double time;
    private int lap;
    private long lastCrossTime;
    private final Score score;
    private int section;
    private int position;

    public RacePlayer(Player player, Score score) {
        this.player = player;
        this.time = 0;
        this.lap = 1;
        lastCrossTime = 0L;
        this.score = score;
        score.setScore(1);
        section = 1;
        position = 0;
    }

    public void nextLap() {
        lap++;
        score.setScore(lap);
        section = 1;
    }

    public int getSection() {
        return section;
    }

    public void setSection(int section) {
        this.section = section;
    }

    public int getLap() {
        return lap;
    }

    public Player getPlayer() {
        return player;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public long getLastCrossTime() {
        return lastCrossTime;
    }

    public void setLastCrossTime(long lastCrossTime) {
        this.lastCrossTime = lastCrossTime;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return player.getDisplayName() + " - " + time + "s";
    }
}
