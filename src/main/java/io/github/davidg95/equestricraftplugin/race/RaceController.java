/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.text.DecimalFormat;
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

    private Race race;

    public RaceController(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (args[0].equalsIgnoreCase("open")) { //Opens a enw race, with the lap count and prize money.
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
                    race = new Race(plugin, laps, prize1, prize2, prize3);
                    Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "***" + laps + " lap race is now open for entries" + (prize1 > 0 ? ". $" + new DecimalFormat("0").format(prize1) + " reward for first place!" : "") + "***");
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
            if (race == null || race.isFinnsihed()) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "No active race session");
                return true;
            }
            if (race.isStarted()) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Race already underway");
                return true;
            }
            int result = race.addPlayer(player);
            if (result == 2) {
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Race already started");
            } else if (result == 3) {
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Max players already reached.");
            } else if (result == 4) {
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are already in the race!.");
            } else {
                player.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "You are in the race!");
                Bukkit.broadcastMessage(player.getDisplayName() + " is in the race!");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("countdown")) {
            if (race == null || race.isFinnsihed()) {
                sender.sendMessage("You must open a new race first. Use /race open <laps> <prize1> <prize2> <prize3>");
                return true;
            } else if (race.isStarted()) {
                sender.sendMessage("The current race must finish first");
                return true;
            } else if (race.getPlayers().isEmpty()) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There are no players in the race yet");
                return true;
            }
            race.countdown();
            return true;
        } else if (args[0].equalsIgnoreCase("start")) {
            if (race == null || race.isFinnsihed()) {
                sender.sendMessage("You must open a new race first. Use /race open <laps> <prize1> <prize2> <prize3>");
                return true;
            } else if (race.isStarted()) {
                sender.sendMessage("The current race must finish first");
                return true;
            } else if (race.getPlayers().isEmpty()) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There are no players in the race yet");
                return true;
            }
            race.start();
            Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "Race has started!");
            return true;
        } else if (args[0].equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command");
                return true;
            }
            if (race == null || race.isFinnsihed()) {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There is not an active race");
            }
            final Player player = (Player) sender;
            if (race.withdraw(player)) {
                player.sendMessage("You have withdrawn from the race");
                Bukkit.broadcastMessage(ChatColor.BOLD + "" + ChatColor.RED + player.getName() + " has withdrawn from the race!");
            } else {
                player.sendMessage("You are not in the race");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("end")) {
            if (race != null) {
                race.terminate();
                race = null;
            } else {
                sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "There is no active race session");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("list")) {
            if (race == null || race.isFinnsihed()) {
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
            } else if (race.isStarted()) {
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
     * Checks if the sender is the console or an op.
     *
     * @param sender the sender.
     * @return true if they are console or op, false otherwise.
     */
    private boolean serverOrOp(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).isOp();
        } else {
            return true;
        }
    }

}
