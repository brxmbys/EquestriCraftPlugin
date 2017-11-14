/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.database;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 *
 * @author David
 */
public class SQLite extends Database {

    String dbname;

    public SQLite(EquestriCraftPlugin plugin) {
        super(plugin);
        dbname = "EquestriDatabase";
    }

    public String createDisciplinesTable = "CREATE TABLE IF NOT EXISTS " + disciplinesTable + "("
            + "`id` integer NO NULL"
            + "`uuid' varchar(60) NOT NULL,"
            + "`discipline` varchar(30) NOT NULL,"
            + "PRIMARY KEY (`id`)"
            + ");";

    @Override
    public Connection getSQLConnection() {
        File datafolder = new File(plugin.getDataFolder(), dbname + ".db");
        if (!datafolder.exists()) {
            try {
                datafolder.createNewFile();
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error creating .db file", ex);
            }
        }
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + datafolder);
            return connection;
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "SQLite library not found", ex);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error creating database", ex);
        }
        return null;
    }

    @Override
    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(createDisciplinesTable);
            s.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        initialize();
    }

}
