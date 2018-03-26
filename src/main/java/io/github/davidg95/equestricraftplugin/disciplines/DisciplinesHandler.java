/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.disciplines;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.text.DecimalFormat;
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
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Command handler for the /disciplines command.
 *
 * @author David
 */
public class DisciplinesHandler implements CommandExecutor, Listener {

    /**
     * The discipline menu.
     */
    private static final Inventory MENU = Bukkit.createInventory(null, 9, "Disciplines");

    private final DisciplinesController cont;

    private final Economy economy;

    static {
        ItemStack i1 = new ItemStack(Material.DIAMOND, 1);
        ItemMeta m1 = i1.getItemMeta();
        m1.setDisplayName("Pole Bending");
        i1.setItemMeta(m1);

        ItemStack i2 = new ItemStack(Material.IRON_INGOT, 1);
        ItemMeta m2 = i1.getItemMeta();
        m2.setDisplayName("Barrel Racing");
        i2.setItemMeta(m2);

        ItemStack i3 = new ItemStack(Material.GOLD_INGOT, 1);
        ItemMeta m3 = i1.getItemMeta();
        m3.setDisplayName("Western Plesure");
        i3.setItemMeta(m3);

        ItemStack i4 = new ItemStack(Material.SADDLE, 1);
        ItemMeta m4 = i1.getItemMeta();
        m4.setDisplayName("Hunt Seat");
        i4.setItemMeta(m4);

        ItemStack i5 = new ItemStack(Material.GOLD_BARDING, 1);
        ItemMeta m5 = i1.getItemMeta();
        m5.setDisplayName("Show Jumping");
        i5.setItemMeta(m5);

        ItemStack i6 = new ItemStack(Material.FEATHER, 1);
        ItemMeta m6 = i1.getItemMeta();
        m6.setDisplayName("Dressage");
        i6.setItemMeta(m6);

        ItemStack i7 = new ItemStack(Material.BRICK, 1);
        ItemMeta m7 = i1.getItemMeta();
        m7.setDisplayName("Cross Country");
        i7.setItemMeta(m7);

        ItemStack i8 = new ItemStack(Material.FISHING_ROD, 1);
        ItemMeta m8 = i1.getItemMeta();
        m8.setDisplayName("Racing");
        i8.setItemMeta(m8);

        ItemStack i9 = new ItemStack(Material.PAPER, 1);
        ItemMeta m9 = i1.getItemMeta();
        m9.setDisplayName("Steeple Chase");
        i9.setItemMeta(m9);

        ItemStack i19 = new ItemStack(Material.BOOK, 1);
        ItemMeta m19 = i19.getItemMeta();
        m19.setDisplayName("Disciplines Information");
        i19.setItemMeta(m19);

        MENU.setItem(0, i1);
        MENU.setItem(1, i2);
        MENU.setItem(2, i3);
        MENU.setItem(3, i4);
        MENU.setItem(4, i5);
        MENU.setItem(5, i6);
        MENU.setItem(6, i7);
        MENU.setItem(7, i8);
        MENU.setItem(8, i9);
    }

