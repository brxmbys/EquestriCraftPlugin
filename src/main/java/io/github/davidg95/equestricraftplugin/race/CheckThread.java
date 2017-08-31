/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class CheckThread extends Thread {

    private final List<RacePlayer> players;
    private final Race race;

    private boolean run;

    public CheckThread(Race r, List<RacePlayer> players) {
        super("RACE_THREAD");
        this.players = players;
        this.race = r;
        run = true;
    }

    @Override
    public void run() {
        FileConfiguration fc = EquestriCraftPlugin.plugin.getConfig();
        int z1 = fc.getInt("z1");
        int z2 = fc.getInt("z2");
        int x1 = fc.getInt("x1");
        int x2 = fc.getInt("x2");
        int yl = fc.getInt("yl");
        Bukkit.broadcastMessage("Finish line coords: " + z1 + "," + z2 + "," + x1 + "," + x2 + "," + yl);
        while (run) {
            for (int i = 0; i < players.size(); i++) {
                final Player player = players.get(i).getPlayer();
                int z = player.getLocation().getBlockZ();
                int x = player.getLocation().getBlockX();
                int y = player.getLocation().getBlockY();
                if (z < z1 && z > z2 && x >= x1 && x <= x2 && y <= yl) {
                    final RacePlayer rp = players.get(i);
                    if (rp.getLap() > 0 && new Date().getTime() - rp.getLastCrossTime() < 5000L) {
                        continue;
                    }
                    rp.setLastCrossTime(new Date().getTime());
                    if (rp.getLap() == race.laps()) {
                        race.completePlayer(rp);
                        continue;
                    }
                    rp.nextLap();
                    player.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "You are on lap " + rp.getLap());
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
