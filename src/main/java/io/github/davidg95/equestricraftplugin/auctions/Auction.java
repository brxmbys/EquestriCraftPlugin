/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.auctions;

import net.milkbowl.vault.economy.Economy;
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

    public Auction(Player seller, int startingBid, int incrementValue) {
        this.seller = seller;
        this.currentBid = startingBid;
        this.incrementValue = incrementValue;
        bidValue = -1;
        this.econ = io.github.davidg95.equestricraftplugin.EquestriCraftPlugin.economy;
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
        if (econ.getBalance(p) < currentBid) {
            return NOT_ENOUGH_MONEY;
        }
        currentBidder = p;
        bidValue = currentBid;
        incrementBid(incrementValue);
        return BID_PLACED;
    }

    public Player getCurrentBidder() {
        return currentBidder;
    }

    public int getCurrentBidderValue() {
        return this.currentBid;
    }

    public void sell() {
        if (currentBidder == null) {
            return;
        }
        econ.withdrawPlayer(currentBidder, bidValue);
        econ.depositPlayer(seller, bidValue);
    }
}
