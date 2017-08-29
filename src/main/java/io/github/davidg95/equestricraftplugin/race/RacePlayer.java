/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class RacePlayer {

    private final Player player;
    private double time;
    private int lap;
    private long lastCrossTime;

    public RacePlayer(Player player) {
        this.player = player;
        this.time = 0;
        this.lap = 1;
        lastCrossTime = 0L;
    }
    
    public void nextLap(){
        lap++;
    }
    
    public int getLap(){
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
    
    @Override
    public String toString(){
        return player.getName() + " - " + time + "s";
    }
}
