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
    private final StampedLock horseLock;
    private final StampedLock doctorLock;

    public static final String HORSES_FILE = "horses.config";

    private DataContainer() {
        doctors = new LinkedList<>();
        horseLock = new StampedLock();
        doctorLock = new StampedLock();
        loadHorses();
    }

    private void pairHorses(List<MyHorse> horses) {
        Bukkit.getLogger().log(Level.INFO, "Loading horses");
        Bukkit.getLogger().log(Level.INFO, "Horses: " + horses.size());
        int horsesInFile = 0;
        int horsesFound = 0;
        try {
            for (MyHorse horse : horses) {
                horsesInFile++;
                if(horsesInFile % 500 == 0){
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

//    /**
//     * Add a new horse.
//     *
//     * @param h the Horse to add.
//     */
//    public void addHorse(Horse h) {
//        try {
////            final Iterator<MyHorse> iter = horses.iterator();
////            while (iter.hasNext()) {
////                final MyHorse horse = iter.next();
////                if (horse.getUuid() == h.getUniqueId()) {
////                    horse.setHorse(h);
////                    horse.persist();
////                    return;
////                }
////            }
//            final MyHorse mh = new MyHorse(h);
//            if (mh.getGender() == -1) {
//                mh.setGender(MyHorse.generateRandomGender());
//            }
//            horses.add(new MyHorse(h));
//        } finally {
//        }
//    }
//    /**
//     * Get the MyHorse object which contains the given Horse object.
//     *
//     * @param h the horse to contain.
//     * @return the yHorse object which contains the horse. Null if it doesn't
//     * exists.
//     */
//    public MyHorse getHorse(Horse h) {
//        final long stamp = horseLock.writeLock();
//        try {
//            for (MyHorse mh : horses) {
//                if (mh.equals(h)) {
//                    return mh;
//                }
//            }
//        } finally {
//            horseLock.unlockWrite(stamp);
//        }
//        return null;
//    }
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

//    /**
//     * Gets the list of horses.
//     *
//     * @return List of type MyHorse.
//     */
//    public List<MyHorse> getHorseList() {
//        return horses;
//    }
//    public long horseReadLock() {
//        return horseLock.readLock();
//    }
//
//    public void horseReadUnlock(long stamp) {
//        horseLock.unlockRead(stamp);
//    }
//
//    public long horseWriteLock() {
//        return horseLock.writeLock();
//    }
//
//    public void horseWriteUnlock(long stamp) {
//        horseLock.unlockWrite(stamp);
//    }

//    /**
//     * Removes dead horses from the list. This is thread safe.
//     */
//    public void removeDeadHorses() {
//        final long stamp = horseLock.writeLock();
//        try {
//            final Iterator<MyHorse> horseIt = horses.iterator();
//            while (horseIt.hasNext()) {
//                final MyHorse horse = horseIt.next();
//                if (horse.getHorse() == null) {
//                    continue;
//                }
//                if (horse.isDead()) {
//                    horseIt.remove();
//                }
//            }
//        } finally {
//            horseLock.unlockWrite(stamp);
//        }
//    }
    private List<MyHorse> getHorses() {
        final List<MyHorse> mHorses = new LinkedList<>();
        for (World w : Bukkit.getWorlds()) {
            for (Horse h : w.getEntitiesByClass(Horse.class)) {
                final MyHorse mHorse = MyHorse.horseToMyHorse(h);
                mHorses.add(mHorse);
            }
        }
        return mHorses;
    }

    /**
     * Persist horses to file.
     */
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
            final List<MyHorse> horses = getHorses();
            final ObjectOutputStream oo = new ObjectOutputStream(os);
            oo.writeObject(horses);
            oo.writeObject(doctors);
        } catch (FileNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Load horses from file
     */
    private void loadHorses() {
        try (InputStream is = new FileInputStream(HORSES_FILE)) {
            final ObjectInputStream oi = new ObjectInputStream(is);
            final List<MyHorse> horses = (List<MyHorse>) oi.readObject();
            pairHorses(horses);
            doctors = (List<UUID>) oi.readObject();
        } catch (FileNotFoundException ex) {
            saveHorses();
        } catch (IOException | ClassNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }
}
