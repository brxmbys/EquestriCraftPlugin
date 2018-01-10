/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import io.github.davidg95.equestricraftplugin.HorseCheckerThread.BuckThread;
import io.github.davidg95.equestricraftplugin.auctions.*;
import io.github.davidg95.equestricraftplugin.database.*;
import io.github.davidg95.equestricraftplugin.disciplines.*;
import io.github.davidg95.equestricraftplugin.race.*;
import java.io.*;
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
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author David
 */
public class EquestriCraftPlugin extends JavaPlugin implements Listener {

    private HorseCheckerThread checkerThread;
    private BuckThread buckThread;

    public static final String MEDICINE = "Healer";
    public static final String GELDING_TOOL = "Gelding Shears";
    public static final String HORSE_WAND = "Horse checking wand";
    public static final String VACCINATION_TOOL = "Vaccination";
    public static final String ONE_USE_VACCINATION = "One use vaccination";
    public static int ONE_USE_COST = 150;
    public static final String DOCTOR_TOOL = "Doctor's Tool";
    public static final String FARRIER_TOOL = "Farrier's Tool";
    public static final String DENTIST_TOOL = "Dentist's Tool";
    public static final String DENTIST_HEALING_TOOL = "Dentist's Healing Tool";
    public static final String NAVIGATOR_TOOL = "Navigator";

    private Permission doctorPerm;
    private Permission farrierPerm;
    private Permission dentistPerm;

    public static final String BREEDING_APPLE = "Breeding Apple";
    public static int GAPPLE_PRICE = 0;

    public static boolean OP_REQ = true;
    public static boolean BLOCK_HUNGER = true;

    /**
     * The amount of time a horse must wait to breed again.
     */
    public static final long BREED_INTERVAL = 86400000L; //One week.

    public static Economy economy;

    private static final Inventory NAVIGATOR = Bukkit.createInventory(null, 45, "Navigator");

    public Database database;

    public RaceController raceController;
    public AuctionHandler auctionHandler;
    public FoodController foodController;
    public DisciplinesHandler discipinesHander;

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

