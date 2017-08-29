/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class CheckThread extends Thread {

    private final List<Player> players;
    private final Race race;

    private boolean run;

    public CheckThread(Race r, List<Player> players) {
        super("RACE_THREAD");
        this.players = players;
        this.race = r;
        run = true;
    }

    @Override
    public void run() {
        while (run) {
            for (int i = 0; i < players.size(); i++) {
                final Player player = players.get(i);
                int z = player.getLocation().getBlockZ();
                int x = player.getLocation().getBlockZ();
                int y = player.getLocation().getBlockY();
                if (z < 11135 && z > 11130 && x >= -2124 && x <= -2093 && y <= 20) {
                    race.completePlayer(player);
                }
            }
            if (race.getCompletedPlayers().size() == race.getPlayers().size()) {
                run = false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(CheckThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        race.finish();
    }

    public void stopRun() {
        run = false;
    }
}
