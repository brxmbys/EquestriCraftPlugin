/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.race;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 *
 * @author david
 */
public class RaceTrack {

    private final String name;

    private final int x1;
    private final int x2;
    private final int z1;
    private final int z2;

    private final int cx1;
    private final int cx2;
    private final int cz1;
    private final int cz2;

    private final Location gateControl;
    
    private Race race;
    
    private final EquestriCraftPlugin plugin;

    public RaceTrack(EquestriCraftPlugin plugin, String name, int x1, int x2, int z1, int z2, int cx1, int cx2, int cz1, int cz2, Location gate) {
        this.plugin = plugin;
        this.name = name;
        this.x1 = x1;
        this.x2 = x2;
        this.z1 = z1;
        this.z2 = z2;
        this.cx1 = cx1;
        this.cx2 = cx2;
        this.cz1 = cz1;
        this.cz2 = cz2;
        this.gateControl = gate;
    }
    
    public void openRace(int laps, double p1, double p2, double p3){
        race = new Race(plugin, this, laps, p1, p2, p3);
    }
    
    public void countdown(){
        race.countdown();
    }
    
    public void start(){
        race.start();
    }
    
    public Race getRace(){
        return race;
    }
    
    public int getState(){
        if(race == null){
            return 0;
        }
        return race.getState();
    }

    public String getName() {
        return name;
    }

    public boolean checkpoint(Player player) {
        return player.getLocation().getX() >= cx1 && player.getLocation().getX() <= cx2 && player.getLocation().getZ() >= cz1 && player.getLocation().getZ() <= cz2;
    }

    public boolean finish(Player player) {
        return player.getLocation().getX() >= x1 && player.getLocation().getX() <= x2 && player.getLocation().getZ() >= z1 && player.getLocation().getZ() <= z2;
    }

    public void setGateOpen(boolean open) {
        if (open) {
            gateControl.getBlock().setType(Material.REDSTONE_BLOCK);
        } else {
            gateControl.getBlock().setType(Material.AIR);
        }
    }
}
