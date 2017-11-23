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
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;

/**
 *
 * @author David
 */
public class DataContainer {

    private static DataContainer container;

    private List<MyHorse> horses;

    private List<UUID> doctors;

    private List<UUID> farriers;

    private Thread saveThread;

    public static final String HORSES_FILE = "horses.config";

    private final StampedLock fileLock;

    private volatile boolean runSave;

    private DataContainer() {
        horses = new LinkedList<>();
        doctors = new LinkedList<>();
        farriers = new LinkedList<>();
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

    public void empty() {
        horses.clear();
        doctors.clear();
        horses = null;
        doctors = null;
    }

    public void cleanHorses() {
        EquestriCraftPlugin.LOG.log(Level.INFO, "Performing clean");
        int removed = 0;
        for (int i = 0; i < horses.size(); i++) {
            if (horses.get(i) == null || horses.get(i).getUuid() == null) {
                horses.remove(i);
                i--;
                removed++;
            }
        }
        EquestriCraftPlugin.LOG.log(Level.INFO, "Clean complete");
        EquestriCraftPlugin.LOG.log(Level.INFO, removed + " horses removed");
        EquestriCraftPlugin.LOG.log(Level.INFO, horses.size() + " horses left");
    }

    /**
     * Cache a horse when its chunk is getting unloaded.
     *
     * @param horse the MyHorse to cache.
     */
    public void addHorse(MyHorse horse) {
        if (horses.size() > 0) {
            for (int i = 0; i < horses.size(); i++) {
                try {
                    if (horses.get(i) == null || horses.get(i).getUuid() == null || horse.getUuid() == null) {
                        continue;
                    }
                    if (horses.get(i).getUuid().equals(horse.getUuid())) {
                        horses.set(i, horse);
                        return;
                    }
                } catch (Exception e) {

                }
            }
        }
        horses.add(horse);
    }

    public MyHorse getHorse(UUID uuid) {
        for (int i = 0; i < horses.size(); i++) {
            if (horses.get(i) == null || horses.get(i).getUuid() == null) {
                continue;
            }
            if (horses.get(i).getUuid().equals(uuid)) {
                return horses.get(i);
            }
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
        for (MyHorse mh : horses) {
            if (mh.getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a horse from the cache by its UUID.
     *
     * @param uuid the UUID of the horse to remove.
     */
    public void removeHorse(UUID uuid) {
        for (int i = 0; i < horses.size(); i++) {
            if (horses.get(i).getUuid().equals(uuid)) {
                horses.remove(i);
                return;
            }
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
                        cleanHorses();
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
        int horsesInWorld = 0;
        int horsesPaired = 0;

        int geld = 0;
        int stal = 0;
        int mare = 0;
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
                for (MyHorse horse : horses) {
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
                        }
                        continue search;
                    }
                }
                //If execution get here, then the horse does not exist in the file, so it needes to be initialised.
//                EquestriCraftPlugin.LOG.log(Level.INFO, "Adding new horse");
//                final MyHorse mh = new MyHorse(h);
//                horsesToAdd.add(mh);
            }
        }
        EquestriCraftPlugin.LOG.log(Level.INFO, "Load complete");
        EquestriCraftPlugin.LOG.log(Level.INFO, "Number of horses in world: " + horsesInWorld);
        EquestriCraftPlugin.LOG.log(Level.INFO, "Number of horses paired: " + horsesPaired);
        EquestriCraftPlugin.LOG.log(Level.INFO, "Stallions: " + stal);
        EquestriCraftPlugin.LOG.log(Level.INFO, "Mares: " + mare);
        EquestriCraftPlugin.LOG.log(Level.INFO, "Geldings: " + geld);
        EquestriCraftPlugin.LOG.log(Level.INFO, "None assigned: " + (horsesInWorld - horsesPaired));
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
        container.empty();
        container = null;
    }

    /**
     * Add a new horse.
     *
     * @param p the Doctor to add.
     */
    public void addDoctor(OfflinePlayer p) {
        doctors.add(p.getUniqueId());
    }

    /**
     * Get the MyHorse object which contains the given Horse object.
     *
     * @param p the player to check.
     * @return the yHorse object which contains the horse. Null if it doesn't
     * exists.
     */
    public boolean isDoctor(OfflinePlayer p) {
        for (UUID u : doctors) {
            if (u.equals(p.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a doctor from the list.
     *
     * @param p the doctor to remove.
     * @return false if the doctor was not found, true of they were.
     */
    public boolean removeDoctor(OfflinePlayer p) {
        for (int i = 0; i < doctors.size(); i++) {
            if (doctors.get(i).equals(p.getUniqueId())) {
                doctors.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Clears all doctors.
     */
    public void resetDoctors() {
        doctors.clear();
    }

    public void addFarrier(OfflinePlayer player) {
        farriers.add(player.getUniqueId());
    }

    public boolean removeFarrier(OfflinePlayer player) {
        for (int i = 0; i < farriers.size(); i++) {
            if (farriers.get(i).equals(player.getUniqueId())) {
                farriers.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean isFarrier(OfflinePlayer player) {
        for (UUID uuid : farriers) {
            if (uuid.equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public void resetFarriers() {
        farriers.clear();
    }

    public List<UUID> getAllFarriers() {
        return farriers;
    }

    /**
     * Get a list of all doctor UUIDs.
     *
     * @return UUIDs as a list.
     */
    public List<UUID> getAllDoctors() {
        return doctors;
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
            try (OutputStream os = new FileOutputStream(HORSES_FILE)) {
                final ObjectOutputStream oo = new ObjectOutputStream(os);
                oo.writeObject(horses);
                oo.writeObject(doctors);
                oo.writeObject(farriers);
            } catch (FileNotFoundException ex) {
                EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
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
            List<MyHorse> horses = (List<MyHorse>) oi.readObject();
            pairHorses();
            doctors = (List<UUID>) oi.readObject();
            farriers = (List<UUID>) oi.readObject();
        } catch (FileNotFoundException ex) {
            throw ex;
        } catch (IOException | ClassNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        } finally {
            fileLock.unlockRead(stamp);
        }
    }
}
