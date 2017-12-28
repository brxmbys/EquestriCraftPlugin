/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import io.github.davidg95.equestricraftplugin.database.Database;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Thread which checks horses.
 *
 * @author David
 */
public class HorseCheckerThread extends Thread {

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
     * The probability of the horse bucking.
     */
    public static double BUCK_PROBABILITY = 0.05;

    /**
     * The probability of a horse breeding.
     */
    public static double BREED_PROBABILITY = 0.2;

    /**
     * The probability of an un vaccinated horse getting sick.
     */
    public static double SICK_PROBABILITY = 0.002;
    /**
     * The probability of a vaccinated horse getting sick.
     */
    public static double VACCINATED_PROBABILITY = 0.0001;

    public static long MAIN_THREAD_INTERVAL = 2000;

    private final Database database;
    private volatile boolean run;

    public static boolean SHOW_TIME = false;

    private final EquestriCraftPlugin plugin;

    public HorseCheckerThread(EquestriCraftPlugin plugin) {
        super("Horse_Checker_Thread");
        database = EquestriCraftPlugin.database;
        run = true;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getLogger().log(Level.INFO, "Starting checker thread");
        while (run) {
            final long start = new Date().getTime();
            int horsesChecked = 0;
            int nulls = 0;
            try {
                List<MyHorse> horses = database.getHorses();
                plugin.getLogger().log(Level.INFO, "Checking " + database.horseCount() + " horses...");
                for (MyHorse horse : horses) {
                    horsesChecked++;
                    if (horse == null) {
                        nulls++;
                        if (nulls % 50 == 0) {
                            plugin.getLogger().log(Level.WARNING, nulls + " null horses");
                        }
                        continue;
                    }
                    if (horse.isSick() && horse.getIllDuration() > SICK_LIMIT) { //Check if the horse has been sick for too long.
                        Horse h = getEntityByUniqueId(horse.getUuid());
                        if (h != null) {
                            h.setHealth(0);
                            database.removeHorse(horse.getUuid());
                            plugin.getLogger().log(Level.INFO, "A horse died of illness");
                        }
                    }
                    if (horse.isHungry() && horse.getHungerDuration() > SICK_LIMIT) { //Kill the horse if it has been hungry longer than the limit.
                        plugin.getLogger().log(Level.INFO, "A horse died of hunger");
                        Horse h = getEntityByUniqueId(horse.getUuid());
                        if (h != null) {
                            h.setHealth(0);
                            database.removeHorse(horse.getUuid());
                        }
                    }
                    if (horse.isThirsty() && horse.getThristDuration() > SICK_LIMIT) { //Kill the horse if it has been thirsty longer than the limit.
                        plugin.getLogger().log(Level.INFO, "A horse died of thirst");
                        Horse h = getEntityByUniqueId(horse.getUuid());
                        if (h != null) {
                            h.setHealth(0);
                            database.removeHorse(horse.getUuid());
                        }
                    }
                    if (horse.getDurationSinceLastEat() > DEFECATE_INTERVAL) { //Check if the horse needs to defecate.
                        if (!horse.hasDefecate()) {
                            horse.defecate();
                            database.saveHorse(horse);
                        }
                    }

                    if (horse.getWellDuration() > ILL_WAIT) { //Check the horse as not been ill too recently.
                        final double r = Math.random();
                        if (horse.isVaccinated()) {
                            if (r <= VACCINATED_PROBABILITY) {
                                horse.setSick(true);
                            }
                        } else {
                            if (r <= SICK_PROBABILITY) {
                                horse.setSick(true);
                            }
                        }
                        database.saveHorse(horse);
                    }

                    if (horse.getAgeInMonths() > horse.getDieAt() && horse.getDieAt() > 300) { //Check if the horse is too old.
                        if (horse.getAgeInMonths() > 300) {
                            Horse h = getEntityByUniqueId(horse.getUuid());
                            if (h != null) {
                                h.setHealth(0);
                                plugin.getLogger().log(Level.INFO, "A horse died at the age of " + horse.getAgeInMonths() + " months old");
                            }
                        }
                    }

                    int months = (int) horse.getAgeInMonths();
                    if (months >= 12 && months <= 24) {//Check if the horse can become an adult
                        Horse h = getEntityByUniqueId(horse.getUuid());
                        if (h != null) {
//                            new BukkitRunnable() {
//                                @Override
//                                public void run() {
                            h.setAdult();
//                                }
//                            }.runTask(EquestriCraftPlugin.plugin);
                        }
                    } else if (months < 12) {
                        Horse h = getEntityByUniqueId(horse.getUuid());
                        if (h != null) {
//                            new BukkitRunnable() {
//                                @Override
//                                public void run() {
                            h.setBaby();
//                                }
//                            }.runTask(EquestriCraftPlugin.plugin);
                        }
                    }
                    if (SHOW_TIME) {
                        if (horsesChecked % 500 == 0) {
                            plugin.getLogger().log(Level.INFO, "Checked " + horsesChecked + " horses");
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error", e);
            }
            final long end = new Date().getTime();
            final long time = end - start;
            double s = time / 1000D;
            if (SHOW_TIME) {
                plugin.getLogger().log(Level.INFO, "Thread run time: " + s + "s");
                plugin.getLogger().log(Level.INFO, "Horses checked: " + horsesChecked);
            }
            try {
                Thread.sleep(MAIN_THREAD_INTERVAL); //Wait
            } catch (Exception ex) {
                Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        plugin.getLogger().log(Level.INFO, "Stopping checker thread");
    }

    private class HorseCont {

        private Horse horse = null;
        private boolean found = false;
    }

    public Horse getEntityByUniqueId(UUID uniqueId) {
        final HorseCont cont = new HorseCont();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Chunk chunk : world.getLoadedChunks()) {
                        for (Entity entity : chunk.getEntities()) {
                            if (entity.getUniqueId().equals(uniqueId)) {
                                cont.horse = (Horse) entity;
                                cont.found = true;
                                return;
                            }
                        }
                    }
                }
                cont.found = true;
            }
        }.runTask(plugin);
        while (!cont.found) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return cont.horse;
    }

    public void setRun(boolean run) {
        this.run = run;
    }

    public class BuckThread extends Thread {

        public BuckThread() {

        }

        @Override
        public void run() {
            plugin.getLogger().log(Level.INFO, "Running Buck Thread every 3 minutes");
            int loop = 1;
            while (run) {
                try {
                    Thread.sleep(180000);
                    final int currentLoop = loop;
                    new BukkitRunnable() {
                        @Override
                        public void run() {

                            List<Horse> horses = new LinkedList<>();
                            List<MyHorse> myHorses = database.getHorses();
                            for (Entity e : Bukkit.getWorld("Equestricraft").getEntities()) {
                                if (e.getType() == EntityType.HORSE) {
                                    horses.add((Horse) e);
                                }
                            }

                            for (Horse h : horses) {
                                for (MyHorse mh : myHorses) {
                                    if (mh.getUuid() == h.getUniqueId()) {
                                        int level = mh.getTrainingLevel();
                                        if (level <= currentLoop) {
                                            h.eject();
                                        }
                                    }
                                }
                            }
                        }
                    }.runTask(plugin);
                } catch (InterruptedException ex) {
                    Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                loop++;
            }
        }
    }
}
