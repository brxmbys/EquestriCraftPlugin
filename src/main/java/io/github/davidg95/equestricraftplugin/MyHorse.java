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

    private transient long lastDefecate; //The time the horse last defecated.

    private final long vaccinationTime;
    private final boolean vaccination;
    private final int gender; //The horses gender.
    private final UUID uuid;

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

    public MyHorse(int gender, boolean vaccination, long vaccinationTime, UUID uuid) {
        this.gender = gender;
        this.vaccination = vaccination;
        this.vaccinationTime = vaccinationTime;
        this.uuid = uuid;
    }

    private static long getCurrentTime() {
        return new Date().getTime();
    }

    private static void setSideEffects(Horse horse, boolean set) {
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
    public static void setHunger(Horse horse, boolean state) {
        setLastEatChange(horse, getCurrentTime());
        horse.setMetadata(META_HUNGER, new FixedMetadataValue(EquestriCraftPlugin.plugin, state));
        if (!state) {
            horse.setMetadata(META_HUNGERTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
        }
        setSideEffects(horse, state);
    }

    /**
     * Set the thirst of the horse.
     *
     * @param state true if they are now thirsty, false if they have drank.
     */
    public static void setThirst(Horse horse, boolean state) {
        setLastDrinkChange(horse, getCurrentTime());
        horse.setMetadata(META_THIRST, new FixedMetadataValue(EquestriCraftPlugin.plugin, state));
        if (!state) {
            horse.setMetadata(META_THRISTTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
        }
        setSideEffects(horse, state);
    }

    /**
     * Get the hunger of the horse.
     *
     * @return true if hungry, false if not.
     */
    public static boolean getHunger(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_HUNGER);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return false;
    }

    public static long getHungerDuration(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_HUNGERTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    public static boolean isThirsty(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_THIRST);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return false;
    }

    public static long getThirstDuration(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_THRISTTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Get the last eat time.
     *
     * @return the last eat time in ms as a Long.
     */
    public static long getDurationSinceLastEat(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_LASTEAT);
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
    private static void setLastEatChange(Horse horse, long lastEat) {
        horse.setMetadata(META_LASTEAT, new FixedMetadataValue(EquestriCraftPlugin.plugin, lastEat));
    }

    /**
     * Get the last drink time.
     *
     * @return the last drink time in ms as a Long.
     */
    public static long getDurationSinceLastDrink(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_LASTDRINK);
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
    private static void setLastDrinkChange(Horse horse, long lastDrink) {
        horse.setMetadata(META_LASTDRINK, new FixedMetadataValue(EquestriCraftPlugin.plugin, lastDrink));
    }

    /**
     * Get the total time the horse has been well.
     *
     * @return the time the horse has been well as a long.
     */
    public static long getWellDuration(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_WELLTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Get the last sickness change time.
     *
     * @return the last change time in ms as a Long.
     */
    public static long getIllDuration(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_ILLTIME);
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
    public static void kill(Horse horse) {
        new BukkitRunnable() {
            @Override
            public void run() {
                horse.setHealth(0);
            }
        }.runTask(EquestriCraftPlugin.plugin);
    }

    /**
     * Make the horse defecate.
     */
    public static void defecate(Horse horse) {
        new BukkitRunnable() {
            @Override
            public void run() {
                final Block block = horse.getLocation().getBlock();
                block.setType(Material.CARPET);
                byte b = 12;
                block.setData(b);
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

    public static void vaccinate(Horse horse) {
        horse.setMetadata(META_VACCINATED, new FixedMetadataValue(EquestriCraftPlugin.plugin, true));
        horse.setMetadata(META_VACCINE_TIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
    }

    public static void removeVaccination(Horse horse) {
        horse.setMetadata(META_VACCINATED, new FixedMetadataValue(EquestriCraftPlugin.plugin, false));
    }

    public static long durationSinceVaccinated(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_VACCINE_TIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return 0;
    }

    /**
     * Get the UUID of the horse.
     *
     * @return the horses UUID.
     */
    public UUID getUuid() {
        return uuid;
    }

    public static long getDurationSinceLastBreed(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_BREED);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return getCurrentTime() - md.asLong();
            }
        }
        return -1;
    }

    public static void setLastBreed(Horse horse, long breed) {
        horse.setMetadata(META_BREED, new FixedMetadataValue(EquestriCraftPlugin.plugin, breed));
    }

    /**
     * Checks if the horse is next to a cauldron or not.
     *
     * @return the block the cauldron is in, null if there is not one nearby.
     */
    public static Block getNearCauldron(Horse horse) {
        for (Block b : getNearby(horse)) {
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
    public static Block getNearHayBale(Horse horse) {
        for (Block b : getNearby(horse)) {
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
    private static List<Block> getNearby(Horse horse) {
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

    public static boolean nearMate(Horse horse) {
        if (getGenderFromMeta(horse) == GELDING) {
            return false;
        }
        if(horse == null){
            return false;
        }
        final List<Entity> nearby = horse.getNearbyEntities(1.5, 1.5, 1.5);
        for (Entity e : nearby) {
            if (e.getType() == EntityType.HORSE) {
                try {
                    final Horse h = (Horse) e;
                    if (getGenderFromMeta(h) == GELDING) {
                        return false;
                    }
                    if (getGenderFromMeta(horse) != getGenderFromMeta(h)) {
                        return true;
                    }
                } finally {
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

    /**
     * Set the gender of the horse in Metadata.
     *
     * @param gender the horses gender. Can be MyHorse.STALLION, MyHorse.MARE or
     * MyHorse.GELDING.
     */
    public static void setHorseGender(int gender, Horse horse) {
        horse.setMetadata(META_GENDER, new FixedMetadataValue(EquestriCraftPlugin.plugin, gender));
    }

    public static int getHorseGender(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_GENDER);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asInt();
            }
        }
        return -1;
    }

    public static boolean getHorseVaccination(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_VACCINATED);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return false;
    }

    public static long getHorseVaccinationTime(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_VACCINE_TIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
            }
        }
        return -1;
    }

    public static void setHorseVaccinated(Horse horse, boolean vaccinated) {
        horse.setMetadata(META_VACCINATED, new FixedMetadataValue(EquestriCraftPlugin.plugin, vaccinated));
    }

    public static void setHorseVaccinationTime(Horse horse, long time) {
        horse.setMetadata(META_VACCINE_TIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, time));
    }

    public static MyHorse horseToMyHorse(Horse horse) {
        final int gender = getHorseGender(horse);
        final boolean vaccination = getHorseVaccination(horse);
        final long vaccinationTime = getHorseVaccinationTime(horse);
        final UUID uuid = horse.getUniqueId();
        final MyHorse mHorse = new MyHorse(gender, vaccination, vaccinationTime, uuid);
        return mHorse;
    }

    public static void myHorseToHorse(MyHorse mHorse, Horse horse) {
        MyHorse.setGenderInMeta(horse, mHorse.getGender());
        MyHorse.setHorseVaccinated(horse, mHorse.isVaccination());
        MyHorse.setHorseVaccinationTime(horse, mHorse.getVaccinationTime());
    }

    public int getGender() {
        return gender;
    }

    public static boolean isHorseSick(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_HEALTH);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return false;
    }

    public static void setHorseSick(Horse horse, boolean sick) {
        horse.setMetadata(META_HEALTH, new FixedMetadataValue(EquestriCraftPlugin.plugin, sick));
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
