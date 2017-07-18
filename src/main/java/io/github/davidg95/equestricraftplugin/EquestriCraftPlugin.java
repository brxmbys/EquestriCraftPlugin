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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author David
 */
public class EquestriCraftPlugin extends JavaPlugin implements Listener {

    /**
     * A reference to the plugin object.
     */
    public static Plugin plugin;

    private DataContainer container;
    private HorseCheckerThread checkerThread;
    private Properties properties;
    private static final String PROPERTIES_FILE = "equestricraftplugin.properties";

    /**
     * The name of the medicine used to heal horses.
     */
    public static final String POTION_NAME = "Healer";
    /**
     * The name of the tool used to geld stallions.
     */
    public static final String SHEARS_NAME = "Gelding Shears";
    /**
     * The name of the tool to check horses.
     */
    public static final String STICK_NAME = "Horse checking wand";

    public static final String VACCINE_NAME = "Vaccination";

    @Override
    public void onEnable() {
        plugin = this;
        properties = new Properties();
        loadProperties();
        container = DataContainer.getInstance();
        checkerThread = new HorseCheckerThread();
        checkerThread.start();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "Saving horses to file");
        container.saveHorses();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("equestristatus")) {
            int count = 0;
            int geld = 0;
            int stal = 0;
            int mare = 0;
            int none = 0;
            for (World w : Bukkit.getWorlds()) {
                final long stamp = HorseCheckerThread.horseLock.readLock();
                try {
                    for (Horse h : w.getEntitiesByClass(Horse.class)) {
                        count++;
                        switch (MyHorse.getGenderFromMeta(h)) {
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
                    }
                } catch (Exception e) {
                } finally {
                    HorseCheckerThread.horseLock.unlockRead(stamp);
                }
            }
            sender.sendMessage("Horses: " + count);
            sender.sendMessage("Stallions: " + stal);
            sender.sendMessage("Mares: " + mare);
            sender.sendMessage("Geldings: " + geld);
            sender.sendMessage("None assigned: " + none);
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
                } else {
                    final Horse h = player.getWorld().spawn(player.getLocation(), Horse.class);
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
                final ItemMeta im = shears.getItemMeta();
                im.setDisplayName(SHEARS_NAME);
                final List<String> comments = new ArrayList<>();
                comments.add("Used for gelding a Stallion");
                im.setLore(comments);
                shears.setItemMeta(im);
                inventory.addItem(shears);
            } else {
                sender.sendMessage("Only players may use this command");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("horsemedicine")) {
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                if (container.isDoctor(player)) {
                    final PlayerInventory inventory = player.getInventory();
                    final ItemStack medicine = new ItemStack(Material.REDSTONE_TORCH_ON, 1);
                    final ItemMeta im = medicine.getItemMeta();
                    im.setDisplayName(POTION_NAME);
                    final List<String> comments = new ArrayList<>();
                    comments.add("Used to heal an ill horse");
                    im.setLore(comments);
                    medicine.setItemMeta(im);
                    inventory.addItem(medicine);
                }
            } else {
                sender.sendMessage("Only players may use this command");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("horsewand")) {
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                final PlayerInventory inventory = player.getInventory();
                final ItemStack stick = new ItemStack(Material.STICK, 1);
                final ItemMeta im = stick.getItemMeta();
                im.setDisplayName(STICK_NAME);
                final List<String> comments = new ArrayList<>();
                comments.add("Used to check a horses gender and health");
                im.setLore(comments);
                stick.setItemMeta(im);
                inventory.addItem(stick);
            } else {
                sender.sendMessage("Only a player may use this command");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("vaccination")) {
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                if (container.isDoctor(player)) {
                    final PlayerInventory inventory = player.getInventory();
                    final ItemStack vaccine = new ItemStack(Material.BLAZE_ROD, 1);
                    final ItemMeta im = vaccine.getItemMeta();
                    im.setDisplayName(VACCINE_NAME);
                    final List<String> comments = new ArrayList<>();
                    comments.add("Used to vaccinate horses.");
                    comments.add("Vaccinations last for 4 weeks");
                    im.setLore(comments);
                    vaccine.setItemMeta(im);
                    inventory.addItem(vaccine);
                } else {
                    sender.sendMessage("Only a doctor can use this command");
                }
            } else {
                sender.sendMessage("Only a player can use this command");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("adddoctor")) {
            if (args.length == 1) {
                if ((sender instanceof Player && ((Player) sender).isOp()) || !(sender instanceof Player)) {
                    final Player player = Bukkit.getPlayer(args[0]);
                    container.addDoctor(player);
                    sender.sendMessage(args[0] + " is now a doctor");
                    player.sendMessage("You are not a doctor!");
                } else {
                    sender.sendMessage("Only ops can use this command");
                }
            } else {
                sender.sendMessage("Usage- /adddoctor <player>");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("changegender")) {
            if (args.length == 1) {
                if ((sender instanceof Player && ((Player) sender).isOp())) {
                    final Player player = (Player) sender;
                    if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                        final Horse horse = (Horse) player.getVehicle();
                        if (args[0].equalsIgnoreCase("stallion")) {
                            MyHorse.setGenderInMeta(horse, MyHorse.STALLION);
                        } else if (args[0].equalsIgnoreCase("mare")) {
                            MyHorse.setGenderInMeta(horse, MyHorse.MARE);
                        } else if (args[0].equalsIgnoreCase("gelding")) {
                            MyHorse.setGenderInMeta(horse, MyHorse.GELDING);
                        } else {
                            return false;
                        }
                        sender.sendMessage("Gender set to " + args[0]);
                    } else {
                        sender.sendMessage("You must be on a horse!");
                    }
                } else {
                    sender.sendMessage("Only ops can use this command");
                }
            } else {
                return false;
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("savehorses")) {
            if ((sender instanceof Player && ((Player) sender).isOp()) || !(sender instanceof Player)) {
                sender.sendMessage("Saving horses...");
                container.saveHorses();
                sender.sendMessage("Save complete...");
            } else {
                sender.sendMessage("Only ops can use this command");
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
        if (inHand == null) {
            return;
        }
        if (null != inHand.getType()) {
            switch (inHand.getType()) {
                case SHEARS:
                    //Check they have shears in their hand.
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the shears have a display name.
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(SHEARS_NAME)) { //Check the shears are the Gelding Shears.
                        return;
                    }
                    if (event.getEntity() instanceof Horse) { //Check it was a horse they are hitting.
                        event.setCancelled(true);
                        final Horse horse = (Horse) event.getEntity(); //Get the Horse instance.
                        if (MyHorse.getGenderFromMeta(horse) != MyHorse.STALLION) { //check it was a stallion.
                            player.sendMessage("This horse is not a stallion");
                            return;
                        }
                        MyHorse.setGenderInMeta(horse, MyHorse.GELDING); //Turn the horse into a gelding.
                        player.sendMessage("This horse has been gelded");
                    }
                    break;
                case STICK:
                    //Horse checking stick
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the shears have a display name.
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(STICK_NAME)) { //Check the shears are the Gelding Shears.
                        return;
                    }
                    if (event.getEntity() instanceof Horse) {
                        event.setCancelled(true);
                        final Horse horse = (Horse) event.getEntity();
                        boolean sickness = false;
                        final List<MetadataValue> mdvss = horse.getMetadata(MyHorse.META_HEALTH);
                        for (MetadataValue md : mdvss) {
                            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                                sickness = md.asBoolean();
                            }
                        }
                        int gender = -1;
                        final List<MetadataValue> mdvsg = horse.getMetadata(MyHorse.META_GENDER);
                        for (MetadataValue md : mdvsg) {
                            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                                gender = md.asInt();
                            }
                        }
                        boolean hunger = false;
                        final List<MetadataValue> mdvsh = horse.getMetadata(MyHorse.META_HUNGER);
                        for (MetadataValue md : mdvsh) {
                            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                                hunger = md.asBoolean();
                            }
                        }
                        boolean thirst = false;
                        final List<MetadataValue> mdvst = horse.getMetadata(MyHorse.META_THIRST);
                        for (MetadataValue md : mdvst) {
                            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                                thirst = md.asBoolean();
                            }
                        }
                        boolean vaccination = false;
                        final List<MetadataValue> mdvsv = horse.getMetadata(MyHorse.META_VACCINATED);
                        for (MetadataValue md : mdvsv) {
                            if (md.getOwningPlugin() == EquestriCraftPlugin.plugin) {
                                vaccination = md.asBoolean();
                            }
                        }
                        final String genderStr = "GENDER: " + (gender == MyHorse.STALLION ? "STALLION" : (gender == MyHorse.MARE ? "MARE" : "GELDING"));
                        final String sickStr = "HEALTH: " + (sickness ? "ILL" : "WELL");
                        final String hungerStr = "HUNGER: " + (hunger ? "HUNGRY" : "NOT HUNGRY");
                        final String thirstStr = "THIRST: " + (thirst ? "THIRSTY" : "NOT THIRSTY");
                        final String vaccinationStr = "Vaccinated: " + (vaccination ? "YES" : "NO");
                        player.sendMessage(genderStr);
                        player.sendMessage(sickStr);
                        player.sendMessage(hungerStr);
                        player.sendMessage(thirstStr);
                        player.sendMessage(vaccinationStr);
                    } else {
                        player.sendMessage("You must click on a horse");
                    }
                    break;
                case BLAZE_ROD:
                    //Vaccination
                    if (!inHand.getItemMeta().hasDisplayName()) {
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(VACCINE_NAME)) {
                        return;
                    }
                    if (event.getEntity() instanceof Horse) {
                        event.setCancelled(true);
                        final Horse horse = (Horse) event.getEntity();
                        horse.setMetadata(MyHorse.META_VACCINATED, new FixedMetadataValue(EquestriCraftPlugin.plugin, true));
                        horse.setMetadata(MyHorse.META_VACCINE_TIME, new FixedMetadataValue(EquestriCraftPlugin.plugin, new Date().getTime()));
                        player.sendMessage("Horse has been vaccinated");
                    }
                    break;
                case REDSTONE_TORCH_ON:
                    //Healing
                    if (!inHand.getItemMeta().hasDisplayName()) {
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(POTION_NAME)) {
                        return;
                    }
                    if (event.getEntity() instanceof Horse) {
                        event.setCancelled(true);
                        final Horse horse = (Horse) event.getEntity();
                        horse.setMetadata(MyHorse.META_HEALTH, new FixedMetadataValue(EquestriCraftPlugin.plugin, false));
                        player.sendMessage("Horse has been cured");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent evt) {
        final ThrownPotion potion = evt.getPotion(); //Get the thrown potion.
        if (potion.getItem().getItemMeta().hasDisplayName() && potion.getItem().getItemMeta().getDisplayName().equals(POTION_NAME)) { //Check the potion is a healing potion.
            for (Entity e : evt.getAffectedEntities()) { //Loop through all the affected entities.
                if (e instanceof Horse) { //Check the entity is a horse.
                    try {
                        if (MyHorse.isHorseSick((Horse) e)) { //Check if the horse was ill.
                            MyHorse.setHorseSick((Horse) e, false);
                        }
                    } finally {
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent evt) {
        if (evt.getEntityType() == EntityType.HORSE) {
            MyHorse.setHorseGender(MyHorse.generateRandomGender(), (Horse) evt.getEntity());
            MyHorse.setLastBreed((Horse) evt.getEntity(), MyHorse.getCurrentTime());
        }
    }

    private void loadProperties() {
        try (InputStream is = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(is);
            HorseCheckerThread.EAT_LIMIT = Long.parseLong(properties.getProperty("EAT_LIMIT")) * 60000;
            HorseCheckerThread.DRINK_LIMIT = Long.parseLong(properties.getProperty("DRINK_LIMIT")) * 60000;
            HorseCheckerThread.SICK_LIMIT = Long.parseLong(properties.getProperty("SICK_LIMIT")) * 60000;
            HorseCheckerThread.DEFECATE_INTERVAL = Long.parseLong(properties.getProperty("DEFECATE_INTERVAL")) * 60000;
            HorseCheckerThread.ILL_WAIT = Long.parseLong(properties.getProperty("ILL_WAIT"));
            HorseCheckerThread.BUCK_PROBABILITY = Double.parseDouble(properties.getProperty("BUCK_PROBABILITY"));
            HorseCheckerThread.BREED_PROBABILITY = Double.parseDouble(properties.getProperty("BREED_PROBABILITY"));
            HorseCheckerThread.SICK_PROBABILITY = Double.parseDouble(properties.getProperty("SICK_PROBABILITY"));
            HorseCheckerThread.VACCINATED_PROBABILITY = Double.parseDouble(properties.getProperty("VACCINATED_SICK_PROBABILITY"));
        } catch (FileNotFoundException ex) {
            saveProperties();
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }

    private void saveProperties() {
        final File f = new File(PROPERTIES_FILE);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
        try (OutputStream os = new FileOutputStream(PROPERTIES_FILE)) {
            properties.setProperty("EAT_LIMIT", Long.toString(HorseCheckerThread.EAT_LIMIT / 60000));
            properties.setProperty("DRINK_LIMIT", Long.toString(HorseCheckerThread.DRINK_LIMIT / 60000));
            properties.setProperty("SICK_LIMIT", Long.toString(HorseCheckerThread.SICK_LIMIT / 60000));
            properties.setProperty("DEFECATE_INTERVAL", Long.toString(HorseCheckerThread.DEFECATE_INTERVAL / 60000));
            properties.setProperty("ILL_WAIT", Long.toString(HorseCheckerThread.ILL_WAIT / 60000));
            properties.setProperty("BUCK_PROBABILITY", Double.toString(HorseCheckerThread.BUCK_PROBABILITY));
            properties.setProperty("BREED_PROBABILITY", Double.toString(HorseCheckerThread.BREED_PROBABILITY));
            properties.setProperty("SICK_PROBABILITY", Double.toString(HorseCheckerThread.SICK_PROBABILITY));
            properties.setProperty("VACCINATED_SICK_PROBABILITY", Double.toString(HorseCheckerThread.VACCINATED_PROBABILITY));
            properties.store(os, null);
        } catch (FileNotFoundException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
    }
}
