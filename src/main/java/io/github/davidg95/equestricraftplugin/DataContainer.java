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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class DataContainer {

    private static DataContainer container;

    private List<MyHorse> horses;
    public final StampedLock horseLock;

    private List<UUID> doctors;
    private final StampedLock doctorLock;

    private Thread saveThread;

    public static final String HORSES_FILE = "horses.config";

    private final StampedLock fileLock;

    private boolean runSave;

    private DataContainer() {
        horses = new LinkedList<>();
        doctors = new LinkedList<>();
        horseLock = new StampedLock();
        doctorLock = new StampedLock();
        fileLock = new StampedLock();
        try {
            loadHorses(); //Load the horses from the file.
        } catch (FileNotFoundException ex) {
            initHorses(); //Initialise the horses.
            saveHorses(); //Save the initialised horses.
        }
        initThread(); //Start the auto save thread.
    }

    private void initHorses() {
        int count = 0;
        for (World w : Bukkit.getWorlds()) {
            for (Horse h : w.getEntitiesByClass(Horse.class)) {
                MyHorse mh = new MyHorse(h);
                addHorse(mh);
                count++;
            }
        }
        EquestriCraftPlugin.LOG.log(Level.INFO, "Assigned " + count + " horses");
    }

    /**
     * Cache a horse when its chunk is getting unloaded.
     *
     * @param horse the MyHorse to cache.
     */
    public void addHorse(MyHorse horse) {
        final long stamp = horseLock.writeLock();
        try {
            horses.add(horse);
        } finally {
            horseLock.unlockWrite(stamp);
        }
    }

    public MyHorse getHorse(UUID uuid) {
        final long stamp = horseLock.readLock();
        try {
            for (MyHorse h : horses) {
                if (h == null || h.getUuid() == null) {
                    continue;
                }
                if (h.getUuid() != null && h.getUuid().equals(uuid)) {
                    return h;
                }
            }
        } finally {
            horseLock.unlockRead(stamp);
        }
        return null;
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
    public void removeHorse(UUID uuid) {
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

    /**
     * Starts the auto save thread.
     */
    private void initThread() {
        final Runnable run = new Runnable() {
            @Override
            public void run() {
                runSave = true;
                EquestriCraftPlugin.LOG.log(Level.INFO, "horses.config will be saved every 10 minutes");
                while (runSave) {
                    try {
                        Thread.sleep(600000); //Ten Minutes
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DataContainer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (runSave) {
                        saveHorses();
                    }
                }
            }

        };
        saveThread = new Thread(run, "Horse_Save_Thread");
        saveThread.setDaemon(true);
        saveThread.start();
    }

    private void pairHorses() {
        final List<MyHorse> horsesToAdd = new LinkedList<>();
        int horsesInWorld = 0;
        int horsesPaired = 0;

        int geld = 0;
        int stal = 0;
        int mare = 0;
        int none = 0;
        for (World world : Bukkit.getWorlds()) {
            search:
            for (Entity entity : world.getEntities()) {
                if (entity.getType() != EntityType.HORSE) {
                    continue;
                }
                final Horse h = (Horse) entity;
                horsesInWorld++;
                if (horsesInWorld % 500 == 0) {
                    EquestriCraftPlugin.LOG.log(Level.INFO, "Scanned: " + horsesInWorld);
                }
                MyHorse current = null;
                for (MyHorse horse : horses) {
                    current = horse;
                    if (horse == null || horse.getUuid() == null) {
                        continue;
                    }
                    if (h.getUniqueId().equals(horse.getUuid())) {
                        horse.setHorse(h);
                        horsesPaired++;
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
                        continue search;
                    }
                }
                if (current != null) {
                    this.addHorse(current);
                }
                final MyHorse mh = new MyHorse(h);
                horsesToAdd.add(mh);
            }
        }
        final long stamp = horseLock.writeLock();
        try {
            for (MyHorse horse : horsesToAdd) {
                horses.add(horse);
            }
        } finally {
            horseLock.unlockWrite(stamp);
        }
        EquestriCraftPlugin.LOG.log(Level.INFO, "Load complete");
        EquestriCraftPlugin.LOG.log(Level.INFO, "Number of horses in world: " + horsesInWorld);
        EquestriCraftPlugin.LOG.log(Level.INFO, "Number of horses paired: " + horsesPaired);
        EquestriCraftPlugin.LOG.log(Level.INFO, "Stallions: " + stal);
        EquestriCraftPlugin.LOG.log(Level.INFO, "Mares: " + mare);
        EquestriCraftPlugin.LOG.log(Level.INFO, "Geldings: " + geld);
        EquestriCraftPlugin.LOG.log(Level.INFO, "None assigned: " + none);
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

    public static void destroyInstance() {
        container.runSave = false;
        container = null;
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

    public List<MyHorse> getAllHorses() {
        return horses;
    }

    /**
     * Persist horses to file.
     */
    public final void saveHorses() {
        final long stamp = fileLock.writeLock();
        try {
            EquestriCraftPlugin.LOG.log(Level.INFO, "Saving horses...");
            final File file = new File(HORSES_FILE);
            if (file.exists()) {
                file.delete();
            }
            try {
                file.createNewFile();
            } catch (IOException ex) {
                EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
            }
            final long hStamp = horseLock.writeLock();
            try (OutputStream os = new FileOutputStream(HORSES_FILE)) {
                final ObjectOutputStream oo = new ObjectOutputStream(os);
                oo.writeObject(horses);
                oo.writeObject(doctors);
            } catch (FileNotFoundException ex) {
                EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
            } finally {
                horseLock.unlockWrite(hStamp);
            }
        } finally {
            fileLock.unlockWrite(stamp);
        }
        EquestriCraftPlugin.LOG.log(Level.INFO, "Save complete...");
    }

    /**
     * Load horses from file
     */
    private void loadHorses() throws FileNotFoundException {
        final long stamp = fileLock.readLock();
        try (InputStream is = new FileInputStream(HORSES_FILE)) {
            final ObjectInputStream oi = new ObjectInputStream(is);
            horses = (List<MyHorse>) oi.readObject();
            pairHorses();
            doctors = (List<UUID>) oi.readObject();
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException | ClassNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        } finally {
            fileLock.unlockRead(stamp);
        }
    }
}
