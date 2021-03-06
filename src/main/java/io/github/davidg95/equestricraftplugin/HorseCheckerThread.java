/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import io.github.davidg95.equestricraftplugin.database.Database;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
        this.database = plugin.getEqDatabase();
        run = true;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getLogger().log(Level.INFO, "Starting checker thread");
        while (run) {
            try {
                for (World world : Bukkit.getWorlds()) {
                    for (Horse horse : world.getEntitiesByClass(Horse.class)) {
                        MyHorse mh = database.getHorse(horse.getUniqueId());
                        if (mh == null) {
                            mh = new MyHorse(horse);
                            database.addHorse(mh);
                        }
                        if (mh.isIgnored()) {
                            continue;
                        }
                        if (mh.isTooHungry() || mh.isTooThirsty()) {
                            horse.setHealth(0);
                            continue;
                        }
                        if (mh.isSick()) {
                            if (mh.getIllDuration() >= SICK_LIMIT) {
                                horse.setHealth(0);
                                continue;
                            }
                        }
                        if (mh.getWellDuration() > ILL_WAIT) {
                            final double r = Math.random();
                            if (mh.isVaccinated()) {
                                if (r <= VACCINATED_PROBABILITY) {
                                    mh.setSick(true);
                                }
                            } else {
                                if (r <= SICK_PROBABILITY) {
                                    mh.setSick(true);
                                }
                            }
                        }
                        if (mh.getAgeInMonths() > mh.getDieAt() && mh.getDieAt() > 300) {
                            if (mh.getAgeInMonths() > 300) {
                                database.removeHorse(horse.getUniqueId());
                                horse.setHealth(0);
                                plugin.getLogger().log(Level.INFO, "A horse died at the age of " + mh.getAgeInMonths() + " months old");
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(MAIN_THREAD_INTERVAL); //Wait
            } catch (InterruptedException ex) {
                plugin.getLogger().log(Level.SEVERE, null, ex);
            }
        }
        plugin.getLogger().log(Level.INFO, "Stopping checker thread");
    }

//    private class HorseCont {
//
//        private Horse horse = null;
//        private boolean found = false;
//    }
//    public Horse getEntityByUniqueId(UUID uniqueId) {
//        final HorseCont cont = new HorseCont();
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                for (World world : Bukkit.getWorlds()) {
//                    for (Chunk chunk : world.getLoadedChunks()) {
//                        for (Entity entity : chunk.getEntities()) {
//                            if (entity.getUniqueId().equals(uniqueId)) {
//                                cont.horse = (Horse) entity;
//                                cont.found = true;
//                                return;
//                            }
//                        }
//                    }
//                }
//                cont.found = true;
//            }
//        }.runTask(plugin);
//        while (!cont.found) {
//            try {
//                Thread.sleep(50);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//        return cont.horse;
//    }
    public void setRun(boolean run) {
        this.run = run;
    }

    public class DefecateThread extends Thread {

        public DefecateThread() {
            super("Defecate_Thread");
        }

        @Override
        public void run() {
            plugin.getLogger().log(Level.INFO, "Running defecate thread every 2 minutes");
            while (run) {
                try {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (World world : Bukkit.getWorlds()) {
                                for (Horse h : world.getEntitiesByClass(Horse.class)) {
                                    MyHorse mh = database.getHorse(h.getUniqueId());
                                    if (mh == null) {
                                        continue;
                                    }
                                    if (mh.getDurationSinceLastEat() > DEFECATE_INTERVAL && !mh.hasDefecate()) {
                                        final Block block = h.getLocation().add(0, -1, 0).getBlock();
                                        if (block.getType() == Material.AIR) {
                                            block.setType(Material.CARPET);
                                            byte b = 12;
                                            block.setData(b);
                                        }
                                        database.defecateHorse(mh.getUuid());
                                    }
                                }
                            }
                        }
                    }.runTask(plugin);
                    Thread.sleep(1200000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public class BuckThread extends Thread {

        private boolean active = true;

        public BuckThread() {
            super("BUCK_THREAD");
        }

        @Override
        public void run() {
//            plugin.getLogger().log(Level.INFO, "Running Buck Thread every 3 minutes");
//            int loop = 1;
//            while (run) {
//                try {
//                    if (active) {
//                        final int currentLoop = loop;
//                        new BukkitRunnable() {
//                            @Override
//                            public void run() {
//                                int count = 0;
//                                for (World world : Bukkit.getWorlds()) {
//                                    for (Horse h : world.getEntitiesByClass(Horse.class)) {
//                                        int level = database.getHorseLevel(h.getUniqueId());
//                                        if (level == -1) {
//                                            continue;
//                                        }
//                                        if (level <= currentLoop) {
//                                            if (h.getPassenger() != null) {
//                                                Player p = (Player) h.getPassenger();
//                                                if (plugin.raceController.race != null && !plugin.raceController.race.isPlayerInRace(p)) {
//                                                    h.eject();
//                                                }
//                                            }
//                                            count++;
//                                        }
//                                    }
//                                }
//                                plugin.getLogger().log(Level.INFO, "Bucked " + count + " Level " + currentLoop + " horses");
//                            }
//                        }.runTask(plugin);
//                    }
//                    Thread.sleep(180000);
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                loop++;
//                if (loop == 11) {
//                    loop = 1;
//                }
//            }
        }

        public boolean toggle() {
            active = !active;
            return active;
        }

        public boolean isActive() {
            return active;
        }
    }
}
