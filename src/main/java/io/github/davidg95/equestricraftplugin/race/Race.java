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
import org.bukkit.block.Sign;
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

    private boolean started; //Boolean indicating whether race has been started.
    private boolean finnished; //Boolean indicating whether race has been complete.

    private long startTime; //Time the race started at.

    private final int laps; //The number of laps.

    private final Economy economy;

    private final double prize1;
    private final double prize2;
    private final double prize3;

    private Scoreboard board;
    private Team team;
    private Objective objective;

    private static final Sign RACE_MONITOR;
    private static final Sign[] PLAYER_SIGNS;
    private static final Sign PODIUM_SIGN;
    private int lap;

    private final EquestriCraftPlugin plugin;

    static {
        Block b = Bukkit.getWorld("EquestriCraft").getBlockAt(-2033, 7, 11125);
        if (b.getType() != Material.SIGN) {
            b.setType(Material.SIGN);
        }
        RACE_MONITOR = (Sign) b.getState();
        PLAYER_SIGNS = new Sign[5];
        for (int i = -2032; i <= -2028; i++) {
            Block bl = Bukkit.getWorld("EquestriCraft").getBlockAt(i, 7, 11125);
            if (bl.getType() != Material.SIGN) {
                bl.setType(Material.SIGN);
            }
            PLAYER_SIGNS[i + 2032] = (Sign) bl.getState();
        }
        Block blo = Bukkit.getWorld("EquestriCraft").getBlockAt(-2034, 7, 11125);
        if (blo.getType() != Material.SIGN) {
            blo.setType(Material.SIGN);
        }
        PODIUM_SIGN = (Sign) blo.getState();
    }

    public Race(EquestriCraftPlugin plugin, int laps, double prize1, double prize2, double prize3) {
        this.laps = laps;
        this.plugin = plugin;
        players = new LinkedList<>();
        complete = new LinkedList<>();
        spectators = new LinkedList<>();
        started = false;
        finnished = false;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        economy = EquestriCraftPlugin.economy;
        this.prize1 = prize1;
        this.prize2 = prize2;
        this.prize3 = prize3;
        initScoreboard();
        RACE_MONITOR.setLine(1, "Laps: " + laps);
        RACE_MONITOR.setLine(2, "Entrants: " + players.size() + "/20");
        RACE_MONITOR.setLine(3, "Open for entries");
        RACE_MONITOR.update();
        PODIUM_SIGN.setLine(1, "1st: --------");
        PODIUM_SIGN.setLine(2, "2nd: --------");
        PODIUM_SIGN.setLine(3, "3rd: --------");
        PODIUM_SIGN.update();

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
        started = true;
        startTime = new Date().getTime();
        thread = new CheckThread(plugin, this, players);
        setGatesOpen(true);
        thread.start();
        RACE_MONITOR.setLine(1, "Laps: 1/" + laps);
        RACE_MONITOR.setLine(3, "Underway");
        RACE_MONITOR.update();
    }

    private void setGatesOpen(boolean state) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 11090; i <= 11120; i += 6) {
                    final Block b = Bukkit.getWorld("Equestricraft").getBlockAt(-2026, 1, i);
                    if (state) {
                        b.setType(Material.AIR);
                    } else {
                        b.setType(Material.REDSTONE_BLOCK);
                    }
                }
            }
        }.runTask(plugin);
    }

    protected void setLap(int lap) {
        if (lap > laps) {
            RACE_MONITOR.setLine(3, "Race complete");
        }
        RACE_MONITOR.setLine(1, "Laps: " + lap + "/" + laps);
        RACE_MONITOR.update();
        this.lap = lap;
    }

    protected int getLap() {
        return lap;
    }

    public void countdown() {
        final Runnable run = () -> {
            try {
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "5");
                RACE_MONITOR.setLine(3, "5");
                RACE_MONITOR.update();
                Thread.sleep(1000);
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "4");
                RACE_MONITOR.setLine(3, "4");
                RACE_MONITOR.update();
                Thread.sleep(1000);
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "3");
                RACE_MONITOR.setLine(3, "3");
                RACE_MONITOR.update();
                Thread.sleep(1000);
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "2");
                RACE_MONITOR.setLine(3, "2");
                RACE_MONITOR.update();
                Thread.sleep(1000);
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "1");
                RACE_MONITOR.setLine(3, "1");
                RACE_MONITOR.update();
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
        finnished = true;
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
        finnished = true;
        if (thread != null) {
            thread.stopRun();
        }
        RACE_MONITOR.setLine(1, "Laps: ---");
        RACE_MONITOR.setLine(2, "Entrants: -/20");
        RACE_MONITOR.setLine(3, "No active session");
        RACE_MONITOR.update();
        clearPlayerSigns();
        setGatesOpen(false);
        HandlerList.unregisterAll(this);
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + "RACE HAS BEEN TERMINATED!");
    }

    /**
     * Add a player to the race.
     *
     * @param p the player to add.
     * @return 1 if the player was added, 2 if the race has started and the
     * player is not added, 3 if the max player count has been reached and the
     * player is not added. 4 if the player is already in the race.
     */
    public synchronized int addPlayer(Player p) {
        if (started) {
            return 2;
        }
        if (players.size() >= 20) {
            return 3;
        }
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPlayer().getName().equals(p.getName())) {
                return 4;
            }
        }
        team.addPlayer(p);
        players.add(new RacePlayer(p, objective.getScore(p)));
        p.setScoreboard(board);
        RACE_MONITOR.setLine(2, "Entrants: " + players.size() + "/20");
        RACE_MONITOR.update();
        setPlayerSigns();
        return 1;
    }

    private void setPlayerSigns() {
        for (int i = 0; i < players.size(); i++) {
            int sign = (int) Math.floor(i / 5);
            int line = (i % 4);
            PLAYER_SIGNS[sign].setLine(line, ChatColor.stripColor(stripName(players.get(i).getPlayer())));
        }
        for (Sign s : PLAYER_SIGNS) {
            s.update();
        }
    }

    private void clearPlayerSigns() {
        for (Sign s : PLAYER_SIGNS) {
            s.setLine(0, "");
            s.setLine(1, "");
            s.setLine(2, "");
            s.setLine(3, "");
            s.update();
        }
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
        String name = stripName(p.getPlayer());
        double prize = 0;
        switch (position) {
            case 1: //1st place
                prize = prize1;
                PODIUM_SIGN.setLine(1, "1st: " + ChatColor.stripColor(name));
                break;
            case 2: //2nd place
                prize = prize2;
                PODIUM_SIGN.setLine(2, "2nd: " + ChatColor.stripColor(name));
                break;
            case 3: //3rd place
                prize = prize3;
                PODIUM_SIGN.setLine(3, "3rd: " + ChatColor.stripColor(name));
                break;
            default:
                break;
        }
        if (prize > 0) { //Check if they have won a prize
            economy.depositPlayer(p.getPlayer(), prize); //Deposit prize money
            p.getPlayer().sendMessage("You have won " + ChatColor.AQUA + "$" + new DecimalFormat("0").format(prize) + "!");
        }
        PODIUM_SIGN.update(); //Update podium sign
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
                RACE_MONITOR.setLine(2, "Entrants: " + players.size() + "/20");
                RACE_MONITOR.update();
                setPlayerSigns();
                return true;
            }
        }
        return false;
    }

    public void addSpectator(Player p) {
        p.setScoreboard(board);
        spectators.add(p);
    }

    public void clearAll() {
        players.clear();
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
