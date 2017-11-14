/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.database;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.HorseBreed;
import io.github.davidg95.equestricraftplugin.Illness;
import io.github.davidg95.equestricraftplugin.MyHorse;
import io.github.davidg95.equestricraftplugin.Personality;
import io.github.davidg95.equestricraftplugin.disciplines.Discipline;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public abstract class Database {

    EquestriCraftPlugin plugin;
    Connection connection;
    public String table = "horses";
    public String disciplinesTable = "disciplines";
    public int tokens = 0;

    public Database(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize() {
        connection = getSQLConnection();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + table);
            ResultSet rs = ps.executeQuery();
            close(ps, rs);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to retrieve connection", ex);
        }
    }

    public void addMember(Player p, Discipline d) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("INSERT INTO " + disciplinesTable + " (uuid, discipline) VALUES ('" + p.getUniqueId() + "', '" + d.toString() + "')");

            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public LinkedList<OfflinePlayer> getMembers(Discipline d) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        LinkedList<OfflinePlayer> members = new LinkedList<>();

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + disciplinesTable + " WHERE disciplines = '" + d.toString());

            rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                OfflinePlayer p = Bukkit.getOfflinePlayer(uuid);
                members.add(p);
            }
            return members;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return new LinkedList<>();
    }

    public LinkedList<MyHorse> getHorses() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        LinkedList<MyHorse> horses = new LinkedList<>();

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + table);

            rs = ps.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                int gender = rs.getInt("gender");
                boolean vaccinated = rs.getBoolean("vaccinated");
                long vacc_time = rs.getLong("vacc_time");
                boolean hungry = rs.getBoolean("hungry");
                long lastEat = rs.getLong("last_eat");
                long hungerTime = rs.getLong("hunger_time");
                boolean thirsty = rs.getBoolean("thristy");
                long lastDrink = rs.getLong("last_drink");
                long thirstTime = rs.getLong("thrist_time");
                long illSince = rs.getLong("ill_since");
                boolean ill = rs.getBoolean("ill");
                long wellSince = rs.getLong("ill_since");
                long lastBreed = rs.getLong("last_breed");
                boolean defacateSinceEat = rs.getBoolean("defacate_since_eat");
                String breed1 = rs.getString("breed1");
                String breed2 = rs.getString("breed2");
                long birth = rs.getLong("birth");
                String personality1 = rs.getString("person1");
                String personality2 = rs.getString("person2");
                int dieat = rs.getInt("dieat");
                String illness = rs.getString("illness");
                boolean shod = rs.getBoolean("shod");
                int trainingLevel = rs.getInt("training_level");

                HorseBreed breed[] = new HorseBreed[]{HorseBreed.valueOf(breed1), HorseBreed.valueOf(breed2)};
                Personality person[] = new Personality[]{Personality.valueOf(personality1), Personality.valueOf(personality2)};
                Illness ill_type = Illness.valueOf(illness);

                MyHorse horse = new MyHorse(vacc_time, vaccinated, gender, uuid, lastEat, hungry, hungerTime, lastDrink, thirsty, thirstTime, illSince, ill, wellSince, lastBreed, defacateSinceEat, breed, birth, person, dieat, ill_type, shod, trainingLevel);
                horses.add(horse);
            }
            return horses;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
        return new LinkedList<>();
    }

    public void close(PreparedStatement ps, ResultSet rs) {
        try {
            if (ps != null) {
                ps.close();
            }
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException ex) {
            Error.close(plugin, ex);
        }
    }
}