        NAVIGATOR.setItem(22, spawn);
        NAVIGATOR.setItem(4, leaseBarn);
        NAVIGATOR.setItem(40, raceTrack);
        NAVIGATOR.setItem(24, rescue);
        NAVIGATOR.setItem(20, town);
        NAVIGATOR.setItem(30, trails);
        NAVIGATOR.setItem(31, showgrounds);
        NAVIGATOR.setItem(32, dentist);
        NAVIGATOR.setItem(44, rules);
    }

    private static List<String> createLore(String lore) {
        List<String> list = new LinkedList<>();
        list.add(lore);
        return list;
    }

    @Override
    public void onEnable() {
        doctorPerm = new Permission("equestricraft.role.doctor");
        farrierPerm = new Permission("equestricraft.role.farrier");
        dentistPerm = new Permission("equestricraft.role.dentist");
        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdir();
        }
        setupDatabase();
        try {
            getConfig().load(getDataFolder() + File.separator + "config.yml");
        } catch (IOException | InvalidConfigurationException ex) {
            initConfig();
            getLogger().log(Level.SEVERE, "Error loading config.yml. It has been regenerated.", ex);
        }
        ONE_USE_COST = getConfig().getInt("tools.one_use_vaccination_price");
        GAPPLE_PRICE = getConfig().getInt("tools.gapple_price");
        loadProperties();
        checkerThread = new HorseCheckerThread(this, database);
        checkerThread.start();
        buckThread = checkerThread.new BuckThread();
        buckThread.start();
        getServer().getPluginManager().registerEvents(this, this);
        if (!setupEconomy()) {
            getLogger().log(Level.SEVERE, "Vault not detected, Disciplines, Auctions, Races and Food have been disabled");
        } else {
            this.raceController = new RaceController(this);
            this.auctionHandler = new AuctionHandler(this);
            this.foodController = new FoodController(this, economy, database);
            this.discipinesHander = new DisciplinesHandler(this);
            this.getCommand("disciplines").setExecutor(discipinesHander);
            this.getCommand("food").setExecutor(foodController);
            this.getCommand("auction").setExecutor(auctionHandler);
            this.getCommand("race").setExecutor(raceController);
        }
    }

    private void setupDatabase() {
        database = new SQLite(this);
        database.load();
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

    private void initConfig() {
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

        getConfig().set("misc.eat_limit", Long.toString(MyHorse.EAT_LIMIT / 60000));
        getConfig().set("misc.drink_limit", Long.toString(MyHorse.DRINK_LIMIT / 60000));
        getConfig().set("misc.sick_limit", Long.toString(HorseCheckerThread.SICK_LIMIT / 60000));
        getConfig().set("misc.defecate_interval", Long.toString(HorseCheckerThread.DEFECATE_INTERVAL / 60000));
        getConfig().set("misc.ill_wait", Long.toString(HorseCheckerThread.ILL_WAIT / 60000));
        getConfig().set("misc.sick_probability", Double.toString(HorseCheckerThread.SICK_PROBABILITY));
        getConfig().set("misc.vaccinated_sick_probability", Double.toString(HorseCheckerThread.VACCINATED_PROBABILITY));
        getConfig().set("misc.op_req", (OP_REQ ? "TRUE" : "FALSE"));
        getConfig().set("misc.block_hunger", Boolean.toString(BLOCK_HUNGER));

        getConfig().set("tools.one_use_vaccination_price", 100);
        try {
            getConfig().save(getDataFolder().getAbsolutePath() + File.separator + "config.yml");
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error saving config.yml", ex);
        }
    }

    @Override
    public void onDisable() {
        checkerThread.setRun(false);
        checkerThread = null;
        HandlerList.unregisterAll((Plugin) this);
        raceController.cancelActiveRace();
        auctionHandler.endActiveAuction();
        getLogger().log(Level.INFO, "All threads stopped");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("equestristatus")) {   //equestristatus command
            sender.sendMessage("Horses in database: " + ChatColor.AQUA + database.horseCount(-1));
            sender.sendMessage("Checker Thread: " + (checkerThread.isAlive() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Not Active"));
            sender.sendMessage("Bucking thread: " + (buckThread.isActive() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Not Active"));
            int count = 0;
            for (World world : Bukkit.getWorlds()) {
                count += world.getEntitiesByClass(Horse.class).size();
            }
            sender.sendMessage("Horses currently in world: " + ChatColor.AQUA + count);
            return true;
        } else if (cmd.getName().equalsIgnoreCase("createhorse")) {   //createhorse command
            if (!sender.hasPermission("equestricraft.spawnhorse")) {
                return true;
            }
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                final Horse h = player.getWorld().spawn(player.getLocation(), Horse.class);
                HorseNMS.setSpeed(h, 0.4);
                h.setJumpStrength(0.68);
                final MyHorse mh = new MyHorse(h);
                h.setBaby();
                database.addHorse(mh);
            } else {
                sender.sendMessage("This command can only be run by a player");
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("geldingtool")) {   //geldingtool command
            if (sender instanceof Player) {
                final Player player = (Player) sender;
                if (!player.hasPermission("equestricraft.role.doctor")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this");
                    return true;
                }
                final PlayerInventory inventory = player.getInventory();
                final ItemStack shears = new ItemStack(Material.SHEARS, 1);
                final ItemMeta im = shears.getItemMeta();
                im.setDisplayName(GELDING_TOOL);
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
                if (player.hasPermission(doctorPerm)) {
                    final PlayerInventory inventory = player.getInventory();
                    final ItemStack medicine = new ItemStack(Material.REDSTONE_TORCH_ON, 1);
                    final ItemMeta im = medicine.getItemMeta();
                    im.setDisplayName(MEDICINE);
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
                im.setDisplayName(HORSE_WAND);
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
                if (player.hasPermission(doctorPerm)) {
                    final PlayerInventory inventory = player.getInventory();
                    final ItemStack vaccine = new ItemStack(Material.BLAZE_ROD, 1);
                    final ItemMeta im = vaccine.getItemMeta();
                    im.setDisplayName(VACCINATION_TOOL);
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
        } else if (cmd.getName().equalsIgnoreCase("changegender")) {   //changegender command
            if (args.length == 1) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (!OP_REQ || player.isOp()) {
                        UUID uuid;
                        if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                            uuid = player.getVehicle().getUniqueId();
                        } else {
                            uuid = UUID.fromString(player.getMetadata("horse").get(0).asString());
                        }
                        if (uuid == null) {
                            player.sendMessage(ChatColor.RED + "No horse selected");
                            return true;
                        }
                        String genderStr;
                        if (args[0].equalsIgnoreCase("stallion")) {
                            database.changeGender(uuid, MyHorse.STALLION);
                            genderStr = ChatColor.DARK_RED + "Stallion";
                        } else if (args[0].equalsIgnoreCase("mare")) {
                            database.changeGender(uuid, MyHorse.MARE);
                            genderStr = ChatColor.DARK_PURPLE + "Mare";
                        } else if (args[0].equalsIgnoreCase("gelding")) {
                            database.changeGender(uuid, MyHorse.GELDING);
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
        } else if (cmd.getName().equalsIgnoreCase("setbreed")) {   //setbreed command
            if (args.length >= 1) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (!OP_REQ || player.isOp()) {
                        MyHorse horse;
                        if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                            horse = database.getHorse(player.getVehicle().getUniqueId());
                        } else {
                            horse = database.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
                        }
                        if (horse == null) {
                            player.sendMessage("No horse selected");
                            return true;
                        }
                        Horse h = this.getEntityByUniqueId(horse.getUuid());
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
                        if (args.length == 1) {
                            horse.setBreed(new HorseBreed[]{br1, br1});
                            sender.sendMessage(ChatColor.BOLD + "Breed set to " + horse.getBreed()[0].toString());
                        } else {
                            horse.setBreed(new HorseBreed[]{br1, br2});
                            sender.sendMessage(ChatColor.BOLD + "Breed set to " + horse.getBreed()[0].toString() + " x " + horse.getBreed()[1].toString());
                        }
                        if (br1 == HorseBreed.Donkey) {
                            h.setVariant(Horse.Variant.DONKEY);
                        } else if (br1 == HorseBreed.Mule) {
                            h.setVariant(Horse.Variant.MULE);
                        } else if (br1 == HorseBreed.FjordHorse) {
                            double d = Math.random();
                            if (d > 0.5) {
                                h.setVariant(Horse.Variant.SKELETON_HORSE);
                            } else {
                                h.setVariant(Horse.Variant.UNDEAD_HORSE);
                            }
                        } else {
                            h.setVariant(Horse.Variant.HORSE);
                        }
                        database.saveHorse(horse);
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
                            horse = database.getHorse(player.getVehicle().getUniqueId());
                        } else {
                            horse = database.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
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
                        database.saveHorse(horse);
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
        } else if (cmd.getName().equalsIgnoreCase("showtraits")) {   //showtraits command
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
                            mh = database.getHorse(horse.getUniqueId());
                        } else {
                            mh = database.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
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
                            if (months >= 12) { //Check if the horse can become an adult
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Horse h = getEntityByUniqueId(mh.getUuid());
                                        h.setAdult();
                                    }
                                }.runTask(this);
                            } else {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Horse h = getEntityByUniqueId(mh.getUuid());
                                        h.setBaby();
                                    }
                                }.runTask(this);
                            }
                            database.saveHorse(mh);
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
                if (player.hasPermission(doctorPerm)) {
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
            if (sender.isOp()) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/equestristatus - " + ChatColor.RESET + "shows horse numbers");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/createhorse - " + ChatColor.RESET + "create a horse");
            }
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/horsewand - " + ChatColor.RESET + "spawn the horse wand tool");
            if (sender.hasPermission(doctorPerm)) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/horsemedicine - " + ChatColor.RESET + "spawn the horse healing tool");
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/vaccination - " + ChatColor.RESET + "spawn the vaccination tool");
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/doctortool - " + ChatColor.RESET + "spawn the doctor tool for checking a horses health");
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/geldingtool - " + ChatColor.RESET + "spawn the gelding tool");
            }
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/vet show-online - " + ChatColor.RESET + "shows online vets");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/vet broadcast <message> - " + ChatColor.RESET + "broadcast a message to online vets");
            if (sender.hasPermission(farrierPerm)) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/farrier tool - " + ChatColor.RESET + "spawns the farrier tool");
            }
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/farrier show-online - " + ChatColor.RESET + "shows online farriers");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/farrier broadcast <message> - " + ChatColor.RESET + "broadcast a message to online farriers");

            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/dentist show-online - " + ChatColor.RESET + "shows online dentists");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/dentist broadcast <message> - " + ChatColor.RESET + "broadcast a message to online dentists");

            if (sender.hasPermission(dentistPerm)) {
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/dentist tool - " + ChatColor.RESET + "spawns the dentist tool");
                sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "/dentist healing-tool - " + ChatColor.RESET + "spawns the dentist healing tool");
            }
            if (sender.isOp()) {
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/changegender <stallion|gelding|mare> - " + ChatColor.RESET + "set the gender of a horse. Must be on the horse");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/setbreed <breed> - " + ChatColor.RESET + "set the breed of the horse");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/setpersonality <personality> - " + ChatColor.RESET + "set the personality of the horse");
                sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/setage <age> - " + ChatColor.RESET + "set the age of the horse in months");
            }
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/showbreeds - " + ChatColor.RESET + "show the list of breeds");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/showtraits - " + ChatColor.RESET + "show the list of personalities");
            sender.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "/eqhelp - " + ChatColor.RESET + "shows this message");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("eqh")) {
            if (args.length >= 1) {
                String arg = args[0];
                if (arg.equalsIgnoreCase("kill")) {
                    if (sender instanceof Player) {
                        final Player player = (Player) sender;
                        if (player.isOp()) {
                            UUID uuid = UUID.fromString(player.getMetadata("horse").get(0).asString());
                            if (uuid == null) {
                                sender.sendMessage("No horse selected");
                                return true;
                            }

                            Horse h = getEntityByUniqueId(uuid);
                            if (h != null) {
                                h.setHealth(0);
                                database.removeHorse(uuid);
                            }
                            return true;
                        }
                        return true;
                    }
                } else if (arg.equalsIgnoreCase("times")) {
                    HorseCheckerThread.SHOW_TIME = !HorseCheckerThread.SHOW_TIME;
                    sender.sendMessage("Horse checker times " + (HorseCheckerThread.SHOW_TIME ? "activated" : "deactivated"));
                } else if (arg.equalsIgnoreCase("db")) {
                    if (args.length >= 2) {
                        if (args[1].equalsIgnoreCase("show-hungry")) {
                            int hungry = database.hungryHorses();
                            sender.sendMessage("There are " + hungry + " hungry horses");
                        } else if (args[1].equalsIgnoreCase("show-thirsty")) {
                            int thirsty = database.thirstyHorses();
                            sender.sendMessage("There are " + thirsty + " thirsty horses");
                        } else if (args[1].equalsIgnoreCase("show-ill")) {
                            int ill = database.illHorses();
                            sender.sendMessage("There are " + ill + " ill horses");
                        } else if (args[1].equalsIgnoreCase("show-vaccs")) {
                            int vaccs = database.vaccedHorses();
                            sender.sendMessage("There are " + vaccs + " vacced horses");
                        } else if (args[1].equalsIgnoreCase("show-shoed")) {
                            int shoed = database.shoedHorses();
                            sender.sendMessage("There are " + shoed + " shoed horses");
                        } else if (args[1].equalsIgnoreCase("show-old")) {
                            int old = database.oldHorses(Integer.parseInt(args[2]));
                            sender.sendMessage("There are " + old + " old horses");
                        } else if (args[1].equalsIgnoreCase("show-dead")) {
                            int dead = database.deadHorses();
                            sender.sendMessage("There are " + dead + " dead horses");
                        } else if (args[1].equalsIgnoreCase("kill-dead")) {
                            database.killDead();
                            sender.sendMessage("Dead horses removed from database");
                        }
                    }
                    return true;
                } else if (arg.equalsIgnoreCase("integrity")) {
                    sender.sendMessage("Searching...");
                    int count = 0;
                    for (World world : Bukkit.getWorlds()) {
                        for (Horse h : world.getEntitiesByClass(Horse.class)) {
                            MyHorse mh = database.getHorse(h.getUniqueId());
                            int amount = database.uuidCheck(h.getUniqueId());
                            sender.sendMessage("Database occurances: " + amount);
                            if (mh == null) {
                                count++;
                            }
                        }
                    }
                    sender.sendMessage("Horses not in database: " + count);
                    return true;
                } else if (arg.equalsIgnoreCase("fix")) {
                    for (MyHorse mh : database.getHorses(-1)) {
                        try {
                            if (mh.getBreed() == null || mh.getBreed().length == 0) {
                                mh.setBreed(new HorseBreed[]{HorseBreed.randomType()});
                            }
                        } catch (Exception e) {
                            if (mh == null) {
                                sender.sendMessage("mh is null");
                                continue;
                            }
                            try {
                                mh.setBreed(new HorseBreed[]{HorseBreed.randomType(), HorseBreed.randomType()});
                            } catch (Exception ex) {
                                sender.sendMessage("Error");
                            }
                        }
                    }
                    Iterator<MyHorse> it = database.getHorses(-1).iterator();
                    while (it.hasNext()) {
                        MyHorse mh = it.next();
                        if (mh == null) {
                            it.remove();
                            sender.sendMessage("One null horse has been removed");
                        }
                    }
                    sender.sendMessage("Assigned breeds to horses");
                    return true;
                } else if (arg.equalsIgnoreCase("velo")) {
                    if (args.length > 1) {
                        Player p = Bukkit.getPlayer(args[1]);
                        if (p == null) {
                            sender.sendMessage("Player not found");
                            return true;
                        }
                    }
                } else if (arg.equalsIgnoreCase("allowbreed")) {
                    Player player = (Player) sender;
                    MyHorse horse;
                    if (player.getVehicle() != null || player.getVehicle() instanceof Horse) {
                        horse = database.getHorse(player.getVehicle().getUniqueId());
                    } else {
                        horse = database.getHorse(UUID.fromString(player.getMetadata("horse").get(0).asString()));
                    }
                    if (horse == null) {
                        player.sendMessage("No horse selected");
                        return true;
                    }
                    horse.allowBreed();
                    database.saveHorse(horse);
                    player.sendMessage("This horse can now breed");
                    return true;
                } else if (arg.equalsIgnoreCase("handt")) {
                    database.removeHungerAndThrist();
                    sender.sendMessage("Hunger and thirst reset on ALL horses");
                } else if (arg.equalsIgnoreCase("set")) {
                    Player player = (Player) sender;
                    UUID uuid = UUID.fromString(player.getMetadata("horse").get(0).asString());
                    Horse h = this.getEntityByUniqueId(uuid);
                    MyHorse horse = new MyHorse(h);
                    database.addHorse(horse);
                    sender.sendMessage("Horse set");
                } else if (arg.equalsIgnoreCase("cure-all")) {
                    database.cureAll();
                    sender.sendMessage("All horses have been cured");
                } else if (arg.equalsIgnoreCase("bucking-toggle")) {
                    String message = "Bucking thread " + (buckThread.toggle() ? ChatColor.GREEN + "Activated" : ChatColor.RED + "Deactivated");
                    sender.sendMessage(message);
                    if (sender instanceof Player) {
                        getLogger().log(Level.INFO, message);
                    }
                } else if (arg.equalsIgnoreCase("rl")) {
                    if (sender.isOp()) {
                        loadProperties();
                        sender.sendMessage("config.yml reloaded");
                    }
                }
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("farrier")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("tool")) {
                    if (sender instanceof Player) {
                        final Player player = (Player) sender;
                        if (player.hasPermission(farrierPerm)) {
                            final PlayerInventory inventory = player.getInventory();
                            final ItemStack farrierTool = new ItemStack(Material.TRIPWIRE_HOOK, 1);
                            final ItemMeta im = farrierTool.getItemMeta();
                            im.setDisplayName(FARRIER_TOOL);
                            final List<String> comments = new ArrayList<>();
                            comments.add("Used to shoe a horse");
                            im.setLore(comments);
                            farrierTool.setItemMeta(im);
                            inventory.addItem(farrierTool);
                        } else {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Only a farrier can use this command");
                        }
                    } else {
                        sender.sendMessage("Only a player can use this command");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("show-online")) {
                    String list = ChatColor.GREEN + "Online Farriers-";
                    int count = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission(farrierPerm)) {
                            list += "\n - " + p.getDisplayName() + (p.isOp() ? "*" : "");
                            count++;
                        }
                    }
                    if (count == 0) {
                        sender.sendMessage(ChatColor.RED + "There are no Farriers online");
                    } else {
                        sender.sendMessage(list);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("broadcast")) {
                    if (args.length > 1) {
                        String message = ChatColor.GREEN + "Message from " + sender.getName() + " to all farriers - \n";
                        for (int i = 1; i < args.length; i++) {
                            message += args[i] + " ";
                        }
                        Bukkit.broadcast(message, farrierPerm.getName());
                        return true;
                    }
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("setlevel")) {
            if (args.length == 1) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (!OP_REQ || player.isOp()) {
                        UUID uuid;
                        if (player.getVehicle() != null && player.getVehicle() instanceof Horse) {
                            final Horse horse = (Horse) player.getVehicle();
                            uuid = horse.getUniqueId();
                        } else {
                            uuid = UUID.fromString(player.getMetadata("horse").get(0).asString());
                        }
                        if (uuid == null) {
                            sender.sendMessage("No horse selected");
                            return true;
                        }
                        try {
                            int level = Integer.parseInt(args[0]);
                            if (level < 1 || level > 10) {
                                player.sendMessage("Level must be between 1 and 10");
                                return true;
                            }
                            database.setLevel(uuid, level);
                            player.sendMessage("Level set");
                        } catch (NumberFormatException ex) {
                            player.sendMessage("Must enter a number for level between 1 and 10");
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
                return true;
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
            sender.sendMessage("Use to navigate Equestricraft");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("breeding")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("gapple")) {
                    if (!getConfig().getBoolean("tools.enable_breeding")) {
                        sender.sendMessage(ChatColor.RED + "This command has been disabled for now");
                        return true;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Only players can use this command");
                        return true;
                    }
                    Player player = (Player) sender;
                    if (economy.getBalance(player) >= GAPPLE_PRICE) {
                        economy.withdrawPlayer(player, GAPPLE_PRICE);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have enough money for a Breeding Apple");
                        return true;
                    }
                    final PlayerInventory inventory = player.getInventory();
                    final ItemStack gapple = new ItemStack(Material.GOLDEN_APPLE, 1);
                    final ItemMeta im = gapple.getItemMeta();
                    im.setDisplayName(BREEDING_APPLE);
                    final List<String> comments = new ArrayList<>();
                    comments.add("Breed a horse");
                    im.setLore(comments);
                    gapple.setItemMeta(im);
                    inventory.addItem(gapple);
                    sender.sendMessage(ChatColor.GREEN + "You have purchased a Golden Apple for " + ChatColor.AQUA + "$" + GAPPLE_PRICE);
                    return true;
                } else if (args[0].equalsIgnoreCase("check")) {
                    OfflinePlayer player;
                    if (args.length > 1) {
                        if (sender.hasPermission("equestricraft.breeding.check")) {
                            player = Bukkit.getOfflinePlayer(args[1]);
                        } else {
                            sender.sendMessage(ChatColor.RED + "You do not have permission to check other peoples breeding");
                            return true;
                        }
                    } else if (!(sender instanceof Player)) {
                        sender.sendMessage("Only players can use this command");
                        return true;
                    } else {
                        player = (OfflinePlayer) sender;
                    }
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found");
                        return true;
                    }
                    int breeds = database.getTotalBreeds(player);
                    long lastBreed = database.getLastBreed(player);
                    sender.sendMessage(ChatColor.GREEN + "Total breeds: " + ChatColor.AQUA + breeds);
                    if (lastBreed != -1) {
                        sender.sendMessage(ChatColor.GREEN + "Last breed: " + ChatColor.AQUA + new Date(lastBreed));
                    }
                    return true;
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("dentist")) {
            if (args.length > 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (player.hasPermission(dentistPerm)) {
                        if (args[0].equalsIgnoreCase("tool")) {
                            final PlayerInventory inventory = player.getInventory();
                            final ItemStack dentistTool = new ItemStack(Material.FLINT_AND_STEEL, 1);
                            final ItemMeta im = dentistTool.getItemMeta();
                            im.setDisplayName(DENTIST_TOOL);
                            final List<String> comments = new ArrayList<>();
                            comments.add("For checking a horses teeth");
                            im.setLore(comments);
                            dentistTool.setItemMeta(im);
                            inventory.addItem(dentistTool);
                        } else if (args[0].equalsIgnoreCase("healing-tool")) {
                            final PlayerInventory inventory = player.getInventory();
                            final ItemStack dentistHealingTool = new ItemStack(Material.STRING, 1);
                            final ItemMeta im = dentistHealingTool.getItemMeta();
                            im.setDisplayName(DENTIST_HEALING_TOOL);
                            final List<String> comments = new ArrayList<>();
                            comments.add("Floss for healing teeth");
                            im.setLore(comments);
                            dentistHealingTool.setItemMeta(im);
                            inventory.addItem(dentistHealingTool);
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Only dentists can use this tool");
                    }
                }
                if (args[0].equalsIgnoreCase("show-online")) {
                    String list = ChatColor.GREEN + "Online Dentists-";
                    int count = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission(dentistPerm)) {
                            list += "\n - " + p.getDisplayName() + (p.isOp() ? "*" : "");
                            count++;
                        }
                    }
                    if (count == 0) {
                        sender.sendMessage(ChatColor.RED + "There are no Dentists online");
                    } else {
                        sender.sendMessage(list);
                    }
                } else if (args[0].equalsIgnoreCase("broadcast")) {
                    if (args.length > 1) {
                        String message = ChatColor.GREEN + "Message from " + sender.getName() + " to all dentists - \n";
                        for (int i = 1; i < args.length; i++) {
                            message += args[i] + " ";
                        }
                        Bukkit.broadcast(message, dentistPerm.getName());
                        return true;
                    }
                    return false;
                }
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("vet")) {
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("show-online")) {
                    String list = ChatColor.GREEN + "Online Vets-";
                    int count = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        list += "\n - " + p.getDisplayName() + (p.isOp() ? "*" : "");
                        count++;
                    }
                    if (count == 0) {
                        sender.sendMessage(ChatColor.RED + "There are no online Vets");
                    } else {
                        sender.sendMessage(list);
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("broadcast")) {
                    if (args.length > 1) {
                        String message = ChatColor.GREEN + "Message from " + sender.getName() + " to all vets - \n";
                        for (int i = 1; i < args.length; i++) {
                            message += args[i] + " ";
                        }
                        Bukkit.broadcast(message, doctorPerm.getName());
                        return true;
                    }
                    return false;
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("color")) {
            sender.sendMessage("Color Codes\n"
                    + "&0 - " + ChatColor.BLACK + "Black\n"
                    + ChatColor.RESET + "&1 - " + ChatColor.DARK_BLUE + "Dark Blue\n"
                    + ChatColor.RESET + "&2 - " + ChatColor.DARK_GREEN + "Dark Green\n"
                    + ChatColor.RESET + "&3 - " + ChatColor.DARK_AQUA + "Dark Aqua\n"
                    + ChatColor.RESET + "&4 - " + ChatColor.DARK_RED + "Dark Red\n"
                    + ChatColor.RESET + "&5 - " + ChatColor.DARK_PURPLE + "Dark Purple\n"
                    + ChatColor.RESET + "&6 - " + ChatColor.GOLD + "Gold\n"
                    + ChatColor.RESET + "&7 - " + ChatColor.GRAY + "Gray\n"
                    + ChatColor.RESET + "&8 - " + ChatColor.DARK_GRAY + "Dark Gray\n"
                    + ChatColor.RESET + "&9 - " + ChatColor.BLUE + "Blue\n"
                    + ChatColor.RESET + "&a - " + ChatColor.GREEN + "Green\n"
                    + ChatColor.RESET + "&b - " + ChatColor.AQUA + "Aqua\n"
                    + ChatColor.RESET + "&c - " + ChatColor.RED + "Red\n"
                    + ChatColor.RESET + "&d - " + ChatColor.LIGHT_PURPLE + "Light Purple\n"
                    + ChatColor.RESET + "&e - " + ChatColor.YELLOW + "Yellow\n"
                    + ChatColor.RESET + "&f - " + ChatColor.WHITE + "White\n"
                    + ChatColor.RESET + "Formatting Codes\n"
                    + ChatColor.RESET + "&k - " + ChatColor.MAGIC + "Obfuscated\n"
                    + ChatColor.RESET + "&l - " + ChatColor.BOLD + "Bold\n"
                    + ChatColor.RESET + "&m - " + ChatColor.STRIKETHROUGH + "Strikethrough\n"
                    + ChatColor.RESET + "&n - " + ChatColor.UNDERLINE + "Underline\n"
                    + ChatColor.RESET + "&o - " + ChatColor.ITALIC + "Italic\n"
                    + ChatColor.RESET + "&r - Reset"
            );
            return true;
        }
        return false;
    }

    public Horse getEntityByUniqueId(UUID uniqueId) {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity.getUniqueId().equals(uniqueId)) {
                        return (Horse) entity;
                    }
                }
            }
        }
        return null;
    }

    /**
     * This method ejects players from horses when they leave.
     *
     * @param evt the PlayerQuitEvent.
     */
    @EventHandler
    public void ejectHorseOnLeave(PlayerQuitEvent evt) {
        Player player = evt.getPlayer();
        Entity e = player.getVehicle();
        if (e == null || !(e instanceof Horse)) {
            return;
        }
        Horse h = (Horse) e;
        h.eject();
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
        if (event.getEntity() == null) {
            return;
        }
        if (null != inHand.getType()) {
            switch (inHand.getType()) {
                case SHEARS: //Gelding Tool
                    //Check they have shears in their hand.
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the shears have a display name.
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(GELDING_TOOL)) { //Check the shears are the Gelding Shears.
                        return;
                    }
                    event.setCancelled(true);
                    if (event.getEntity() instanceof Horse) { //Check it was a horse they are hitting.
                        UUID uuid = event.getEntity().getUniqueId(); //Get the Horse instance.
                        int currentGender = database.getGender(uuid);
                        if (currentGender != MyHorse.STALLION) { //Check it was a stallion.
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "This horse is not a stallion");
                            return;
                        }
                        database.changeGender(uuid, MyHorse.GELDING); //Turn the horse into a gelding.
                        player.sendMessage(ChatColor.BOLD + "This horse has been gelded");
                    }
                    break;
                case STICK: //Horse wand
                    //Horse checking stick
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the stick has a display name.
                        return;
                    }
                    if (inHand.getItemMeta().getDisplayName().equals(HORSE_WAND)) { //Check the stick is the horse wand.
                        event.setCancelled(true);
                        if (event.getEntity() instanceof Horse) {
                            MyHorse horse = database.getHorse(event.getEntity().getUniqueId()); //Get the horse that was clicked on.
                            if (horse == null) {
                                player.sendMessage(ChatColor.RED + "Error retrieving horse details. Error code 2");
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
                            final String vaccinationStr = ChatColor.BOLD + "Vaccinated: " + ChatColor.RESET + "" + (vaccination ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No") + "\nVaccinated on " + new Date(horse.getVaccinationTime());
                            final String shodStr = ChatColor.BOLD + "Shoed: " + ChatColor.RESET + "" + (shod ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No");
                            final String levelStr = ChatColor.BOLD + "Training Level: " + ChatColor.RESET + horse.getTrainingLevel();

                            Horse h = this.getEntityByUniqueId(horse.getUuid());

                            String rank = ChatColor.BOLD + "Rank: " + ChatColor.RESET;
                            int r = MyHorse.getRank(h);
                            switch (r) {
                                case 8:
                                    rank += ChatColor.AQUA + "Olympian";
                                    break;
                                case 7:
                                    rank += ChatColor.GOLD + "Elite";
                                    break;
                                case 6:
                                    rank += ChatColor.DARK_AQUA + "Champion";
                                    break;
                                case 5:
                                    rank += ChatColor.GRAY + "Instructor";
                                    break;
                                case 4:
                                    rank += ChatColor.DARK_PURPLE + "Master";
                                    break;
                                case 3:
                                    rank += ChatColor.BLUE + "Advanced";
                                    break;
                                case 2:
                                    rank += ChatColor.GREEN + "Intermediate";
                                    break;
                                case 1:
                                    rank += ChatColor.YELLOW + "Novice";
                                    break;
                                default:
                                    rank += ChatColor.RED + "No Rank";
                            }

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
                            player.sendMessage(rank);
                            player.sendMessage(">------------------------------<");
                        } else {
                            player.sendMessage("You must click on a horse");
                        }
                    } else if (inHand.getItemMeta().getDisplayName().equals(DOCTOR_TOOL)) {
                        if (event.getEntity() instanceof Horse) {
                            event.setCancelled(true);
                            MyHorse horse = database.getHorse(event.getEntity().getUniqueId()); //Get the horse that was clicked on.
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
                    if (inHand.getItemMeta().getDisplayName().equals(VACCINATION_TOOL)) {
                        event.setCancelled(true);
                        if (event.getEntity() instanceof Horse) {
                            UUID uuid = event.getEntity().getUniqueId();
                            database.vaccinateHorse(uuid);
                            player.sendMessage(ChatColor.BOLD + "Horse has been vaccinated");
                        }
                    } else if (inHand.getItemMeta().getDisplayName().equals(ONE_USE_VACCINATION)) {
                        event.setCancelled(true);
                        if (event.getEntity() instanceof Horse) {
                            UUID uuid = event.getEntity().getUniqueId();
                            database.vaccinateHorse(uuid);
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
                    if (!inHand.getItemMeta().getDisplayName().equals(MEDICINE)) {
                        return;
                    }
                    event.setCancelled(true);
                    if (event.getEntity() instanceof Horse) {
                        final MyHorse horse = database.getHorse(event.getEntity().getUniqueId());
                        horse.setSick(false);
                        database.saveHorse(horse);
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
                        UUID uuid = event.getEntity().getUniqueId();
                        database.shoeHorse(uuid);
                        player.sendMessage(ChatColor.BOLD + "Horse has been shoed");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Opens the navigator menu when the player clicks with the compass.
     *
     * @param evt the event.
     */
    @EventHandler
    public void onCompassUse(PlayerInteractEvent evt) {
        if (evt.getItem() == null) {
            return;
        }
        if (evt.getItem().getType() != Material.COMPASS) {
            return;
        }
        if (evt.getItem().getItemMeta().getDisplayName() == null) {
            return;
        }
        if (!evt.getItem().getItemMeta().getDisplayName().equals(NAVIGATOR_TOOL)) {
            return;
        }
        evt.setCancelled(true);
        evt.getPlayer().openInventory(NAVIGATOR);
    }

    @EventHandler
    public void playerUse(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final ItemStack inHand = player.getItemInHand(); //Get the item in hand.
        if (inHand == null) {
            return;
        }
        final Entity rightClicked = event.getRightClicked();
        if (null != inHand.getType()) {
            switch (inHand.getType()) {
                case STICK: //Horse wand
                    if (!(rightClicked instanceof Horse)) {
                        return;
                    }
                    //Horse checking stick
                    if (!inHand.getItemMeta().hasDisplayName()) { //Check the shears have a display name.
                        return;
                    }
                    if (!inHand.getItemMeta().getDisplayName().equals(HORSE_WAND)) { //Check it is the horse wand.
                        return;
                    }
                    if (player.hasPermission("equestricraft.tools.edithorse")) {
                        final Horse horse = (Horse) event.getRightClicked(); //Get the horse that was clicked on.
                        if (database.getHorse(horse.getUniqueId()) == null) {
                            getLogger().log(Level.INFO, "Creating horse in database");
                            MyHorse mh = new MyHorse(horse);
                            database.addHorse(mh);
                        }
                        player.setMetadata("horse", new FixedMetadataValue(this, horse.getUniqueId()));
                        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "You are now editing this horse");
                    }
                    event.setCancelled(true);
                    break;
                case GOLDEN_APPLE:
                    try {
                        event.setCancelled(true);
                        if (!inHand.getItemMeta().hasDisplayName()) {
                            player.sendMessage(ChatColor.RED + "You must purchase a breeding apple using " + ChatColor.AQUA + "/breeding gapple");
                            return;
                        }
                        if (!inHand.getItemMeta().getDisplayName().equals(BREEDING_APPLE)) {
                            return;
                        }
                        if (!(rightClicked instanceof Horse)) {
                            player.sendMessage(ChatColor.RED + "You must click on a horse");
                            return;
                        }
                        if (!getConfig().getBoolean("tools.enable_breeding")) {
                            player.sendMessage(ChatColor.RED + "Breeding disabled");
                            return;
                        }
                        final Horse horse = (Horse) rightClicked;
                        MyHorse mh1 = database.getHorse(horse.getUniqueId());
                        if (database.hadBredRecently(player)) {
                            player.sendMessage(ChatColor.RED + "You have already bred a horse recently");
                            return;
                        }
                        if (mh1.getGender() == MyHorse.GELDING) {
                            player.sendMessage(ChatColor.RED + "Cannot breed a gelding");
                            return;
                        }
                        if (mh1.getDurationSinceLastBreed() < BREED_INTERVAL) {
                            player.sendMessage(ChatColor.RED + "This horse breed " + mh1.getDurationSinceLastBreed() / 1000 / 60 + "m ago, you must wait before it can breed again");
                            return;
                        }
                        if (player.hasMetadata("HORSE_BREED")) {
                            player.sendMessage(ChatColor.GREEN + "Second horse set");
                            List<MetadataValue> md = player.getMetadata("HORSE_BREED");
                            UUID uuid = null;
                            for (MetadataValue m : md) {
                                if (m.getOwningPlugin() == this) {
                                    uuid = UUID.fromString(m.asString());
                                }
                            }
                            if (uuid == null) {
                                player.sendMessage(ChatColor.RED + "Breeding failed. Error code 1");
                                player.removeMetadata("HORSE_BREED", this);
                                return;
                            }
                            MyHorse mh2 = database.getHorse(uuid);
                            if (mh1.getBreed() == mh2.getBreed()) {
                                player.sendMessage(ChatColor.RED + "Horses must be a different gender");
                                return;
                            }
                            mh2.setLastBreed();
                            mh1.setLastBreed();
                            Horse foal = (Horse) horse.getWorld().spawnEntity(horse.getLocation(), EntityType.HORSE);
                            MyHorse mhFoal = new MyHorse(foal);
                            HorseBreed br1 = mh1.getBreed()[0];
                            HorseBreed br2 = mh2.getBreed()[1];
                            mhFoal.setBreed(new HorseBreed[]{br1, br2});
                            if (br1 == HorseBreed.Donkey) {
                                horse.setVariant(Horse.Variant.DONKEY);
                            } else if (br1 == HorseBreed.Mule) {
                                horse.setVariant(Horse.Variant.MULE);
                            } else if (br1 == HorseBreed.FjordHorse) {
                                double d = Math.random();
                                if (d > 0.5) {
                                    horse.setVariant(Horse.Variant.SKELETON_HORSE);
                                } else {
                                    horse.setVariant(Horse.Variant.UNDEAD_HORSE);
                                }
                            } else {
                                horse.setVariant(Horse.Variant.HORSE);
                            }
                            foal.setBaby();
                            HorseNMS.setSpeed(horse, 0.4);
                            foal.setJumpStrength(0.68);
                            database.addHorse(mhFoal);
                            database.saveHorse(mh1);
                            database.saveHorse(mh2);
                            if (inHand.getAmount() == 1) {
                                player.getInventory().removeItem(inHand);
                            } else {
                                inHand.setAmount(inHand.getAmount() - 1);
                            }
                            player.removeMetadata("HORSE_BREED", this);
                            player.sendMessage(ChatColor.GREEN + "Breeding Successful");
                            getLogger().log(Level.INFO, player.getName() + " has bred a horse");
                        } else {
                            player.setMetadata("HORSE_BREED", new FixedMetadataValue(this, horse.getUniqueId().toString()));
                            player.sendMessage(ChatColor.GREEN + "First horse set");
                            if (mh1.getGender() == MyHorse.STALLION) {
                                player.sendMessage(ChatColor.GREEN + "Select a mare");
                            } else {
                                player.sendMessage(ChatColor.GREEN + "Select a stallion");
                            }
                            if (inHand.getAmount() == 0) {
                                final PlayerInventory inventory = player.getInventory();
                                final ItemStack gapple = new ItemStack(Material.GOLDEN_APPLE, 1);
                                final ItemMeta im = gapple.getItemMeta();
                                im.setDisplayName(BREEDING_APPLE);
                                final List<String> comments = new ArrayList<>();
                                comments.add("Breed a horse");
                                im.setLore(comments);
                                gapple.setItemMeta(im);
                                inventory.addItem(gapple);
                            } else {
//                                inHand.setAmount(inHand.getAmount() + 1);
                            }
                        }
                    } catch (Exception e) {
                        getLogger().log(Level.SEVERE, "Error with breeding", e);
                        player.sendMessage(ChatColor.RED + "Error breeding. Error code 2");
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @EventHandler
    public void giveNavigatorNewPlayer(PlayerJoinEvent event) {
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
    public void removeHorseOnDeath(EntityDeathEvent evt) {
        Entity e = evt.getEntity();
        if (!(e instanceof Horse)) {
            return;
        }
        UUID uuid = e.getUniqueId();
        if (uuid == null) {
            return;
        }
        database.removeHorse(uuid);
        getLogger().log(Level.INFO, "A horse was killed, so it has been removed from the database");
    }

    /**
     * Handles players clicking with the navigator
     *
     * @param event the event.
     */
    @EventHandler
    public void onNavigatorClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().equals(NAVIGATOR.getName())) {
            return;
        }
        event.setCancelled(true);
        player.closeInventory();
        World w = Bukkit.getWorld("Equestricraft");
        Location l;
        if (null == clicked.getType()) {
            return;
        } else {
            switch (clicked.getType()) {
                case MONSTER_EGG:
                    //Spawn
                    l = new Location(w, 3442, 7, 215, 90, 0);
                    player.sendMessage("Teleporting to Spawn...");
                    break;
                case NAME_TAG:
                    //Lease Barn
                    l = new Location(w, 621, 4.8, 2078);
                    player.sendMessage("Teleporting to the Lease Barn...");
                    break;
                case SADDLE:
                    //Race Track
                    l = new Location(w, -2268, 4.8, 10966);
                    player.sendMessage("Teleporting to the Race Track...");
                    break;
                case GOLD_BARDING:
                    //Rescue
                    l = new Location(w, -856, 4.8, 473, 90, 0);
                    player.sendMessage("Teleporting to the Rescue...");
                    break;
                case WOOD:
                    //Town
                    l = new Location(w, 161, 4.8, 1460, 180, 0);
                    player.sendMessage("Teleporting to the Town...");
                    break;
                case MAP:
                    //Trails
                    l = new Location(w, -3199, 4.8, 4800);
                    player.sendMessage("Teleporting to the Trails...");
                    break;
                case FISHING_ROD:
                    //Showgrounds
                    l = new Location(w, 635, 4.8, 1498);
                    player.sendMessage("Teleporting to the Showgrounds...");
                    break;
                case SHEARS:
                    //Dentist
                    l = new Location(w, -93, 4.8, 1166);
                    player.sendMessage("Teleporting to the Dentist...");
                    break;
                case BOOK:
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
                default:
                    return;
            }
        }

        player.teleport(l);
        player.closeInventory();
    }

    private void loadProperties() {
        MyHorse.EAT_LIMIT = getConfig().getInt("misc.eat_limit") * 60000;
        MyHorse.DRINK_LIMIT = getConfig().getInt("misc.drink_limit") * 60000;
        HorseCheckerThread.SICK_LIMIT = getConfig().getInt("misc.sick_limit") * 60000;
        HorseCheckerThread.DEFECATE_INTERVAL = getConfig().getInt("misc.defecate_interval") * 60000;
        HorseCheckerThread.ILL_WAIT = getConfig().getLong("misc.ill_wait");
        HorseCheckerThread.SICK_PROBABILITY = getConfig().getInt("misc.sick_probability");
        HorseCheckerThread.VACCINATED_PROBABILITY = getConfig().getInt("misc.vaccinated_sick_probability");
        OP_REQ = getConfig().getBoolean("misc.op_req");
        BLOCK_HUNGER = getConfig().getBoolean("misc.block_hunger");
    }
}
