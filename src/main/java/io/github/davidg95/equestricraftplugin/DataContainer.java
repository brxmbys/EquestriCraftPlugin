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

    private List<UUID> doctors;
    private final StampedLock doctorLock;

    private Thread saveThread;

    public static final String HORSES_FILE = "horses.config";

    private final StampedLock fileLock;

    private DataContainer() {
        doctors = new LinkedList<>();
        doctorLock = new StampedLock();
        fileLock = new StampedLock();
        loadHorses();
        initThread();
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
        try {
            for (MyHorse horse : horses) {
                horsesInFile++;
                if (horsesInFile % 500 == 0) {
                    Bukkit.getLogger().log(Level.INFO, "Scanned: " + horsesInFile);
                }
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (entity.getUniqueId().equals(horse.getUuid())) {
                            MyHorse.myHorseToHorse(horse, (Horse) entity);
                        }
                    }
                }
            }
        } finally {
        }
        Bukkit.getLogger().log(Level.INFO, "Load complete");
        Bukkit.getLogger().log(Level.INFO, "Number of horses in file: " + horsesInFile);
        Bukkit.getLogger().log(Level.INFO, "Number of hroses found in world: " + horsesFound);
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
        for (World w : Bukkit.getWorlds()) {
            final long stamp = HorseCheckerThread.horseLock.readLock();
            try {
                for (Horse h : w.getEntitiesByClass(Horse.class)) {
                    final MyHorse mHorse = MyHorse.horseToMyHorse(h);
                    mHorses.add(mHorse);
                }
            } finally {
                HorseCheckerThread.horseLock.unlockRead(stamp);
            }
        }
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
                final List<MyHorse> horses = getHorses();
                final ObjectOutputStream oo = new ObjectOutputStream(os);
                oo.writeObject(horses);
                oo.writeObject(doctors);
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
}
