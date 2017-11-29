/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.database;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.HorseBreed;
import io.github.davidg95.equestricraftplugin.Illness;
import io.github.davidg95.equestricraftplugin.MyHorse;
import io.github.davidg95.equestricraftplugin.Personality;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public String breedTable = "breedLog";
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

    public void removeHorse(UUID uuid) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("DELETE FROM " + table + " WHERE uuid='" + uuid.toString() + "'");
            ps.executeUpdate();
            plugin.getLogger().log(Level.INFO, "Horse " + uuid + " has been removed from the database");
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

    public int horseCount() {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) FROM " + table);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getInt(1);
            }
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
        return 0;
    }

    public void saveHorse(MyHorse h) {
        Connection conn = null;
        PreparedStatement ps = null;

        conn = getSQLConnection();
        try {
            ps = conn.prepareStatement("INSERT INTO " + table + " (uuid, gender, vaccinationTime, last_eat, last_drink, ill_since, well_since, last_breed, defacate_since_eat, breed1, breed2, birth, person1, person2, dieat, illness, shoed, training_level) VALUES ('"
                    + h.getUuid() + "',"
                    + h.getGender() + ","
                    + h.getVaccinationTime() + ","
                    + h.getLastEat() + ","
                    + h.getLastDrink() + ","
                    + h.getIllSince() + ","
                    + h.getWellSince() + ","
                    + h.getLastBreed() + ","
                    + (h.hasDefecate() ? "1" : "0") + ",'"
                    + h.getBreed()[0].name() + "','"
                    + h.getBreed()[1].name() + "',"
                    + h.getBirthTime() + ",'"
                    + h.getPersonalities()[0].name() + "','"
                    + h.getPersonalities()[1].name() + "',"
                    + h.getDieAt() + ",'"
                    + h.getIllnessString() + "',"
                    + (h.isShod() ? "1" : "0") + ","
                    + h.getTrainingLevel() + ")");
            ps.executeUpdate();
        } catch (SQLException ex) {
            try {
                ps = conn.prepareStatement("UPDATE " + table
                        + " SET gender = " + h.getGender()
                        + ", vaccinationTime = " + h.getVaccinationTime()
                        + ", last_eat = " + h.getLastEat()
                        + ", last_drink = " + h.getLastDrink()
                        + ", ill_since = " + h.getIllSince()
                        + ", well_since = " + h.getWellSince()
                        + ", last_breed = " + h.getLastBreed()
                        + ", defacate_since_eat = " + (h.hasDefecate() ? "1" : "0")
                        + ", breed1 = '" + h.getBreed()[0].name()
                        + "', breed2 = '" + h.getBreed()[1].name()
                        + "', birth = " + h.getBirthTime()
                        + ", person1 = '" + h.getPersonalities()[0].name()
                        + "', person2 = '" + h.getPersonalities()[1].name()
                        + "', dieat = " + h.getDieAt()
                        + ", illness = '" + h.getIllnessString()
                        + "', shoed = " + (h.isShod() ? "1" : "0")
                        + ", training_level = " + h.getTrainingLevel()
                        + " WHERE uuid = '" + h.getUuid() + "'");
                ps.executeUpdate();
            } catch (SQLException ex1) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex1);
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
                long vacc_time = rs.getLong("vaccinationTime");
                long lastEat = rs.getLong("last_eat");
                long lastDrink = rs.getLong("last_drink");
                long illSince = rs.getLong("ill_since");
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
                boolean shoed = rs.getBoolean("shoed");
                int trainingLevel = rs.getInt("training_level");

                HorseBreed breed[] = new HorseBreed[]{HorseBreed.valueOf(breed1), HorseBreed.valueOf(breed2)};
                Personality person[] = new Personality[]{Personality.valueOf(personality1), Personality.valueOf(personality2)};
                Illness ill_type = null;
                if (illness != null && !illness.equals("") && !illness.equals("null")) {
                    ill_type = Illness.valueOf(illness);
                }

                MyHorse horse = new MyHorse(vacc_time, gender, uuid, lastEat, lastDrink, illSince, wellSince, lastBreed, defacateSinceEat, breed, birth, person, dieat, ill_type, shoed, trainingLevel);
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

    public MyHorse getHorse(UUID uuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        MyHorse horse;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + table + " WHERE UUID = '" + uuid.toString() + "'");

            rs = ps.executeQuery();
            while (rs.next()) {
                int gender = rs.getInt("gender");
                long vacc_time = rs.getLong("vaccinationTime");
                long lastEat = rs.getLong("last_eat");
                long lastDrink = rs.getLong("last_drink");
                long illSince = rs.getLong("ill_since");
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
                boolean shoed = rs.getBoolean("shoed");
                int trainingLevel = rs.getInt("training_level");

                HorseBreed breed[] = new HorseBreed[]{HorseBreed.valueOf(breed1), HorseBreed.valueOf(breed2)};
                Personality person[] = new Personality[]{Personality.valueOf(personality1), Personality.valueOf(personality2)};
                Illness ill_type = null;
                if (illness != null && !illness.equals("") && !illness.equals("null")) {
                    ill_type = Illness.valueOf(illness);
                }

                horse = new MyHorse(vacc_time, gender, uuid, lastEat, lastDrink, illSince, wellSince, lastBreed, defacateSinceEat, breed, birth, person, dieat, ill_type, shoed, trainingLevel);
                return horse;
            }
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
        return null;
    }

    public boolean hadBredRecently(Player p) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + breedTable + " WHERE UUID = '" + p.getUniqueId().toString() + "' ORDER BY time DESC");

            rs = ps.executeQuery();
            while (rs.next()) {
                long breedTime = rs.getLong("time");
                if (new Date().getTime() < breedTime) {
                    return true;
                }
                return false;
            }
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
        return false;
    }

    public void breedNow(Player p) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("INSERT INTO " + breedTable + " (uuid, time) VALUES('" + p.getUniqueId() + "'," + new Date().getTime() + ")");
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

    public int getTotalBreeds(OfflinePlayer p) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT COUNT(*) FROM " + breedTable + " WHERE UUID = '" + p.getUniqueId().toString() + "'");

            rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getInt(1);
            }
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
        return 0;
    }
    
    public long getLastBreed(OfflinePlayer p){
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = getSQLConnection();
            ps = conn.prepareStatement("SELECT * FROM " + breedTable + " WHERE UUID = '" + p.getUniqueId().toString() + "' ORDER BY time DESC");

            rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getLong("time");
            }
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
        return -1;
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
