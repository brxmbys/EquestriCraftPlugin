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
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author David
 */
public class MyHorse implements Serializable {

    private static final long serialVersionUID = 1L;

    private long vaccinationTime;
    private boolean vaccination;
    private int gender; //The horses gender.
    private final UUID uuid;
    private long lastEat;
    private boolean hunger;
    private long hungerSince;
    private long lastDrink;
    private boolean thirst;
    private long thirstSince;
    private long illSince;
    private boolean ill;
    private long wellSince;
    private long lastBreed;
    private boolean defecateSinceEat;
    private HorseBreed breed;
    private long birthTime;
    private final Personality[] personality;
    private int dieat;
    private Illness illness;

    private transient Horse horse;

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

    public MyHorse(Horse h) {
        this.gender = MyHorse.generateRandomGender();
        this.vaccination = false;
        this.vaccinationTime = 0;
        this.lastEat = getCurrentTime();
        this.hunger = false;
        this.lastDrink = getCurrentTime();
        this.hunger = false;
        this.ill = false;
        this.thirst = false;
        this.thirstSince = getCurrentTime();
        this.illSince = 0;
        this.wellSince = getCurrentTime();
        this.lastBreed = getCurrentTime();
        this.defecateSinceEat = true;
        this.uuid = h.getUniqueId();
        this.breed = HorseBreed.randomType();
        if (h.getVariant() == Variant.DONKEY) {
            breed = HorseBreed.Donkey;
        } else if (h.getVariant() == Variant.MULE) {
            breed = HorseBreed.Mule;
        }
        this.birthTime = getCurrentTime();
        Personality p1 = Personality.randomType();
        Personality p2;
        while (true) {
            p2 = Personality.randomType();
            if (!p1.equals(p2)) {
                break;
            }
        }
        this.personality = new Personality[]{p1, p2};
        this.horse = h;
        this.dieat = randomDieAt();
        this.illness = null;
        h.setBaby();
        h.setAgeLock(true);
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
     * Generate a random month between the age of 25 and 35.
     *
     * @return in between 300 and 420
     */
    public static int randomDieAt() {
        double r = Math.random();
        return (int) (r * 120) + 300;
    }

    /**
     * Kill the horse.
     */
    public void kill() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (horse != null) {
                    horse.setHealth(0);
                }
            }
        }.runTask(EquestriCraftPlugin.plugin);
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
     * Checks if the horse is next to a cauldron or not.
     *
     * @param horse the horse to apply to.
     * @return the block the cauldron is in, null if there is not one nearby.
     */
    public static Block getNearCauldron(MyHorse horse) {
        if (horse == null || horse.getHorse() == null) {
            return null;
        }
        final Horse h = horse.getHorse();
        for (Block b : getNearbyBlocks(h)) {
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
    public static Block getNearHayBale(MyHorse horse) {
        if (horse == null || horse.getHorse() == null) {
            return null;
        }
        final Horse h = horse.getHorse();
        for (Block b : getNearbyBlocks(h)) {
            if (b.getType() == Material.HAY_BLOCK) {
                return b;
            }
        }
        return null;
    }

    /**
     * Get a List of all the block next to the horse.
     *
     * @param horse the horse object.
     * @return the blocks as a list.
     */
    private static List<Block> getNearbyBlocks(Horse horse) {
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
    public static boolean nearMate(MyHorse horse) {
        if (horse.getGender() == GELDING) { //If it is a gelding, return false.
            return false;
        }
        final List<Entity> nearby = horse.getHorse().getNearbyEntities(1.5, 1.5, 1.5); //Get entires withing a 1.5 block radius.
        for (Entity e : nearby) {
            if (e.getType() == EntityType.HORSE) { //Check if the entity is a horse.
                final Horse h = (Horse) e;
                final MyHorse mh = DataContainer.getInstance().getHorse(h.getUniqueId());
                if (horse.getGender() == GELDING) { //If it is a gelding, return false.
                    return false;
                }
                if (horse.getGender() != mh.getGender()) { //If it is the opposite gender, return true.
                    return true;
                }
            }
        }
        return false;
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

    public int getGender() {
        return gender;
    }

    public long getVaccinationTime() {
        return vaccinationTime;
    }

    public boolean isVaccinated() {
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

    public void setLastBreed() {
        this.lastBreed = getCurrentTime();
    }

    public Horse getHorse() {
        return horse;
    }

    /**
     * Returns the duration in ms since that last vaccination.
     *
     * @return the duration as a long.
     */
    public long getDurationSinceLastVaccinated() {
        return getCurrentTime() - this.vaccinationTime;
    }

    /**
     * Sets the vaccination state of the horse, if they are being vaccinated,
     * the time will be set.
     *
     * @param vaccination the vaccination state as a boolean.
     */
    public void setVaccinated(boolean vaccination) {
        if (vaccination) {
            this.vaccinationTime = getCurrentTime();
        }
        this.vaccination = vaccination;
    }

    /**
     * Sets the last drink time to the current time.
     *
     * @param thirst the thirst state of the horse.
     */
    public void setThirst(boolean thirst) {
        if (!thirst) {
            this.lastDrink = getCurrentTime();
        } else {
            this.thirstSince = getCurrentTime();
        }
        this.thirst = thirst;
    }

    /**
     * Get the thirst state of the horse.
     *
     * @return the thirst state as a boolean.
     */
    public boolean isThirsty() {
        return this.thirst;
    }

    /**
     * Get the duration the horse has been thirsty for in ms.
     *
     * @return the duration a long.
     */
    public long getThristDuration() {
        return getCurrentTime() - this.thirstSince;
    }

    /**
     * Sets the hunger state of the horse.
     *
     * @param hunger the hunger as a boolean.
     */
    public void setHunger(boolean hunger) {
        if (!hunger) {
            this.lastEat = getCurrentTime();
            this.defecateSinceEat = false;
        } else {
            this.hungerSince = getCurrentTime();
        }
        this.hunger = hunger;
    }

    /**
     * Check if the horse has defecated since eating.
     *
     * @return if the horse has defecated since eating.
     */
    public boolean hasDefecate() {
        return this.defecateSinceEat;
    }

    /**
     * Make the horse defecate.
     */
    public void defecate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (horse != null) {
                    final Block block = horse.getLocation().getBlock();
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.CARPET);
                        byte b = 12;
                        block.setData(b);
                    }
                }
            }
        }.runTask(EquestriCraftPlugin.plugin);
        this.defecateSinceEat = true;
    }

    /**
     * Get the hunger state of the horse.
     *
     * @return the state as a boolean.
     */
    public boolean isHungry() {
        return this.hunger;
    }

    /**
     * Get the time the horse has been hungry for in ms.
     *
     * @return the time as a long.
     */
    public long getHungerDuration() {
        return getCurrentTime() - this.hungerSince;
    }

    /**
     * Get the duration since the horse last drank.
     *
     * @return the time since the horse last drank.
     */
    public long getDurationSinceLastDrink() {
        return getCurrentTime() - this.lastDrink;
    }

    /**
     * Get the duration since the horse last ate.
     *
     * @return the time since the horse last ate.
     */
    public long getDurationSinceLastEat() {
        return getCurrentTime() - this.lastEat;
    }

    /**
     * Get the ill state of the horse.
     *
     * @return ill state as a boolean.
     */
    public boolean isSick() {
        return this.ill;
    }

    /**
     * Set the sickness of the horse.
     *
     * @param sick the sickness as a boolean.
     */
    public void setSick(boolean sick) {
        if (sick) {
            this.illSince = getCurrentTime();
            while (true) {
                this.illness = Illness.randomIllness();
                if (gender == MyHorse.MARE) {
                    if (illness != Illness.MareReproductiveLoss) {
                        break;
                    }
                } else {
                    break;
                }
            }
        } else {
            this.wellSince = getCurrentTime();
        }
        this.ill = sick;
    }

    /**
     * Get the duration the horse has been ill for is ms.
     *
     * @return the duration as a long.
     */
    public long getIllDuration() {
        return getCurrentTime() - this.illSince;
    }

    /**
     * Get the time the horse has been well for in ms.
     *
     * @return the time as a long.
     */
    public long getWellDuration() {
        return getCurrentTime() - this.wellSince;
    }

    /**
     * If there is a player on the horse, they will be removed and lose a
     * fraction of their health.
     */
    public void buck() {
        if (horse == null) {
            return;
        }
        final Player player = (Player) horse.getPassenger();
        if (player != null) {
            horse.eject();
            player.setHealth(player.getHealth() - (player.getHealthScale() / 10));
        }
    }

    /**
     * Get the time since the horse last bred in ms.
     *
     * @return the time as a long.
     */
    public long getDurationSinceLastBreed() {
        return getCurrentTime() - this.lastBreed;
    }

    /**
     * Set the gender of the horse.
     *
     * @param gender the gender.
     */
    public void setGender(int gender) {
        this.gender = gender;
    }

    /**
     * Set the horse object.
     *
     * @param h the horse object.
     */
    public void setHorse(Horse h) {
        this.horse = h;
    }

    /**
     * Get the HorseBreed.
     *
     * @return the HorseBreed.
     */
    public HorseBreed getBreed() {
        return breed;
    }

    /**
     * Set the breed of the horse.
     *
     * @param br the HorseBreed.
     */
    public void setBreed(HorseBreed br) {
        this.breed = br;
    }

    /**
     * Get the time the horse was born in ms.
     *
     * @return the time as a long.
     */
    public long getBirthTime() {
        return birthTime;
    }

    /**
     * Get the duration since the horse was born in ms.
     *
     * @return the duration as a long.
     */
    public long getAge() {
        return getCurrentTime() - birthTime;
    }

    /**
     * Set the age of the horse in ms. This method simply edits the birth time
     * of the horse.
     *
     * @param age the age in ms as a long.
     */
    public void setAge(long age) {
        birthTime = getCurrentTime() - age;
    }

    /**
     * Get the horses age in months.
     *
     * @return the age of the horse in months.
     */
    public double getAgeInMonths() {
        final double days = ((double) getAge() / 1000L / 60L / 60L / 24L);
        final double y = days / 2.5;
        return y;
    }

    /**
     * Set the age of the horse in months, this method changes the birth time.
     *
     * @param months the age in months as an int.
     */
    public void setAgeInMonths(int months) {
        double m = (double) months;
        long age = (long) (m * 2.5) * 24L * 60L * 60L * 1000L;
        setAge(age);
    }

    /**
     * Get the age the horse will die at.
     *
     * @return horse death age as an int.
     */
    public int getDieAt() {
        return dieat;
    }

    /**
     * Set the horses death age.
     *
     * @param dieat the age in months the horse will die at.
     */
    public void setDieat(int dieat) {
        this.dieat = dieat;
    }

    /**
     * Get the horses personality.
     *
     * @return the horses personality.
     */
    public Personality[] getPersonalities() {
        return personality;
    }

    /**
     * Set the horses personality.
     *
     * @param p1 the first personality.
     * @param p2 the second personality.
     */
    public void setPersonalities(Personality p1, Personality p2) {
        personality[0] = p1;
        personality[1] = p2;
    }

    /**
     * Get the horses illness.
     *
     * @return the horses illness.
     */
    public Illness getIllness() {
        return illness;
    }

    /**
     * Set the horses illness.
     *
     * @param illness the illness.
     */
    public void setIllness(Illness illness) {
        this.illness = illness;
    }

    @Override
    public String toString() {
        return "Horse";
    }
}
