/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.buildpay;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.database.Database;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class BuildPayHandler implements CommandExecutor {

    private final EquestriCraftPlugin plugin;
    private final Database database;
    private final Economy economy;

    public BuildPayHandler(EquestriCraftPlugin plugin, Database database, Economy economy) {
        this.plugin = plugin;
        this.database = database;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("log")) {
                if (args.length >= 2) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(args[1]));
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found");
                        return true;
                    }
                    List<PayLog> logs = database.getPayLogs(player);
                    if (logs.isEmpty()) {
                        sender.sendMessage("Player has no builds");
                        return true;
                    }
                    String output = "Pay Log for " + player.getName() + "-\n";
                    for (PayLog log : logs) {
                        output += "$" + log.getValue() + " - " + log.getReason();
                    }
                    sender.sendMessage(output);
                    return true;
                } else {
                    List<PayLog> logs = database.getPayLogs(null);
                    if (logs.isEmpty()) {
                        sender.sendMessage("No builds");
                        return true;
                    }
                    String output = "Pay Log for all players-\n";
                    for (PayLog log : logs) {
                        output += log.toString();
                    }
                    sender.sendMessage(output);
                    return true;
                }
            }
        }
        if (args.length >= 2) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                int value = Integer.parseInt(args[0]);
                String reason = args[1];
                PayLog log = new PayLog(player, value, reason);
                if (economy.getBalance(player) < value) {
                    player.sendMessage(ChatColor.RED + "You do not have enough funds");
                    return true;
                }
                economy.withdrawPlayer(player, value);
                database.addPayLog(log);
                player.sendMessage(ChatColor.GREEN + "You have now paid for this build");
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can use this command");
            }
            return true;
        }
        return false;
    }

}
