/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class Race {

    private final List<Player> players;
    private final List<Player> complete;

    private CheckThread thread;

    private boolean started;

    private long startTime;

    public Race() {
        players = new LinkedList<>();
        complete = new LinkedList<>();
        started = false;
    }

    public void start() {
        started = true;
        startTime = new Date().getTime();
        thread = new CheckThread(this, players);
        thread.start();
    }

    public void finish() {
        Bukkit.broadcastMessage("RACE COMPLETE!");
        Bukkit.broadcastMessage("Rankings-");
        for (int i = 0; i < players.size(); i++) {
            Bukkit.broadcastMessage((i + 1) + "- " + players.get(i).getName());
        }
    }

    public boolean addPlayer(Player p) {
        if (started) {
            return false;
        }
        players.add(p);
        return true;
    }

    public List getPlayers() {
        return players;
    }

    public void completePlayer(Player p) {
        complete.add(p);
    }

    public List getCompletedPlayers() {
        return players;
    }
}
