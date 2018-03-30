/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

/**
 *
 * @author david
 */
public class CustomHandler implements CommandExecutor, Listener {

    private final EquestriCraftPlugin plugin;
    private static final Inventory genderScreen = Bukkit.createInventory(null, 9, "Select Gender");
    private static final Inventory[] breedScreens;
    private static final Inventory personalityScreen1 = Bukkit.createInventory(null, 18, "Select Personality 1");
    private static final Inventory personalityScreen2 = Bukkit.createInventory(null, 18, "Select Personality 2");
    private static final Inventory variantScreen = Bukkit.createInventory(null, 9, "Select Variant");
    private static final Inventory colorScreen = Bukkit.createInventory(null, 9, "Select Color");

    static {
        genderScreen.setItem(3, createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.RED.getData()), "Stallion", "Male horse"));
        genderScreen.setItem(4, createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.PURPLE.getData()), "Mare", "Female horse"));
        genderScreen.setItem(5, createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.LIGHT_BLUE.getData()), "Gelding", "Gelded male horse"));

        double breedCount = HorseBreed.values().length;

        int screenCount = (int) Math.ceil(breedCount / 54d);

        breedScreens = new Inventory[screenCount];
        int screen = 0;
        breedScreens[screen] = Bukkit.createInventory(null, 54, "Select Breed (1/" + screenCount + ")");
        for (int i = 0; i < breedCount; i++) {
            if (i % 53 == 0 && i != 0) {
                breedScreens[screen].addItem(createItem(new ItemStack(Material.PAPER, 1), "Next page", "Go to page " + (screen + 2)));
                screen++;
                breedScreens[screen] = Bukkit.createInventory(null, 54, "Select Breed (" + (screen + 1) + "/" + screenCount + ")");
            }
            HorseBreed br = HorseBreed.values()[i];
            breedScreens[screen].addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1), br.toString(), "Use this breed"));
        }

        double personCount = Personality.values().length;
        for (int i = 0; i < personCount; i++) {
            Personality per = Personality.values()[i];
            personalityScreen1.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1), per.toString(), "Use this personality"));
            personalityScreen2.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1), per.toString(), "Use this personality"));
        }

        variantScreen.addItem(createItem(new ItemStack(Material.STONE, 1), "Blaze", "Select this variant"));
        variantScreen.addItem(createItem(new ItemStack(Material.STONE, 1), "Appaloosa", "Select this variant"));
        variantScreen.addItem(createItem(new ItemStack(Material.STONE, 1), "Pinto", "Select this variant"));
        variantScreen.addItem(createItem(new ItemStack(Material.STONE, 1), "Bald Face", "Select this variant"));
        variantScreen.addItem(createItem(new ItemStack(Material.STONE, 1), "Solid", "Select this variant"));

        colorScreen.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.BROWN.getData()), "Buckskin", "Select this color"));
        colorScreen.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.GRAY.getData()), "Bay", "Select this color"));
        colorScreen.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.BLUE.getData()), "Palomino", "Select this color"));
        colorScreen.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.SILVER.getData()), "Dappled Gray", "Select this color"));
        colorScreen.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.WHITE.getData()), "White", "Select this color"));
        colorScreen.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.BLACK.getData()), "Black", "Select this color"));
        colorScreen.addItem(createItem(new ItemStack(Material.STAINED_GLASS_PANE, 1, DyeColor.YELLOW.getData()), "Chestnut", "Select this color"));
    }

    private static ItemStack createItem(ItemStack item, String name, String lore) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(createLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> createLore(String lore) {
        List<String> list = new LinkedList<>();
        list.add(lore);
        return list;
    }

    public CustomHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                try {
                    int tokens = plugin.getEqDatabase().getTokens(player);
                    if (tokens < 1) {
                        player.sendMessage(ChatColor.RED + "You do not have any tokens");
                        return true;
                    }
                    player.openInventory(genderScreen);
                } catch (SQLException ex) {
                    Logger.getLogger(CustomHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
                return true;
            }
        } else {
            if (args[0].equalsIgnoreCase("allow")) {
                if (sender.isOp()) {
                    if (args.length >= 2) {
                        OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                        int amount = 1;
                        if (args.length >= 3) {
                            amount = Integer.parseInt(args[2]);
                        }
                        try {
                            plugin.getEqDatabase().addTokens(player, amount);
                            int total = plugin.getEqDatabase().getTokens(player);
                            sender.sendMessage(ChatColor.GREEN + "This player now has " + total + " tokens");
                            if (player.isOnline()) {
                                Player onlinePlayer = (Player) player;
                                onlinePlayer.sendMessage(ChatColor.GREEN + "You have been given " + amount + " custom horse tokens\nCurrent tokens: " + total);
                            }
                        } catch (SQLException ex) {
                            sender.sendMessage(ChatColor.RED + "There was an error setting the players tokens");
                        }
                        return true;
                    } else {
                        sender.sendMessage("Invalid args");
                    }
                }
            } else if (args[0].equalsIgnoreCase("list")) {
                if (sender.isOp()) {
                    try {
                        List<String> results = plugin.getEqDatabase().getAllTokens();
                        if (results.isEmpty()) {
                            sender.sendMessage("No tokens yet");
                            return true;
                        }
                        String output = "";
                        for (String s : results) {
                            output += s;
                        }
                        sender.sendMessage(output);
                    } catch (SQLException ex) {
                        Logger.getLogger(CustomHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("check")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    try {
                        int amount = plugin.getEqDatabase().getTokens(player);
                        player.sendMessage(ChatColor.GREEN + "You have got " + ChatColor.AQUA + amount + ChatColor.GREEN + " custom tokens");
                    } catch (SQLException ex) {
                        plugin.getLogger().log(Level.SEVERE, null, ex);
                    }
                    return true;
                }
            }
        }

        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        try {
            int total = plugin.getEqDatabase().getTokens(evt.getPlayer());
            evt.getPlayer().sendMessage(ChatColor.GREEN + "You have " + ChatColor.AQUA + total + ChatColor.GREEN + " custom tokens");
        } catch (SQLException ex) {
            evt.getPlayer().sendMessage("Error getting custom tokens");
        }
    }

    @EventHandler
    public void onGenderScreenClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().contains("Select Gender")) {
            return;
        }
        event.setCancelled(true);
        if (null == clicked.getType()) {
            return;
        } else {
            String gender = clicked.getItemMeta().getDisplayName();
            player.setMetadata("ch_gender", new FixedMetadataValue(plugin, gender.replace(" ", "")));
            player.sendMessage("You have chosen " + gender);
            player.closeInventory();
            player.openInventory(breedScreens[0]);
        }
    }

    @EventHandler
    public void onBreedScreenClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().contains("Select Breed")) {
            return;
        }
        event.setCancelled(true);
        if (null == clicked.getType()) {
            return;
        } else {
            player.closeInventory();
            if (clicked.getType() == Material.PAPER) {
                player.openInventory(breedScreens[1]);
            } else {
                String breed = clicked.getItemMeta().getDisplayName();
                player.setMetadata("ch_breed", new FixedMetadataValue(plugin, breed.replace(" ", "")));
                player.sendMessage("You have chosen " + breed);
                player.openInventory(personalityScreen1);
            }
        }
    }

    @EventHandler
    public void onPersonalityScreen1Click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().contains("Select Personality 1")) {
            return;
        }
        event.setCancelled(true);
        if (null == clicked.getType()) {
            return;
        } else {
            player.closeInventory();
            String personality = clicked.getItemMeta().getDisplayName();
            player.setMetadata("ch_personality1", new FixedMetadataValue(plugin, personality.replace(" ", "")));
            player.sendMessage("You have chosen " + personality);
            player.openInventory(personalityScreen2);
        }
    }

    @EventHandler
    public void onPersonalityScreen2Click(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().contains("Select Personality 2")) {
            return;
        }
        event.setCancelled(true);
        if (null == clicked.getType()) {
            return;
        } else {
            player.closeInventory();
            String personality = clicked.getItemMeta().getDisplayName();
            player.setMetadata("ch_personality2", new FixedMetadataValue(plugin, personality.replace(" ", "")));
            player.sendMessage("You have chosen " + personality);
            player.openInventory(variantScreen);
        }
    }

    @EventHandler
    public void onVariantScreenClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().contains("Select Variant")) {
            return;
        }
        event.setCancelled(true);
        if (null == clicked.getType()) {
            return;
        } else {
            player.closeInventory();
            String variant = clicked.getItemMeta().getDisplayName();
            player.setMetadata("ch_variant", new FixedMetadataValue(plugin, variant.replace(" ", "")));
            player.sendMessage("You have chosen " + variant);
            player.openInventory(colorScreen);
        }
    }

    @EventHandler
    public void onColorScreenClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().contains("Select Color")) {
            return;
        }
        event.setCancelled(true);
        if (null == clicked.getType()) {
            return;
        } else {
            player.closeInventory();
            String color = clicked.getItemMeta().getDisplayName();
            player.setMetadata("ch_color", new FixedMetadataValue(plugin, color.replace(" ", "")));
            player.sendMessage("You have chosen " + color);
            createHorse(player);
        }
    }

    private void createHorse(Player player) {
        String genderStr = player.getMetadata("ch_gender").get(0).asString();
        String breedStr = player.getMetadata("ch_breed").get(0).asString();
        String personalityStr1 = player.getMetadata("ch_personality1").get(0).asString();
        String personalityStr2 = player.getMetadata("ch_personality2").get(0).asString();
        String variantStr = player.getMetadata("ch_variant").get(0).asString();
        String colorStr = player.getMetadata("ch_color").get(0).asString();

        int gender;
        if (genderStr.equalsIgnoreCase("stallion")) {
            gender = MyHorse.STALLION;
        } else if (genderStr.equalsIgnoreCase("mare")) {
            gender = MyHorse.MARE;
        } else {
            gender = MyHorse.GELDING;
        }

        HorseBreed breed = HorseBreed.valueOf(breedStr);
        Personality personality1 = Personality.valueOf(personalityStr1);
        Personality personality2 = Personality.valueOf(personalityStr2);
        Color color;
        if (colorStr.equalsIgnoreCase("Buckskin")) {
            color = Color.BROWN;
        } else if (colorStr.equalsIgnoreCase("Bay")) {
            color = Color.DARK_BROWN;
        } else if (colorStr.equalsIgnoreCase("Palomino")) {
            color = Color.CREAMY;
        } else if (colorStr.equalsIgnoreCase("Dappled Gray")) {
            color = Color.GRAY;
        } else if (colorStr.equalsIgnoreCase("white")) {
            color = Color.WHITE;
        } else {
            color = Color.BLACK;
        }
        Horse h = player.getWorld().spawn(player.getLocation(), Horse.class);
        MyHorse horse = new MyHorse(h, gender, breed, personality1, personality2, color);
        plugin.getEqDatabase().addHorse(horse);
        try {
            plugin.getEqDatabase().removeToken(player);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }

}
