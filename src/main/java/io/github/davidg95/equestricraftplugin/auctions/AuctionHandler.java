/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.auctions;

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
public class AuctionHandler implements CommandExecutor {

    private Auction auction;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only a player can use auction commands");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            return false;
        }
        if (args[0].equalsIgnoreCase("open")) {
            if (args.length < 3) {
                return false;
            }
            try {
                int startingBid = Integer.parseInt(args[1]);
                int incrementValue = Integer.parseInt(args[2]);
                if (auction != null) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "There is currently an active auction");
                    return true;
                }
                auction = new Auction(player, startingBid, incrementValue);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (args[0].equalsIgnoreCase("bid")) {
            if (auction == null) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No active auction");
                return true;
            }
            auction.placeBid(player);
            return true;
        } else if (args[0].equalsIgnoreCase("sell")) {
            if (auction == null) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No active auction");
                return true;
            }
            if (auction.getSeller().getUniqueId() != player.getUniqueId()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Only the seller can use this command");
            }
            int res = auction.sell();
            if (res == Auction.AUCTION_COMPLETE) {
                auction = null;
                Bukkit.broadcastMessage(ChatColor.RED + "---AUCTION OVER---");
            }
            return true;
        }
        return false;
    }

}
