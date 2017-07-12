/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Horse;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Thread which checks horses.
 *
 * @author David
 */
public class HorseCheckerThread extends Thread {

    /**
     * The length of time a horse can go without eating before getting sick.
     */
    public static long EAT_LIMIT = 604800000; //One week.
    /**
     * The length of time a horse can go without drinking before getting sick.
     */
    public static long DRINK_LIMIT = 604800000L; //One week.
    /**
     * The length of time a horse can be sick for before dying.
     */
    public static long SICK_LIMIT = 604800000L; //One week.
    /**
     * The length of time a horse can go after eating before it defecates.
     */
    public static long DEFECATE_INTERVAL = 7200000L; //Two Hours.
    /**
     * The length of time a horse will wait before getting ill again.
     */
    public static long ILL_WAIT = 259200000L; //Three days.

    /**
     * The length of time a vaccination will last.
     */
    public static long VACCINATION_DURATION = 2419200000L; //Four weeks.

    /**
     * The probability of the horse bucking.
     */
    public static double BUCK_PROBABILITY = 0.05;

    /**
     * The probability of a horse breeding.
     */
    public static double BREED_PROBABILITY = 0.2;

    /**
     * The amount of time a horse must wait to breed again.
     */
    public static final long BREED_INTERVAL = 86400000L; //One week.

    /**
     * The probability of an un vaccinated horse getting sick.
     */
    public static double SICK_PROBABILITY = 0.002;
    /**
     * The probability of a vaccinated horse getting sick.
     */
    public static double VACCINATED_PROBABILITY = 0.0001;

    private final BreedCheckerThread breedThread;

    private final List<Block> bales;
    private final List<Block> cauldrons;

    private final StampedLock horseLock;

    public HorseCheckerThread() {
        super("Horse_Checker_Thread");
        bales = new LinkedList<>();
        cauldrons = new LinkedList<>();
        breedThread = new BreedCheckerThread();
        horseLock = new StampedLock();
    }

    private void init() {
        breedThread.start();
    }

