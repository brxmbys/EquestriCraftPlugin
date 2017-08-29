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
 *
 * @author David
 */
public class Race {

    private final List<Player> players;
    private final List<PlayerTime> complete;

    private CheckThread thread;

    private boolean started;
    private boolean finnished;

    private long startTime;

    public Race() {
        players = new LinkedList<>();
        complete = new LinkedList<>();
        started = false;
        finnished = false;
    }

    public void start() {
        started = true;
        startTime = new Date().getTime();
        thread = new CheckThread(this, players);
        thread.start();
    }

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

    public void terminate() {
        finnished = true;
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + "RACE HAS BEEN TERMINATED!");
    }

    public boolean addPlayer(Player p) {
        if (started) {
            return false;
        }
        players.add(p);
        return true;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void completePlayer(Player p) {
        final long time = new Date().getTime();
        final long raceTime = time - startTime;
        final double seconds = raceTime / 1000;
        final int position = complete.size() + 1;
        for (PlayerTime pt : complete) {
            if (pt.name.equals(p.getName())) {
                return;
            }
        }
        complete.add(new PlayerTime(p.getName(), seconds));
        p.sendMessage("Position: " + position);
        p.sendMessage("Your time: " + seconds + "s");
        p.setMetadata("time", new FixedMetadataValue(EquestriCraftPlugin.plugin, time));
    }

    public List getCompletedPlayers() {
        return complete;
    }

    public boolean withdraw(Player player) {
        return players.remove(player);
    }

    public boolean isFinnsihed() {
        return finnished;
    }

    public boolean isStarted() {
        return started;
    }

    public class PlayerTime {

        private String name;
        private double time;

        public PlayerTime(String name, double time) {
            this.name = name;
            this.time = time;
        }

        @Override
        public String toString() {
            return name + " - " + time + "s";
        }
    }
}
