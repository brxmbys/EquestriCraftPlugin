/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Thread for checking the location of each race player.
 *
 * @author David
 */
public class CheckThread extends Thread {

    private final List<RacePlayer> players; //All the players in the race.
    private final Race race; //The race.

    private boolean run;

    private final EquestriCraftPlugin plugin;

    public CheckThread(EquestriCraftPlugin plugin, Race r, List<RacePlayer> players) {
        super("RACE_THREAD");
        this.players = players;
        this.race = r;
        run = true;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        FileConfiguration fc = plugin.getConfig();
        //Get the finishing line coords
        int fz1 = fc.getInt("finish.z1");
        int fz2 = fc.getInt("finish.z2");
        int fx1 = fc.getInt("finish.x1");
        int fx2 = fc.getInt("finish.x2");
        int fy1 = fc.getInt("finish.yl");

        //Get the checkpoint coords
        int cz1 = fc.getInt("check.z1");
        int cz2 = fc.getInt("check.z2");
        int cx1 = fc.getInt("check.x1");
        int cx2 = fc.getInt("check.x2");
        int cy1 = fc.getInt("check.yl");
        while (run) {
            for (int i = 0; i < players.size(); i++) {
                final Player player = players.get(i).getPlayer();
                //Get the players location
                int z = player.getLocation().getBlockZ();
                int x = player.getLocation().getBlockX();
                int y = player.getLocation().getBlockY();
                //Check if they are at the checkpoint
                if (z < cz1 && z > cz2 && x >= cx1 && x <= cx2 && y <= cy1) {
                    final RacePlayer rp = players.get(i);
                    if (rp.getSection() == 1) {
                        rp.setSection(2);
                    }
                }

                //Check if they are at the finish line
                if (z < fz1 && z > fz2 && x >= fx1 && x <= fx2 && y <= fy1) {
                    final RacePlayer rp = players.get(i);
                    //Check if they have first passed the checkpoint.
                    if (rp.getSection() == 1) {
                        continue;
                    }
                    //Set the last cross time
                    rp.setLastCrossTime(new Date().getTime());
                    //Check if they have finished already
                    if (rp.getLap() > race.laps()) {
                        continue;
                    }
                    //Check if they have compelted the race
                    if (rp.getLap() == race.laps()) {
                        race.completePlayer(rp);
                        rp.nextLap();
                        continue;
                    }
                    rp.nextLap();
                    //Check if they are the player in front
                    if (rp.getLap() > race.getLap()) {
                        race.setLap(rp.getLap());
                    }
                    player.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "You are on lap " + rp.getLap());
                }
            }
            //Check if all players have crossed
            if (race.getCompletedPlayers().size() == race.getPlayers().size()) {
                run = false;
            }
            try {
                Thread.sleep(35);
            } catch (InterruptedException ex) {
                Logger.getLogger(CheckThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //End the race
        race.finish();
    }

    public void stopRun() {
        run = false;
    }
}
