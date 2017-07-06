/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.block.Block;

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
//    public static int EAT_LIMIT = 604800000; //One week.
    public static int EAT_LIMIT = 30000;
    /**
     * The length of time a horse can go without drinking before getting sick.
     */
//    public static int DRINK_LIMIT = 604800000; //One week.
    public static int DRINK_LIMIT = 30000;
    /**
     * The length of time a horse can be sick for before dying.
     */
//    public static int SICK_LIMIT = 604800000; //One week.
    public static int SICK_LIMIT = 30000;
    /**
     * The length of time a horse can go after eating before it defecates.
     */
//    public static int DEFECATE_INTERVAL = 300000; //Five minutes.
    public static int DEFECATE_INTERVAL = 10000;
    /**
     * The length of time a horse will wait before getting ill again.
     */
//    public static int ILL_WAIT = 300000; //Five minutes.
    public static int ILL_WAIT = 30000;

    public HorseCheckerThread() {
        container = DataContainer.getInstance();
    }

    @Override
    public void run() {
        while (true) {
            final long stamp = container.horseReadLock();
            try {
                for (MyHorse horse : container.getHorseList()) {
                    final Block cauldron = horse.getNearCauldron(); //Get the nearby cauldron if there is one.
                    if (cauldron != null) { //Check if they are next to a cauldron.
                        horse.setLastDrink(getCurrentTime());
                        horse.setSickness(MyHorse.WELL);
                    }
                    final Block bale = horse.getNearHayBale(); //Get the nearby hay bale if there is one.
                    if (bale != null) { //Check if they are next to a hay bale.
                        horse.setLastEat(getCurrentTime());
                        horse.setSickness(MyHorse.WELL);
                    }
                    if (getCurrentTime() - horse.getLastDrink() > DRINK_LIMIT) { //Check if the horse is thirsty.
                        horse.setSickness(MyHorse.HUNGRY);
                    }
                    if (getCurrentTime() - horse.getLastEat() > EAT_LIMIT) { //Check if the horse is hungry.
                        horse.setSickness(MyHorse.HUNGRY);
                    }
                    if ((horse.getSickness() == MyHorse.HUNGRY || horse.getSickness() == MyHorse.ILL) && (getCurrentTime() - horse.getLastTimeWell()) > SICK_LIMIT) { //check if the horse has been sick fo too long.
                        horse.kill();
                    }
                    if (getCurrentTime() - horse.getLastEat() > DEFECATE_INTERVAL && horse.getLastDefecate() > horse.getLastEat()) {
                        horse.defecate();
                    }

                    //Roughly every 3 and a half days a horse will get ill.
                    if (getCurrentTime() - horse.getLastIll() >= ILL_WAIT) { //Check the horse as not been ill too recently.
                        final double random = Math.random() * 30000000;
                        if (random <= 5) {
                            horse.setSickness(MyHorse.ILL);
                        }
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

}
