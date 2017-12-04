/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import io.github.davidg95.equestricraftplugin.database.Database;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
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
 *
 * @author David
 */
public class FoodController implements CommandExecutor, Listener {

    private final EquestriCraftPlugin plugin;
    private final Economy econ;
    private final Database database;

    private final int seeds_cost;
    private final int wheat_cost;
    private final int water_cost;

    public FoodController(EquestriCraftPlugin plugin, Economy econ, Database database) {
        this.plugin = plugin;
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
            sender.sendMessage("Usage-\n"
                    + "/food buy-seeds\n"
                    + "/food buy-wheat\n"
                    + "/food buy-water\n");
            return true;
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (args[0].equalsIgnoreCase("buy-seeds")) {
            if (args.length == 2) {
                String name = args[1];
                player = Bukkit.getPlayer(name);
            }
            if (econ.getBalance(player) < seeds_cost) {
                player.sendMessage(ChatColor.RED + "You do not have enough money");
                return true;
            }
            ItemStack seeds = new ItemStack(Material.SEEDS, 1);
            ItemMeta meta = seeds.getItemMeta();
            meta.setDisplayName("Feeding Seeds");
            meta.setLore(createLore("Used to feed your horse"));
            seeds.setItemMeta(meta);
            player.getInventory().addItem(seeds);
            econ.withdrawPlayer(player, seeds_cost);
            player.sendMessage(ChatColor.GREEN + "You have been charged " + ChatColor.AQUA + "$" + seeds_cost);
        } else if (args[0].equalsIgnoreCase("buy-wheat")) {
            if (econ.getBalance(player) < wheat_cost) {
                player.sendMessage(ChatColor.RED + "You do not have enough money");
                return true;
            }
            econ.withdrawPlayer(player, wheat_cost);
            ItemStack seeds = new ItemStack(Material.WHEAT, 1);
            ItemMeta meta = seeds.getItemMeta();
            meta.setDisplayName("Feeding Wheat");
            meta.setLore(createLore("Used to feed your horse"));
            seeds.setItemMeta(meta);
            player.getInventory().addItem(seeds);
            econ.withdrawPlayer(player, wheat_cost);
            player.sendMessage(ChatColor.GREEN + "You have been charged " + ChatColor.AQUA + "$" + wheat_cost);
        } else if (args[0].equalsIgnoreCase("buy-water")) {
            if (econ.getBalance(player) < water_cost) {
                player.sendMessage(ChatColor.RED + "You do not have enough money");
                return true;
            }
            ItemStack seeds = new ItemStack(Material.WATER_BUCKET, 1);
            ItemMeta meta = seeds.getItemMeta();
            meta.setDisplayName("Drinking Water");
            meta.setLore(createLore("Give your horse a drink of water"));
            seeds.setItemMeta(meta);
            player.getInventory().addItem(seeds);
            econ.withdrawPlayer(player, water_cost);
            player.sendMessage(ChatColor.GREEN + "You have been charged " + ChatColor.AQUA + "$" + water_cost);
        }
        return true;
    }

    private List<String> createLore(String lore) {
        List<String> lores = new ArrayList<>();
        lores.add(lore);
        return lores;
    }

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
        MyHorse mh = database.getHorse(horse.getUniqueId());
        if (horse == null) {
            return;
        }
        mh.eat();
        if (inHand.getAmount() == 0) {
            player.getInventory().remove(inHand);
        } else {
            inHand.setAmount(inHand.getAmount() - 1);
        }
        player.sendMessage(ChatColor.GREEN + "Your horse has been fed");
    }

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
        MyHorse mh = database.getHorse(horse.getUniqueId());
        if (horse == null) {
            return;
        }
        mh.eat();
        if (inHand.getAmount() == 0) {
            player.getInventory().remove(inHand);
        } else {
            inHand.setAmount(inHand.getAmount() - 1);
        }
        player.sendMessage(ChatColor.GREEN + "Your horse has been fed");
    }

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
        MyHorse mh = database.getHorse(horse.getUniqueId());
        if (horse == null) {
            return;
        }
        mh.eat();
        inHand.setType(Material.BUCKET);
        player.sendMessage(ChatColor.GREEN + "Your horse has had a drink");
    }
}
