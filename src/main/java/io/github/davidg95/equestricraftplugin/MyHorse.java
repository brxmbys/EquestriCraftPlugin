/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author David
 */
public class MyHorse implements Serializable {

    private static final long serialVersionUID = 2573463;

    private transient Horse horse;

    private transient long lastDefecate; //The time the horse last defecated.
    private long vaccinationTime;
    private boolean vaccination;

    private int gender; //The horses gender.
    private UUID uuid;

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

    public static final String META_LASTEAT = "LastEatTime";

    public static final String META_HUNGER = "hunger";

    public static final String META_LASTDRINK = "LastDrinkTime";

    public static final String META_THIRST = "thirst";

    public static final String META_HUNGERTIME = "HungerTime";

    public static final String META_THRISTTIME = "ThirstTime";

    public static final String META_HEALTH = "sickness";

    public static final String META_ILLTIME = "IllTime";

    public static final String META_WELLTIME = "WellTime";

    public static final String META_GENDER = "gender";

    public static final String META_BREED = "LastBreed";

    public static final String META_VACCINATED = "vaccinated";

    public static final String META_VACCINE_TIME = "VaccineTime";

    public MyHorse(Horse horse) {
        horse.setMetadata(META_BREED, new FixedMetadataValue(EquestriCraftPlugin.plugin, 0));
        vaccinationTime = 0L;
        this.horse = horse;
        this.uuid = horse.getUniqueId();
        setSickness(false);
        setHunger(false);
        setThirst(false);
        gender = -1;
        horse.setBreed(false);
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

    private void setSideEffects(boolean set) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (set) {
                    horse.setJumpStrength(1);
                    horse.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (HorseCheckerThread.SICK_LIMIT * 20), 1));
                } else {
                    horse.setJumpStrength(2);
                    horse.removePotionEffect(PotionEffectType.SLOW);
                }
            }
        }.runTask(EquestriCraftPlugin.plugin);
    }

    /**
     * Set the hunger of the horse.
     *
     * @param state true if they are now hungry, false if they have just eaten.
     */
    public final void setHunger(boolean state) {
        setLastEatChange(getCurrentTime());
        boolean last = this.getHunger();
        horse.setMetadata(META_HUNGER, new FixedMetadataValue(EquestriCraftPlugin.plugin, state));
        if (!last) {
            horse.setMetadata(META_HUNGERTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
        }
        setSideEffects(state);
    }

    /**
     * Set the thirst of the horse.
     *
     * @param state true if they are now thirsty, false if they have drank.
     */
    public final void setThirst(boolean state) {
        setLastDrinkChange(getCurrentTime());
        boolean last = this.getThirst();
        horse.setMetadata(META_THIRST, new FixedMetadataValue(EquestriCraftPlugin.plugin, state));
        if (!last) {
            horse.setMetadata(META_THRISTTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
        }
        setSideEffects(state);
    }

    /**
     * Get the hunger of the horse.
     *
     * @return true if hungry, false if not.
     */
    public boolean getHunger() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_HUNGER);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return false;
    }

    public long getHungerDuration() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_HUNGERTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    public long getThirstDuration() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_THRISTTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Get the thirst of the horse.
     *
     * @return true if thirsty, false if not.
     */
    public boolean getThirst() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_THIRST);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return false;
    }

    /**
     * Get the last eat time.
     *
     * @return the last eat time in ms as a Long.
     */
    public long getDurationSinceLastEat() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_LASTEAT);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0;
    }

    /**
     * Set the last eat time.
     *
     * @param lastEat the last eat time in ms as a Long.
     */
    private void setLastEatChange(long lastEat) {
        horse.setMetadata(META_LASTEAT, new FixedMetadataValue(EquestriCraftPlugin.plugin, lastEat));
    }

    /**
     * Get the last drink time.
     *
     * @return the last drink time in ms as a Long.
     */
    public long getDurationSinceLastDrink() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_LASTDRINK);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Set the last drink time.
     *
     * @param lastDrink the last drink time in ms as a Long.
     */
    private void setLastDrinkChange(long lastDrink) {
        horse.setMetadata(META_LASTDRINK, new FixedMetadataValue(EquestriCraftPlugin.plugin, lastDrink));
    }

    /**
     * Set the sickness metadata of the horse.
     *
     * @param state true for sick, false for well.
     */
    public final void setSickness(boolean state) {
        this.horse.setMetadata(META_HEALTH, new FixedMetadataValue(EquestriCraftPlugin.plugin, state));
        setSideEffects(state);
        if (state) {
            this.horse.setMetadata(META_ILLTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
        } else {
            this.horse.setMetadata(META_WELLTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
        }
    }

    /**
     * Get the total time the horse has been well.
     *
     * @return the time the horse has been well as a long.
     */
    public long getWellDuration() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_WELLTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Get the current sickness of the horse.
     *
     * @return true if they are sick, false if they are healthy.
     */
    public boolean getSickness() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_HEALTH);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return false;
    }

    /**
     * Get the last sickness change time.
     *
     * @return the last change time in ms as a Long.
     */
    public long getIllDuration() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_ILLTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Kill the horse.
     */
    public void kill() {
        new BukkitRunnable() {
            @Override
            public void run() {
                MyHorse.this.horse.setHealth(0);
            }
        }.runTask(EquestriCraftPlugin.plugin);
    }

    /**
     * Make the horse defecate.
     */
    public void defecate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final Block block = horse.getLocation().getBlock();
                block.setType(Material.CARPET);
                byte b = 12;
                block.setData(b);
                MyHorse.this.lastDefecate = getCurrentTime();
            }
        }.runTask(EquestriCraftPlugin.plugin);
    }

    /**
     * Get the last time the horse defecated.
     *
     * @return the last time the horse defecated in ms as a Long.
     */
    public long getLastDefecate() {
        return this.lastDefecate;
    }

    public void vaccinate() {
        horse.setMetadata(META_VACCINATED, new FixedMetadataValue(EquestriCraftPlugin.plugin, true));
        horse.setMetadata(META_VACCINE_TIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
        vaccinationTime = getCurrentTime();
        vaccination = true;
    }

    public void removeVaccination() {
        horse.setMetadata(META_VACCINATED, new FixedMetadataValue(EquestriCraftPlugin.plugin, false));
        vaccination = true;
    }

    public long durationSinceVaccinated() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_VACCINE_TIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0;
    }

    public boolean isVaccinated() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_VACCINATED);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return false;
    }

    /**
     * Set the gender of the horse in Metadata.
     *
     * @param gender the horses gender. Can be MyHorse.STALLION, MyHorse.MARE or
     * MyHorse.GELDING.
     */
    public void setGender(int gender) {
        horse.setMetadata(META_GENDER, new FixedMetadataValue(EquestriCraftPlugin.plugin, gender));
        this.gender = gender;
    }

    /**
     * Get the gender of the horse in MetaData.
     *
     * @return the gender. Can be MyHorse.STALLION, MyHorse.MARE or
     * MyHorse.GELDING.
     */
    public int getGender() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_GENDER);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asInt();
            }
        }
        return -1;
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
     * Get the UUID of the horse.
     *
     * @return the horses UUID.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Set the horse object. This also updates the UUID.
     *
     * @param horse the horse.
     */
    public void setHorse(Horse horse) {
        this.horse = horse;
        this.uuid = horse.getUniqueId();
    }

    public long getLastBreed() {
        final List<MetadataValue> mdvs = this.horse.getMetadata(META_BREED);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
            }
        }
        return -1;
    }

    public void setLastBreed(long breed) {
        horse.setMetadata(META_BREED, new FixedMetadataValue(EquestriCraftPlugin.plugin, breed));
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

    public boolean nearMate() {
        final DataContainer cont = DataContainer.getInstance();
        if (getGender() == GELDING) {
            return false;
        }
        final List<Entity> nearby = horse.getNearbyEntities(1, 1, 1);
        if (!nearby.isEmpty()) {
            Bukkit.broadcastMessage("Found entities");
        } else {
            return false;
        }
        Bukkit.broadcastMessage("Starting loop");
        for (Entity e : nearby) {
            if (e.getType() == EntityType.HORSE) {
                Bukkit.broadcastMessage("Found horse");
//                final long stamp = DataContainer.getInstance().horseReadLock();
                try {
                    final Horse h = (Horse) e;
                    if (getGenderFromMeta(h) == GELDING) {
                        Bukkit.broadcastMessage("Found gelding");
                        return false;
                    }
                    if (getGender() != getGenderFromMeta(h)) {
                        Bukkit.broadcastMessage("Pair found");
                        return true;
                    }
                } finally {
//                    DataContainer.getInstance().horseReadUnlock(stamp);
                }
            }
        }
        return false;
    }

    public static int getGenderFromMeta(Horse h) {
        final List<MetadataValue> mdvs = h.getMetadata(META_GENDER);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asInt();
            }
        }
        return -1;
    }

    public static void setGenderInMeta(Horse h, int gender) {
        h.setMetadata(META_GENDER, new FixedMetadataValue(EquestriCraftPlugin.plugin, gender));
    }

    public boolean equals(Horse h) {
        return h.getEntityId() == horse.getEntityId();
    }

    /**
     * Method to generate a random gender. Either MyHorse.STALLION or
     * MyHorse.MARE.
     *
     * @return the gender.
     */
    public static int generateRandomGender() {
        final double r = Math.random();
        if (r < 0.5) {
            return STALLION;
        } else {
            return MARE;
        }
    }

    public void persist() {
        horse.setMetadata(META_VACCINATED, new FixedMetadataValue(EquestriCraftPlugin.plugin, vaccination));
        horse.setMetadata(META_VACCINE_TIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, vaccinationTime));
        horse.setMetadata(META_GENDER, new FixedMetadataValue(EquestriCraftPlugin.plugin, gender));
    }

    public long getVaccinationTime() {
        return vaccinationTime;
    }

    public boolean isVaccination() {
        return vaccination;
    }

    @Override
    public String toString() {
        return "Horse";
    }
}
