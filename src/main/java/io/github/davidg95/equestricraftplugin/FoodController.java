/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import io.github.davidg95.equestricraftplugin.database.Database;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class for handling the food side of the plugin.
 *
 * @author David
 */
public class FoodController implements CommandExecutor, Listener {

    private final Economy econ;
    private final Database database;

    private final int seeds_cost;
    private final int wheat_cost;
    private final int water_cost;

    public FoodController(EquestriCraftPlugin plugin, Economy econ, Database database) {
        this.econ = econ;
        this.database = database;

        seeds_cost = plugin.getConfig().getInt("food.seeds");
        wheat_cost = plugin.getConfig().getInt("food.wheat");
        water_cost = plugin.getConfig().getInt("food.water");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GREEN + "Options-\n"
                    + ChatColor.RESET + "/food buy-seeds - " + ChatColor.AQUA + "$" + seeds_cost + "\n"
                    + ChatColor.RESET + "/food buy-wheat - " + ChatColor.AQUA + "$" + wheat_cost + "\n"
                    + ChatColor.RESET + "/food buy-water - " + ChatColor.AQUA + "$" + water_cost + "\n");
            return true;
        }
        Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            sender.sendMessage("Only a player can use this command");
            return false;
        }
        if (args[0].equalsIgnoreCase("buy-seeds")) {
            int quantity = 1;
            if (args.length == 2) {
                quantity = Integer.parseInt(args[1]);
            }
            double cost = seeds_cost * quantity;
            if (econ.getBalance(player) < cost) {
                player.sendMessage(ChatColor.RED + "You do not have enough money");
                return true;
            }
            ItemStack seeds = new ItemStack(Material.SEEDS, quantity);
            ItemMeta meta = seeds.getItemMeta();
            meta.setDisplayName("Feeding Seeds");
            meta.setLore(createLore("Used to feed your horse"));
            seeds.setItemMeta(meta);
            player.getInventory().addItem(seeds);
            econ.withdrawPlayer(player, cost);
            player.sendMessage(ChatColor.GREEN + "You have been charged " + ChatColor.AQUA + "$" + cost);
        } else if (args[0].equalsIgnoreCase("buy-wheat")) {
            int quantity = 1;
            if (args.length == 2) {
                quantity = Integer.parseInt(args[1]);
            }
            double cost = wheat_cost * quantity;
            if (econ.getBalance(player) < cost) {
                player.sendMessage(ChatColor.RED + "You do not have enough money");
                return true;
            }
            ItemStack seeds = new ItemStack(Material.WHEAT, quantity);
            ItemMeta meta = seeds.getItemMeta();
            meta.setDisplayName("Feeding Wheat");
            meta.setLore(createLore("Used to feed your horse"));
            seeds.setItemMeta(meta);
            player.getInventory().addItem(seeds);
            econ.withdrawPlayer(player, cost);
            player.sendMessage(ChatColor.GREEN + "You have been charged " + ChatColor.AQUA + "$" + cost);
        } else if (args[0].equalsIgnoreCase("buy-water")) {
            int quantity = 1;
            if (args.length == 2) {
                quantity = Integer.parseInt(args[1]);
            }
            double cost = water_cost * quantity;
            if (econ.getBalance(player) < cost) {
                player.sendMessage(ChatColor.RED + "You do not have enough money");
                return true;
            }
            ItemStack seeds = new ItemStack(Material.WATER_BUCKET, quantity);
            ItemMeta meta = seeds.getItemMeta();
            meta.setDisplayName("Drinking Water");
            meta.setLore(createLore("Give your horse a drink of water"));
            seeds.setItemMeta(meta);
            player.getInventory().addItem(seeds);
            econ.withdrawPlayer(player, cost);
            player.sendMessage(ChatColor.GREEN + "You have been charged " + ChatColor.AQUA + "$" + cost);
        } else {
            sender.sendMessage("Incorrect command\n"
                    + ChatColor.RESET + "/food buy-seeds - " + ChatColor.AQUA + "$" + seeds_cost + "\n"
                    + ChatColor.RESET + "/food buy-wheat - " + ChatColor.AQUA + "$" + wheat_cost + "\n"
                    + ChatColor.RESET + "/food buy-water - " + ChatColor.AQUA + "$" + water_cost + "\n");
        }
        return true;
    }

    private List<String> createLore(String lore) {
        List<String> lores = new ArrayList<>();
        lores.add(lore);
        return lores;
    }

    /**
     * Handles when a horse is fed wheat.
     *
     * @param event the event.
     */
    @EventHandler
    public void onWheatFeed(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final ItemStack inHand = player.getItemInHand(); //Get the item in hand.
        if (inHand == null) {
            return;
        }
        if (inHand.getType() != Material.WHEAT) {
            return;
        }
        if (!inHand.getItemMeta().getDisplayName().equals("Feeding Wheat")) {
            return;
        }
        if (!(event.getRightClicked() instanceof Horse)) {
            return;
        }
        event.setCancelled(true);
        final Horse horse = (Horse) event.getRightClicked();
        UUID uuid = horse.getUniqueId();
        database.feedHorse(uuid);
        if (inHand.getAmount() == 1) {
            player.getInventory().remove(inHand);
        } else {
            inHand.setAmount(inHand.getAmount() - 1);
        }
        player.sendMessage(ChatColor.GREEN + "Your horse has been fed");
    }

    /**
     * Handles when a horse is fed seeds.
     *
     * @param event the event.
     */
    @EventHandler
    public void onSeedFeed(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final ItemStack inHand = player.getItemInHand(); //Get the item in hand.
        if (inHand == null) {
            return;
        }
        if (inHand.getType() != Material.SEEDS) {
            return;
        }
        if (!inHand.getItemMeta().getDisplayName().equals("Feeding Seeds")) {
            return;
        }
        if (!(event.getRightClicked() instanceof Horse)) {
            return;
        }
        event.setCancelled(true);
        final Horse horse = (Horse) event.getRightClicked();
        UUID uuid = horse.getUniqueId();
        database.feedHorse(uuid);
        if (inHand.getAmount() == 1) {
            player.getInventory().remove(inHand);
        } else {
            inHand.setAmount(inHand.getAmount() - 1);
        }
        player.sendMessage(ChatColor.GREEN + "Your horse has been fed");
    }

    /**
     * Handles when a horse is fed water.
     *
     * @param event the event.
     */
    @EventHandler
    public void onWaterFeed(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final ItemStack inHand = player.getItemInHand(); //Get the item in hand.
        if (inHand == null) {
            return;
        }
        if (inHand.getType() != Material.WATER_BUCKET) {
            return;
        }
        if (!inHand.getItemMeta().getDisplayName().equals("Drinking Water")) {
            return;
        }
        if (!(event.getRightClicked() instanceof Horse)) {
            return;
        }
        event.setCancelled(true);
        final Horse horse = (Horse) event.getRightClicked();
        UUID uuid = horse.getUniqueId();
        database.waterHorse(uuid);
        if (inHand.getAmount() == 1) {
            player.getInventory().remove(inHand);
        } else {
            inHand.setAmount(inHand.getAmount() - 1);
        }
        player.sendMessage(ChatColor.GREEN + "Your horse has had a drink");
    }
}
