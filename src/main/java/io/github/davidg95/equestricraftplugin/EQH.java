/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import io.github.davidg95.equestricraftplugin.database.Database;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;

/**
 *
 * @author david
 */
public class EQH implements CommandExecutor {

    private final Database database;
    private final EquestriCraftPlugin plugin;

    public EQH(EquestriCraftPlugin plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("kill")) {
                if (sender instanceof Player) {
                    final Player player = (Player) sender;
                    if (player.isOp()) {
                        UUID uuid = UUID.fromString(player.getMetadata("horse").get(0).asString());
                        if (uuid == null) {
                            sender.sendMessage("No horse selected");
                            return true;
                        }

                        Horse h = plugin.getEntityByUniqueId(uuid);
                        if (h != null) {
                            h.setHealth(0);
                            database.removeHorse(uuid);
                        }
                        return true;
                    }
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("times")) {
                HorseCheckerThread.SHOW_TIME = !HorseCheckerThread.SHOW_TIME;
                sender.sendMessage("Horse checker times " + (HorseCheckerThread.SHOW_TIME ? "activated" : "deactivated"));
            } else if (args[0].equalsIgnoreCase("db")) {
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
                    } else if (args[1].equalsIgnoreCase("show-age")) {
                        if (args.length >= 3) {
                            int old = database.oldHorses(Integer.parseInt(args[2]));
                            sender.sendMessage("There are " + old + " horses older than " + args[2] + " months");
                        } else {
                            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "Must specify the age");
                        }
                    } else if (args[1].equalsIgnoreCase("show-dead")) {
                        int dead = database.deadHorses();
                        sender.sendMessage("There are " + dead + " dead horses");
                    } else if (args[1].equalsIgnoreCase("kill-dead")) {
                        database.killDead();
                        sender.sendMessage("Dead horses removed from database");
                    } else if (args[1].equalsIgnoreCase("comm")) {
                        if (sender.isOp()) {
                            if (args.length > 2) {
                                String command = "";
                                for (int i = 2; i < args.length; i++) {
                                    command += args[i] + " ";
                                }
                                Object res = database.submitCommand(command);
                                if (res == null) {
                                    sender.sendMessage("No return");
                                    return true;
                                }
                                sender.sendMessage(res.toString());
                            }
                        }
                        return true;
                    } else if (args[1].equalsIgnoreCase("no-ignore")) {
                        database.noIgnore();
                        sender.sendMessage("No horses will be ignored");
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "db command '" + args[1] + "' not recognised");
                    }
                }
                return true;
            } else if (args[0].equalsIgnoreCase("allowbreed")) {
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
            } else if (args[0].equalsIgnoreCase("handt")) {
                database.removeHungerAndThrist();
                sender.sendMessage("Hunger and thirst reset on ALL horses");
            } else if (args[0].equalsIgnoreCase("cure-all")) {
                database.cureAll();
                sender.sendMessage("All horses have been cured");
            } else if (args[0].equalsIgnoreCase("rl")) {
                if (sender.isOp()) {
                    plugin.loadProperties();
                    sender.sendMessage("config.yml reloaded");
                }
            } else if (args[0].equalsIgnoreCase("attr")) {
                String attr = args[1];
                Player player = (Player) sender;
                UUID uuid = UUID.fromString(player.getMetadata("horse").get(0).asString());
                if (uuid == null) {
                    sender.sendMessage("No horse selected");
                    return true;
                }

                Horse h = plugin.getEntityByUniqueId(uuid);
                Object o = HorseNMS.getAttribute(h, attr);
                player.sendMessage("Type: " + o.getClass().toString() + "=" + o);
            } else if (args[0].equalsIgnoreCase("show-nulls")) {
                int count = 0;
                for (World w : Bukkit.getWorlds()) {
                    for (Horse h : w.getEntitiesByClass(Horse.class)) {
                        MyHorse mh = database.getHorse(h.getUniqueId());
                        if (mh == null) {
                            count++;
                        }
                    }
                }
                sender.sendMessage(count + " null horses");
            } else if (args[0].equalsIgnoreCase("fix-nulls")) {
                int count = 0;
                for (World w : Bukkit.getWorlds()) {
                    for (Horse h : w.getEntitiesByClass(Horse.class)) {
                        MyHorse mh = database.getHorse(h.getUniqueId());
                        if (mh == null) {
                            mh = new MyHorse(h);
                            database.addHorse(mh);
                            count++;
                        }
                    }
                }
                sender.sendMessage(count + " horses added");
            } else if (args[0].equalsIgnoreCase("age")) {
                if (args.length >= 2) {
                    final Player player = (Player) sender;
                    if (player.isOp()) {
                        UUID uuid = UUID.fromString(player.getMetadata("horse").get(0).asString());
                        if (uuid == null) {
                            sender.sendMessage("No horse selected");
                            return true;
                        }
                        final Horse h = plugin.getEntityByUniqueId(uuid);
                        if (args[1].equalsIgnoreCase("foal")) {
                            h.setBaby();
                        } else if (args[1].equalsIgnoreCase("adult")) {
                            h.setAdult();
                        }
                        sender.sendMessage("Age set");
                        return true;
                    }
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("ignore")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Only a player can use this command");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Must specify true or false");
                    return true;
                }
                String boolStr = args[1];
                boolean ignore;
                if (boolStr.equalsIgnoreCase("true")) {
                    ignore = true;
                } else if (boolStr.equalsIgnoreCase("false")) {
                    ignore = false;
                } else {
                    sender.sendMessage(ChatColor.RED + "Must specify true or false");
                    return true;
                }
                final Player player = (Player) sender;
                if (player.isOp()) {
                    UUID uuid = UUID.fromString(player.getMetadata("horse").get(0).asString());
                    if (uuid == null) {
                        sender.sendMessage("No horse selected");
                        return true;
                    }
                    database.setIgnore(uuid, ignore);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("status")) {
                String message = "";
                message += ChatColor.GREEN + "Horses in database: " + ChatColor.AQUA + database.horseCount(-1) + "\n";
                message += ChatColor.GREEN + "Ignored horses: " + ChatColor.AQUA + database.ignoredHorses() + "\n";
                message += ChatColor.AQUA + "Checker thread: " + (plugin.checkerThread.isAlive() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Not Active") + "\n";
                message += ChatColor.AQUA + "Bucking thread: " + (plugin.buckThread.isActive() ? ChatColor.GREEN + "Active" : ChatColor.RED + "Not Active") + "\n";
                int count = 0;
                for (World world : Bukkit.getWorlds()) {
                    count += world.getEntitiesByClass(Horse.class).size();
                }
                message += ChatColor.GREEN + "Horses currently in world: " + ChatColor.AQUA + count;
                sender.sendMessage(message);
                return true;
            }
        }
        return true;
    }

}
