/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.disciplines;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class CommandHandler implements CommandExecutor {

    private final EquestriCraftPlugin plugin;

    public CommandHandler(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {

        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {

                return true;
            } else if (args[0].equalsIgnoreCase("joined")) {

                return true;
            } else if (args[0].equalsIgnoreCase("reset")) {
                if (serverOrOp(sender)) {
                    
                }
                return true;
            }
        }
        return false;
    }

    private boolean serverOrOp(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).isOp();
        } else {
            return true;
        }
    }

}
