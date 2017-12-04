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
import org.bukkit.OfflinePlayer;

/**
 *
 * @author David
 */
public class DataContainer {

    private static DataContainer container;

    private List<MyHorse> horses;

    private List<UUID> doctors;

    private List<UUID> farriers;

    public static final String HORSES_FILE = "horses.config";

    private final StampedLock fileLock;

    private DataContainer() {
        horses = new LinkedList<>();
        doctors = new LinkedList<>();
        farriers = new LinkedList<>();
        fileLock = new StampedLock();
        try {
            loadHorses(); //Load the horses from the file.
        } catch (FileNotFoundException ex) {
            saveHorses(); //Save the initialised horses.
        }
    }

    public void empty() {
        horses.clear();
        doctors.clear();
        horses = null;
        doctors = null;
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
