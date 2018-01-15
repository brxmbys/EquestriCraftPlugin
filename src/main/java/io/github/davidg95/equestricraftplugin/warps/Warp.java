/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.warps;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/**
 *
 * @author David
 */
public class Warp {

    private final OfflinePlayer player;
    private final String name;
    private final Location location;

    public Warp(OfflinePlayer player, String name, Location location) {
        this.player = player;
        this.name = name;
        this.location = location;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "Warp: " + name + " belonging to " + player.getName();
    }
}
