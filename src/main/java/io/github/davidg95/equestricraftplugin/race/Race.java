/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

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
        EquestriCraftPlugin.LOG.log(Level.INFO, "RACE COMPLETE!");
        EquestriCraftPlugin.LOG.log(Level.INFO, "Rankings-");
        for (int i = 0; i < players.size(); i++) {
            EquestriCraftPlugin.LOG.log(Level.INFO, (i + 1) + "- " + players.get(i).getName());
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
        final long time = new Date().getTime();
        final long raceTime = time - startTime;
        final double seconds = raceTime / 1000;
        final int position = complete.size() + 1;
        complete.add(p);
        p.sendMessage("Position: " + position);
        p.sendMessage("Your time: " + seconds + "s");
        p.setMetadata("time", new FixedMetadataValue(EquestriCraftPlugin.plugin, time));
    }

    public List getCompletedPlayers() {
        return complete;
    }
}
