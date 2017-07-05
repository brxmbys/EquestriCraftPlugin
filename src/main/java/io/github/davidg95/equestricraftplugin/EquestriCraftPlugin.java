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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author David
 */
public class EquestriCraftPlugin extends JavaPlugin implements Listener {

    public static Plugin plugin;

    private DataContainer container;
    public List<World> worlds;
    private HorseCheckerThread checkerThread;
    private Properties properties;
    private static final String PROPERTIES_FILE = "equestricraftplugin.properties";

    public static final String POTION_NAME = "Medicine";

    @Override
    public void onEnable() {
        plugin = this;
        worlds = Bukkit.getWorlds();
        properties = new Properties();
        loadProperties();
        container = DataContainer.getInstance();
        getServer().getPluginManager().registerEvents(this, this);
        for (World w : worlds) {
            for (Horse h : w.getEntitiesByClass(Horse.class)) {
                container.addHorse(h);
            }
        }
        checkerThread = new HorseCheckerThread();
        checkerThread.start();
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("equestristatus")) {
            final int nohorses = container.getHorseList().size();
            int sickhorses = 0;
            final long stamp = container.horseReadLock();
            try {
                for (MyHorse h : container.getHorseList()) {
                    if (h.getSickness() == MyHorse.SICK) {
                        sickhorses++;
                    }
                }
            } finally {
                container.horseReadUnlock(stamp);
            }
            sender.sendMessage("Horses: " + nohorses + "\nSick horses: " + sickhorses);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("createhorse")) {
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                if (args.length == 1) {
                    final String genderarg = args[0];
                    int gender;
                    if (genderarg.equalsIgnoreCase("stallion")) {
                        gender = MyHorse.STALLION;
                    } else if (genderarg.equalsIgnoreCase("mare")) {
                        gender = MyHorse.MARE;
                    } else {
                        sender.sendMessage("Unrecognised gender. Must be STALLION or MARE");
                        return true;
                    }
                    final Horse h = player.getWorld().spawn(player.getLocation(), Horse.class);
                    final MyHorse horse = new MyHorse(h);
                    horse.setGender(gender);
                }
            } else {
                sender.sendMessage("This command can only be run by a player");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("geldingtool")) {
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                final PlayerInventory inventory = player.getInventory();
                final ItemStack shears = new ItemStack(Material.SHEARS, 1);
                final String shearsName = "Gelding Shears";
                final ItemMeta im = shears.getItemMeta();
                im.setDisplayName(shearsName);
                final List<String> comments = new ArrayList<>();
                comments.add("Used for gelding a Stallion");
                im.setLore(comments);
                shears.setItemMeta(im);
                inventory.addItem(shears);
            } else {
                sender.sendMessage("Only players may use this command");
            }
            return true;
        }
        return false;
    }

    @EventHandler
    public void onPlayerUse(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) { //Check the damager is a player.
            return;
        }
        final Player player = (Player) event.getDamager(); //Get the player.
        final ItemStack inHand = player.getItemInHand(); //Get the item in hand.
        if (inHand == null || inHand.getType() != Material.SHEARS) { //Check they have shears in their hand.
            return;
        }
        if (!inHand.getItemMeta().hasDisplayName()) { //Check the shears have a display name.
            return;
        }
        if (!inHand.getItemMeta().getDisplayName().equals("Gelding Shears")) { //Check the shears are the Gelding Shears.
            return;
        }
        if (event.getEntity() instanceof Horse) { //Check it was a horse they are hitting.
            final MyHorse horse = container.getHorse((Horse) event.getEntity()); //Get the MyHorse instance.
            if (horse.getGender() != MyHorse.STALLION) { //check it was a stallion.
                player.sendMessage("This horse is not a stallion");
                return;
            }
            horse.setGender(MyHorse.GELDING); //Turn the horse into a gelding.
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent evt) {
        for (Entity e : evt.getAffectedEntities()) {
            ThrownPotion potion = evt.getPotion();
            if (potion.getCustomName().equals(POTION_NAME)) {
                if (e instanceof Horse) {
                    final MyHorse mh = container.getHorse((Horse) e);
                    if (mh.getSickness() == MyHorse.ILL) {
                        mh.setSickness(MyHorse.WELL);
                        mh.setLastIll(getCurrentTime());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent evt) {
        if (evt.getEntityType() == EntityType.HORSE) {
            container.addHorse((Horse) evt.getEntity());
        }
    }

    /**
     * Get the current time in ms.
     *
     * @return the current time in ms as a long.
     */
    private long getCurrentTime() {
        return new Date().getTime();
    }

    private void loadProperties() {
        try (InputStream is = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(is);
            HorseCheckerThread.EAT_LIMIT = Integer.parseInt(properties.getProperty("EAT_LIMIT"));
            HorseCheckerThread.DRINK_LIMIT = Integer.parseInt(properties.getProperty("DRINK_LIMIT"));
            HorseCheckerThread.SICK_LIMIT = Integer.parseInt(properties.getProperty("SICK_LIMIT"));
            HorseCheckerThread.DEFECATE_INTERVAL = Integer.parseInt(properties.getProperty("DEFECATE_INTERVAL"));
            HorseCheckerThread.ILL_WAIT = Integer.parseInt(properties.getProperty("ILL_WAIT"));
        } catch (FileNotFoundException ex) {
            saveProperties();
        } catch (IOException ex) {
            Logger.getLogger(EquestriCraftPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void saveProperties() {
        File f = new File(PROPERTIES_FILE);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(EquestriCraftPlugin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try (OutputStream os = new FileOutputStream(PROPERTIES_FILE)) {
            properties.setProperty("EAT_LIMIT", Integer.toString(HorseCheckerThread.EAT_LIMIT));
            properties.setProperty("DRINK_LIMIT", Integer.toString(HorseCheckerThread.DRINK_LIMIT));
            properties.setProperty("SICK_LIMIT", Integer.toString(HorseCheckerThread.SICK_LIMIT));
            properties.setProperty("DEFECATE_INTERVAL", Integer.toString(HorseCheckerThread.DEFECATE_INTERVAL));
            properties.setProperty("ILL_WAIT", Integer.toString(HorseCheckerThread.ILL_WAIT));
            properties.store(os, null);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EquestriCraftPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EquestriCraftPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
