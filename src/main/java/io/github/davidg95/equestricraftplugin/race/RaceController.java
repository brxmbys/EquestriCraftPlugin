/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.text.DecimalFormat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    public Race race;

    private final Economy economy;

    public RaceController(EquestriCraftPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void open(int laps, double p1, double p2, double p3) {
        race = new Race(plugin, economy, laps, p1, p2, p3);
        Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "***" + laps + " lap race is now open for entries" + (p1 > 0 ? ". $" + new DecimalFormat("0").format(p1) + " reward for first place!" : "") + "***");
    }

    /**
     * Begin the countdown to start a race.
     *
     * @return if the race will start or not.
     */
    public boolean countdown() {
        if (race == null || race.getState() == Race.OPEN) {
            if(race.getPlayers().isEmpty()){
                return false;
            }
            race.countdown();
            return true;
        }
        return false;
    }

    /**
     * Start a race.
     *
     * @return if the race will start or not.
     */
    public boolean start() {
        if (race == null || race.getState() == Race.OPEN) {
            race.start();
            return true;
        }
        return false;
    }

    /**
     * End the race
     */
    public void end() {
        if (race != null) {
            race.terminate();
        }
    }

    /**
     * Add a player to the race.
     *
     * @param name the name of the player to add.
     * @return if they were added or not.
     */
    public boolean addPlayer(String name) {
        Player player = Bukkit.getPlayer(name);
        if (race == null || player == null) {
            return false;
        }
        return race.addPlayer(player);
    }

    public void withdrawPlayer(String name) {
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
            if (args.length >= 5) {
                try {
                    int laps = Integer.parseInt(args[1]);
                    if (laps < 1) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must be 1 or greater");
                        return true;
                    }
                    double prize1 = Double.parseDouble(args[2]);
                    double prize2 = Double.parseDouble(args[3]);
                    double prize3 = Double.parseDouble(args[4]);
                    if (!sender.hasPermission("equestricraft.race.prize")) {
                        prize1 = 0;
                        prize2 = 0;
                        prize3 = 0;
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You do not have permission to set a race with a prize");
                    }
                    open(laps, prize1, prize2, prize3);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must specify a number value");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must specify the number of laps, and the prizes for 1st, 2nd and 3rd place");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("join")) {
            Player player;
            if (args.length == 2) {
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
            return true;
        } else if (args[0].equalsIgnoreCase("countdown")) {
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
            countdown();
            return true;
        } else if (args[0].equalsIgnoreCase("start")) {
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
            start();
            Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "Race has started!");
            return true;
        } else if (args[0].equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command");
                return true;
            }
            if (race == null || race.getState() == Race.FINISHED) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There is not an active race");
            }
            final Player player = (Player) sender;
            race.withdraw(player);
            return true;
        } else if (args[0].equalsIgnoreCase("end")) {
            if (race != null) {
                end();
            } else {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There is no active race session");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("list")) {
            if (race == null || race.getState() == Race.FINISHED) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There is not an active race");
                return true;
            }
            sender.sendMessage("Race entrants:");
            for (RacePlayer p : race.getPlayers()) {
                sender.sendMessage("- " + p.getPlayer().getName());
            }
            sender.sendMessage("Total entrants: " + race.getPlayers().size());
            return true;
        } else if (args[0].equalsIgnoreCase("clearall")) {
            if (race == null) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You must open a race first");
                return true;
            } else if (race.getState() == Race.STARTED || race.getState() == Race.STARTING) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "This race is already underway");
                return true;
            }
            race.clearAll();
            Bukkit.broadcastMessage(ChatColor.BOLD + "All race entrants have been cleared!");
            return true;
        } else if (args[0].equalsIgnoreCase("spectate")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Must be a player");
                return true;
            }
            race.addSpectator((Player) sender);
            return true;
        } else {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Unknown command");
        }
        return false;
    }

    /**
     * Ends any active race.
     */
    public void cancelActiveRace() {
        if (race == null) {
            return;
        }
        race.terminate();
    }

}
