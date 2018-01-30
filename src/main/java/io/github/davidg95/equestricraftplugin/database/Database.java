/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.database;

import io.github.davidg95.equestricraftplugin.buildpay.PayLog;
import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import io.github.davidg95.equestricraftplugin.HorseBreed;
import io.github.davidg95.equestricraftplugin.Illness;
import io.github.davidg95.equestricraftplugin.MyHorse;
import io.github.davidg95.equestricraftplugin.Personality;
import io.github.davidg95.equestricraftplugin.warps.Warp;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
    public String warpsTable = "warps";
    public String payTable = "payTable";
    public int tokens = 0;

    private final StampedLock lock;

    public Database(EquestriCraftPlugin plugin) {
        this.plugin = plugin;
        lock = new StampedLock();
    }

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM " + table);
            close(s, rs);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to retrieve connection", ex);
        }
    }

    public int hungryHorses() {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + new Date().getTime() + " - last_eat > 604800000");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public int thirstyHorses() {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + new Date().getTime() + " - last_drink > 604800000");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public int illHorses() {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE ill_since > well_since");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public int vaccedHorses() {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + new Date().getTime() + " - vaccinationTime > 2419200000");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public int shoedHorses() {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE shoed = 1");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public int oldHorses(int months) {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + new Date().getTime() + " - birth > " + months + " * 2.5 * 24 * 60 * 60 * 1000");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public int deadHorses() {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE ill_since > well_since AND " + new Date().getTime() + " - ill_since > 7 * 24 * 60 * 60 * 1000");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public int killDead() {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            return s.executeUpdate("DELETE FROM " + table + " WHERE ill_since > well_since AND " + new Date().getTime() + " - ill_since > 7 * 24 * 60 * 60 * 1000");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public int uuidCheck(UUID uuid) {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE '" + uuid.toString() + "' = uuid");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public void removeHorse(UUID uuid) {
        Connection conn = null;
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            conn = getSQLConnection();
            s = conn.createStatement();
            s.executeUpdate("DELETE FROM " + table + " WHERE uuid='" + uuid.toString() + "'");
            plugin.getLogger().log(Level.INFO, "Horse " + uuid + " has been removed from the database");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error removing horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public int ignoredHorses() {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            ResultSet set = s.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE ignore = 1");
            while (set.next()) {
                return set.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting ignored horse count", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return -1;
    }

    public int horseCount(int gender) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            String query;
            if (gender == -1) {
                query = "SELECT COUNT(*) FROM " + table;
            } else {
                query = "SELECT COUNT(*) FROM " + table + " WHERE gender = " + gender;
            }

            ResultSet rs = s.executeQuery(query);
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse count", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return 0;
    }

    public void addHorse(MyHorse h) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("INSERT INTO " + table + " (uuid, gender, vaccinationTime, last_eat, last_drink, ill_since, well_since, last_breed, defacate_since_eat, breed1, breed2, birth, person1, person2, dieat, illness, shoed, training_level) VALUES ('"
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
                    + (h.isShoed() ? "1" : "0") + ","
                    + h.getTrainingLevel() + ")");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error adding new horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void changeGender(UUID uuid, int gender) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table
                    + " SET gender = " + gender
                    + " WHERE uuid = '" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error saving horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void changeBreed(UUID uuid, HorseBreed b1, HorseBreed b2) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table
                    + " SET breed1 = " + b1.name()
                    + ", SET breed2 = " + b2.name()
                    + " WHERE uuid = '" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error saving horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void changePersonality(UUID uuid, Personality p1, Personality p2) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table
                    + " SET person1 = " + p1.name()
                    + ", SET person2 = " + p2.name()
                    + " WHERE uuid = '" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error saving horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void vaccinateHorse(UUID uuid) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET vaccinationTime = " + new Date().getTime() + " WHERE uuid = '" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error vaccinating horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void shoeHorse(UUID uuid) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET shoed = 1 WHERE uuid = '" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error shoeing horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void setGender(UUID uuid, int gender) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET gender = " + gender + " WHERE uuid = '" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error changing gender", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public int getGender(UUID uuid) {
        Connection conn = getSQLConnection();
        Statement s = null;
        ResultSet set = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            set = s.executeQuery("SELECT gender FROM " + table + " WHERE uuid = '" + uuid + "'");
            while (set.next()) {
                return set.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting Gender", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (set != null) {
                    set.close();
                }
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
        return -1;
    }

    public void setLevel(UUID uuid, int level) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET training_level = " + level + " WHERE uuid = '" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error setting level", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void feedHorse(UUID uuid) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET last_eat = " + new Date().getTime() + ", defacate_since_eat = 0 WHERE uuid='" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error feeding horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void waterHorse(UUID uuid) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET last_drink = " + new Date().getTime() + " WHERE uuid='" + uuid + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error watering horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public void saveHorse(MyHorse h) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table
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
                    + "', shoed = " + (h.isShoed() ? "1" : "0")
                    + ", training_level = " + h.getTrainingLevel()
                    + " WHERE uuid = '" + h.getUuid() + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error saving horse", ex);
        } finally {
            lock.unlockWrite(stamp);
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
        }
    }

    public LinkedList<MyHorse> getAllHorses(int g) {
        Connection conn = getSQLConnection();
        Statement s = null;
        ResultSet rs;

        LinkedList<MyHorse> horses = new LinkedList<>();

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            if (g == -1) {
                rs = s.executeQuery("SELECT * FROM " + table);
            } else {
                rs = s.executeQuery("SELECT * FROM " + table + " WHERE gender = " + g);
            }
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
                boolean ignored = rs.getBoolean("ignored");

                HorseBreed breed[] = new HorseBreed[]{HorseBreed.valueOf(breed1), HorseBreed.valueOf(breed2)};
                Personality person[] = new Personality[]{Personality.valueOf(personality1), Personality.valueOf(personality2)};
                Illness ill_type = null;
                if (illness != null && !illness.equals("") && !illness.equals("null")) {
                    ill_type = Illness.valueOf(illness);
                }

                MyHorse horse = new MyHorse(vacc_time, gender, uuid, lastEat, lastDrink, illSince, wellSince, lastBreed, defacateSinceEat, breed, birth, person, dieat, ill_type, shoed, trainingLevel, ignored);
                horses.add(horse);
            }
            return horses;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horses", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return new LinkedList<>();
    }

    public LinkedList<MyHorse> getHorses(int g) {
        Connection conn = getSQLConnection();
        Statement s = null;
        ResultSet rs;

        LinkedList<MyHorse> horses = new LinkedList<>();

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            if (g == -1) {
                rs = s.executeQuery("SELECT * FROM " + table + " WHERE ignore = 0");
            } else {
                rs = s.executeQuery("SELECT * FROM " + table + " WHERE gender = " + g + " AND ignore = 0");
            }
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
                boolean ignored = rs.getBoolean("ignore");

                HorseBreed breed[] = new HorseBreed[]{HorseBreed.valueOf(breed1), HorseBreed.valueOf(breed2)};
                Personality person[] = new Personality[]{Personality.valueOf(personality1), Personality.valueOf(personality2)};
                Illness ill_type = null;
                if (illness != null && !illness.equals("") && !illness.equals("null")) {
                    ill_type = Illness.valueOf(illness);
                }

                MyHorse horse = new MyHorse(vacc_time, gender, uuid, lastEat, lastDrink, illSince, wellSince, lastBreed, defacateSinceEat, breed, birth, person, dieat, ill_type, shoed, trainingLevel, ignored);
                horses.add(horse);
            }
            return horses;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horses", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return new LinkedList<>();
    }

    public MyHorse getHorse(UUID uuid) {
        Connection conn = getSQLConnection();
        Statement ps = null;
        ResultSet rs;

        MyHorse horse;

        final long stamp = lock.writeLock();

        try {
            ps = conn.createStatement();

            rs = ps.executeQuery("SELECT * FROM " + table + " WHERE UUID = '" + uuid.toString() + "'");
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
                boolean ignored = rs.getBoolean("ignore");

                HorseBreed breed[] = new HorseBreed[]{HorseBreed.valueOf(breed1), HorseBreed.valueOf(breed2)};
                Personality person[] = new Personality[]{Personality.valueOf(personality1), Personality.valueOf(personality2)};
                Illness ill_type = null;
                if (illness != null && !illness.equals("") && !illness.equals("null")) {
                    ill_type = Illness.valueOf(illness);
                }

                horse = new MyHorse(vacc_time, gender, uuid, lastEat, lastDrink, illSince, wellSince, lastBreed, defacateSinceEat, breed, birth, person, dieat, ill_type, shoed, trainingLevel, ignored);
                return horse;
            }
            plugin.getLogger().log(Level.WARNING, "Horse not found");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse", ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Error closing statement");
                }
                if (conn != null) {
                    conn.close();
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Error closing connection");
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return null;
    }

    public boolean hadBredRecently(Player p) {
        Connection conn = getSQLConnection();
        Statement s = null;
        ResultSet rs;

        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            rs = s.executeQuery("SELECT * FROM " + breedTable + " WHERE UUID = '" + p.getUniqueId().toString() + "' ORDER BY time DESC");
            while (rs.next()) {
                long breedTime = rs.getLong("time");
                return new Date().getTime() < breedTime;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
            }
        }
        return false;
    }

    public void breedNow(Player p) {
        Connection conn = null;
        Statement s = null;

        try {
            conn = getSQLConnection();
            s = conn.createStatement();
            s.executeUpdate("INSERT INTO " + breedTable + " (uuid, time) VALUES('" + p.getUniqueId() + "'," + new Date().getTime() + ")");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
            }
        }
    }

    public int getTotalBreeds(OfflinePlayer p) {
        Connection conn = getSQLConnection();
        Statement s = null;
        ResultSet rs;

        try {
            s = conn.createStatement();

            rs = s.executeQuery("SELECT COUNT(*) FROM " + breedTable + " WHERE UUID = '" + p.getUniqueId().toString() + "'");
            while (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
            }
        }
        return 0;
    }

    public long getLastBreed(OfflinePlayer p) {
        Connection conn = getSQLConnection();
        Statement s = null;
        ResultSet rs;

        try {
            s = conn.createStatement();

            rs = s.executeQuery("SELECT * FROM " + breedTable + " WHERE UUID = '" + p.getUniqueId().toString() + "' ORDER BY time DESC");
            while (rs.next()) {
                return rs.getLong("time");
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
            }
        }
        return -1;
    }

    public void removeHungerAndThrist() {
        Connection conn = null;
        Statement s = null;

        long now = new Date().getTime();

        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            s.executeUpdate("UPDATE " + table + " SET last_eat = " + now + ", last_drink= " + now);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
            }
        }
    }

    public void cureAll() {
        Connection conn = null;
        Statement s = null;

        long now = new Date().getTime();

        try {
            conn = getSQLConnection();
            s = conn.createStatement();

            s.executeUpdate("UPDATE " + table + " SET well_since = " + now);
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "DB Error", ex);
            }
        }
    }

    public int getHorseLevel(UUID uuid) {
        Connection conn = getSQLConnection();
        Statement ps = null;
        ResultSet rs;

        final long stamp = lock.writeLock();

        try {
            ps = conn.createStatement();

            rs = ps.executeQuery("SELECT training_level FROM " + table + " WHERE UUID = '" + uuid.toString() + "'");
            while (rs.next()) {
                return rs.getInt("training_level");
            }
            plugin.getLogger().log(Level.WARNING, "Horse not found");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting horse", ex);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Error closing statement");
                }
                if (conn != null) {
                    conn.close();
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Error closing connection");
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return -1;
    }

    public Object submitCommand(String command) {
        Connection conn = getSQLConnection();
        Statement s = null;
        ResultSet rs;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            rs = s.executeQuery(command);
            while (rs.next()) {
                return rs.getObject(1);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error submitting command", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Error closing statement");
                }
                if (conn != null) {
                    conn.close();
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Error closing connection");
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return null;
    }

    public void defecateHorse(UUID uuid) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET defacate_since_eat = 1 WHERE uuid='" + uuid.toString() + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error defecating horse", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
    }

    public void setIgnore(UUID uuid, boolean ignore) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET ignore = " + (ignore ? "1" : "0") + " WHERE uuid='" + uuid.toString() + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error changing horse", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
    }

    public void noIgnore() {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("UPDATE " + table + " SET ignore = 0");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error changing horses", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
    }

    public void addWarp(Warp warp) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("insert into " + warpsTable + " (owner, name, world, x, y, z) values ('" + warp.getPlayer().getUniqueId() + "','" + warp.getName() + "','" + warp.getLocation().getWorld().getName() + "'," + warp.getLocation().getX() + "," + warp.getLocation().getY() + "," + warp.getLocation().getZ() + ")");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error saving warp", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
    }

    public List<Warp> getPlayerWarps(OfflinePlayer p) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            ResultSet set = s.executeQuery("SELECT * FROM " + warpsTable + " WHERE owner='" + p.getUniqueId() + "'");
            List<Warp> warps = new LinkedList<>();
            while (set.next()) {
                String name = set.getString(2);
                String world = set.getString(3);
                int x = set.getInt(4);
                int y = set.getInt(5);
                int z = set.getInt(6);
                Location l = new Location(Bukkit.getWorld(world), x, y, z);
                Warp warp = new Warp(p, name, l);
                warps.add(warp);
            }
            return warps;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player warps", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return new LinkedList<>();
    }

    public Warp getPlayerWarp(OfflinePlayer p, String w) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            ResultSet set = s.executeQuery("select * from " + warpsTable + " where owner='" + p.getUniqueId() + "' and name='" + w + "'");
            while (set.next()) {
                String name = set.getString(2);
                String world = set.getString(3);
                int x = set.getInt(4);
                int y = set.getInt(5);
                int z = set.getInt(6);
                Location l = new Location(Bukkit.getWorld(world), x, y, z);
                Warp warp = new Warp(p, name, l);
                return warp;
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting player warps", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return null;
    }

    public List<Warp> getAllWarps() {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            ResultSet set = s.executeQuery("select * from " + warpsTable);
            List<Warp> warps = new LinkedList<>();
            while (set.next()) {
                UUID uuid = UUID.fromString(set.getString(1));
                String name = set.getString(2);
                String world = set.getString(3);
                int x = set.getInt(4);
                int y = set.getInt(5);
                int z = set.getInt(6);
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                Location l = new Location(Bukkit.getWorld(world), x, y, z);
                Warp warp = new Warp(player, name, l);
                warps.add(warp);
            }
            return warps;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting warps", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return null;
    }

    public void removeWarp(Player p, String warp) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("delete from " + warpsTable + " where owner = '" + p.getUniqueId() + "' and name = '" + warp + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error removing warp", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
    }

    public void addPayLog(PayLog pl) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            s.executeUpdate("insert into " + payTable + " (uuid, value, reason) values ('" + pl.getPlayer().getUniqueId() + "'," + pl.getValue() + ",'" + pl.getReason() + "'");
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error adding warp", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
    }

    public List<PayLog> getPayLogs(OfflinePlayer p) {
        Connection conn = getSQLConnection();
        Statement s = null;

        final long stamp = lock.writeLock();
        try {
            s = conn.createStatement();
            ResultSet set;
            if (p == null) {
                set = s.executeQuery("select * from " + payTable);
            } else {
                set = s.executeQuery("select * from " + payTable + " where uuid='" + p.getUniqueId() + "'");
            }
            List<PayLog> logs = new LinkedList<>();
            while (set.next()) {
                int id = set.getInt(1);
                UUID uuid = UUID.fromString(set.getString(2));
                int value = set.getInt(3);
                String reason = set.getString(4);
                PayLog log = new PayLog(id, Bukkit.getOfflinePlayer(uuid), value, reason);
                logs.add(log);
            }
            return logs;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error getting build logs", ex);
        } finally {
            try {
                if (s != null) {
                    s.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Error closing connection", ex);
            }
            lock.unlockWrite(stamp);
        }
        return new LinkedList<>();
    }

    private void close(Statement s, ResultSet rs) {
        try {
            if (s != null) {
                s.close();
            }
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }
}
