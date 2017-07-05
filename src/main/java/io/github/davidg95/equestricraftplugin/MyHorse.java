/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Horse;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

/**
 *
 * @author David
 */
public class MyHorse implements Serializable {

    private final Horse horse;

    private transient long lastEat; //The time the horse last ate.
    private transient long lastDrink; //The time the horse last drank.
    private transient long lastSicknessChange; //The time the horses sickness state changed.
    private transient long lastDefecate; //The time the horse last defecated.
    private transient long lastIll; //The time the horse was healed.

    private int gender; //The horses gender.
    private final UUID uuid;

    /**
     * Indicate that the horse is well. Value = 1.
     */
    public static final int WELL = 1;
    /**
     * Indicate that the horse is sick. Value = 2.
     */
    public static final int SICK = 2;
    /**
     * Indicates that the horse is ill. Value = 3.
     */
    public static final int ILL = 3;

    /**
     * Indicates the horses gender is a stallion. Value = 1.
     */
    public static final int STALLION = 1;
    /**
     * Indicates the horses gender is a mare. Value = 2.
     */
    public static final int MARE = 2;
    /**
     * Indicates the horses gender is a gelding. Value = 3.
     */
    public static final int GELDING = 3;

    public MyHorse(Horse horse) {
        this.horse = horse;
        this.uuid = horse.getUniqueId();
        setSickness(WELL);
        lastEat = getCurrentTime();
        lastDrink = getCurrentTime();
        lastDefecate = getCurrentTime();
    }

    private long getCurrentTime() {
        return new Date().getTime();
    }

    /**
     * Gets the horse instance.
     *
     * @return the Horse.
     */
    public Horse getHorse() {
        return horse;
    }

    /**
     * Get the last eat time.
     *
     * @return the last eat time in ms as a Long.
     */
    public long getLastEat() {
        return lastEat;
    }

    /**
     * Set the last eat time.
     *
     * @param lastEat the last eat time in ms as a Long.
     */
    public void setLastEat(long lastEat) {
        this.lastEat = lastEat;
    }

    /**
     * Get the last drink time.
     *
     * @return the last drink time in ms as a Long.
     */
    public long getLastDrink() {
        return lastDrink;
    }

    /**
     * Set the last drink time.
     *
     * @param lastDrink the last drink time in ms as a Long.
     */
    public void setLastDrink(long lastDrink) {
        this.lastDrink = lastDrink;
    }

    /**
     * Set the sickness metadata of the horse.
     *
     * @param sickness the sickness. Can be either MyHorse.WELL or MyHorse.SICK.
     */
    public final void setSickness(int sickness) {
        this.horse.setMetadata("SICKNESS", new FixedMetadataValue(EquestriCraftPlugin.plugin, sickness));
        this.lastSicknessChange = new Date().getTime();
        switch (sickness) {
            case WELL:
                horse.setJumpStrength(2);
                break;
            case SICK:
                horse.setJumpStrength(0);
                break;
            default:
                horse.setJumpStrength(1);
                break;
        }
    }

    /**
     * Get the current sickness of the horse.
     *
     * @return MyHorse.SICK or MyHorse.WELL.
     */
    public int getSickness() {
        final List<MetadataValue> mdvs = this.horse.getMetadata("SICKNESS");
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asInt();
            }
        }
        return 0;
    }

    /**
     * Get the last sickness change time.
     *
     * @return the last change time in ms as a Long.
     */
    public long getLastSicknessChange() {
        return this.lastSicknessChange;
    }

    /**
     * Kill the horse.
     */
    public void kill() {
        this.horse.setHealth(0);
    }

    /**
     * Make the horse defecate.
     */
    public void defecate() {
        final Block block = horse.getLocation().getBlock();
        block.setType(Material.CARPET);
        byte b = 12;
        block.setData(b);
        this.lastDefecate = getCurrentTime();
    }

    /**
     * Get the last time the horse defecated.
     *
     * @return the last time the horse defecated in ms as a Long.
     */
    public long getLastDefecate() {
        return this.lastDefecate;
    }

    /**
     * Set the time the horse was last ill.
     *
     * @param lastIll the time they were last ill as a long.
     */
    public void setLastIll(long lastIll) {
        this.lastIll = lastIll;
    }

    /**
     * Get the time they were last ill.
     *
     * @return the time they were last ill as a long.
     */
    public long getLastIll() {
        return this.lastIll;
    }

    /**
     * Set the gender of the horse in Metadata.
     *
     * @param gender the horses gender. Can be MyHorse.STALLION, MyHorse.MARE or
     * MyHorse.GELDING.
     */
    public void setGender(int gender) {
        horse.setMetadata("gender", new FixedMetadataValue(EquestriCraftPlugin.plugin, gender));
        this.gender = gender;
    }

    /**
     * Get the gender of the horse in MetaData.
     *
     * @return the gender. Can be MyHorse.STALLION, MyHorse.MARE or
     * MyHorse.GELDING.
     */
    public int getGender() {
        return gender;
    }

    /**
     * Check if the horse is dead.
     *
     * @return true if the horse is dead, false if they are alive.
     */
    public boolean isDead() {
        return this.horse.isDead();
    }

    /**
     * Checks if the horse is next to a cauldron or not.
     *
     * @return the block the cauldron is in, null if there is not one nearby.
     */
    public Block getNearCauldron() {
        for (Block b : getNearby()) {
            if (b.getType() == Material.CAULDRON) {
                return b;
            }
        }
        return null;
    }

    /**
     * Checks if the horse is next to a hay bale.
     *
     * @return the block the hay bale is in, null if there is not one nearby.
     */
    public Block getNearHayBale() {
        for (Block b : getNearby()) {
            if (b.getType() == Material.HAY_BLOCK) {
                return b;
            }
        }
        return null;
    }

    /**
     * Get a List of all the block next to the horse.
     *
     * @return the blocks as a list.
     */
    private List<Block> getNearby() {
        final Location location = horse.getLocation();

        final List<Block> nearby = new ArrayList<>();
        nearby.add(location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ() - 1));
        nearby.add(location.getWorld().getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ() + 1));
        nearby.add(location.getWorld().getBlockAt(location.getBlockX() - 1, location.getBlockY(), location.getBlockZ()));
        nearby.add(location.getWorld().getBlockAt(location.getBlockX() + 1, location.getBlockY(), location.getBlockZ()));
        nearby.add(location.getWorld().getBlockAt(location.getBlockX() - 1, location.getBlockY(), location.getBlockZ() - 1));
        nearby.add(location.getWorld().getBlockAt(location.getBlockX() + 1, location.getBlockY(), location.getBlockZ() - 1));
        nearby.add(location.getWorld().getBlockAt(location.getBlockX() - 1, location.getBlockY(), location.getBlockZ() + 1));
        nearby.add(location.getWorld().getBlockAt(location.getBlockX() + 1, location.getBlockY(), location.getBlockZ() + 1));

        return nearby;
    }

    public boolean equals(Horse h) {
        return h.getEntityId() == horse.getEntityId();
    }

}
