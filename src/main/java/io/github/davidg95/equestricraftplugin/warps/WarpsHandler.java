/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.warps;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.database.Database;
import java.util.List;
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
public class WarpsHandler implements CommandExecutor {

    private final EquestriCraftPlugin plugin;
    private final Database database;

    public WarpsHandler(EquestriCraftPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (!(sender instanceof Player)) {
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
        } else if (args[0].equalsIgnoreCase("go")) {
            if (!(sender instanceof Player)) {
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
        } else if (args[0].equalsIgnoreCase("show-all")) {
            if (!(sender instanceof Player)) {
                return true;
            }
            Player player = (Player) sender;
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
