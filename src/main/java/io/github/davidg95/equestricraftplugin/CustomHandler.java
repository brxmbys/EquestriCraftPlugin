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
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
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
    private static final Inventory personalityScreen1 = Bukkit.createInventory(null, 54, "Select Personality 1");
    private static final Inventory personalityScreen2 = Bukkit.createInventory(null, 54, "Select Personality 2");
    private static final Inventory variantScreen = Bukkit.createInventory(null, 9, "Select Variant");

    static {
        genderScreen.setItem(3, createItem(Material.STONE, "Stallion", "Male horse"));
        genderScreen.setItem(4, createItem(Material.STONE, "Mare", "Female horse"));
        genderScreen.setItem(5, createItem(Material.STONE, "Gelding", "Gelded male horse"));

        double breedCount = HorseBreed.values().length;

        int screenCount = (int) Math.ceil(breedCount / 54d);

        breedScreens = new Inventory[screenCount];
        int screen = 0;
        breedScreens[screen] = Bukkit.createInventory(null, 54, "Select Breed (1/" + screenCount + ")");
        for (int i = 0; i < breedCount; i++) {
            if (i % 53 == 0 && i != 0) {
                breedScreens[screen].addItem(createItem(Material.WOOD, "Next page", "Go to page " + (screen + 2)));
                screen++;
                breedScreens[screen] = Bukkit.createInventory(null, 54, "Select Breed (" + (screen + 1) + "/" + screenCount + ")");
            }
            HorseBreed br = HorseBreed.values()[i];
            breedScreens[screen].addItem(createItem(Material.STONE, br.toString(), "Use this breed"));
        }

        double personCount = Personality.values().length;
        for (int i = 0; i < personCount; i++) {
            Personality per = Personality.values()[i];
            personalityScreen1.addItem(createItem(Material.STONE, per.toString(), "Use this personality"));
            personalityScreen2.addItem(createItem(Material.STONE, per.toString(), "Use this personality"));
        }

        variantScreen.addItem(createItem(Material.STONE, "Blaze", "Select this variant"));
        variantScreen.addItem(createItem(Material.STONE, "Appaloosa", "Select this variant"));
        variantScreen.addItem(createItem(Material.STONE, "Pinto", "Select this variant"));
        variantScreen.addItem(createItem(Material.STONE, "Bald Face", "Select this variant"));
        variantScreen.addItem(createItem(Material.STONE, "Solid", "Select this variant"));
    }

    private static ItemStack createItem(Material m, String name, String lore) {
        ItemStack item = new ItemStack(m, 1);
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
                            if(player.isOnline()){
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
                        player.sendMessage("You have got " + amount + " custom tokens");
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
            evt.getPlayer().sendMessage(ChatColor.GREEN + "You have " + total + " custom tokens");
        } catch (SQLException ex) {
            evt.getPlayer().sendMessage("Error getting custom tokens");
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
        player.closeInventory();
        if (null == clicked.getType()) {
            return;
        } else {
            if (clicked.getType() == Material.STONE) {
                String breed = clicked.getItemMeta().getDisplayName();
                player.setMetadata("ch_breed", new FixedMetadataValue(plugin, breed.replace(" ", "")));
                player.sendMessage("You have chosen " + breed);
                player.openInventory(personalityScreen1);
            } else if (clicked.getType() == Material.WOOD) {
                player.openInventory(breedScreens[1]);
            }
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
        player.closeInventory();
        if (null == clicked.getType()) {
            return;
        } else {
            if (clicked.getType() == Material.STONE) {
                String gender = clicked.getItemMeta().getDisplayName();
                player.setMetadata("ch_gender", new FixedMetadataValue(plugin, gender.replace(" ", "")));
                player.sendMessage("You have chosen " + gender);
                player.openInventory(breedScreens[0]);
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
        player.closeInventory();
        if (null == clicked.getType()) {
            return;
        } else {
            if (clicked.getType() == Material.STONE) {
                String personality = clicked.getItemMeta().getDisplayName();
                player.setMetadata("ch_personality1", new FixedMetadataValue(plugin, personality.replace(" ", "")));
                player.sendMessage("You have chosen " + personality);
                player.openInventory(personalityScreen2);
            }
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
        player.closeInventory();
        if (null == clicked.getType()) {
            return;
        } else {
            if (clicked.getType() == Material.STONE) {
                String personality = clicked.getItemMeta().getDisplayName();
                player.setMetadata("ch_personality2", new FixedMetadataValue(plugin, personality.replace(" ", "")));
                player.sendMessage("You have chosen " + personality);
                player.openInventory(variantScreen);
            }
        }
    }

    @EventHandler
    public void onVriantScreenClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().contains("Select Variant")) {
            return;
        }
        event.setCancelled(true);
        player.closeInventory();
        if (null == clicked.getType()) {
            return;
        } else {
            if (clicked.getType() == Material.STONE) {
                String variant = clicked.getItemMeta().getDisplayName();
                player.setMetadata("ch_variant", new FixedMetadataValue(plugin, variant.replace(" ", "")));
                player.sendMessage("You have chosen " + variant);
                createHorse(player);
            }
        }
    }

    private void createHorse(Player player) {
        String genderStr = player.getMetadata("ch_gender").get(0).asString();
        String breedStr = player.getMetadata("ch_breed").get(0).asString();
        String personalityStr1 = player.getMetadata("ch_personality1").get(0).asString();
        String personalityStr2 = player.getMetadata("ch_personality2").get(0).asString();
        String variantStr = player.getMetadata("ch_variant").get(0).asString();

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
        player.sendMessage("Creating " + genderStr + " of breed " + breed.toString() + " with personallity " + personality1.toString() + " and " + personality2.toString() + "of variant " + variantStr);
        Horse h = player.getWorld().spawn(player.getLocation(), Horse.class);
        MyHorse horse = new MyHorse(h, gender, breed, personality1, personality2);
        plugin.getEqDatabase().addHorse(horse);
        try {
            plugin.getEqDatabase().removeToken(player);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }

}
