/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Horse;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Thread which checks horses.
 *
 * @author David
 */
public class HorseCheckerThread extends Thread {

    private final DataContainer container; //The container for the horses and the data.

    /**
     * The length of time a horse can go without eating before getting sick.
     */
    public static long EAT_LIMIT = 604800000; //One week.
    /**
     * The length of time a horse can go without drinking before getting sick.
     */
    public static long DRINK_LIMIT = 604800000L; //One week.
    /**
     * The length of time a horse can be sick for before dying.
     */
    public static long SICK_LIMIT = 604800000L; //One week.
    /**
     * The length of time a horse can go after eating before it defecates.
     */
    public static long DEFECATE_INTERVAL = 18000000L; //Five Hours.
    /**
     * The length of time a horse will wait before getting ill again.
     */
    public static long ILL_WAIT = 259200000L; //Three days.

    public static long VACCINATION_DURATION = 2419200000L; //Four weeks.

    public static final double BUCK_PROBABILITY = 0.005;

    public static final double BREED_PROBABILITY = 0.2;
    public static final long BREED_INTERVAL = 86400000L;

    public static final double SICK_PROBABILITY = 0.002;
    public static final double VACCINATED_PROBABILITY = 0.0001;

    private final List<Block> bales;
    private final List<Block> cauldrons;
    private final StampedLock baleLock;

    public HorseCheckerThread() {
        container = DataContainer.getInstance();
        bales = new LinkedList<>();
        cauldrons = new LinkedList<>();
        baleLock = new StampedLock();
    }

    @Override
    public void run() {
        while (true) {
            final long stamp = container.horseReadLock();
            try {
                for (MyHorse horse : container.getHorseList()) {
                    if (horse.getHorse() == null) {
                        continue;
                    }
                    final Block cauldron = horse.getNearCauldron(); //Get the nearby cauldron if there is one.
                    if (cauldron != null) { //Check if they are next to a cauldron.
                        if (this.getFirstEat(cauldron) == -1) {
                            this.setFirstEat(cauldron);
                            cauldrons.add(cauldron);
                        }
                        horse.setThirst(false);
                    }
                    final Block bale = horse.getNearHayBale(); //Get the nearby hay bale if there is one.
                    if (bale != null) { //Check if they are next to a hay bale.
                        if (this.getFirstEat(bale) == -1) {
                            this.setFirstEat(bale);
                            final long bStamp = baleLock.writeLock();
                            try {
                                bales.add(bale);
                            } finally {
                                baleLock.unlockWrite(bStamp);
                            }
                        }
                        horse.setHunger(false);
                    }
                    if (horse.getDurationSinceLastDrink() > DRINK_LIMIT) { //Check if the horse is thirsty.
                        horse.setThirst(true);
                    }
                    if (horse.getDurationSinceLastEat() > EAT_LIMIT) { //Check if the horse is hungry.
                        horse.setHunger(true);
                    }
                    if (horse.getSickness() && horse.getIllDuration() > SICK_LIMIT) { //Check if the horse has been sick fo too long.
                        horse.kill();
                    }
                    if (horse.getHunger() && horse.getHungerDuration() > SICK_LIMIT) { //Kill the horse if it has been hungry longer than the limit.
                        horse.kill();
                    }
                    if (horse.getThirst() && horse.getThirstDuration() > SICK_LIMIT) { //Kill the horse if it has been thirsty longer than the limit.
                        horse.kill();
                    }
                    if (horse.getDurationSinceLastEat() > DEFECATE_INTERVAL) {
                        horse.defecate();
                    }
                    for (Block ba : bales) {
                        if ((getCurrentTime() - this.getFirstEat(ba)) > 10800000L) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    ba.setType(Material.AIR);
                                }
                            }.runTask(EquestriCraftPlugin.plugin);
                        }
                    }
                    for (Block ba : cauldrons) {
                        if ((getCurrentTime() - this.getFirstEat(ba)) > 10800000L) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    ba.setData((byte) 0);
                                }
                            }.runTask(EquestriCraftPlugin.plugin);
                        }
                    }

                    if (horse.durationSinceVaccinated() > VACCINATION_DURATION) {
                        horse.removeVaccination();
                    }

                    {
                        final double r = Math.random();

                        if (r <= BREED_PROBABILITY) {
                            new Thread() {
                                @Override
                                public void run() {
                                    if ((getCurrentTime() - horse.getLastBreed()) < BREED_INTERVAL) {
                                        if (horse.nearMate()) {
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    final Horse h = horse.getHorse().getWorld().spawn(horse.getHorse().getLocation(), Horse.class);
                                                    h.setStyle(horse.getHorse().getStyle());
                                                    horse.setLastBreed(getCurrentTime());
                                                }
                                            }.runTask(EquestriCraftPlugin.plugin);
                                        }
                                    }
                                }
                            }.start();
                        }
                    }

                    //Roughly every 3 and a half days a horse will get ill.
                    if (horse.getWellDuration() > ILL_WAIT) { //Check the horse as not been ill too recently.
                        final double r = Math.random();
                        if (horse.durationSinceVaccinated() < VACCINATION_DURATION) {
                            if (r <= VACCINATED_PROBABILITY) {
                                horse.setSickness(true);
                            }
                        } else {
                            if (r <= SICK_PROBABILITY) {
                                horse.setSickness(true);
                            }
                        }
                    }

                    final double r = Math.random();
                    if (r <= BUCK_PROBABILITY) {
                        horse.getHorse().eject();
                    }
                }
            } finally {
                container.horseReadUnlock(stamp);
            }
            container.removeDeadHorses();
            try {
                Thread.sleep(50); //Wait 10 minutes then loop again.
            } catch (InterruptedException ex) {
                Logger.getLogger(HorseCheckerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Get the current time in ms.
     *
     * @return the current time as a long.
     */
    private long getCurrentTime() {
        return new Date().getTime();
    }

    private long getFirstEat(Block b) {
        final List<MetadataValue> mdvs = b.getMetadata("FIRSTEAT");
        for (MetadataValue md : mdvs) {
            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                return md.asLong();
            }
        }
        return -1;
    }

    private void setFirstEat(Block b) {
        b.setMetadata("FIRSTEAT", new FixedMetadataValue(EquestriCraftPlugin.plugin, getCurrentTime()));
    }

}
