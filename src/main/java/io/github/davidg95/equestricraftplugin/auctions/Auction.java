/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.auctions;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 *
 * @author David
 */
public class Auction implements Listener {

    private final Economy econ;

    private final Player seller;
    private int currentBid;

    private final int incrementValue;

    private Player currentBidder;
    private int bidValue;

    public static int BID_PLACED = 1;
    public static int NOT_ENOUGH_MONEY = 2;
    public static int CANT_BID_ON_SELF = 3;
    public static int WAIT = 4;

    public static int AUCTION_COMPLETE = 1;
    public static int AUCTION_NOT_COMPLETE = 2;

    public static int CLOSED = -1;

    private boolean complete;

    private Scoreboard board;
    private Team team;
    private Objective objective;
    private Score score;
    
    public Auction(Player seller, int startingBid, int incrementValue) {
        this.seller = seller;
        this.currentBid = startingBid;
        this.incrementValue = incrementValue;
        bidValue = -1;
        this.econ = io.github.davidg95.equestricraftplugin.EquestriCraftPlugin.economy;
        complete = false;
        initScoreboard();
        seller.setScoreboard(board);
        Bukkit.broadcastMessage(seller.getDisplayName() + ChatColor.GREEN + " has started an auction at " + ChatColor.AQUA + "$" + startingBid + ChatColor.GREEN + "!");
        Bukkit.getServer().getPluginManager().registerEvents(this, EquestriCraftPlugin.plugin);
    }

    private void initScoreboard() {
        board = Bukkit.getScoreboardManager().getNewScoreboard();

        team = board.registerNewTeam(ChatColor.BOLD + "" + ChatColor.GREEN + "Auction");
        objective = board.registerNewObjective("Auction", "Bid");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(ChatColor.BOLD + "" + ChatColor.GREEN + "Auction - " + ChatColor.BOLD + "" + ChatColor.AQUA + seller.getName());
        score = objective.getScore("Current Bid");
        score.setScore(currentBid);
    }

    public Player getSeller() {
        return seller;
    }

    private void incrementBid(int amount) {
        this.currentBid += amount;
    }

    public synchronized int placeBid(Player p, int amount) {
        if (complete) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Auction closed!");
            return CLOSED;
        }
        if (p.getUniqueId() == seller.getUniqueId()) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You cannot place a bid on your own auction!");
            return CANT_BID_ON_SELF;
        }
        if (currentBidder != null && currentBidder.getUniqueId() == p.getUniqueId()) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must wait for someone else to place a bid!");
            return WAIT;
        }
        if (econ.getBalance(p) < currentBid) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You do not have enough money");
            return NOT_ENOUGH_MONEY;
        }
        currentBidder = p;
        if (amount == -1) {
            bidValue = currentBid;
        } else if (amount <= currentBid) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Bid must be higher than current bid");
            return NOT_ENOUGH_MONEY;
        } else {
            bidValue = amount;
            currentBid = amount;
        }
        p.sendMessage(ChatColor.GREEN + "Bid of " + ChatColor.AQUA + "$" + bidValue + ChatColor.GREEN + " placed");
        incrementBid(incrementValue);
        score.setScore(currentBid);
        p.setScoreboard(board);
        Bukkit.broadcastMessage(p.getDisplayName() + ChatColor.GREEN + " has placed a bid of " + ChatColor.AQUA + "$" + bidValue + ChatColor.GREEN + ". Next value is " + ChatColor.AQUA + "$" + currentBid);
        return BID_PLACED;
    }

    public int sell() {
        if (complete) {
            return CLOSED;
        }
        if (currentBidder == null) {
            seller.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "There has been no bids");
            return AUCTION_NOT_COMPLETE;
        }
        econ.withdrawPlayer(currentBidder, bidValue);
        currentBidder.sendMessage(ChatColor.GREEN + "You have won the bid! Withdrawing " + ChatColor.AQUA + "$" + bidValue + ChatColor.GREEN + " from your account");
        seller.sendMessage(ChatColor.GREEN + "You have had " + ChatColor.AQUA + "$" + bidValue + ChatColor.GREEN + " deposited");
        Bukkit.broadcastMessage(currentBidder.getDisplayName() + ChatColor.GREEN + " has won the bid!");
        econ.depositPlayer(seller, bidValue);
        HandlerList.unregisterAll(this);
        clearBoard();
        return AUCTION_COMPLETE;
    }

    public void end() {
        HandlerList.unregisterAll(this);
        clearBoard();
        complete = true;
        Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Auction has been stopped!");
    }

    public boolean isComplete() {
        return complete;
    }

    private void clearBoard() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboard() == board) {
                p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (currentBidder != null && event.getPlayer().getUniqueId() == currentBidder.getUniqueId()) {
            currentBidder = null;
            currentBid = -1;
            Bukkit.broadcastMessage(event.getPlayer().getDisplayName() + ChatColor.GREEN + " has left, their bid has been retracted");
        } else if (event.getPlayer().getUniqueId() == seller.getUniqueId()) {
            complete = true;
            Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "The seller has left. Auction cancelled");
            end();
        }
    }
}
