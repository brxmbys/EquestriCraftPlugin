/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class DataContainer {

    private static DataContainer container;

    private final List<MyHorse> horses;
    private final StampedLock horseLock;

    private List<UUID> doctors;
    private final StampedLock doctorLock;

    private Thread saveThread;

    public static final String HORSES_FILE = "horses.config";

    private final StampedLock fileLock;

    private DataContainer() {
        horses = new LinkedList<>();
        doctors = new LinkedList<>();
        horseLock = new StampedLock();
        doctorLock = new StampedLock();
        fileLock = new StampedLock();
        loadHorses();
        initThread();
    }

    /**
     * Cache a horse when its chunk is getting unloaded.
     *
     * @param horse the MyHorse to cache.
     */
    public void cacheHorse(MyHorse horse) {
        final long stamp = horseLock.writeLock();
        try {
            horses.add(horse);
        } finally {
            horseLock.unlockWrite(stamp);
        }
    }

    /**
     * Check if a horse is in the cache by its UUID.
     *
     * @param uuid the UUID to check.
     * @return the true if it is in the cache, false if it is not.
     */
    public boolean isHorseInCache(UUID uuid) {
        final long stamp = horseLock.readLock();
        try {
            for (MyHorse mh : horses) {
                if (mh.getUuid().equals(uuid)) {
                    return true;
                }
            }
        } finally {
            horseLock.unlockRead(stamp);
        }
        return false;
    }

    /**
     * Remove a horse from the cache by its UUID.
     *
     * @param uuid the UUID of the horse to remove.
     */
    public void removeHorseFromCache(UUID uuid) {
        final long stamp = horseLock.writeLock();
        try {
            for (int i = 0; i < horses.size(); i++) {
                if (horses.get(i).getUuid().equals(uuid)) {
                    horses.remove(i);
                    return;
                }
            }
        } finally {
            horseLock.unlockWrite(stamp);
        }
    }

    private void initThread() {
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                Bukkit.getLogger().log(Level.INFO, "horses.config will be saved every 10 minutes");
                while (true) {
                    try {
                        Thread.sleep(600000); //Ten Minutes
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DataContainer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    saveHorses();
                }
            }

        };
        saveThread = new Thread(run, "Horse_Save_Thread");
        saveThread.setDaemon(true);
        saveThread.start();
    }

    private void pairHorses(List<MyHorse> horses) {
        Bukkit.getLogger().log(Level.INFO, "Loading horses");
        Bukkit.getLogger().log(Level.INFO, "Horses: " + horses.size());
        int horsesInFile = 0;
        int horsesFound = 0;

        int geld = 0;
        int stal = 0;
        int mare = 0;
        int none = 0;
        try {
            for (MyHorse horse : horses) {
                switch (horse.getGender()) {
                    case MyHorse.GELDING:
                        geld++;
                        break;
                    case MyHorse.MARE:
                        mare++;
                        break;
                    case MyHorse.STALLION:
                        stal++;
                        break;
                    default:
                        none++;
                        break;
                }
                horsesInFile++;
                if (horsesInFile % 500 == 0) {
                    Bukkit.getLogger().log(Level.INFO, "Scanned: " + horsesInFile);
                }
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity.getUniqueId().equals(horse.getUuid())) {
                            MyHorse.myHorseToHorse(horse, (Horse) entity);
                            horsesFound++;
                            break;
                        }
                    }
                }
            }
        } finally {
        }
        Bukkit.getLogger().log(Level.INFO, "Load complete");
        Bukkit.getLogger().log(Level.INFO, "Number of horses in file: " + horsesInFile);
        Bukkit.getLogger().log(Level.INFO, "Number of horses found in world: " + horsesFound);
        Bukkit.getLogger().log(Level.INFO, "Stallions: " + stal);
        Bukkit.getLogger().log(Level.INFO, "Mares: " + mare);
        Bukkit.getLogger().log(Level.INFO, "Geldings: " + geld);
        Bukkit.getLogger().log(Level.INFO, "None assigned: " + none);
    }

    /**
     * Returns an instance of the data container.
     *
     * @return the DataContainer.
     */
    public static DataContainer getInstance() {
        if (container == null) {
            container = new DataContainer();
        }
        return container;
    }

    /**
     * Add a new horse.
     *
     * @param p the Doctor to add.
     */
    public void addDoctor(Player p) {
        final long stamp = doctorLock.writeLock();
        try {
            doctors.add(p.getUniqueId());
        } finally {
            doctorLock.unlockWrite(stamp);
        }
    }

    /**
     * Get the MyHorse object which contains the given Horse object.
     *
     * @param p the player to check.
     * @return the yHorse object which contains the horse. Null if it doesn't
     * exists.
     */
    public boolean isDoctor(Player p) {
        final long stamp = doctorLock.readLock();
        try {
            for (UUID u : doctors) {
                if (u.equals(p.getUniqueId())) {
                    return true;
                }
            }
        } finally {
            doctorLock.unlockRead(stamp);
        }
        return false;
    }

    private List<MyHorse> getHorses() {
        final List<MyHorse> mHorses = new LinkedList<>();
        int geld = 0;
        int stal = 0;
        int mare = 0;
        int none = 0;
        for (World w : Bukkit.getWorlds()) {
            final long stamp = HorseCheckerThread.horseLock.readLock();
            try {
                for (Horse h : w.getEntitiesByClass(Horse.class)) {
                    final MyHorse mHorse = MyHorse.horseToMyHorse(h);
                    switch (MyHorse.getGenderFromMeta(h)) {
                        case MyHorse.GELDING:
                            geld++;
                            break;
                        case MyHorse.MARE:
                            mare++;
                            break;
                        case MyHorse.STALLION:
                            stal++;
                            break;
                        default:
                            none++;
                            break;
                    }
                    mHorses.add(mHorse);
                }
            } finally {
                HorseCheckerThread.horseLock.unlockRead(stamp);
            }
        }
        mHorses.addAll(horses);
        Bukkit.getLogger().log(Level.INFO, "Stallions: " + stal);
        Bukkit.getLogger().log(Level.INFO, "Mares: " + mare);
        Bukkit.getLogger().log(Level.INFO, "Geldings: " + geld);
        Bukkit.getLogger().log(Level.INFO, "None assigned: " + none);
        return mHorses;
    }

    /**
     * Persist horses to file.
     */
    public void saveHorses() {
        final long stamp = fileLock.writeLock();
        try {
            Bukkit.getLogger().log(Level.INFO, "Saving horses...");
            final File file = new File(HORSES_FILE);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException ex) {
                    EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
                }
            }
            try (OutputStream os = new FileOutputStream(HORSES_FILE)) {
                final long hStamp = horseLock.readLock();
                try {
                    final List<MyHorse> horses = getHorses();
                    final ObjectOutputStream oo = new ObjectOutputStream(os);
                    oo.writeObject(horses);
                    oo.writeObject(doctors);
                } finally {
                    horseLock.unlockRead(hStamp);
                }
            } catch (FileNotFoundException ex) {
                EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
            }
        } finally {
            fileLock.unlockWrite(stamp);
        }
        Bukkit.getLogger().log(Level.INFO, "Save complete...");
    }

    /**
     * Load horses from file
     */
    private void loadHorses() {
        final long stamp = fileLock.readLock();
        try (InputStream is = new FileInputStream(HORSES_FILE)) {
            final ObjectInputStream oi = new ObjectInputStream(is);
            final List<MyHorse> horses = (List<MyHorse>) oi.readObject();
            Bukkit.getLogger().log(Level.INFO, "Horses loaded: " + horses.size());
            pairHorses(horses);
            doctors = (List<UUID>) oi.readObject();
        } catch (FileNotFoundException ex) {
            saveHorses();
        } catch (IOException | ClassNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        } finally {
            fileLock.unlockRead(stamp);
        }
    }

    public MyHorse getHorseFromFile(Horse horse) {
        final long stamp = fileLock.readLock();
        try (InputStream is = new FileInputStream(HORSES_FILE)) {
            final ObjectInputStream oi = new ObjectInputStream(is);
            final List<MyHorse> horses = (List<MyHorse>) oi.readObject();
            Bukkit.getLogger().log(Level.INFO, "Horses loaded: " + horses.size());
            for (MyHorse mh : horses) {
                if (mh.getUuid().equals(horse.getEntityId())) {
                    return mh;
                }
            }
        } catch (FileNotFoundException ex) {
            saveHorses();
        } catch (IOException | ClassNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        } finally {
            fileLock.unlockRead(stamp);
        }
        return null;
    }
}
