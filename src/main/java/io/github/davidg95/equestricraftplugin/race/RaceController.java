/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class RaceController implements CommandExecutor {

    private final EquestriCraftPlugin plugin;

    private final List<RaceTrack> tracks;

    public RaceController(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
        this.tracks = new LinkedList<>();
        setTracks();
    }

    private void setTracks() {
        List<String> st = (List<String>) plugin.getConfig().getList("race.tracks");
        for (String s : st) {
            int x1 = plugin.getConfig().getInt("race." + s + ".finish.x1");
            int x2 = plugin.getConfig().getInt("race." + s + ".finish.x2");
            int z1 = plugin.getConfig().getInt("race." + s + ".finish.z1");
            int z2 = plugin.getConfig().getInt("race." + s + ".finish.z2");
            int cx1 = plugin.getConfig().getInt("race." + s + ".check.x1");
            int cx2 = plugin.getConfig().getInt("race." + s + ".check.x2");
            int cz1 = plugin.getConfig().getInt("race." + s + ".check.z1");
            int cz2 = plugin.getConfig().getInt("race." + s + ".check.z2");

            int gx = plugin.getConfig().getInt("race." + s + ".gate.x");
            int gy = plugin.getConfig().getInt("race." + s + ".gate.y");
            int gz = plugin.getConfig().getInt("race." + s + ".gate.z");

            Location l = new Location(Bukkit.getWorld("Equestricraft"), gx, gy, gz);
            RaceTrack track = new RaceTrack(plugin, s, x1, x2, z1, z2, cx1, cx2, cz1, cz2, l);
            tracks.add(track);
        }
    }

    public List<RaceTrack> getTracks() {
        return tracks;
    }

    public void open(RaceTrack track, int laps, double p1, double p2, double p3) {
        track.openRace(laps, p1, p2, p3);
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "***" + laps + " lap race is now open for entries at " + track.getName() + (p1 > 0 ? ". $" + new DecimalFormat("0").format(p1) + " reward for first place!" : "") + "***");
    }

    /**
     * Begin the countdown to start a race.
     *
     * @param track the track.
     * @return if the race will start or not.
     */
    public boolean countdown(RaceTrack track) {
        Race race = track.getRace();
        if (race != null && race.getState() == Race.OPEN) {
            if (race.getPlayers().isEmpty()) {
                return false;
            }
            track.countdown();
            return true;
        }
        return false;
    }

    /**
     * Start a race.
     *
     * @param track the track.
     * @return if the race will start or not.
     */
    public boolean start(RaceTrack track) {
        Race race = track.getRace();
        if (race != null && race.getState() == Race.OPEN) {
            track.start();
            return true;
        }
        return false;
    }

    /**
     * End the race
     *
     * @param track the track.
     */
    public void end(RaceTrack track) {
        Race race = track.getRace();
        if (race != null) {
            race.terminate();
        }
    }

    /**
     * Add a player to the race.
     *
     * @param name the name of the player to add.
     * @param track the track.
     * @return if they were added or not.
     */
    public boolean addPlayer(String name, RaceTrack track) {
        Race race = track.getRace();
        Player player = Bukkit.getPlayer(name);
        if (race == null || player == null) {
            return false;
        }
        return race.addPlayer(player);
    }

    public void withdrawPlayer(String name, RaceTrack track) {
        Race race = track.getRace();
        Player player = Bukkit.getPlayer(name);
        if (race == null || player == null) {

        } else {
            race.withdraw(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (args[0].equalsIgnoreCase("open")) { //Opens a new race, with the lap count and prize money.
            if (args.length >= 6) {
                try {
                    String trackName = args[1];
                    RaceTrack track = null;
                    for (RaceTrack t : tracks) {
                        if (t.getName().equalsIgnoreCase(trackName)) {
                            track = t;
                        }
                    }
                    if (track == null) {
                        sender.sendMessage("Track not found");
                        return true;
                    }
                    int laps = Integer.parseInt(args[2]);
                    if (laps < 1) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must be 1 or greater");
                        return true;
                    }
                    double prize1 = Double.parseDouble(args[3]);
                    double prize2 = Double.parseDouble(args[4]);
                    double prize3 = Double.parseDouble(args[5]);
                    if (!sender.hasPermission("equestricraft.race.prize")) {
                        prize1 = 0;
                        prize2 = 0;
                        prize3 = 0;
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You do not have permission to set a race with a prize");
                    }
                    open(track, laps, prize1, prize2, prize3);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must specify a number value");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must specify the number of laps, and the prizes for 1st, 2nd and 3rd place");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("join")) {
            Player player;
            if (args.length >= 2) {
                RaceTrack track = getTrackyName(args[1]);
                if (track == null) {
                    sender.sendMessage("Track not found");
                    return true;
                }
                Race race = track.getRace();
                if (args.length == 3) {
                    player = Bukkit.getPlayer(args[1]);
                } else {
                    if (sender instanceof Player) {
                        player = (Player) sender;
                    } else {
                        sender.sendMessage("Must enter player name");
                        return true;
                    }
                }
                if (player == null) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Player not found");
                    return true;
                }
                if (race == null || race.getState() == Race.FINISHED) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "No active race session");
                    return true;
                }
                if (race.getState() == Race.STARTED || race.getState() == Race.STARTING) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Race already underway");
                    return true;
                }
                race.addPlayer(player);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("countdown")) {
            if (args.length > 1) {
                RaceTrack track = getTrackyName(args[1]);
                if (track == null) {
                    sender.sendMessage("Track not found");
                    return true;
                }
                Race race = track.getRace();
                if (race == null || race.getState() == Race.FINISHED) {
                    sender.sendMessage("You must open a new race first. Use /race open <laps> <prize1> <prize2> <prize3>");
                    return true;
                } else if (race.getState() == Race.STARTED || race.getState() == Race.STARTING) {
                    sender.sendMessage("There is already an active race session");
                    return true;
                } else if (race.getPlayers().isEmpty()) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There are no players in the race yet");
                    return true;
                }
                if (!countdown(track)) {
                    sender.sendMessage("No active session");
                }
            }
            return true;
        } else if (args[0].equalsIgnoreCase("start")) {
            if (args.length > 1) {
                RaceTrack track = getTrackyName(args[1]);
                if (track == null) {
                    sender.sendMessage("Track not found");
                    return true;
                }
                Race race = track.getRace();
                if (race == null || race.getState() == Race.FINISHED) {
                    sender.sendMessage("You must open a new race first. Use /race open <laps> <prize1> <prize2> <prize3>");
                    return true;
                } else if (race.getState() == Race.STARTED || race.getState() == Race.STARTING) {
                    sender.sendMessage("The current race must finish first");
                    return true;
                } else if (race.getPlayers().isEmpty()) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There are no players in the race yet");
                    return true;
                }
                if (start(track)) {
                    Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "Race has started!");
                } else {
                    sender.sendMessage("No active session");
                }
            }
            return true;
        } else if (args[0].equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command");
                return true;
            }
            if (args.length > 1) {
                RaceTrack track = getTrackyName(args[1]);
                if (track == null) {
                    sender.sendMessage("Track not found");
                    return true;
                }
                Race race = track.getRace();
                if (race == null || race.getState() == Race.FINISHED) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There is not an active race");
                    return true;
                }
                final Player player = (Player) sender;
                race.withdraw(player);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("end")) {
            if (args.length > 1) {
                RaceTrack track = getTrackyName(args[1]);
                if (track == null) {
                    sender.sendMessage("Track not found");
                    return true;
                }
                Race race = track.getRace();
                if (race != null) {
                    end(track);
                } else {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There is no active race session");
                }
            }
            return true;
        } else if (args[0].equalsIgnoreCase("list")) {
            if (args.length > 1) {
                RaceTrack track = getTrackyName(args[1]);
                if (track == null) {
                    sender.sendMessage("Track not found");
                    return true;
                }
                Race race = track.getRace();
                if (race == null || race.getState() == Race.FINISHED) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There is not an active race");
                    return true;
                }
                sender.sendMessage("Race entrants:");
                for (RacePlayer p : race.getPlayers()) {
                    sender.sendMessage("- " + p.getPlayer().getName());
                }
                sender.sendMessage("Total entrants: " + race.getPlayers().size());
            }
            return true;
        } else if (args[0].equalsIgnoreCase("clearall")) {
            if (args.length > 1) {
                RaceTrack track = getTrackyName(args[1]);
                if (track == null) {
                    sender.sendMessage("Track not found");
                    return true;
                }
                Race race = track.getRace();
                if (race == null) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You must open a race first");
                    return true;
                } else if (race.getState() == Race.STARTED || race.getState() == Race.STARTING) {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "This race is already underway");
                    return true;
                }
                race.clearAll();
                Bukkit.broadcastMessage(ChatColor.BOLD + "All race entrants have been cleared!");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("spectate")) {
            if (args.length > 1) {
                RaceTrack track = getTrackyName(args[1]);
                if (track == null) {
                    sender.sendMessage("Track not found");
                    return true;
                }
                Race race = track.getRace();
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Must be a player");
                    return true;
                }
                race.addSpectator((Player) sender);
            }
            return true;
        } else if (args[0].equalsIgnoreCase("track-list")) {
            String output = "Tracks-";
            for (RaceTrack track : tracks) {
                output += "\n" + track.getName();
            }
            sender.sendMessage(output);
            return true;
        } else {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Unknown command");
        }
        return false;
    }

    /**
     * Ends any active race.
     */
    public void cancelActiveRaces() {
        for (RaceTrack track : tracks) {
            Race race = track.getRace();
            if (race == null) {
                return;
            }
            race.terminate();
        }
    }

    public RaceTrack getTrackyName(String name) {
        for (RaceTrack t : tracks) {
            if (t.getName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

}
