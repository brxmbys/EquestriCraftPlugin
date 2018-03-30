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

    public CheckThread(Race r, List<RacePlayer> players) {
        super("RACE_THREAD");
        this.players = players;
        this.race = r;
        run = true;
    }

    @Override
    public void run() {
        while (run) {
            for (int i = 0; i < players.size(); i++) {
                final Player player = players.get(i).getPlayer();

                //Check if they are at the checkpoint
                if (race.getTrack().checkpoint(player)) {
                    final RacePlayer rp = players.get(i);
                    if (rp.getSection() == 1) {
                        rp.setSection(2);
                    }
                }

                //Check if they are at the finish line
                if (race.getTrack().finish(player)) {
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
