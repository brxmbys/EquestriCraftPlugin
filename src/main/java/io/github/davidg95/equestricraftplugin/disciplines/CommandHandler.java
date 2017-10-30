/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.disciplines;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author David
 */
public class CommandHandler implements CommandExecutor, Listener {

    private final EquestriCraftPlugin plugin;

    private static Inventory menu = Bukkit.createInventory(null, 9, "Disciplines");

    private final DisciplinesController cont;

    private Economy economy;

    static {
        ItemStack i1 = new ItemStack(Material.DIAMOND, 1);
        ItemMeta m1 = i1.getItemMeta();
        //m1.setLore(toLore("Pole Bending"));
        m1.setDisplayName("Pole Bending");
        i1.setItemMeta(m1);

        ItemStack i2 = new ItemStack(Material.IRON_INGOT, 1);
        ItemMeta m2 = i1.getItemMeta();
        //m2.setLore(toLore("Barrel Racing"));
        m2.setDisplayName("Barrel Racing");
        i2.setItemMeta(m2);

        ItemStack i3 = new ItemStack(Material.GOLD_INGOT, 1);
        ItemMeta m3 = i1.getItemMeta();
        //m3.setLore(toLore("Western Plesure"));
        m3.setDisplayName("Western Plesure");
        i3.setItemMeta(m3);

        ItemStack i4 = new ItemStack(Material.SADDLE, 1);
        ItemMeta m4 = i1.getItemMeta();
        //m4.setLore(toLore("Hunt Seat"));
        m4.setDisplayName("Hunt Seat");
        i4.setItemMeta(m4);

        ItemStack i5 = new ItemStack(Material.GOLD_BARDING, 1);
        ItemMeta m5 = i1.getItemMeta();
        //m5.setLore(toLore("Show Jumping"));
        m5.setDisplayName("Show Jumping");
        i5.setItemMeta(m5);

        ItemStack i6 = new ItemStack(Material.FEATHER, 1);
        ItemMeta m6 = i1.getItemMeta();
        //m6.setLore(toLore("Dressage"));
        m6.setDisplayName("Dressage");
        i6.setItemMeta(m6);

        ItemStack i7 = new ItemStack(Material.BRICK, 1);
        ItemMeta m7 = i1.getItemMeta();
        //m7.setLore(toLore("Cross Country"));
        m7.setDisplayName("Cross Country");
        i7.setItemMeta(m7);

        ItemStack i8 = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta m8 = i1.getItemMeta();
        //m8.setLore(toLore("Racing"));
        m8.setDisplayName("Racing");
        i8.setItemMeta(m8);

        ItemStack i9 = new ItemStack(Material.PAPER, 1);
        ItemMeta m9 = i1.getItemMeta();
        //m9.setLore(toLore("Steeple Chase"));
        m9.setDisplayName("Steeple Chase");
        i9.setItemMeta(m9);

        menu.setItem(0, i1);
        menu.setItem(1, i2);
        menu.setItem(2, i3);
        menu.setItem(3, i4);
        menu.setItem(4, i5);
        menu.setItem(5, i6);
        menu.setItem(6, i7);
        menu.setItem(7, i8);
        menu.setItem(8, i9);
    }

    private static List<String> toLore(String text) {
        List<String> comments = new ArrayList<>();
        comments.add(text);
        return comments;
    }

    public CommandHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        cont = new DisciplinesController();
        economy = EquestriCraftPlugin.economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.openInventory(menu);
            } else {
                sender.sendMessage("Only players can use this command");
            }
            return true;
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                String disciplines = "";
                for (Discipline d : Discipline.values()) {
                    disciplines += d.toString() + ", ";
                }
                sender.sendMessage(disciplines);
                return true;
            } else if (args[0].equalsIgnoreCase("joined")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    List<Discipline> ds = cont.getMemberships(player);
                    String output = "Disciplines you have signed up for-";
                    for (Discipline d : ds) {
                        output += "\n" + d.toString();
                    }
                    player.sendMessage(output);
                } else {
                    sender.sendMessage("Only players can use this command");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("reset")) {
                if (serverOrOp(sender)) {
                    cont.reset();
                    Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Discipline memberships have been reset!");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("view")) {
                if (serverOrOp(sender)) {
                    List<Membership> memberships = cont.getAll();
                    String output = ChatColor.BOLD + "Discipline memberships-" + ChatColor.RESET;
                    for (Discipline d : Discipline.values()) {
                        output += ChatColor.GREEN + "\n" + "-" + d.toString() + "-" + ChatColor.RESET;
                        for (Membership m : memberships) {
                            if (m.getDiscipline().equals(d)) {
                                output += "\n--" + Bukkit.getPlayer(m.getPlayer()).getName();
                            }
                        }
                    }
                    sender.sendMessage(output);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("refund-all")) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().equals(menu.getName())) {
            return;
        }
        event.setCancelled(true);
        player.closeInventory();
        double v = 0;
        Discipline d;
        if (clicked.getType() == Material.DIAMOND) {
            //Pole Bending
            d = Discipline.PoleBending;
            v = cont.checkMembership(player, Discipline.PoleBending);
        } else if (clicked.getType() == Material.IRON_INGOT) {
            //Barrel Racing
            d = Discipline.BarrelRacing;
            v = cont.checkMembership(player, Discipline.BarrelRacing);
        } else if (clicked.getType() == Material.GOLD_INGOT) {
            //Western Plesure
            d = Discipline.WesternPlesure;
            v = cont.checkMembership(player, Discipline.WesternPlesure);
        } else if (clicked.getType() == Material.SADDLE) {
            //Hunt Seat
            d = Discipline.HuntSeat;
            v = cont.checkMembership(player, Discipline.HuntSeat);
        } else if (clicked.getType() == Material.GOLD_BARDING) {
            //Show Jumping
            d = Discipline.ShowJumping;
            v = cont.checkMembership(player, Discipline.ShowJumping);
        } else if (clicked.getType() == Material.FEATHER) {
            //Dressage
            d = Discipline.Dressage;
            v = cont.checkMembership(player, Discipline.Dressage);
        } else if (clicked.getType() == Material.BRICK) {
            //Cross Country
            d = Discipline.CrossCountry;
            v = cont.checkMembership(player, Discipline.CrossCountry);
        } else if (clicked.getType() == Material.FISHING_ROD) {
            //Racing
            d = Discipline.Racing;
            v = cont.checkMembership(player, Discipline.Racing);
        } else if (clicked.getType() == Material.PAPER) {
            //Steeple Chase
            d = Discipline.SteepleChase;
            v = cont.checkMembership(player, Discipline.SteepleChase);
        } else {
            return;
        }

        if (v == -1) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are already in that discipline");
        } else if (v == -2) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are already in two disciplines");
        } else {
            if (economy.getBalance(player) < v) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You do not have enough funds. Required $" + new DecimalFormat("0.00").format(v));
                return;
            }
            EconomyResponse r = economy.withdrawPlayer(player, v);
            if (r.transactionSuccess()) {
                cont.addMembership(player, d);
                player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "You are in the " + d.toString() + " discipline!");
                player.sendMessage("You have been charged " + ChatColor.AQUA + "$" + new DecimalFormat("0.00").format(v));
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + r.errorMessage);
            }
        }
    }

    private boolean serverOrOp(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).isOp();
        } else {
            return true;
        }
    }

}
