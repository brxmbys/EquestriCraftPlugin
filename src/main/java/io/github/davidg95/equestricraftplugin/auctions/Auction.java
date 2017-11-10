/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.auctions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class Auction {

    private final Economy econ;

    private final Player seller;
    private int currentBid;

    private final int incrementValue;

    private Player currentBidder;
    private int bidValue;

    public static int BID_PLACED = 1;
    public static int NOT_ENOUGH_MONEY = 2;
    public static int ERROR = 3;

    public static int AUCTION_COMPLETE = 1;
    public static int AUCTION_NOT_COMPLETE = 2;

    public Auction(Player seller, int startingBid, int incrementValue) {
        this.seller = seller;
        this.currentBid = startingBid;
        this.incrementValue = incrementValue;
        bidValue = -1;
        this.econ = io.github.davidg95.equestricraftplugin.EquestriCraftPlugin.economy;
        Bukkit.broadcastMessage(seller.getDisplayName() + ChatColor.GREEN + " has stated an auction at " + ChatColor.AQUA + "$" + startingBid + ChatColor.GREEN + "!");
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(int currentBid) {
        this.currentBid = currentBid;
    }

    public Player getSeller() {
        return seller;
    }

    public void incrementBid(int amount) {
        this.currentBid += amount;
    }

    public int placeBid(Player p) {
        if (p.getUniqueId() == seller.getUniqueId()) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You cannot place a bid on your own auction!");
            return ERROR;
        }
        if (econ.getBalance(p) < currentBid) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You do not have enough money");
            return NOT_ENOUGH_MONEY;
        }
        currentBidder = p;
        bidValue = currentBid;
        p.sendMessage(ChatColor.GREEN + "Bid of " + ChatColor.AQUA + "$" + bidValue + ChatColor.GREEN + " placed");
        incrementBid(incrementValue);
        Bukkit.broadcastMessage(p.getDisplayName() + ChatColor.GREEN + " has placed a bid of " + ChatColor.AQUA + "$" + bidValue + ChatColor.GREEN + ". Next value is " + ChatColor.AQUA + "$" + currentBid);
        return BID_PLACED;
    }

    public Player getCurrentBidder() {
        return currentBidder;
    }

    public int getCurrentBidderValue() {
        return this.currentBid;
    }

    public int sell() {
        if (currentBidder == null) {
            seller.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "There has been no bids");
            return AUCTION_NOT_COMPLETE;
        }
        econ.withdrawPlayer(currentBidder, bidValue);
        currentBidder.sendMessage(ChatColor.GREEN + "You have won the bid! Withdrawing " + ChatColor.AQUA + "$" + bidValue + ChatColor.GREEN + " from your account!");
        seller.sendMessage(ChatColor.GREEN + "You have had " + ChatColor.AQUA + "$" + bidValue + ChatColor.GREEN + " deposited");
        Bukkit.broadcastMessage(currentBidder.getDisplayName() + ChatColor.GREEN + " has won the bid!");
        econ.depositPlayer(seller, bidValue);
        return AUCTION_COMPLETE;
    }
}
