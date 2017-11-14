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
import java.util.UUID;
import java.util.logging.Level;
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
    public String rolesTable = "roles";
    public int tokens = 0;

    public Database(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
    }

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize() {
        connection = getSQLConnection();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + disciplinesTable);
            ResultSet rs = ps.executeQuery();
            close(ps, rs);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to retrieve connection", ex);
        }
    }

    public void setDocor(OfflinePlayer p, boolean set) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + rolesTable + " SET doctor = " + set + " WHERE uuid = '" + p.getUniqueId() + "'");
            ps.executeUpdate();
        } catch (SQLException ex) {
            conn = getSQLConnection();
            try {
                ps = conn.prepareStatement("INSERT INTO " + rolesTable + " (uuid, doctor, farrier) VALUES ('" + p.getUniqueId() + "','" + set + "', 'false')");
            } catch (SQLException ex1) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex1);
            }
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

    public void setFarrier(OfflinePlayer p, boolean set) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("UPDATE " + rolesTable + " SET farrier = " + set + " WHERE uuid = '" + p.getUniqueId() + "'");
            ps.executeUpdate();
        } catch (SQLException ex) {
            conn = getSQLConnection();
            try {
                ps = conn.prepareStatement("INSERT INTO " + rolesTable + " (uuid, farrier, doctor) VALUES ('" + p.getUniqueId() + "','" + Boolean.toString(set) + "', 'false')");
            } catch (SQLException ex1) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex1);
            }
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

    public boolean isDoctor(OfflinePlayer p) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT uuid, doctor FROM " + rolesTable + " WHERE uuid = '" + p.getUniqueId() + "'");

            rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getBoolean("doctor");
            }
        } catch (SQLException ex) {
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
        return false;
    }

    public boolean isFarrier(OfflinePlayer p) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT uuid, farrier FROM " + rolesTable + " WHERE uuid = '" + p.getUniqueId() + "'");

            rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getBoolean("farrier");
            }
        } catch (SQLException ex) {
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
        return false;
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
            ps = conn.prepareStatement("SELECT * FROM " + disciplinesTable + " WHERE discipline = '" + d.toString() + "'");

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

    public void saveHorse(MyHorse h) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("INSERT INTO " + table + " (uuid, gender, vacc_time, last_eat, last_drink, ill_since, ill, well_since, last_breed, defacate_since_eat, breed1, breed2, birth, person1, person2, dieat, illness, shod, training_level) VALUES ('"
                    + h.getUuid() + "'," + h.getGender() + "," + h.getVaccinationTime() + "," + h.getLastEat() + "," + h.getLastDrink() + "," + h.getIllDuration() + "," + h.isSick() + "," + h.getWellSince() + "," + h.getLastBreed() + "," + h.hasDefecate() + ",'" + h.getBreed()[0].toString() + "','" + h.getBreed()[0].toString() + "'," + h.getBirthTime() + ",'" + h.getPersonalities()[0].toString() + "','" + h.getPersonalities()[1].toString() + "'," + h.getDieAt() + ",'" + h.getIllness().toString() + "'," + h.isShod() + "," + h.getTrainingLevel() + ")");
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
                long wellSince = rs.getLong("well_since");
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
