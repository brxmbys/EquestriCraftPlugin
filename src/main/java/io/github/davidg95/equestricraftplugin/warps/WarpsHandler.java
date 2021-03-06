/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.warps;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.database.Database;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

/**
 *
 * @author David
 */
public class WarpsHandler implements CommandExecutor {

    private final EquestriCraftPlugin plugin;
    private final Database database;
    private final Permission gotoPerm = new Permission("equestricraft.pwarp.goto");
    private final Permission showPerm = new Permission("equestricraft.pwarp.show");

    public WarpsHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
        this.database = plugin.getEqDatabase();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only a player can use this command");
                return true;
            }
            if (args.length >= 2) {
                Player player = (Player) sender;
                String name = args[1];
                Location l = player.getLocation();
                Warp warp = new Warp(player, name, l);
                database.addWarp(warp);
                sender.sendMessage(ChatColor.GREEN + "Warp added");
                return true;
            }
        } else if (args[0].equalsIgnoreCase("remove")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only a player can use this command");
                return true;
            }
            if (args.length >= 2) {
                Player player = (Player) sender;
                String warp = args[1];
                database.removeWarp(player, warp);
                sender.sendMessage(ChatColor.GREEN + "Warp " + warp + " removed");
                return true;
            }
        } else if (args[0].equalsIgnoreCase("go")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only a player can use this command");
                return true;
            }
            if (args.length >= 2) {
                Player player = (Player) sender;
                String name = args[1];
                Warp warp = database.getPlayerWarp(player, name);
                if (warp == null) {
                    sender.sendMessage(ChatColor.RED + "Warp not found");
                    return true;
                }
                sender.sendMessage("Teleporting to " + warp.getName() + "...");
                player.teleport(warp.getLocation());
                return true;
            }
        } else if (args[0].equalsIgnoreCase("go-to")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
            Player player = (Player) sender;
            if (player.hasPermission(gotoPerm)) {
                if (args.length >= 3) {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(args[1]));
                    String name = args[2];
                    Warp warp = database.getPlayerWarp(p, name);
                    player.sendMessage("Teleporting to " + p.getName() + ":" + name + "...");
                    player.teleport(warp.getLocation());
                }
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to go to other players pwarps");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("show-all")) {
            if (!(sender instanceof Player)) {
                List<Warp> warps = database.getAllWarps();
                if (warps == null) {
                    sender.sendMessage("Error getting warps");
                    return true;
                }
                if (warps.isEmpty()) {
                    sender.sendMessage("No warps in database");
                    return true;
                }
                String message = "";
                for (Warp warp : warps) {
                    message += warp.toString() + "\n";
                }
                sender.sendMessage(message);
                return true;
            }
            Player player = (Player) sender;
            if (player.hasPermission(showPerm)) {
                if (args.length >= 2) {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
                    List<Warp> warps = database.getPlayerWarps(offlinePlayer);
                    String message = "Warps for " + offlinePlayer.getName() + "-\n";
                    for (Warp warp : warps) {
                        message += warp.getName() + ", ";
                    }
                    sender.sendMessage(message);
                }
            }
            List<Warp> warps = database.getPlayerWarps(player);
            if (warps.isEmpty()) {
                sender.sendMessage("You have no private warps");
                return true;
            }
            String message = "";
            for (Warp warp : warps) {
                message += warp.getName() + ", ";
            }
            sender.sendMessage(message);
            return true;
        }
        return false;
    }

}