    @Override
    public void run() {
        init();
        while (true) {
            for (World w : Bukkit.getWorlds()) {
                final long stamp = horseLock.writeLock();
                try {
                    for (Horse horse : w.getEntitiesByClass(Horse.class)) {
                        final Block cauldron = MyHorse.getNearCauldron(horse); //Get the nearby cauldron if there is one.
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (cauldron != null) { //Check if they are next to a cauldron.
                                    if (getFirstEat(cauldron) == -1) {
                                        getFirstEat(cauldron);
                                        cauldrons.add(cauldron);
                                    }
                                    MyHorse.setThirst(horse, false);
                                }
                            }
                        }.runTask(EquestriCraftPlugin.plugin);

                        final Block bale = MyHorse.getNearHayBale(horse); //Get the nearby hay bale if there is one.
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (bale != null) { //Check if they are next to a hay bale.
                                    if (getFirstEat(bale) == -1) {
                                        setFirstEat(bale);
                                        bales.add(bale);
                                    }
                                    MyHorse.setHunger(horse, false);
                                }
                            }
                        }.runTask(EquestriCraftPlugin.plugin);

                        if (MyHorse.getDurationSinceLastDrink(horse) > DRINK_LIMIT) { //Check if the horse is thirsty.
                            MyHorse.setThirst(horse, true);
                        }
                        if (MyHorse.getDurationSinceLastEat(horse) > EAT_LIMIT) { //Check if the horse is hungry.
                            MyHorse.setHunger(horse, true);
                        }
                        if (MyHorse.isHorseSick(horse) && MyHorse.getIllDuration(horse) > SICK_LIMIT) { //Check if the horse has been sick fo too long.
                            MyHorse.kill(horse);
                        }
                        if (MyHorse.getHunger(horse) && MyHorse.getHungerDuration(horse) > SICK_LIMIT) { //Kill the horse if it has been hungry longer than the limit.
                            MyHorse.kill(horse);
                        }
                        if (MyHorse.isThirsty(horse) && MyHorse.getThirstDuration(horse) > SICK_LIMIT) { //Kill the horse if it has been thirsty longer than the limit.
                            MyHorse.kill(horse);
                        }
                        if (MyHorse.getDurationSinceLastEat(horse) > DEFECATE_INTERVAL) { //Check if the horse needs to defecate.
                            MyHorse.defecate(horse);
                        }
                        for (Block ba : bales) { //Check if any bales need removed.
                            if ((getCurrentTime() - this.getFirstEat(ba)) > 10800000L) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        ba.setType(Material.AIR);
                                    }
                                }.runTask(EquestriCraftPlugin.plugin);
                            }
                        }
                        for (Block ba : cauldrons) { //Check if any cauldrons need emptied.
                            if ((getCurrentTime() - this.getFirstEat(ba)) > 10800000L) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        ba.setData((byte) 0);
                                    }
                                }.runTask(EquestriCraftPlugin.plugin);
                            }
                        }

                        if (MyHorse.durationSinceVaccinated(horse) > VACCINATION_DURATION) { //Check if any vaccinations have expired.
                            MyHorse.removeVaccination(horse);
                        }

                        if (MyHorse.getWellDuration(horse) > ILL_WAIT) { //Check the horse as not been ill too recently.
                            final double r = Math.random();
                            if (MyHorse.durationSinceVaccinated(horse) < VACCINATION_DURATION) {
                                if (r <= VACCINATED_PROBABILITY) {
                                    MyHorse.setHorseSick(horse, true);
                                }
                            } else {
                                if (r <= SICK_PROBABILITY) {
                                    MyHorse.setHorseSick(horse, true);
                                }
                            }
                        }

                        final double r = Math.random();
                        if (r <= BUCK_PROBABILITY) { //Check if a horse can buck.
                            horse.eject();
                        }
                    }
                } catch (Exception e) {
                } finally {
                    horseLock.unlockWrite(stamp);
                }
            }
            try {
                Thread.sleep(100); //Wait 0.1s.
            } catch (InterruptedException ex) {
                Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Get the current time in ms.
     *
     * @return the current time as a long.
     */
    private long getCurrentTime() {
        return new Date().getTime();
    }

    private long getFirstEat(Block b) {
        final List<MetadataValue> mdvs = b.getMetadata("FIRSTEAT");
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
            }
        }
        return -1;
    }

    private void setFirstEat(Block b) {
        b.setMetadata("FIRSTEAT", new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
    }

    public int getEatenBales() {
        return bales.size();
    }

    public int getDrunkCauldrons() {
        return cauldrons.size();
    }

    public class BreedCheckerThread extends Thread {

        public BreedCheckerThread() {
            super("Breed_Checker_Thread");
        }

        @Override
        public void run() {
            while (true) {
                for (World w : Bukkit.getWorlds()) {
                    final long stamp = horseLock.writeLock();
                    try {
                        for (Horse horse : w.getEntitiesByClass(Horse.class)) {
                            final double r = Math.random();
                            if (r <= BREED_PROBABILITY) {
                                final long timeSinceLast = MyHorse.getDurationSinceLastBreed(horse);
                                if (timeSinceLast > BREED_INTERVAL) {
                                    if (MyHorse.nearMate(horse)) {
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                final Horse h = horse.getWorld().spawn(horse.getLocation(), Horse.class);
                                                h.setStyle(horse.getStyle());
                                                MyHorse.setLastBreed(horse, getCurrentTime());
                                            }
                                        }.runTask(EquestriCraftPlugin.plugin);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {

                    } finally {
                        horseLock.unlockWrite(stamp);
                    }
                }
                try {
                    Thread.sleep(100); //Wait 0.1s.
                } catch (InterruptedException ex) {
                    Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }
}
