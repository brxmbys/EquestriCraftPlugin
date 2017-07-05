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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import org.bukkit.entity.Horse;

/**
 *
 * @author David
 */
public class DataContainer {

    private static final DataContainer CONTAINER;

    private List<MyHorse> horses;
    private final StampedLock horseLock;

    public static final String HORSES_FILE = "horses.config";

    private DataContainer() {
        horses = new LinkedList<>();
        horseLock = new StampedLock();
        loadHorses();
    }

    static {
        CONTAINER = new DataContainer();
    }

    /**
     * Returns an instance of the data container.
     *
     * @return the DataContainer.
     */
    public static DataContainer getInstance() {
        return CONTAINER;
    }

    /**
     * Add a new horse.
     *
     * @param h the Horse to add.
     */
    public void addHorse(Horse h) {
        final long stamp = horseLock.writeLock();
        try {
            horses.add(new MyHorse(h));
        } finally {
            horseLock.unlockWrite(stamp);
        }
    }

    public MyHorse getHorse(Horse h) {
        final long stamp = horseLock.writeLock();
        try {
            for (MyHorse mh : horses) {
                if (mh.equals(h)) {
                    return mh;
                }
            }
        } finally {
            horseLock.unlockWrite(stamp);
        }
        return null;
    }

    /**
     * Gets the list of horses.
     *
     * @return List of type MyHorse.
     */
    public List<MyHorse> getHorseList() {
        return horses;
    }

    public long horseReadLock() {
        return horseLock.readLock();
    }

    public void horseReadUnlock(long stamp) {
        horseLock.unlockRead(stamp);
    }

    public long horseWriteLock() {
        return horseLock.writeLock();
    }

    public void horseWriteLock(long stamp) {
        horseLock.unlockWrite(stamp);
    }

    /**
     * Removes dead horses from the list. This is thread safe.
     */
    public void removeDeadHorses() {
        final long stamp = horseLock.writeLock();
        try {
            final Iterator horseIt = horses.iterator();
            while (horseIt.hasNext()) {
                final MyHorse horse = (MyHorse) horseIt.next();
                if (horse.isDead()) {
                    horseIt.remove();
                }
            }
        } finally {
            horseLock.unlockWrite(stamp);
        }
    }

    public void saveHorses() {
        final File file = new File(HORSES_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
            }
        }
        try (OutputStream os = new FileOutputStream(HORSES_FILE)) {
            final ObjectOutputStream oo = new ObjectOutputStream(os);
            oo.writeObject(horses);
        } catch (FileNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }

    private void loadHorses() {
        try (InputStream is = new FileInputStream(HORSES_FILE)) {
            final ObjectInputStream oi = new ObjectInputStream(is);
            horses = (List<MyHorse>) oi.readObject();
        } catch (FileNotFoundException ex) {
            saveHorses();
        } catch (IOException | ClassNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }
}
