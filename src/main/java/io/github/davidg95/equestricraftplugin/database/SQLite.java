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
            + "id integer NOT NULL,"
            + "uuid varchar(60) NOT NULL,"
            + "discipline varchar(30) NOT NULL,"
            + "PRIMARY KEY (id)"
            + ")";

    public String createHorsesTable = "CREATE TABLE IF NOT EXISTS " + table + "("
            + "id integer NOT NULL,"
            + "uuid varchar(60) NOT NULL,"
            + "gender integer NOT NULL,"
            + "vaccinated boolean NOT NULL,"
            + "vacc_time long,"
            + "hungry boolean NOT NULL,"
            + "last_eat long,"
            + "hunger_time long,"
            + "thristy boolean NOT NULL,"
            + "last_drink long,"
            + "thrist_time long,"
            + "ill_since long,"
            + "ill boolean NOT NULL,"
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
            + "shod boolean NOT NULL,"
            + "training_level integer NOT NULL,"
            + "PRIMARY KEY (id)"
            + ")";

    public String createRolesTable = "CREATE TABLE IF NOT EXISTS " + rolesTable + "("
            + "uuid varchar(60) NOT NULL,"
            + "farrier boolean NOT NULL,"
            + "doctor boolean NOT NULL,"
            + "PRIMARY KEY (uuid)"
            + ")";

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
            s.executeUpdate(createHorsesTable);
            s.executeUpdate(createRolesTable);
            s.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        initialize();
    }

}
