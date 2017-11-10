/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import io.github.davidg95.equestricraftplugin.auctions.AuctionHandler;
import io.github.davidg95.equestricraftplugin.disciplines.DisciplinesHandler;
import io.github.davidg95.equestricraftplugin.race.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.logging.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;

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
    public static final String ONE_USE_VACCINATION = "One use vaccination";
    public static int ONE_USE_COST = 150;
    public static final String DOCTOR_TOOL = "Doctor's Tool";
    public static final String FARRIER_TOOL = "Farrier's Tool";
    public static final String NAVIGATOR_TOOL = "Navigator";

    public static boolean OP_REQ = true;
    public static boolean BLOCK_HUNGER = true;

    public static Economy economy;

    private static final Inventory navigator = Bukkit.createInventory(null, 45, "Navigator");

    public static String motd = "";

    static {
        ItemStack spawn = new ItemStack(Material.MONSTER_EGG, 1);
        ItemMeta m1 = spawn.getItemMeta();
        m1.setDisplayName("Spawn");
        m1.setLore(createLore("Go to the world spawn"));
        spawn.setItemMeta(m1);

        ItemStack leaseBarn = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta m2 = leaseBarn.getItemMeta();
        m2.setDisplayName("Lease Barn");
        m2.setLore(createLore("Lease a horse"));
        leaseBarn.setItemMeta(m2);

        ItemStack raceTrack = new ItemStack(Material.SADDLE, 1);
        ItemMeta m3 = raceTrack.getItemMeta();
        m3.setDisplayName("Race Track");
        m3.setLore(createLore("Participate in races"));
        raceTrack.setItemMeta(m3);

        ItemStack rescue = new ItemStack(Material.GOLD_BARDING, 1);
        ItemMeta m4 = rescue.getItemMeta();
        m4.setDisplayName("Rescue");
        m4.setLore(createLore("Go to the horse rescue"));
        rescue.setItemMeta(m4);

        ItemStack town = new ItemStack(Material.WOOD, 1);
        ItemMeta m5 = town.getItemMeta();
        m5.setDisplayName("Town");
        m5.setLore(createLore("Go to the town"));
        town.setItemMeta(m5);

        ItemStack trails = new ItemStack(Material.MAP, 1);
        ItemMeta m6 = trails.getItemMeta();
        m6.setDisplayName("Trails");
        m6.setLore(createLore("Go to the trails area"));
        trails.setItemMeta(m6);

        ItemStack showgrounds = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta m7 = showgrounds.getItemMeta();
        m7.setDisplayName("Showgrounds");
        m7.setLore(createLore("Go to the showgrounds"));
        showgrounds.setItemMeta(m7);

        ItemStack dentist = new ItemStack(Material.SHEARS, 1);
        ItemMeta m8 = dentist.getItemMeta();
        m8.setDisplayName("Dentist");
        m8.setLore(createLore("Go to the dentist"));
        dentist.setItemMeta(m8);

        ItemStack rules = new ItemStack(Material.BOOK, 1);
        ItemMeta m9 = rules.getItemMeta();
        m9.setDisplayName("Rules");
        m9.setLore(createLore("View the server rules"));
        rules.setItemMeta(m9);

        navigator.setItem(22, spawn);
        navigator.setItem(4, leaseBarn);
        navigator.setItem(40, raceTrack);
        navigator.setItem(24, rescue);
        navigator.setItem(20, town);
        navigator.setItem(30, trails);
        navigator.setItem(31, showgrounds);
        navigator.setItem(32, dentist);
        navigator.setItem(44, rules);
    }

    private static List<String> createLore(String lore) {
        List<String> list = new LinkedList<>();
        list.add(lore);
        return list;
    }

    @Override
    public void onEnable() {
        plugin = this;
        LOG = plugin.getLogger();
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        try {
            getConfig().load(getDataFolder() + File.separator + "race.yml");
        } catch (IOException | InvalidConfigurationException ex) {
            initRaceConfig();
            LOG.log(Level.SEVERE, "Error loading race.yml", ex);
        }
        ONE_USE_COST = getConfig().getInt("tools.one_use_vaccination_price");
        properties = new Properties();
        loadProperties();
        container = DataContainer.getInstance();
        checkerThread = new HorseCheckerThread();
        checkerThread.start();
        getServer().getPluginManager().registerEvents(this, this);
        if (!setupEconomy()) {
            LOG.log(Level.SEVERE, "Vault not detected, Disciplines has been disabled");
        } else {
            this.getCommand("disciplines").setExecutor(new DisciplinesHandler(this));
        }
        this.getCommand("race").setExecutor(new RaceController(this));
        this.getCommand("auction").setExecutor(new AuctionHandler());
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void initRaceConfig() {
        getConfig().set("finish.z1", 11135);
        getConfig().set("finish.z2", 11130);
        getConfig().set("finish.x1", -2124);
        getConfig().set("finish.x2", -2093);
        getConfig().set("finish.yl", 20);

        getConfig().set("check.z1", 11155);
        getConfig().set("check.z2", 11150);
        getConfig().set("check.x1", -2124);
        getConfig().set("check.x2", -2093);
        getConfig().set("check.yl", 20);

        getConfig().set("tools.one_use_vaccination_price", 100);
        try {
            getConfig().save(getDataFolder().getAbsolutePath() + File.separator + "race.yml");
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error saving race.yml", ex);
        }
    }

    @Override
    public void onDisable() {
        LOG.log(Level.INFO, "Saving horses to file");
        container.saveHorses();
        checkerThread.setRun(false);
        checkerThread = null;
        HandlerList.unregisterAll(plugin);
        DataContainer.destroyInstance();
        container = null;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("equestristatus")) {   //equestristatus command
            sender.sendMessage("Horses in list: " + container.getAllHorses().size());
            sender.sendMessage("Checker Thread: " + checkerThread.isAlive());
            return true;
        } else if (cmd.getName().equalsIgnoreCase("createhorse")) {   //createhorse command
            switch (args.length) {
                case 0:
                    if (sender instanceof Player) {
                        final Player player = (Player) sender;
                        if (player.isOp()) {
                            final Horse h = player.getWorld().spawn(player.getLocation(), Horse.class);
                        } else {
                            player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You must be an op to use this command!");
                        }
                    } else {
                        sender.sendMessage("This command can only be run by a player");
                    }
                    break;
                case 1:
                    final String name = args[0];
                    final Player pl = Bukkit.getPlayer(name);
                    if (pl == null) {
                        sender.sendMessage(ChatColor.BOLD + "Player not found");
                        return true;
                    }
                    final Horse h = pl.getWorld().spawn(pl.getLocation(), Horse.class);
                    sender.sendMessage(ChatColor.BOLD + "Created horse for " + pl.getName());
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
                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("buy")) {
                        if (economy.getBalance(player) >= ONE_USE_COST) {
                            economy.withdrawPlayer(player, ONE_USE_COST);
                            player.sendMessage(ChatColor.GREEN + "You have been charged $" + ONE_USE_COST);
                            final PlayerInventory inventory = player.getInventory();
                            final ItemStack vaccine = new ItemStack(Material.BLAZE_ROD, 1);
                            final ItemMeta im = vaccine.getItemMeta();
                            im.setDisplayName(ONE_USE_VACCINATION);
                            final List<String> comments = new ArrayList<>();
                            comments.add("Useful for one vaccination.");
                            comments.add("Vaccinations last for 4 weeks");
                            im.setLore(comments);
                            vaccine.setItemMeta(im);
                            inventory.addItem(vaccine);
                        } else {
                            player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You do not have enough money");
                        }
                        return true;
                    }
                    return false;
                }
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
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Only a doctor can use this command");
                }
            } else {
                sender.sendMessage("Only a player can use this command");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("doctor")) {   //adddoctor command
            if (args.length == 2) {
                if ((sender instanceof Player && ((Player) sender).isOp()) || !(sender instanceof Player)) {
                    final OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                    if (player == null) {
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Player not found");
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("add")) {
                        container.addDoctor(player);
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + args[1] + " is now a doctor");
                        if (player.isOnline()) {
                            Player pl = (Player) player;
                            pl.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "You are now a doctor!");
                        }
                        return true;
                    } else if (args[0].equalsIgnoreCase("remove")) {
                        if (container.removeDoctor(player)) {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + args[1] + " is no longer a doctor");
                            if (player.isOnline()) {
                                Player pl = (Player) player;
                                pl.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "You are no longer a doctor!");
                            }
                        } else {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Player not found");
                        }
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Only ops can use this command");
                }
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reset")) {
                    if (sender.isOp() || !(sender instanceof Player)) {
                        container.resetDoctors();
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "Doctors have been reset");
                    } else {
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You cannot use this command");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("list")) {
                    List<UUID> d = container.getAllDoctors();
                    if (!d.isEmpty()) {
                        List<OfflinePlayer> doctors = new LinkedList<>();
                        for (UUID uuid : d) {
                            for (OfflinePlayer pl : Bukkit.getOfflinePlayers()) {
                                if (pl.getUniqueId().equals(uuid)) {
                                    doctors.add(pl);
                                }
                            }
                        }
                        String message = ChatColor.BOLD + "Doctors-\n";
                        for (OfflinePlayer player : doctors) {
                            message += player.getName();
                        }
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(ChatColor.BOLD + "No doctors");
                    }
                    return true;
                }
            }
            return false;
        } else if (cmd.getName().equalsIgnoreCase("changegender")) {   //changegender command
            if (args.length == 1) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (!OP_REQ || player.isOp()) {
                        MyHorse horse;
                        if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                            horse = container.getHorse(player.getVehicle().getUniqueId());
                        } else {
                            horse = container.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
                        }
                        if (horse == null) {
                            player.sendMessage("No horse selected");
                            return true;
                        }
                        String genderStr;
                        if (args[0].equalsIgnoreCase("stallion")) {
                            horse.setGender(MyHorse.STALLION);
                            genderStr = ChatColor.DARK_RED + "Stallion";
                        } else if (args[0].equalsIgnoreCase("mare")) {
                            horse.setGender(MyHorse.MARE);
                            genderStr = ChatColor.DARK_PURPLE + "Mare";
                        } else if (args[0].equalsIgnoreCase("gelding")) {
                            horse.setGender(MyHorse.GELDING);
                            genderStr = ChatColor.DARK_AQUA + "Gelding";
                        } else {
                            return false;
                        }
                        sender.sendMessage(ChatColor.BOLD + "Gender set to " + genderStr);
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
            if (args.length >= 1) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (!OP_REQ || player.isOp()) {
                        MyHorse horse;
                        if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                            horse = container.getHorse(player.getVehicle().getUniqueId());
                        } else {
                            horse = container.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
                        }
                        if (horse == null) {
                            player.sendMessage("No horse selected");
                            return true;
                        }
                        HorseBreed br1 = null;
                        HorseBreed br2 = null;
                        switch (args.length) {
                            case 2: {
                                for (HorseBreed br : HorseBreed.values()) {
                                    if (br.name().equalsIgnoreCase(args[1])) {
                                        br2 = br;
                                        break;
                                    }
                                }
                            }
                            case 1: {
                                for (HorseBreed br : HorseBreed.values()) {
                                    if (br.name().equalsIgnoreCase(args[0])) {
                                        br1 = br;
                                        break;
                                    }
                                }
                            }
                        }
                        if (br2 == null) {
                            horse.setBreed(new HorseBreed[]{br1, br1});
                        } else {
                            horse.setBreed(new HorseBreed[]{br1, br2});
                        }
                        sender.sendMessage(ChatColor.BOLD + "Breed set to " + horse.getBreed()[0].toString());
                    }
                } else {
                    sender.sendMessage("Only ops can use this command");
                }
            } else {
                return false;
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("setpersonality")) {   //setpersonality command
            if (args.length == 2) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (!OP_REQ || player.isOp()) {
                        MyHorse horse;
                        if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                            horse = container.getHorse(player.getVehicle().getUniqueId());
                        } else {
                            horse = container.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
                        }
                        if (args[0].equalsIgnoreCase(args[1])) {
                            player.sendMessage("You must select two different personalities");
                        }
                        Personality p1 = null;
                        Personality p2 = null;
                        for (Personality p : Personality.values()) {
                            if (p.toString().equalsIgnoreCase(args[0])) {
                                p1 = p;
                                break;
                            }
                        }
                        for (Personality p : Personality.values()) {
                            if (p.toString().equalsIgnoreCase(args[1])) {
                                p2 = p;
                                break;
                            }
                        }
                        if (p1 == null || p2 == null) {
                            player.sendMessage("Some personalities could not be found");
                            return true;
                        }
                        horse.setPersonalities(p1, p2);
                        sender.sendMessage(ChatColor.BOLD + "Personality set to " + p1.toString() + " and " + p2.toString());
                    }
                } else {
                    sender.sendMessage("Only ops can use this command");
                }
            } else {
                return false;
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("showbreeds")) {   //showbreeds command
            String breeds = "";
            for (HorseBreed br : HorseBreed.values()) {
                breeds += br.toString() + ", ";
            }
            sender.sendMessage(breeds);
            return true;
        } else if (cmd.getName()
                .equalsIgnoreCase("showtraits")) {   //showtraits command
            String traits = "";
            for (Personality per : Personality.values()) {
                traits += per.toString() + ", ";
            }
            sender.sendMessage(traits);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("setage")) {   //setage command
            if (args.length == 1) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (!OP_REQ || player.isOp()) {
                        MyHorse mh;
                        if (player.getVehicle() != null && player.getVehicle() instanceof Horse) {
                            final Horse horse = (Horse) player.getVehicle();
                            mh = container.getHorse(horse.getUniqueId());
                        } else {
                            mh = container.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
                        }
                        if (mh == null) {
                            sender.sendMessage("No horse selected");
                            return true;
                        }
                        try {
                            int months = Integer.parseInt(args[0]);
                            if (months >= 300) {
                                player.sendMessage("Must be value under 300");
                                return true;
                            }
                            mh.setAgeInMonths(months);
                            player.sendMessage("Age set");
                        } catch (NumberFormatException ex) {
                            player.sendMessage("Must enter a number for months");
                        }
                        return true;
                    }
                    return false;
                }
                sender.sendMessage("Only ops can use this command");
                return true;
            }
            return false;
        } else if (cmd.getName().equalsIgnoreCase("doctortool")) {   //doctortool command
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                if (container.isDoctor(player)) {
                    final PlayerInventory inventory = player.getInventory();
                    final ItemStack doctorTool = new ItemStack(Material.STICK, 1);
                    final ItemMeta im = doctorTool.getItemMeta();
                    im.setDisplayName(DOCTOR_TOOL);
                    final List<String> comments = new ArrayList<>();
                    comments.add("Used to vaccinate horses.");
                    comments.add("Vaccinations last for 4 weeks");
                    im.setLore(comments);
                    doctorTool.setItemMeta(im);
                    inventory.addItem(doctorTool);
                } else {
                    sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Only a doctor can use this command");
                }
            } else {
                sender.sendMessage("Only a player can use this command");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("eqhelp")) {   //eqhelp command
            boolean op = false;
            boolean console = false;
            boolean doctor = false;
            if (sender instanceof Player) {
                op = ((Player) sender).isOp();
                doctor = container.isDoctor((Player) sender);
            } else {
                console = true;
            }

            if (op || console) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/equestristatus - " + ChatColor.RESET + "shows horse numbers");
            }
            if (op) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/createhorse - " + ChatColor.RESET + "create a horse");
            }
            if (!console) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/geldingtool - " + ChatColor.RESET + "spawn the gelding tool");
            }
            if (!console) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/horsewand - " + ChatColor.RESET + "spawn the horse wand tool");
            }
            if (doctor) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/horsemedicine - " + ChatColor.RESET + "spawn the horse healing tool");
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/vaccination - " + ChatColor.RESET + "spawn the vaccination tool");
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/doctortool - " + ChatColor.RESET + "spawn the doctor tool for checking a horses health");
            }
            if (op || console) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/doctor add <player> - " + ChatColor.RESET + "make a player a doctor");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/doctor remove <player> - " + ChatColor.RESET + "remove a doctor");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/doctor reset - " + ChatColor.RESET + "reset the doctors");
            }
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/doctor list - " + ChatColor.RESET + "list all doctors");
            if (op || console) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/farrier add <player> - " + ChatColor.RESET + "make a player a farrier");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/farrier remove <player> - " + ChatColor.RESET + "remove a farrier");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/farrier reset - " + ChatColor.RESET + "reset the farriers");
            }
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/farrier list - " + ChatColor.RESET + "list all farriers");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/farrier tool - " + ChatColor.RESET + "equip the farriers tool (farriers only)");
            if (op) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/changegender <stallion|gelding|mare> - " + ChatColor.RESET + "set the gender of a horse. Must be on the horse");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/setbreed <breed> - " + ChatColor.RESET + "set the breed of the horse");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/setpersonality <personality> - " + ChatColor.RESET + "set the personality of the horse");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/setage <age> - " + ChatColor.RESET + "set the age of the horse in months");
            }
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/showbreeds - " + ChatColor.RESET + "show the list of breeds");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/showtraits - " + ChatColor.RESET + "show the list of personalities");
            if (op || console) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/savehorses - " + ChatColor.RESET + "save the horses to file");
            }
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/eqhelp - " + ChatColor.RESET + "shows this message");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("cleanup")) {
            if (sender instanceof Player) {
                sender.sendMessage("This command can only be run from the console");
                return true;
            }
            final Runnable run = new Runnable() {
                @Override
                public void run() {
                    container.cleanHorses();
                }
            };
            final Thread thread = new Thread(run, "Cleanup_Thread");
            thread.start();
        } else if (cmd.getName().equalsIgnoreCase("eqh")) {
            if (args.length >= 1) {
                String arg = args[0];
                if (arg.equalsIgnoreCase("kill")) {
                    if (sender instanceof Player) {
                        final Player player = (Player) sender;
                        if (player.isOp()) {
                            MyHorse mh;
                            if (player.getVehicle() != null && player.getVehicle() instanceof Horse) {
                                final Horse horse = (Horse) player.getVehicle();
                                mh = container.getHorse(horse.getUniqueId());
                            } else {
                                mh = container.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
                            }
                            if (mh == null) {
                                sender.sendMessage("No horse selected");
                                return true;
                            }
                            mh.kill();
                            return true;
                        }
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("times")) {
                    HorseCheckerThread.SHOW_TIME = !HorseCheckerThread.SHOW_TIME;
                } else if (args[0].equalsIgnoreCase("motd")) {
                    motd = ChatColor.RESET + "";
                    for (int i = 1; i < args.length; i++) {
                        motd += args[i] + " ";
                    }
                    for (int i = 0; i < motd.length(); i++) {
                        char c = motd.charAt(i);
                        if (c == '&') {
                            char code = motd.charAt(i + 1);
                            ChatColor color = ChatColor.getByChar(code);
                            String s = new String(new char[]{c, code});
                            motd = motd.replaceAll(s, color.toString());
                        }
                    }
                    sender.sendMessage("MOTD set to-\n" + motd);
                }
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("farrier")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("list")) {
                    List<UUID> f = container.getAllFarriers();
                    if (!f.isEmpty()) {
                        List<OfflinePlayer> farriers = new LinkedList<>();
                        for (UUID uuid : f) {
                            for (OfflinePlayer pl : Bukkit.getOfflinePlayers()) {
                                if (pl.getUniqueId().equals(uuid)) {
                                    farriers.add(pl);
                                }
                            }
                        }
                        String message = ChatColor.BOLD + "Farriers-\n";
                        for (OfflinePlayer player : farriers) {
                            message += player.getName();
                        }
                        sender.sendMessage(message);
                    } else {
                        sender.sendMessage(ChatColor.BOLD + "No farriers");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("reset")) {
                    if (sender.isOp() || !(sender instanceof Player)) {
                        container.resetFarriers();
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "Farriers reset");
                    } else {
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You cannot use this command");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("tool")) {
                    if (sender instanceof Player) {
                        final Player player = (Player) sender;
                        if (container.isFarrier(player)) {
                            final PlayerInventory inventory = player.getInventory();
                            final ItemStack doctorTool = new ItemStack(Material.TRIPWIRE_HOOK, 1);
                            final ItemMeta im = doctorTool.getItemMeta();
                            im.setDisplayName(FARRIER_TOOL);
                            final List<String> comments = new ArrayList<>();
                            comments.add("Used to shod a horse");
                            im.setLore(comments);
                            doctorTool.setItemMeta(im);
                            inventory.addItem(doctorTool);
                        } else {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Only a farrier can use this command");
                        }
                    } else {
                        sender.sendMessage("Only a player can use this command");
                    }
                    return true;
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("add")) {
                    if (sender.isOp() || !(sender instanceof Player)) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                        if (player == null) {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Player not found");
                            return true;
                        }
                        if (container.isFarrier(player)) {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Already a farrier");
                            return true;
                        }
                        container.addFarrier(player);
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + args[1] + " is now a farrier");
                        if (player.isOnline()) {
                            Player p = (Player) player;
                            p.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + "You are now a farrier!");
                        }
                    } else {
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You cannot use this command");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("remove")) {
                    if (sender.isOp() || !(sender instanceof Player)) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                        if (player == null) {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Player not found");
                            return true;
                        }
                        if (container.removeFarrier(player)) {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.GREEN + args[1] + " is no longer a farrier");
                            if (player.isOnline()) {
                                Player p = (Player) player;
                                p.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are now longer a farrier");
                            }
                        } else {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + args[1] + " is not a farrier");
                        }
                    } else {
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You cannot use this command");
                    }
                    return true;
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("setlevel")) {
            if (args.length == 1) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (!OP_REQ || player.isOp()) {
                        MyHorse mh;
                        if (player.getVehicle() != null && player.getVehicle() instanceof Horse) {
                            final Horse horse = (Horse) player.getVehicle();
                            mh = container.getHorse(horse.getUniqueId());
                        } else {
                            mh = container.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
                        }
                        if (mh == null) {
                            sender.sendMessage("No horse selected");
                            return true;
                        }
                        try {
                            int level = Integer.parseInt(args[0]);
                            if (level < 1 || level > 10) {
                                player.sendMessage("Level must be between 1 and 10");
                                return true;
                            }
                            mh.setTrainingLevel(level);
                            player.sendMessage("Level set");
                        } catch (NumberFormatException ex) {
                            player.sendMessage("Must enter a number for months");
                        }
                        return true;
                    }
                    return false;
                }
                sender.sendMessage("Only ops can use this command");
                return true;
            }
            return false;
        } else if (cmd.getName().equalsIgnoreCase("navigator")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command");
            }
            Player player = (Player) sender;
            final PlayerInventory inventory = player.getInventory();
            final ItemStack navTool = new ItemStack(Material.COMPASS, 1);
            final ItemMeta im = navTool.getItemMeta();
            im.setDisplayName(NAVIGATOR_TOOL);
            final List<String> comments = new ArrayList<>();
            comments.add("Used to navigate Equestricraft");
            im.setLore(comments);
            navTool.setItemMeta(im);
            inventory.addItem(navTool);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("build")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command");
                return true;
            }
            final Player player = (Player) sender;
            if (args.length < 3) {
                return false;
            }
            try {
                String op = args[0];
                int width = Integer.parseInt(args[1]);
                int height = Integer.parseInt(args[2]);
                if (width <= 0 || height <= 0) {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must be a value greater than 0");
                    return true;
                }
                double value = width * height;
                if (op.equalsIgnoreCase("pay")) {
                    if (!economy.has(player, value)) {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You do not have enough money");
                        return true;
                    }
                    economy.withdrawPlayer(player, value);
                    player.sendMessage(ChatColor.AQUA + "$" + new DecimalFormat("0").format(value) + ChatColor.GREEN + " has been withdrawn for a build");
                    Bukkit.getLogger().log(Level.INFO, player.getName() + " has paid $" + new DecimalFormat("0").format(value) + " for a build");
                } else if (op.equalsIgnoreCase("enq")) {
                    player.sendMessage(ChatColor.GREEN + "This build will cost " + ChatColor.AQUA + "$" + new DecimalFormat("0").format(value) + ChatColor.GREEN + ". /build pay " + width + " " + height + " to pay");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must enter a numerical value");
            }
            return true;
        }
        return false;
    }

    /**
     * This method ejects players from horses when they leave.
     *
     * @param evt the PlayerQuitEvent.
     */
    @EventHandler
    public void onLeave(PlayerQuitEvent evt) {
        Player player = evt.getPlayer();
        Entity e = player.getVehicle();
        if (e == null || !(e instanceof Horse)) {
            return;
        }
        Horse h = (Horse) e;
        h.eject();
    }

    /**
     * This method will get the horses from a chunk load event and ensure pair
     * them with horses from the file.
     *
     * @param event
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        try {
            for (final Entity e : event.getChunk().getEntities()) {
                if (e.getType() == EntityType.HORSE) {
                    final MyHorse mh = container.getHorse(e.getUniqueId());
                    if (mh == null) {
                        container.addHorse(mh);
                    } else {
                        container.getHorse(e.getUniqueId()).setHorse((Horse) e);
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error on chunk load", e);
        }
    }

    @EventHandler
    public void onFoodChangeEvent(FoodLevelChangeEvent evt) {
        if (BLOCK_HUNGER) {
            evt.setCancelled(true);
        }
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
                    event.setCancelled(true);
                    if (event.getEntity() instanceof Horse) { //Check it was a horse they are hitting.
                        final MyHorse horse = container.getHorse(event.getEntity().getUniqueId()); //Get the Horse instance.
                        if (horse.getGender() != MyHorse.STALLION) { //Check it was a stallion.
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "This horse is not a stallion");
                            return;
                        }
                        horse.setGender(MyHorse.GELDING); //Turn the horse into a gelding.
                        player.sendMessage(ChatColor.BOLD + "This horse has been gelded");
                    }
                    break;
                case STICK: //Horse wand
                    //Horse checking stick
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the stick has a display name.
                        return;
                    }
                    if (inHand.getItemMeta().getDisplayName().equals(STICK_NAME)) { //Check the stick is the horse wand.
                        event.setCancelled(true);
                        if (event.getEntity() instanceof Horse) {
                            MyHorse horse = container.getHorse(event.getEntity().getUniqueId()); //Get the horse that was clicked on.
                            if (horse == null) {
                                player.sendMessage("This horse has no details assigned");
                                return;
                            }
                            boolean sickness = horse.isSick();
                            int gender = horse.getGender();
                            boolean hunger = horse.isHungry();
                            boolean thirst = horse.isThirsty();
                            boolean vaccination = horse.isVaccinated();
                            boolean shod = horse.isShod();
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
                            String brStr;
                            if (horse.getBreed().length == 1) {
                                brStr = horse.getBreed()[0].toString();
                            } else {
                                if (horse.getBreed()[0] == horse.getBreed()[1]) {
                                    brStr = horse.getBreed()[0] + "";
                                } else {
                                    brStr = horse.getBreed()[0] + " X " + horse.getBreed()[1];
                                }
                            }
                            final String breedStr = ChatColor.BOLD + "Breed: " + ChatColor.RESET + brStr;
                            final String personalityStr = ChatColor.BOLD + "Personalites: " + ChatColor.RESET + horse.getPersonalities()[0].toString() + ", " + horse.getPersonalities()[1].toString();
                            final String ageStr = ChatColor.BOLD + "Age: " + durToStringYears(horse.getAgeInMonths()) + " old";
                            String sickSince = durToString(horse.getIllDuration());
                            final String sickStr = ChatColor.BOLD + "Health: " + ChatColor.RESET + "" + (sickness ? ChatColor.RED + "Ill" + ChatColor.RESET + " for " + sickSince : ChatColor.GREEN + "Well") + (sickness ? ChatColor.RED + "\nYou will need to take the horse to a vet to find out the exact illness." : "");
                            String hungerSince = durToString(horse.getHungerDuration());
                            final String hungerStr = ChatColor.BOLD + "Hunger: " + ChatColor.RESET + "" + (hunger ? ChatColor.RED + "Hungry" + ChatColor.RESET + " for " + hungerSince : ChatColor.GREEN + "Not Hungry");
                            String thirstSince = durToString(horse.getThristDuration());
                            final String thirstStr = ChatColor.BOLD + "Thirst: " + ChatColor.RESET + "" + (thirst ? ChatColor.RED + "Thirsty" + ChatColor.RESET + " for " + thirstSince : ChatColor.GREEN + "Not Thirsty");
                            final String vaccinationStr = ChatColor.BOLD + "Vaccinated: " + ChatColor.RESET + "" + (vaccination ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No");
                            final String shodStr = ChatColor.BOLD + "Shoed: " + ChatColor.RESET + "" + (shod ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No");
                            final String levelStr = ChatColor.BOLD + "Training Level: " + ChatColor.RESET + horse.getTrainingLevel();
                            player.sendMessage(">------------------------------<");
                            player.sendMessage(genderStr);
                            player.sendMessage(breedStr);
                            player.sendMessage(personalityStr);
                            player.sendMessage(ageStr);
                            player.sendMessage(sickStr);
                            player.sendMessage(hungerStr);
                            player.sendMessage(thirstStr);
                            player.sendMessage(vaccinationStr);
                            player.sendMessage(shodStr);
                            player.sendMessage(levelStr);
                            player.sendMessage(">------------------------------<");
                        } else {
                            player.sendMessage("You must click on a horse");
                        }
                    } else if (inHand.getItemMeta().getDisplayName().equals(DOCTOR_TOOL)) {
                        if (event.getEntity() instanceof Horse) {
                            event.setCancelled(true);
                            MyHorse horse = container.getHorse(event.getEntity().getUniqueId()); //Get the horse that was clicked on.
                            if (horse == null) {
                                player.sendMessage(ChatColor.RED + "Error checking horse");
                                return;
                            }
                            boolean sickness = horse.isSick();
                            if (sickness) {
                                Illness illness = horse.getIllness();
                                if (illness == null) {
                                    while (true) {
                                        illness = Illness.randomIllness();
                                        if (horse.getGender() == MyHorse.MARE) {
                                            if (illness != Illness.MareReproductiveLoss) {
                                                break;
                                            }
                                        } else {
                                            break;
                                        }
                                    }
                                }
                                if (illness != null) {
                                    player.sendMessage(ChatColor.BOLD + "This horse has got " + ChatColor.AQUA + illness.toString());
                                } else {
                                    player.sendMessage("Unknown illness");
                                }
                            } else {
                                player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Horse is not sick");
                            }
                        } else {
                            player.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You must click on a horse");
                        }
                    }
                    break;
                case BLAZE_ROD: //Vaccination
                    if (!inHand.getItemMeta().hasDisplayName()) {
                        return;
                    }
                    if (inHand.getItemMeta().getDisplayName().equals(VACCINE_NAME)) {
                        event.setCancelled(true);
                        if (event.getEntity() instanceof Horse) {
                            final MyHorse horse = container.getHorse(event.getEntity().getUniqueId());
                            horse.setVaccinated(true);
                            player.sendMessage(ChatColor.BOLD + "Horse has been vaccinated");
                        }
                    } else if (inHand.getItemMeta().getDisplayName().equals(ONE_USE_VACCINATION)) {
                        event.setCancelled(true);
                        if (event.getEntity() instanceof Horse) {
                            final MyHorse horse = container.getHorse(event.getEntity().getUniqueId());
                            horse.setVaccinated(true);
                            player.sendMessage(ChatColor.BOLD + "Horse has been vaccinated");
                            if (inHand.getAmount() > 1) {
                                inHand.setAmount(inHand.getAmount() - 1);
                            } else {
                                player.getInventory().removeItem(inHand);
                            }
                        }
                    }
                    break;
                case REDSTONE_TORCH_ON: //Healing
                    if (!inHand.getItemMeta().hasDisplayName()) {
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(POTION_NAME)) {
                        return;
                    }
                    event.setCancelled(true);
                    if (event.getEntity() instanceof Horse) {
                        final MyHorse horse = container.getHorse(event.getEntity().getUniqueId());
                        horse.setSick(false);
                        player.sendMessage(ChatColor.BOLD + "Horse has been cured");
                    }
                    break;
                case TRIPWIRE_HOOK: //Farrier tool
                    if (!inHand.getItemMeta().hasDisplayName()) {
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(FARRIER_TOOL)) {
                        return;
                    }
                    event.setCancelled(true);
                    if (event.getEntity() instanceof Horse) {
                        final MyHorse horse = container.getHorse(event.getEntity().getUniqueId());
                        horse.setShod(true);
                        player.sendMessage(ChatColor.BOLD + "Horse has been shod");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void playerUse(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final ItemStack inHand = player.getItemInHand(); //Get the item in hand.
        if (inHand == null) {
            return;
        }
        if (null != inHand.getType()) {
            switch (inHand.getType()) {
                case STICK: //Horse wand
                    if (!(event.getRightClicked() instanceof Horse)) {
                        return;
                    }
                    //Horse checking stick
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the shears have a display name.
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(STICK_NAME)) { //Check it is the horse wand.
                        return;
                    }
                    if (!OP_REQ || player.isOp()) {
                        final Horse horse = (Horse) event.getRightClicked(); //Get the horse that was clicked on.
                        MyHorse mh = container.getHorse(horse.getUniqueId());
                        if (mh == null) {
                            mh = new MyHorse(horse);
                            container.addHorse(mh);
                        }
                        player.setMetadata("horse", new FixedMetadataValue(EquestriCraftPlugin.plugin, horse.getUniqueId()));
                        player.sendMessage("You are now editing this horse");
//                    if (horse.getTarget() == null) {
//                        horse.setTarget(player);
//                        player.sendMessage("Horse will follow you");
//                    } else {
//                        horse.setTarget(null);
//                        player.sendMessage("Horse will not follow you");
//                    }
                    }
                    event.setCancelled(true);
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final ItemStack inHand = player.getItemInHand(); //Get the item in hand.
        if (inHand == null) {
            return;
        }
        if (null != inHand.getType()) {
            switch (inHand.getType()) {
                case COMPASS:
                    if (!inHand.getItemMeta().hasDisplayName()) {
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(NAVIGATOR_TOOL)) {
                        return;
                    }
                    event.setCancelled(true);
                    player.openInventory(navigator);
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getLastPlayed() == 0) {
            final PlayerInventory inventory = player.getInventory();
            final ItemStack navTool = new ItemStack(Material.COMPASS, 1);
            final ItemMeta im = navTool.getItemMeta();
            im.setDisplayName(NAVIGATOR_TOOL);
            final List<String> comments = new ArrayList<>();
            comments.add("Used to navigate Equestricraft");
            im.setLore(comments);
            navTool.setItemMeta(im);
            inventory.addItem(navTool);
        }
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        event.setMotd(event.getMotd() + motd);
    }

    /**
     * Convert a long durations in ms to a string displaying the days and hours.
     *
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

    /**
     * Convert a long durations in ms to a string displaying the months and
     * years where a year lasts one month.
     *
     * @param dur the duration in ms as a long.
     * @return String displaying the months and years
     */
    private String durToStringYears(double m) {
        final int years = (int) Math.floor(m / 12);
        final int months = (int) (m - (years * 12));
        return "" + ChatColor.BOLD + ChatColor.AQUA + years + ChatColor.RESET + (years == 1 ? " year" : " years") + " and " + ChatColor.BOLD + ChatColor.AQUA + months + ChatColor.RESET + (months == 1 ? " month" : " months");
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent evt) {
        if (evt.getEntityType() == EntityType.HORSE) {
            final Horse horse = (Horse) evt.getEntity();
            MyHorse mh = new MyHorse(horse);
            horse.setBaby();
            if (evt.getEntity().getMetadata("breed").size() > 1) {
                String breed = evt.getEntity().getMetadata("breed").get(0).asString();
                HorseBreed br = HorseBreed.valueOf(breed);
                mh.setBreed(new HorseBreed[]{br});
                if (br == HorseBreed.Donkey) {
                    horse.setVariant(Horse.Variant.DONKEY);
                } else if (br == HorseBreed.Mule) {
                    horse.setVariant(Horse.Variant.MULE);
                } else if (br == HorseBreed.FjordHorse) {
                    double d = Math.random();
                    if (d > 0.5) {
                        horse.setVariant(Horse.Variant.SKELETON_HORSE);
                    } else {
                        horse.setVariant(Horse.Variant.UNDEAD_HORSE);
                    }
                }
            }
            container.addHorse(mh);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().equals(navigator.getName())) {
            return;
        }
        event.setCancelled(true);
        player.closeInventory();
        World w = Bukkit.getWorld("Equestricraft");
        Location l;
        if (clicked.getType() == Material.MONSTER_EGG) {
            //Spawn
            l = new Location(w, 4111, 5, -2264, 180, 0);
            player.sendMessage("Teleporting to Spawn...");
        } else if (clicked.getType() == Material.NAME_TAG) {
            //Lease Barn
            l = new Location(w, 621, 4.8, 2078);
            player.sendMessage("Teleporting to the Lease Barn...");
        } else if (clicked.getType() == Material.SADDLE) {
            //Race Track
            l = new Location(w, -2268, 4.8, 10966);
            player.sendMessage("Teleporting to the Race Track...");
        } else if (clicked.getType() == Material.GOLD_BARDING) {
            //Rescue
            l = new Location(w, -856, 4.8, 473, 90, 0);
            player.sendMessage("Teleporting to the Rescue...");
        } else if (clicked.getType() == Material.WOOD) {
            //Town
            l = new Location(w, 161, 4.8, 1460, 180, 0);
            player.sendMessage("Teleporting to the Town...");
        } else if (clicked.getType() == Material.MAP) {
            //Trails
            l = new Location(w, -3199, 4.8, 4800);
            player.sendMessage("Teleporting to the Trails...");
        } else if (clicked.getType() == Material.FISHING_ROD) {
            //Showgrounds
            l = new Location(w, 653, 4.8, 1501);
            player.sendMessage("Teleporting to the Showgrounds...");
        } else if (clicked.getType() == Material.SHEARS) {
            //Dentist
            l = new Location(w, -93, 4.8, 1166);
            player.sendMessage("Teleporting to the Dentist...");
        } else if (clicked.getType() == Material.BOOK) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
            BookMeta bm = (BookMeta) book.getItemMeta();
            String page1 = "EQUESTRICRAFT\n\n"
                    + "[1] Do not log off on horses.\n"
                    + "[2] Do not use strong language.\n"
                    + "[3] Respect others.\n"
                    + "[4] Griefing is not tolerated\n"
                    + "[5] Breeding is not aloud until advanced+\n"
                    + "[6] You can not Spawn or take horses from the wild.";

            bm.addPage("1");
            bm.setPage(1, page1);
            bm.setAuthor("EquestriCraft");
            bm.setTitle("Rules");
            book.setItemMeta(bm);

            player.getInventory().addItem(book);
            return;
        } else {
            return;
        }

        player.teleport(l);
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
            OP_REQ = properties.getProperty("OP_REQ", "TRUE").equals("TRUE");
            BLOCK_HUNGER = Boolean.parseBoolean(properties.getProperty("BLOCK_HUNGER", Boolean.toString(BLOCK_HUNGER)));
            saveProperties();
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
            properties.setProperty("OP_REQ", (OP_REQ ? "TRUE" : "FALSE"));
            properties.setProperty("BLOCK_HUNGER", Boolean.toString(BLOCK_HUNGER));
            properties.store(os, null);
        } catch (FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
}
