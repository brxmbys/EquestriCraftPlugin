/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
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

    public static long MAIN_THREAD_INTERVAL = 2000;

    private final DataContainer container;

    private volatile boolean run;

    public static boolean SHOW_TIME = false;

    public HorseCheckerThread() {
        super("Horse_Checker_Thread");
        bales = new LinkedList<>();
        cauldrons = new LinkedList<>();
        breedThread = new BreedCheckerThread();
        baleLock = new StampedLock();
        cauldronLock = new StampedLock();
        container = DataContainer.getInstance();
        run = true;
    }

    private void init() {
        breedThread.start();
        //Create the bale and cauldron checking thread.
        final Runnable baleRun = new Runnable() {
            @Override
            public void run() {
                while (run) {
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
                    } catch (Exception ex) {
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
                    } catch (Exception ex) {
                        Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        //Create the vaccination checking thread.
        final Runnable vacRun = new Runnable() {
            @Override
            public void run() {
                while (run) {
                    try {
                        for (MyHorse horse : container.getAllHorses()) {
                            if (horse.getDurationSinceLastVaccinated() > VACCINATION_DURATION) { //Check if any vaccinations have expired.
                                horse.setVaccinated(false);
                            }
                        }
                    } catch (Exception e) {
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (Exception ex) {
                        Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };

        bAndCThread = new Thread(baleRun, "Bale_Cauldron_Checker");
        bAndCThread.setDaemon(true);
        bAndCThread.start(); //Start the able and cauldron thread.

        vacThread = new Thread(vacRun, "Vaccination_Checker");
        vacThread.setDaemon(true);
        vacThread.start(); //Start the vaccination thread.
    }

    @Override
    public void run() {
        init(); //Start the secondary threads.
        while (run) {
            final long start = new Date().getTime();
            int horsesChecked = 0;
            try {
                Iterator it = container.getAllHorses().iterator();
                while (it.hasNext()) {
                    final MyHorse horse = (MyHorse) it.next();
                    if (horse == null) {
                        continue;
                    }
                    if (horse.getHorse() == null) {
                        continue;
                    }

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            final Block cauldron = MyHorse.getNearCauldron(horse); //Get the nearby cauldron if there is one.
                            if (cauldron != null) { //Check if they are next to a cauldron.
                                if (getFirstEat(cauldron) == -1) {
                                    setFirstEat(cauldron);
                                }
                                horse.setThirst(false);
                            }
                            final Runnable caulAdd = new Runnable() {
                                @Override
                                public void run() {
                                    final long cstamp = cauldronLock.writeLock();
                                    try {
                                        cauldrons.add(cauldron);
                                    } finally {
                                        cauldronLock.unlockWrite(cstamp);
                                    }
                                }
                            };
                            new Thread(caulAdd, "Caul_Add").start();
                        }
                    }.runTask(EquestriCraftPlugin.plugin);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            final Block bale = MyHorse.getNearHayBale(horse); //Get the nearby hay bale if there is one.
                            if (bale != null) { //Check if they are next to a hay bale.
                                if (getFirstEat(bale) == -1) {
                                    setFirstEat(bale);
                                }
                                horse.setHunger(false);
                            }
                            final Runnable baleAdd = new Runnable() {
                                @Override
                                public void run() {
                                    final long bstamp = baleLock.writeLock();
                                    try {
                                        bales.add(bale);
                                    } finally {
                                        baleLock.unlockWrite(bstamp);
                                    }
                                }
                            };
                            new Thread(baleAdd, "Bale_Add").start();
                        }
                    }.runTask(EquestriCraftPlugin.plugin);

                    if (horse.getDurationSinceLastDrink() > DRINK_LIMIT) { //Check if the horse is thirsty.
                        horse.setThirst(true);
                    }
                    if (horse.getDurationSinceLastEat() > EAT_LIMIT) { //Check if the horse is hungry.
                        horse.setHunger(true);
                    }
                    if (horse.isSick() && horse.getIllDuration() > SICK_LIMIT) { //Check if the horse has been sick fo too long.
                        EquestriCraftPlugin.LOG.log(Level.INFO, "A horse died of illness");
                        horse.kill();
                        it.remove();
                    }
                    if (horse.isHungry() && horse.getHungerDuration() > SICK_LIMIT) { //Kill the horse if it has been hungry longer than the limit.
                        EquestriCraftPlugin.LOG.log(Level.INFO, "A horse died of hunger");
                        horse.kill();
                        it.remove();
                    }
                    if (horse.isThirsty() && horse.getThristDuration() > SICK_LIMIT) { //Kill the horse if it has been thirsty longer than the limit.
                        EquestriCraftPlugin.LOG.log(Level.INFO, "A horse died of thirst");
                        horse.kill();
                        it.remove();
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

                    if (horse.getAgeInMonths() > horse.getDieAt() && horse.getDieAt() > 300) { //Check if the horse is too old.
                        if (horse.getAgeInMonths() > 300) {
                            EquestriCraftPlugin.LOG.log(Level.INFO, "A horse died at the age of " + horse.getAgeInMonths() + " months old");
                            horse.kill();
                            it.remove();
                        }
                    }

                    if (horse.getBreed() == null) {
                        horse.setBreed(new HorseBreed[]{horse.getOldBreed(), horse.getOldBreed()});
                    }

                    if (horse.getAgeInMonths() >= 12) { //Check if the horse can become an adult
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                horse.getHorse().setAdult();
                            }
                        }.runTask(EquestriCraftPlugin.plugin);
                    } else {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                horse.getHorse().setBaby();
                            }
                        }.runTask(EquestriCraftPlugin.plugin);
                    }
                    horsesChecked++;
                }
            } catch (Exception e) {
                EquestriCraftPlugin.LOG.log(Level.WARNING, "Error", e);
            }
            final long end = new Date().getTime();
            final long time = end - start;
            double s = time / 1000D;
            if (SHOW_TIME) {
                EquestriCraftPlugin.LOG.log(Level.INFO, "Thread run time: " + s + "s");
                EquestriCraftPlugin.LOG.log(Level.INFO, "Horses checked: " + horsesChecked);
            }
            try {
                Thread.sleep(MAIN_THREAD_INTERVAL); //Wait
            } catch (Exception ex) {
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

    public void setRun(boolean run) {
        this.run = run;
    }

    public class BreedCheckerThread extends Thread {

        public BreedCheckerThread() {
            super("Breed_Checker_Thread");
        }

        @Override
        public void run() {
            while (run) {
                try {
                    for (int i = 0; i < container.getAllHorses().size(); i++) {
                        MyHorse horse = container.getAllHorses().get(i);
                        if (horse.getAgeInMonths() < 12) {
                            continue;
                        }
                        final double r = Math.random();
                        if (r <= BREED_PROBABILITY) { //If the breed probability is met.
                            final long timeSinceLast = horse.getDurationSinceLastBreed(); //Get the time since the horse last bred.
                            if (timeSinceLast > BREED_INTERVAL) { //Check if it is greater then the breed inverval.
                                HorseBreed br = MyHorse.nearMate(horse);
                                if (br != null) { //Check if the horse is near a valid mate.
                                    horse.setLastBreed();
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            final Horse h = horse.getHorse().getWorld().spawn(horse.getHorse().getLocation(), Horse.class); //Spawn a new horse.
                                            h.setStyle(horse.getHorse().getStyle()); //Copy the parent style.
                                            h.setMetadata("breed1", new FixedMetadataValue(EquestriCraftPlugin.plugin, horse.getBreed()[0].name()));
                                            h.setMetadata("breed2", new FixedMetadataValue(EquestriCraftPlugin.plugin, br.name()));
                                        }
                                    }.runTask(EquestriCraftPlugin.plugin);
                                }
                            }
                        }
                    }
                } catch (Exception e) {

                }
                try {
                    Thread.sleep(BREED_THREAD_INTERVAL); //Wait
                } catch (Exception ex) {
                    Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
