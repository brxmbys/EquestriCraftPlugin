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

    public String createHorsesTable = "CREATE TABLE IF NOT EXISTS " + table + "("
            + "uuid varchar(60) NOT NULL,"
            + "gender integer NOT NULL,"
            + "vaccinationTime long,"
            + "last_eat long,"
            + "last_drink long,"
            + "ill_since long,"
            + "well_since long,"
            + "last_breed long,"
            + "defacate_since_eat boolean NOT NULL,"
            + "breed1 varchar(30) NOT NULL,"
            + "breed2 varchar(30),"
            + "birth long NOT NULL,"
            + "person1 varchar(30) NOT NULL,"
            + "person2 varchar(30) NOT NULL,"
            + "dieat long NOT NULL,"
            + "illness varchar(30),"
            + "shoed boolean NOT NULL,"
            + "training_level integer NOT NULL,"
            + "PRIMARY KEY (uuid)"
            + ")";

    public String createBreedLogTable = "CREATE TABLE IF NOT EXISTS " + breedTable + "("
            + "id int NOT NULL,"
            + "uuid varchar(60) NOT NULL,"
            + "time long NOT NULL,"
            + "PRIMARY KEY (id)"
            + ")";

    @Override
    public Connection getSQLConnection() {
        File datafolder = new File(plugin.getDataFolder(), dbname + ".db");
        if (!datafolder.exists()) {
            try {
                plugin.getLogger().log(Level.INFO, "Database does not exist, creating it");
                datafolder.createNewFile();
                plugin.getLogger().log(Level.INFO, "Created database at " + datafolder.getAbsolutePath());
                load();
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
            s.executeUpdate(createHorsesTable);
            s.executeUpdate(createBreedLogTable);
            s.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        initialize();
    }

}
