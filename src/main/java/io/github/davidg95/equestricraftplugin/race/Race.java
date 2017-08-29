/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * Class which models a race.
 *
 * @author David
 */
public class Race {

    private final List<RacePlayer> players; //The players entering the race.
    private final List<RacePlayer> complete; //The name and the times of the players who have completed the race.

    private CheckThread thread; //CheckerThread.

    private boolean started; //Boolean indicating whether race has been started.
    private boolean finnished; //Boolean indicating whether race has been complete.

    private long startTime; //Time the race started at.

    private final int laps; //The number of laps.

    public Race(int laps) {
        this.laps = laps;
        players = new LinkedList<>();
        complete = new LinkedList<>();
        started = false;
        finnished = false;
    }

    /**
     * Starts the race.
     */
    public void start() {
        started = true;
        startTime = new Date().getTime();
        thread = new CheckThread(this, players);
        thread.start();
    }

    /**
     * Finishes the race and displays the listings.
     */
    public void finish() {
        finnished = true;
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "RACE COMPLETE!");
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Rankings-");
        for (int i = 0; i < complete.size(); i++) {
            if (i == 0) {
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GOLD + "1st- " + ChatColor.RESET + "" + ChatColor.BOLD + complete.get(i).toString());
            } else if (i == 1) {
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GRAY + "2nd- " + ChatColor.RESET + "" + ChatColor.BOLD + complete.get(i).toString());
            } else if (i == 2) {
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.YELLOW + "3rd- " + ChatColor.RESET + "" + ChatColor.BOLD + complete.get(i).toString());
            } else {
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + (i + 1) + "th- " + complete.get(i).toString());
            }
        }
    }

    /**
     * Ends the race session immediately.
     */
    public void terminate() {
        finnished = true;
        if (thread != null) {
            thread.stopRun();
        }
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + "RACE HAS BEEN TERMINATED!");
    }

    /**
     * Add a player to the race.
     *
     * @param p the player to add.
     * @return true if the race has not yet started, false if the race has
     * started already. If the race has stared, the player will not be added.
     */
    public boolean addPlayer(Player p) {
        if (started) {
            return false;
        }
        players.add(new RacePlayer(p));
        return true;
    }

    /**
     * Get all entrants.
     *
     * @return List of type RacePlayer.
     */
    public List<RacePlayer> getPlayers() {
        return players;
    }

    /**
     * Calculates the players time and adds them to the complete list. Displays
     * their time and position.
     *
     * @param p the player.
     */
    public void completePlayer(RacePlayer p) {
        final long time = new Date().getTime();
        final long raceTime = time - startTime;
        final double seconds = raceTime / 1000;
        final int position = complete.size() + 1;
        p.setTime(seconds);
        for (RacePlayer rp : complete) {
            if (rp.getPlayer().getName().equals(p.getPlayer().getName())) {
                return;
            }
        }
        complete.add(p);
        p.getPlayer().sendMessage("Position: " + position);
        p.getPlayer().sendMessage("Your time: " + seconds + "s");
        p.getPlayer().setMetadata("time", new FixedMetadataValue(EquestriCraftPlugin.plugin, time));
    }

    /**
     * Get the complete players and times.
     *
     * @return List of type RacePlayer.
     */
    public List<RacePlayer> getCompletedPlayers() {
        return complete;
    }

    /**
     * Remove a player from the race.
     *
     * @param player the player to remove.
     * @return true if they were in the race, false if they were not.
     */
    public boolean withdraw(Player player) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayer().getName().equals(player.getName())) {
                players.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the race has finished.
     *
     * @return boolean indicating state.
     */
    public boolean isFinnsihed() {
        return finnished;
    }

    /**
     * Check if the race has started.
     *
     * @return boolean indicating state.
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Get the laps of the race.
     *
     * @return the laps as an int.
     */
    public int laps() {
        return laps;
    }
}
