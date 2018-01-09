/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.auctions;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
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

    private final EquestriCraftPlugin plugin;

    public AuctionHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

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
                if (startingBid <= 0 || incrementValue <= 0) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must be values greater than 0");
                    return true;
                }
                if (auction == null) {
                    auction = new Auction(plugin, player, startingBid, incrementValue);
                    return true;
                }
                if (auction.isComplete()) {
                    auction = new Auction(plugin, player, startingBid, incrementValue);
                    return true;
                }
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "There is currently an active auction");
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (args[0].equalsIgnoreCase("bid")) {
            if (auction == null) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No active auction");
                return true;
            }
            try {
                if (args.length == 1) {
                    auction.placeBid(player, -1);
                } else {
                    auction.placeBid(player, Integer.parseInt(args[1]));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must enter a number");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("sell")) {
            if (auction == null) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No active auction");
                return true;
            }
            if (auction.getSeller().getUniqueId() != player.getUniqueId()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Only the seller can use this command");
                return true;
            }
            int res = auction.sell();
            if (res == Auction.AUCTION_COMPLETE) {
                auction = null;
                Bukkit.broadcastMessage(ChatColor.RED + "---AUCTION OVER---");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("end")) {
            if (auction == null) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No active auction");
                return true;
            }
            if (player.getUniqueId() != auction.getSeller().getUniqueId()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Only the seller can end the auction");
                return true;
            }
            auction.end();
            auction = null;
            return true;
        }
        return false;
    }

    /**
     * End an active auction.
     */
    public void endActiveAuction() {
        if (auction == null) {
            return;
        }
        auction.end();
    }

}
