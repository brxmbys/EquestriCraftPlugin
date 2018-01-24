/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.BuildPay;

import org.bukkit.OfflinePlayer;

/**
 *
 * @author David
 */
public class PayLog {

    private int id;
    private OfflinePlayer player;
    private int value;
    private String reason;

    public PayLog(OfflinePlayer player, int value, String reason) {
        this.player = player;
        this.value = value;
        this.reason = reason;
    }

    public PayLog(int id, OfflinePlayer player, int value, String reason) {
        this(player, value, reason);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public int getValue() {
        return value;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return this.player.getName() + " - $" + this.value + " - " + this.reason;
    }

}
