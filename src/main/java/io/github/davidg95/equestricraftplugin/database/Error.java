/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.database;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.util.logging.Level;

/**
 *
 * @author David
 */
public class Error {

    public static void execute(EquestriCraftPlugin plugin, Exception ex) {
        plugin.getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }

    public static void close(EquestriCraftPlugin plugin, Exception ex) {
        plugin.getLogger().log(Level.SEVERE, "Couldn't close connection: ", ex);
    }
}