    public DisciplinesHandler(EquestriCraftPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        cont = new DisciplinesController(plugin);
        this.economy = plugin.getEconomy();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (args.length) {
            case 0:
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.openInventory(MENU);
                } else {
                    sender.sendMessage("Only players can use this command");
                }
                return true;
            case 1:
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
                        String output = ChatColor.BOLD + "Disciplines you have signed up for-" + ChatColor.RESET + ChatColor.GREEN;
                        for (Discipline d : ds) {
                            output += "\n-" + d.toString();
                        }
                        if (ds.isEmpty()) {
                            output += "\n" + ChatColor.RED + "You have not yet signed up for any";
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
                    if (sender.hasPermission("equestricraft.disciplines.view")) {
                        List<Membership> memberships = cont.getAll();
                        String output = ChatColor.BOLD + "Discipline memberships-" + ChatColor.RESET;
                        for (Discipline d : Discipline.values()) {
                            output += ChatColor.AQUA + "\n\n" + ">" + d.toString() + ChatColor.RESET;
                            int count = 0;
                            for (Membership m : memberships) {
                                if (m.getDiscipline().equals(d)) {
                                    output += ", " + Bukkit.getOfflinePlayer(m.getPlayer()).getName();
                                    count++;
                                }
                            }
                            if (count == 0) {
                                output += "\n  " + ChatColor.RED + "No entrants" + ChatColor.RESET;
                            } else {
                                output += "\n  " + ChatColor.GREEN + count + (count > 1 ? " entrants" : " entrant") + ChatColor.RESET;
                            }
                        }
                        sender.sendMessage(output);
                    } else {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to view the disciplines memberships");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("refund-all")) {
                    return true;
                }
                break;
            default:
                if (args[0].equalsIgnoreCase("view")) {
                    if (!sender.hasPermission("equestricraft.disciplines.view")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to view the disciplines memberships");
                        return true;
                    }
                    String dStr = "";
                    for (int i = 1; i < args.length; i++) {
                        dStr += args[i];
                    }
                    try {
                        Discipline d = Discipline.valueOf(dStr);
                        List<Player> members = cont.getDisciplineMembers(d);
                        if (members.isEmpty()) {
                            sender.sendMessage("No entrants to " + d.toString());
                            return true;
                        }
                        String output = "Members of " + d.toString() + "-";
                        for (Player p : members) {
                            output += "\n-" + p.getName();
                        }
                        sender.sendMessage(output);
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Discipline " + dStr + " not found");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("leave")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage("Only players can use this command");
                        return true;
                    }
                    Player player = (Player) sender;
                    String dStr = "";
                    for (int i = 1; i < args.length; i++) {
                        dStr += args[i];
                    }
                    try {
                        Discipline d = Discipline.valueOf(dStr);
                        withdraw(player, d);
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Discipline " + dStr + " not found");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("reset")) {
                    if (sender.hasPermission("equestricraft.disciplines.reset")) {
                        sender.sendMessage(ChatColor.RED + "You do not have permission to reset the disciplines memberships");
                        return true;
                    }
                    String dStr = "";
                    for (int i = 1; i < args.length; i++) {
                        dStr += args[i];
                    }
                    try {
                        Discipline d = Discipline.valueOf(dStr);
                        cont.reset(d);
                        Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + d.toString() + " has benn reset!");
                    } catch (Exception e) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Discipline " + dStr + " not found");
                    }
                    return true;
                } else if (args[0].equalsIgnoreCase("send")) {
                    if (args.length <= 2) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must include messgae body");
                        return true;
                    }
                    String dStr = args[1];
                    String message = "";
                    for (int i = 2; i < args.length; i++) {
                        message += args[i] + " ";
                    }
                    Discipline d = Discipline.valueOf(dStr);
                    if (d == null) {
                        sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Discipline " + dStr + " not found");
                        return true;
                    }
                    List<Player> players = cont.getDisciplineMembers(d);
                    for (Player p : players) {
                        Bukkit.dispatchCommand(sender, "mail send " + p.getName() + " " + message);
                    }
                    Bukkit.broadcastMessage(ChatColor.GREEN + "Message sent to all players of the " + d.toString() + " discipline");
                    return true;
                }
                break;
        }
        return false;
    }

    private void withdraw(Player p, Discipline d) {
        double v = cont.removeMembership(p, d);
        if (v == -1) {
            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are not in that discipline!");
            return;
        }
        economy.depositPlayer(p, v);
        p.sendMessage("You have withdrawn from " + d.toString() + " and have been refunded " + ChatColor.AQUA + "$" + new DecimalFormat("0").format(v));
    }

    /**
     * Will check if they player clicked on a Disciplines inventory item.
     *
     * @param event the event.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        Inventory inventory = event.getInventory();
        if (!inventory.getName().equals(MENU.getName())) {
            return;
        }
        event.setCancelled(true);
        player.closeInventory();
        double v;
        Discipline d;
        if (null == clicked.getType()) {
            return;
        } else switch (clicked.getType()) {
            case DIAMOND:
                //Pole Bending
                d = Discipline.PoleBending;
                v = cont.checkMembership(player, Discipline.PoleBending);
                break;
            case IRON_INGOT:
                //Barrel Racing
                d = Discipline.BarrelRacing;
                v = cont.checkMembership(player, Discipline.BarrelRacing);
                break;
            case GOLD_INGOT:
                //Western Plesure
                d = Discipline.WesternPlesure;
                v = cont.checkMembership(player, Discipline.WesternPlesure);
                break;
            case SADDLE:
                //Hunt Seat
                d = Discipline.HuntSeat;
                v = cont.checkMembership(player, Discipline.HuntSeat);
                break;
            case GOLD_BARDING:
                //Show Jumping
                d = Discipline.ShowJumping;
                v = cont.checkMembership(player, Discipline.ShowJumping);
                break;
            case FEATHER:
                //Dressage
                d = Discipline.Dressage;
                v = cont.checkMembership(player, Discipline.Dressage);
                break;
            case BRICK:
                //Cross Country
                d = Discipline.CrossCountry;
                v = cont.checkMembership(player, Discipline.CrossCountry);
                break;
            case FISHING_ROD:
                //Racing
                d = Discipline.Racing;
                v = cont.checkMembership(player, Discipline.Racing);
                break;
            case PAPER:
                //Steeple Chase
                d = Discipline.SteepleChase;
                v = cont.checkMembership(player, Discipline.SteepleChase);
                break;
            case BOOK:
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
                BookMeta bm = (BookMeta) book.getItemMeta();
                String text = "***PRICE INFORMATION***\n\nFirst Discipline - $1000\nSecond Discipline - $5000\n\nRefunds - 50% of entry fee.\n\nMaximum 2 disciplines.";
                
                bm.addPage("1");
                bm.setPage(1, text);
                bm.setAuthor("EquestriCraft");
                bm.setTitle("Disciplines");
                book.setItemMeta(bm);
                
                player.getInventory().addItem(book);
                return;
            default:
                return;
        }

        if (v == -1) {
            withdraw(player, d);
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
                player.sendMessage("You have been charged " + ChatColor.AQUA + "$" + new DecimalFormat("0").format(v));
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + r.errorMessage);
            }
        }
    }

    /**
     * Checks if the sender is the console or an op.
     *
     * @param sender the sender.
     * @return true if they are console or op, false otherwise.
     */
    private boolean serverOrOp(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).isOp();
        } else {
            return true;
        }
    }

}
