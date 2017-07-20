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

    private final long vaccinationTime;
    private final boolean vaccination;
    private final int gender; //The horses gender.
    private final UUID uuid;
    private long lastEat;
    private long lastDrink;
    private long illSince;
    private long wellSince;
    private long lastBreed;

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

    public static final String META_DEFECATE_SINCE_EAT = "DefecateSinceEat";

    public MyHorse(int gender, boolean vaccination, long vaccinationTime, UUID uuid, long lastEat, long lastDrink, long illSince, long wellSince, long lastBreed) {
        this.gender = gender;
        this.vaccination = vaccination;
        this.vaccinationTime = vaccinationTime;
        this.uuid = uuid;
        this.lastEat = lastEat;
        this.lastDrink = lastDrink;
        this.illSince = illSince;
        this.wellSince = wellSince;
        this.lastBreed = lastBreed;

    }

    public static long getCurrentTime() {
        return new Date().getTime();
    }

    private static void setSideEffects(Horse horse, boolean set) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (set) {
//                    horse.setJumpStrength(0.2);
                    horse.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (HorseCheckerThread.SICK_LIMIT * 20), 1));
                } else {
//                    horse.setJumpStrength(0.7);
                    horse.removePotionEffect(PotionEffectType.SLOW);
                }
            }
        }.runTask(EquestriCraftPlugin.plugin);
    }

    /**
     * Set the hunger of the horse.
     *
     * @param horse the horse to apply to.
     * @param state true if they are now hungry, false if they have just eaten.
     */
    public static void setHunger(Horse horse, boolean state) {
        setLastEatChange(horse, getCurrentTime());
        horse.setMetadata(META_HUNGER, new FixedMetadataValue(EquestriCraftPlugin.plugin, state));
        if (!state) {
            horse.setMetadata(META_HUNGERTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
            horse.setMetadata(META_DEFECATE_SINCE_EAT, new FixedMetadataValue(EquestriCraftPlugin.plugin, false));
        }
        setSideEffects(horse, state);
    }

    /**
     * Set the thirst of the horse.
     *
     * @param horse the horse to apply to.
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
     * @param horse the horse to apply to.
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
     * @param horse the horse to apply to.
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
     * Get the last eat time.
     *
     * @param horse the horse to apply to.
     * @return the last eat time in ms as a Long.
     */
    private static long getLastEatTime(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_LASTEAT);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
            }
        }
        return 0;
    }

    private static long getLastThirstTime(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_THRISTTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
            }
        }
        return 0L;
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
     * @param horse the horse to apply to.
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
     * @param horse the horse to apply to.
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
     * @param horse the horse to apply to.
     * @return the last change time in ms as a Long.
     */
    public static long getIllDuration(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_ILLTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                if (md.asLong() == 0) {
                    return 0;
                }
                return getCurrentTime() - md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Get the last sickness change time.
     *
     * @param horse the horse to apply to.
     * @return the last change time in ms as a Long.
     */
    private static long getIllTime(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_ILLTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Get the total time the horse has been well.
     *
     * @param horse the horse to apply to.
     * @return the time the horse has been well as a long.
     */
    public static long getWellTime(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_WELLTIME);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
            }
        }
        return 0L;
    }

    /**
     * Kill the horse.
     *
     * @param horse the horse to apply to.
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
     *
     * @param horse the horse to apply to.
     */
    public static void defecate(Horse horse) {
        new BukkitRunnable() {
            @Override
            public void run() {
                final Block block = horse.getLocation().getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.CARPET);
                    byte b = 12;
                    block.setData(b);
                }
            }
        }.runTask(EquestriCraftPlugin.plugin);
        horse.setMetadata(META_DEFECATE_SINCE_EAT, new FixedMetadataValue(EquestriCraftPlugin.plugin, true));
    }

    public static boolean hasDefecateSinceEat(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_DEFECATE_SINCE_EAT);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asBoolean();
            }
        }
        return true;
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

    public static long getLastBreed(Horse horse) {
        final List<MetadataValue> mdvs = horse.getMetadata(META_BREED);
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
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
     * @param horse the horse to apply to.
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
     * @param horse the horse to apply to.
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

    /**
     * Check if a horse is near a mate.
     *
     * @param horse the horse.
     * @return true if they are near a mate, false if they are not.
     */
    public static boolean nearMate(Horse horse) {
        if (getGenderFromMeta(horse) == GELDING) { //If it is a gelding, return false.
            return false;
        }
        if (horse == null) {
            return false;
        }
        final List<Entity> nearby = horse.getNearbyEntities(1.5, 1.5, 1.5); //Get entires withing a 1.5 block radius.
        for (Entity e : nearby) {
            if (e.getType() == EntityType.HORSE) { //Check if the entity is a horse.
                final Horse h = (Horse) e;
                if (getGenderFromMeta(h) == GELDING) { //If it is a gelding, return false.
                    return false;
                }
                if (getGenderFromMeta(horse) != getGenderFromMeta(h)) { //If it is the opposite gender, return true.
                    return true;
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
     * @param horse the horse to apply to.
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

    /**
     * Set the gender of the horse in Metadata.
     *
     * @param ill the horses gender. Can be MyHorse.STALLION, MyHorse.MARE or
     * MyHorse.GELDING.
     * @param horse the horse to apply to.
     */
    public static void setLastIllTime(Horse horse, long ill) {
        horse.setMetadata(META_ILLTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, ill));
    }

    /**
     * Set the gender of the horse in Metadata.
     *
     * @param well the horses gender. Can be MyHorse.STALLION, MyHorse.MARE or
     * MyHorse.GELDING.
     * @param horse the horse to apply to.
     */
    public static void setLastWellTime(Horse horse, long well) {
        horse.setMetadata(META_WELLTIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, well));
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
        final long lastEat = getLastEatTime(horse);
        final long lastDrink = getLastThirstTime(horse);
        final long illSince = getIllTime(horse);
        final long wellSince = getWellTime(horse);
        final long lastBreed = getLastBreed(horse);
        final MyHorse mHorse = new MyHorse(gender, vaccination, vaccinationTime, uuid, lastEat, lastDrink, illSince, wellSince, lastBreed);
        return mHorse;
    }

    public static void myHorseToHorse(MyHorse mHorse, Horse horse) {
        MyHorse.setGenderInMeta(horse, mHorse.getGender());
        MyHorse.setHorseVaccinated(horse, mHorse.isVaccination());
        MyHorse.setHorseVaccinationTime(horse, mHorse.getVaccinationTime());
        MyHorse.setLastEatChange(horse, mHorse.getLastEat());
        MyHorse.setLastDrinkChange(horse, mHorse.getLastDrink());
        MyHorse.setLastIllTime(horse, mHorse.getIllSince());
        MyHorse.setLastWellTime(horse, mHorse.getWellSince());
        MyHorse.setLastBreed(horse, mHorse.getLastBreed());
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

    public static MyHorse initHorse(Horse h) {
        MyHorse.setGenderInMeta(h, MyHorse.generateRandomGender());
        MyHorse.setHunger(h, false);
        MyHorse.setLastEatChange(h, getCurrentTime());
        MyHorse.setThirst(h, false);
        MyHorse.setLastDrinkChange(h, getCurrentTime());
        MyHorse.setHorseSick(h, false);
        MyHorse.setLastIllTime(h, getCurrentTime());
        MyHorse.setLastWellTime(h, getCurrentTime());
        return MyHorse.horseToMyHorse(h);
    }

    public long getVaccinationTime() {
        return vaccinationTime;
    }

    public boolean isVaccination() {
        return vaccination;
    }

    public long getLastEat() {
        return lastEat;
    }

    public void setLastEat(long lastEat) {
        this.lastEat = lastEat;
    }

    public long getLastDrink() {
        return lastDrink;
    }

    public void setLastDrink(long lastDrink) {
        this.lastDrink = lastDrink;
    }

    public long getIllSince() {
        return illSince;
    }

    public void setIllSince(long illSince) {
        this.illSince = illSince;
    }

    public long getWellSince() {
        return wellSince;
    }

    public void setWellSince(long wellSince) {
        this.wellSince = wellSince;
    }

    public long getLastBreed() {
        return lastBreed;
    }

    public void setLastBreed(long lastBreed) {
        this.lastBreed = lastBreed;
    }

    @Override
    public String toString() {
        return "Horse";
    }
}
