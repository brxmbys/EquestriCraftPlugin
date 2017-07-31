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

    private Thread bAndCThread;
    private Thread vacThread;

    private final List<Block> bales;
    private final List<Block> cauldrons;

    private final StampedLock baleLock;
    private final StampedLock cauldronLock;

    public static long BREED_THREAD_INTERVAL = 500;

    public static long MAIN_TRHEAD_INTERVAL = 100;

    private final DataContainer container;

    public HorseCheckerThread() {
        super("Horse_Checker_Thread");
        bales = new LinkedList<>();
        cauldrons = new LinkedList<>();
        breedThread = new BreedCheckerThread();
        baleLock = new StampedLock();
        cauldronLock = new StampedLock();
        container = DataContainer.getInstance();
    }

    private void init() {
        breedThread.start();
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    final long bstamp = baleLock.readLock();
                    try {
                        for (Block ba : bales) { //Check if any bales need removed.
                            if ((getCurrentTime() - HorseCheckerThread.this.getFirstEat(ba)) > 10800000L) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        ba.setType(Material.AIR);
                                    }
                                }.runTask(EquestriCraftPlugin.plugin);
                            }
                        }
                    } catch (Exception e) {

                    } finally {
                        baleLock.unlockRead(bstamp);
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    final long cstamp = cauldronLock.readLock();
                    try {
                        for (Block ba : cauldrons) { //Check if any cauldrons need emptied.
                            if ((getCurrentTime() - HorseCheckerThread.this.getFirstEat(ba)) > 10800000L) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        ba.setData((byte) 0);
                                    }
                                }.runTask(EquestriCraftPlugin.plugin);
                            }
                        }
                    } catch (Exception e) {

                    } finally {
                        cauldronLock.unlockRead(cstamp);
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        final Runnable vacRun = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    final long stamp = container.horseLock.writeLock();
                    try {
                        for (MyHorse horse : container.getAllHorses()) {
                            if (horse.getDurationSinceLastVaccinated() > VACCINATION_DURATION) { //Check if any vaccinations have expired.
                                horse.setVaccinated(false);
                            }
                        }
                    } catch (Exception e) {
                    } finally {
                        container.horseLock.unlockWrite(stamp);
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        bAndCThread = new Thread(run, "Bale_Cauldron_Checker");
        bAndCThread.setDaemon(true);
        bAndCThread.start();
        vacThread = new Thread(vacRun, "Vaccination_Checker");
        vacThread.setDaemon(true);
        vacThread.start();
    }

    @Override
    public void run() {
        init(); //Start the secondary threads.
        while (true) {
            final long stamp = container.horseLock.writeLock();
            try {
                for (MyHorse horse : container.getAllHorses()) {
                    final Block cauldron = MyHorse.getNearCauldron(horse); //Get the nearby cauldron if there is one.
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (cauldron != null) { //Check if they are next to a cauldron.
                                if (getFirstEat(cauldron) == -1) {
                                    getFirstEat(cauldron);
                                }
                                horse.setThirst(false);
                            }
                        }
                    }.runTask(EquestriCraftPlugin.plugin);
                    final long cstamp = cauldronLock.writeLock();
                    try {
                        cauldrons.add(cauldron);
                    } finally {
                        cauldronLock.unlockWrite(cstamp);
                    }

                    final Block bale = MyHorse.getNearHayBale(horse); //Get the nearby hay bale if there is one.
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (bale != null) { //Check if they are next to a hay bale.
                                if (getFirstEat(bale) == -1) {
                                    setFirstEat(bale);
                                }
                                horse.setHunger(false);
                            }
                        }
                    }.runTask(EquestriCraftPlugin.plugin);
                    final long bstamp = baleLock.writeLock();
                    try {
                        bales.add(bale);
                    } finally {
                        baleLock.unlockWrite(bstamp);
                    }

                    if (horse.getDurationSinceLastDrink() > DRINK_LIMIT) { //Check if the horse is thirsty.
                        horse.setThirst(true);
                    }
                    if (horse.getDurationSinceLastEat() > EAT_LIMIT) { //Check if the horse is hungry.
                        horse.setHunger(true);
                    }
                    if (horse.isSick() && horse.getIllDuration() > SICK_LIMIT) { //Check if the horse has been sick fo too long.
                        EquestriCraftPlugin.LOG.log(Level.INFO, "A horse died of illness");
                        horse.kill();
                    }
                    if (horse.isHungry() && horse.getHungerDuration() > SICK_LIMIT) { //Kill the horse if it has been hungry longer than the limit.
                        EquestriCraftPlugin.LOG.log(Level.INFO, "A horse died of hunger");
                        horse.kill();
                    }
                    if (horse.isThirsty() && horse.getThristDuration() > SICK_LIMIT) { //Kill the horse if it has been thirsty longer than the limit.
                        EquestriCraftPlugin.LOG.log(Level.INFO, "A horse died of thirst");
                        horse.kill();
                    }
                    if (horse.getDurationSinceLastEat() > DEFECATE_INTERVAL) { //Check if the horse needs to defecate.
                        if (!horse.hasDefecate()) {
                            horse.defecate();
                        }
                    }

                    if (horse.getWellDuration() > ILL_WAIT) { //Check the horse as not been ill too recently.
                        final double r = Math.random();
                        if (horse.getDurationSinceLastVaccinated() < VACCINATION_DURATION) {
                            if (r <= VACCINATED_PROBABILITY) {
                                horse.setSick(true);
                            }
                        } else {
                            if (r <= SICK_PROBABILITY) {
                                horse.setSick(true);
                            }
                        }
                    }

                    final double r = Math.random();
                    if (r <= BUCK_PROBABILITY) { //Check if a horse can buck.
                        horse.buck();
                    }
                }
            } catch (Exception e) {
            } finally {
                container.horseLock.unlockWrite(stamp);
            }
            try {
                Thread.sleep(MAIN_TRHEAD_INTERVAL); //Wait
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
                final long stamp = container.horseLock.writeLock();
                try {
                    for (MyHorse horse : container.getAllHorses()) {
                        final double r = Math.random();
                        if (r <= BREED_PROBABILITY) { //If the breed probability is met.
                            final long timeSinceLast = horse.getDurationSinceLastBreed(); //Get the time since the horse last bred.
                            if (timeSinceLast > BREED_INTERVAL) { //Check if it is greater then the breed inverval.
                                if (MyHorse.nearMate(horse)) { //Check if the horse is near a valid mate.
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            final Horse h = horse.getHorse().getWorld().spawn(horse.getHorse().getLocation(), Horse.class); //Spawn a new horse.
                                            h.setStyle(horse.getHorse().getStyle()); //Copy the parent style.
                                            horse.setLastBreed();
                                        }
                                    }.runTask(EquestriCraftPlugin.plugin);
                                }
                            }
                        }
                    }
                } catch (Exception e) {

                } finally {
                    container.horseLock.unlockWrite(stamp);
                }
                try {
                    Thread.sleep(BREED_THREAD_INTERVAL); //Wait
                } catch (InterruptedException ex) {
                    Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
