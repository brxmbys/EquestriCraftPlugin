/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.database;

/**
 *
 * @author David
 */
public class Errors {

    public static String sqlConnectionExecute() {
        return "Couldn't execute MySQL statement: ";
    }

    public static String sqlConnectionClose() {
        return "Failed to clode MySQL connection: ";
    }

    public static String noSQLConnection() {
        return "Unable to retrieve MySQL connection: ";
    }

    public static String noTableFound() {
        return "Database Error: No Table Found";
    }
}
