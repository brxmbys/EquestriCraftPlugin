/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Class which models a race.
 *
 * @author David
 */
public class Race implements Listener {

    private final List<RacePlayer> players; //The players entering the race.
    private final List<RacePlayer> complete; //The name and the times of the players who have completed the race.
    private final List<Player> spectators;

    private CheckThread thread; //CheckerThread.

    private int state;

    public static final int OPEN = 1;
    public static final int STARTING = 2;
    public static final int STARTED = 3;
    public static final int FINISHED = 4;

    private long startTime; //Time the race started at.

    private final int laps; //The number of laps.

    private final Economy economy;

    private final double prize1;
    private final double prize2;
    private final double prize3;

    private Scoreboard board;
    private Team team;
    private Objective objective;

    private int lap;

    private final EquestriCraftPlugin plugin;

    private final RaceTrack track;

    static {
        try {
            Block b = Bukkit.getWorld("EquestriCraft").getBlockAt(-2033, 7, 11125);
            if (b.getType() != Material.SIGN) {
                b.setType(Material.SIGN);
            }
            for (int i = -2032; i <= -2028; i++) {
                Block bl = Bukkit.getWorld("EquestriCraft").getBlockAt(i, 7, 11125);
                if (bl.getType() != Material.SIGN) {
                    bl.setType(Material.SIGN);
                }
            }
            Block blo = Bukkit.getWorld("EquestriCraft").getBlockAt(-2034, 7, 11125);
            if (blo.getType() != Material.SIGN) {
                blo.setType(Material.SIGN);
            }
        } catch (Exception e) {
        }
    }

    public Race(EquestriCraftPlugin plugin, RaceTrack track, int laps, double prize1, double prize2, double prize3) {
        this.laps = laps;
        this.plugin = plugin;
        this.track = track;
        players = new LinkedList<>();
        complete = new LinkedList<>();
        spectators = new LinkedList<>();
        state = OPEN;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        this.economy = plugin.getEconomy();
        if (economy == null) {
            Bukkit.broadcastMessage("Economy not detected, prizes disabled");
        }
        this.prize1 = prize1;
        this.prize2 = prize2;
        this.prize3 = prize3;
        initScoreboard();

    }

