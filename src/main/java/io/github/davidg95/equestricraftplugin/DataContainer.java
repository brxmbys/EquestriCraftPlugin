/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import org.bukkit.entity.Horse;

/**
 *
 * @author David
 */
public class DataContainer {

    private static final DataContainer CONTAINER;

    private final List<MyHorse> horses;
    private final StampedLock horseLock;

    private DataContainer() {
        horses = new LinkedList<>();
        horseLock = new StampedLock();
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
}
