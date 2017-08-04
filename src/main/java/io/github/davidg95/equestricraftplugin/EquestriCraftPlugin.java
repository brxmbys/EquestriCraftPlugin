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
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author David
 */
public class EquestriCraftPlugin extends JavaPlugin implements Listener {

    public static Logger LOG;

    public static Plugin plugin;

    private DataContainer container;
    private HorseCheckerThread checkerThread;

    private Properties properties;
    private static final String PROPERTIES_FILE = "equestricraftplugin.properties";

    public static final String POTION_NAME = "Healer";
    public static final String SHEARS_NAME = "Gelding Shears";
    public static final String STICK_NAME = "Horse checking wand";
    public static final String VACCINE_NAME = "Vaccination";

    @Override
    public void onEnable() {
        plugin = this;
        LOG = plugin.getLogger();
        properties = new Properties();
        loadProperties();
        container = DataContainer.getInstance();
        checkerThread = new HorseCheckerThread();
        checkerThread.start();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        LOG.log(Level.INFO, "Saving horses to file");
        container.saveHorses();
        DataContainer.destroyInstance();
        checkerThread.setRun(false);
        checkerThread = null;
        HandlerList.unregisterAll(plugin);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("equestristatus")) {   //equestristatus command
            new Thread() {
                @Override
                public void run() {
                    int count = 0;
                    int geld = 0;
                    int stal = 0;
                    int mare = 0;
                    int none = 0;
                    final long stamp = container.horseLock.writeLock();
                    try {
                        for (MyHorse h : container.getAllHorses()) {
                            count++;
                            switch (h.getGender()) {
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
                        container.horseLock.unlockWrite(stamp);
                    }
                    sender.sendMessage("Horses: " + count);
                    sender.sendMessage("Stallions: " + stal);
                    sender.sendMessage("Mares: " + mare);
                    sender.sendMessage("Geldings: " + geld);
                    sender.sendMessage("None assigned: " + none);
                }
            }.start();
            return true;
        } else if (cmd.getName().equalsIgnoreCase("createhorse")) {   //createhorse command
            switch (args.length) {
                case 0:
                    if (sender instanceof Player) {
                        final Player player = (Player) sender;
                        if (player.isOp()) {
                            final Horse h = player.getWorld().spawn(player.getLocation(), Horse.class);
                        } else {
                            player.sendMessage("You must be an op to use this command!");
                        }
                    } else {
                        sender.sendMessage("This command can only be run by a player");
                    }
                    break;
                case 1:
                    final String name = args[0];
                    final Player pl = Bukkit.getPlayer(name);
                    if (pl == null) {
                        sender.sendMessage("Player not found");
                        return true;
                    }
                    final Horse h = pl.getWorld().spawn(pl.getLocation(), Horse.class);
                    sender.sendMessage("Created horse for " + pl.getName());
                    break;
                default:
                    break;
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("geldingtool")) {   //geldingtool command
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
        } else if (cmd.getName().equalsIgnoreCase("horsemedicine")) {   //horsemedicine command
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
        } else if (cmd.getName().equalsIgnoreCase("horsewand")) {   //horsewand command
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
        } else if (cmd.getName().equalsIgnoreCase("vaccination")) {   //vaccination command
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
        } else if (cmd.getName().equalsIgnoreCase("adddoctor")) {   //adddoctor command
            if (args.length == 1) {
                if ((sender instanceof Player && ((Player) sender).isOp()) || !(sender instanceof Player)) {
                    final Player player = Bukkit.getPlayer(args[0]);
                    if (player == null) {
                        return true;
                    }
                    container.addDoctor(player);
                    sender.sendMessage(args[0] + " is now a doctor");
                    player.sendMessage("You are now a doctor!");
                } else {
                    sender.sendMessage("Only ops can use this command");
                }
            } else {
                sender.sendMessage("Usage- /adddoctor <player>");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("changegender")) {   //changegender command
            if (args.length == 1) {
                if ((sender instanceof Player && ((Player) sender).isOp())) {
                    final Player player = (Player) sender;
                    if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                        final MyHorse horse = container.getHorse(player.getVehicle().getUniqueId());
                        if (args[0].equalsIgnoreCase("stallion")) {
                            horse.setGender(MyHorse.STALLION);
                        } else if (args[0].equalsIgnoreCase("mare")) {
                            horse.setGender(MyHorse.MARE);
                        } else if (args[0].equalsIgnoreCase("gelding")) {
                            horse.setGender(MyHorse.GELDING);
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
        } else if (cmd.getName().equalsIgnoreCase("savehorses")) {   //savehorses command
            if ((sender instanceof Player && ((Player) sender).isOp()) || !(sender instanceof Player)) {
                sender.sendMessage("Saving horses...");
                container.saveHorses();
                sender.sendMessage("Save complete...");
            } else {
                sender.sendMessage("Only ops can use this command");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("setbreed")) {   //setbreed command
            if (args.length == 1) {
                if ((sender instanceof Player && ((Player) sender).isOp())) {
                    final Player player = (Player) sender;
                    if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                        final MyHorse horse = container.getHorse(player.getVehicle().getUniqueId());
                        for (HorseBreed br : HorseBreed.values()) {
                            if (br.toString().equalsIgnoreCase(args[0])) {
                                horse.setBreed(br);
                                break;
                            }
                        }
                        sender.sendMessage("Breed set to " + horse.getBreed().toString());
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
        } else if (cmd.getName().equalsIgnoreCase("setpersonality")) {   //setpersonality command
            if (args.length == 1) {
                if ((sender instanceof Player && ((Player) sender).isOp())) {
                    final Player player = (Player) sender;
                    if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                        final MyHorse horse = container.getHorse(player.getVehicle().getUniqueId());
                        for (Personality p : Personality.values()) {
                            if (p.toString().equalsIgnoreCase(args[0])) {
                                horse.setPersonality(p);
                                break;
                            }
                        }
                        sender.sendMessage("Personality set to " + horse.getPersonality().toString());
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
        }
        return false;
    }

    /**
     * This method will get the horses from a chunk load event and ensure pair
     * them with horses from the file.
     *
     * @param event
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (final Entity e : event.getChunk().getEntities()) {
                        if (e.getType() == EntityType.HORSE) {
                            final MyHorse mh = container.getHorse(e.getUniqueId());
                            if (mh == null) {
                                container.addHorse(mh);
                            } else {
                                mh.setHorse((Horse) e);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error on chunk load", e);
                }
            }
        }.runTask(plugin);
    }

    /**
     * This method will check for players using the tools.
     *
     * @param event
     */
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
                case SHEARS: //Gelding Tool
                    //Check they have shears in their hand.
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the shears have a display name.
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(SHEARS_NAME)) { //Check the shears are the Gelding Shears.
                        return;
                    }
                    if (event.getEntity() instanceof Horse) { //Check it was a horse they are hitting.
                        event.setCancelled(true);
                        final MyHorse horse = container.getHorse(event.getEntity().getUniqueId()); //Get the Horse instance.
                        if (horse.getGender() != MyHorse.STALLION) { //Check it was a stallion.
                            player.sendMessage("This horse is not a stallion");
                            return;
                        }
                        horse.setGender(MyHorse.GELDING); //Turn the horse into a gelding.
                        player.sendMessage("This horse has been gelded");
                    }
                    break;
                case STICK: //Horse wand
                    //Horse checking stick
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the shears have a display name.
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(STICK_NAME)) { //Check the shears are the Gelding Shears.
                        return;
                    }
                    if (event.getEntity() instanceof Horse) {
                        event.setCancelled(true);
                        final MyHorse horse = container.getHorse(event.getEntity().getUniqueId()); //Get the horse that was clicked on.
                        if(horse == null){
                            player.sendMessage("This horse has no details, this is an error and should be reported to an admin or dev.");
                            return;
                        }
                        boolean sickness = horse.isSick();
                        int gender = horse.getGender();
                        boolean hunger = horse.isHungry();
                        boolean thirst = horse.isThirsty();
                        boolean vaccination = horse.isVaccinated();
                        String genderStr = ChatColor.BOLD + "Gender: " + ChatColor.RESET;
                        switch (gender) {
                            case MyHorse.STALLION:
                                genderStr = genderStr + ChatColor.DARK_RED + "Stallion";
                                break;
                            case MyHorse.MARE:
                                genderStr = genderStr + ChatColor.DARK_PURPLE + "Mare";
                                break;
                            case MyHorse.GELDING:
                                genderStr = genderStr + ChatColor.DARK_AQUA + "Gelding";
                                break;
                            default:
                                genderStr = genderStr + "None";
                                break;
                        }
                        final String name = ChatColor.BOLD + "Name: " + ChatColor.RESET + (horse.getHorse().getCustomName() == null ? "No name" : horse.getHorse().getCustomName());
                        final String breedStr = ChatColor.BOLD + "Breed: " + ChatColor.RESET + horse.getBreed().toString();
                        final String personalityStr = ChatColor.BOLD + "Personality: " + ChatColor.RESET + horse.getPersonality().toString();
                        final String ageStr = ChatColor.BOLD + "Age: " + durToString(horse.getAge()) + " old";
                        String sickSince = durToString(horse.getIllDuration());
                        final String sickStr = ChatColor.BOLD + "Health: " + ChatColor.RESET + "" + (sickness ? ChatColor.RED + "Ill" + ChatColor.RESET + " for " + sickSince : ChatColor.GREEN + "Well");
                        String hungerSince = durToString(horse.getHungerDuration());
                        final String hungerStr = ChatColor.BOLD + "Hunger: " + ChatColor.RESET + "" + (hunger ? ChatColor.RED + "Hungry" + ChatColor.RESET + " for " + hungerSince : ChatColor.GREEN + "Not Hungry");
                        String thirstSince = durToString(horse.getThristDuration());
                        final String thirstStr = ChatColor.BOLD + "Thirst: " + ChatColor.RESET + "" + (thirst ? ChatColor.RED + "Thirsty" + ChatColor.RESET + " for " + thirstSince : ChatColor.GREEN + "Not Thirsty");
                        final String vaccinationStr = ChatColor.BOLD + "Vaccinated: " + ChatColor.RESET + "" + (vaccination ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No");
                        player.sendMessage(">------------------------------<");
//                        player.sendMessage(name);
                        player.sendMessage(genderStr);
                        player.sendMessage(breedStr);
                        player.sendMessage(personalityStr);
                        player.sendMessage(ageStr);
                        player.sendMessage(sickStr);
                        player.sendMessage(hungerStr);
                        player.sendMessage(thirstStr);
                        player.sendMessage(vaccinationStr);
                        player.sendMessage(">------------------------------<");
                    } else {
                        player.sendMessage("You must click on a horse");
                    }
                    break;
                case BLAZE_ROD: //Vaccination
                    //Vaccination
                    if (!inHand.getItemMeta().hasDisplayName()) {
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(VACCINE_NAME)) {
                        return;
                    }
                    if (event.getEntity() instanceof Horse) {
                        event.setCancelled(true);
                        final MyHorse horse = container.getHorse(event.getEntity().getUniqueId());
                        horse.setVaccinated(true);
                        player.sendMessage("Horse has been vaccinated");
                    }
                    break;
                case REDSTONE_TORCH_ON: //Healing
                    //Healing
                    if (!inHand.getItemMeta().hasDisplayName()) {
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(POTION_NAME)) {
                        return;
                    }
                    if (event.getEntity() instanceof Horse) {
                        event.setCancelled(true);
                        final MyHorse horse = container.getHorse(event.getEntity().getUniqueId());
                        horse.setSick(false);
                        player.sendMessage("Horse has been cured");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Convert a long durations in ms to a string displaying the days and hours.
     * @param dur the duration in ms as a long.
     * @return String displaying the days and hours.
     */
    private String durToString(long dur) {
        final int days = (int) (dur / 1000 / 60 / 60 / 24);
        int hours;
        if (days > 0) {
            hours = (int) (dur % days);
        } else {
            hours = (int) dur / 1000 / 60 / 60;
        }
        return "" + ChatColor.BOLD + ChatColor.AQUA + days + ChatColor.RESET + (days == 1 ? " day" : " days") + " and " + ChatColor.BOLD + ChatColor.AQUA + hours + ChatColor.RESET + (hours == 1 ? " hour" : " hours");
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent evt) {
        if (evt.getEntityType() == EntityType.HORSE) {
            MyHorse mh = new MyHorse((Horse) evt.getEntity());
            if (evt.getEntity().getMetadata("breed").size() > 1) {
                String breed = evt.getEntity().getMetadata("breed").get(0).asString();
                HorseBreed br = HorseBreed.valueOf(breed);
                mh.setBreed(br);
            }
            container.addHorse(mh);
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
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private void saveProperties() {
        final File f = new File(PROPERTIES_FILE);
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
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
            LOG.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