    private void initScoreboard() {
        board = Bukkit.getScoreboardManager().getNewScoreboard();

        team = board.registerNewTeam(ChatColor.BOLD + "" + ChatColor.GREEN + "Race");
        objective = board.registerNewObjective("Race", "Win the race");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.BOLD + "" + ChatColor.GREEN + "Race");
    }

    /**
     * Starts the race.
     */
    public void start() {
        state = STARTED;
        startTime = new Date().getTime();
        thread = new CheckThread(plugin, this, players);
        setGatesOpen(true);
        thread.start();
    }

    public RaceTrack getTrack() {
        return track;
    }

    private void setGatesOpen(boolean state) {
        new BukkitRunnable() {
            @Override
            public void run() {
                track.setGateOpen(state);
            }
        }.runTask(plugin);
    }

    protected void setLap(int lap) {
        this.lap = lap;
    }

    protected int getLap() {
        return lap;
    }

    public void countdown() {
        state = STARTING;
        final Runnable run = () -> {
            try {
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "5");
                Thread.sleep(1000);
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "4");
                Thread.sleep(1000);
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "3");
                Thread.sleep(1000);
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "2");
                Thread.sleep(1000);
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "1");
                Thread.sleep(1000);
                start();
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "Race has started!");
            } catch (InterruptedException ex) {
                Logger.getLogger(Race.class.getName()).log(Level.SEVERE, null, ex);
            }
        };
        final Thread countThread = new Thread(run, "COUNTDOWN");
        countThread.start();
    }

    /**
     * Finishes the race and displays the listings.
     */
    public void finish() {
        state = FINISHED;
        if (complete.isEmpty()) {
            return;
        }
        HandlerList.unregisterAll(this);
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "RACE COMPLETE!");
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Rankings-");
        for (int i = 0; i < complete.size(); i++) {
            switch (i) {
                case 0:
                    Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "1st- " + ChatColor.RESET + "" + ChatColor.BOLD + complete.get(i).toString());
                    break;
                case 1:
                    Bukkit.broadcastMessage(ChatColor.GRAY + "" + ChatColor.BOLD + "2nd- " + ChatColor.RESET + "" + ChatColor.BOLD + complete.get(i).toString());
                    break;
                case 2:
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "3rd- " + ChatColor.RESET + "" + ChatColor.BOLD + complete.get(i).toString());
                    break;
                default:
                    Bukkit.broadcastMessage(ChatColor.BOLD + "" + (i + 1) + "th- " + complete.get(i).toString());
                    break;
            }
        }
        terminate();
    }

    /**
     * Ends the race session immediately.
     */
    public void terminate() {
        for (RacePlayer p : players) {
            p.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        for (Player p : spectators) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        state = FINISHED;
        if (thread != null) {
            thread.stopRun();
        }
        setGatesOpen(false);
        HandlerList.unregisterAll(this);
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + "RACE HAS BEEN TERMINATED!");
    }

    /**
     * Add a player to the race.
     *
     * @param p the player to add.
     * @return true or false.
     */
    public synchronized boolean addPlayer(Player p) {
        if (state == STARTED || state == STARTING || state == FINISHED || players.size() >= 20) {
            p.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Error joining race");
            return false;
        }
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayer().getName().equals(p.getName())) {
                p.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Error joining race");
                return false;
            }
        }
        team.addPlayer(p);
        players.add(new RacePlayer(p, objective.getScore(p)));
        p.setScoreboard(board);
        p.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "You are in the race!");
        Bukkit.broadcastMessage(p.getDisplayName() + " is in the race!");
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

    private String stripName(Player p) {
        String name = p.getPlayer().getDisplayName(); //Get thier display name
        int index = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == ']') {
                index = i;
                break;
            }
        }
        return name.substring(index + 1); //Display name stripped of thier rank
    }

    /**
     * Calculates the players time and adds them to the complete list. Displays
     * their time and position.
     *
     * @param p the player.
     */
    public void completePlayer(RacePlayer p) {
        final long time = new Date().getTime(); //Time the crossed the line
        final long raceTime = time - startTime; //Total race time
        final double seconds = raceTime / 1000D; //Race time in seconds
        final int position = complete.size() + 1; //Their position
        double prize = 0;
        p.setPosition(position);
        switch (position) {
            case 1: //1st place
                prize = prize1;
                break;
            case 2: //2nd place
                prize = prize2;
                break;
            case 3: //3rd place
                prize = prize3;
                break;
            default:
                break;
        }
        if (prize > 0) { //Check if they have won a prize
            if (economy != null) {
                economy.depositPlayer(p.getPlayer(), prize); //Deposit prize money
                p.getPlayer().sendMessage("You have won " + ChatColor.AQUA + "$" + new DecimalFormat("0").format(prize) + "!");
            }
        }
        p.setTime(seconds);
        for (RacePlayer rp : complete) {
            if (rp.getPlayer().getName().equals(p.getPlayer().getName())) {
                return;
            }
        }
        complete.add(p);
        p.getPlayer().sendMessage("Position: " + position);
        p.getPlayer().sendMessage("Your time: " + seconds + "s");
        p.getPlayer().setMetadata("time", new FixedMetadataValue(plugin, time));
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
                team.removePlayer(player);
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                player.sendMessage("You have withdrawn from the race");
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + player.getName() + " has withdrawn from the race!");
                return true;
            }
        }
        player.sendMessage("You are not in the race");
        return false;
    }

    public void addSpectator(Player p) {
        p.setScoreboard(board);
        spectators.add(p);
    }

    public void clearAll() {
        players.clear();
    }

    public int getState() {
        return state;
    }

    /**
     * Get the laps of the race.
     *
     * @return the laps as an int.
     */
    public int laps() {
        return laps;
    }

    public double prize1() {
        return prize1;
    }

    public double prize2() {
        return prize2;
    }

    public double prize3() {
        return prize3;
    }

    public long getStartTime() {
        return startTime;
    }

    /**
     * When a player leaves, withdraw them from the race.
     *
     * @param evt PlayerQuitEvent.
     */
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent evt) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayer().getName().equals(evt.getPlayer().getName())) {
                withdraw(evt.getPlayer());
                Bukkit.broadcastMessage(ChatColor.BOLD + evt.getPlayer().getName() + " has withdrawn from the race!");
                return;
            }
        }
    }

    /**
     * Check if a player is in the race.
     *
     * @param player the player to check.
     * @return true if they are in the race, false if they are not.
     */
    public boolean isPlayerInRace(Player player) {
        for (RacePlayer p : complete) {
            if (p.getPlayer().equals(player)) {
                return false;
            }
        }
        for (RacePlayer p : players) {
            if (p.getPlayer().equals(player)) {
                return true;
            }
        }
        return false;
    }
}
