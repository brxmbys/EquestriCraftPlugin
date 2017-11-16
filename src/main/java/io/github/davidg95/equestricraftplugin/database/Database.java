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
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Level;

/**
 *
 * @author David
 */
public abstract class Database {

    EquestriCraftPlugin plugin;
    Connection connection;
    public String table = "horses";
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

        try {
            conn = getSQLConnection();
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
                    + h.getBreed()[0].toString() + "','"
                    + h.getBreed()[1].toString() + "',"
                    + h.getBirthTime() + ",'"
                    + h.getPersonalities()[0].toString() + "','"
                    + h.getPersonalities()[1].toString() + "',"
                    + h.getDieAt() + ",'"
                    + h.getIllnessString() + "',"
                    + (h.isShod() ? "1" : "0") + ","
                    + h.getTrainingLevel() + ")");
            ps.executeUpdate();
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Database error", ex);
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
                String createHorsesTable = "CREATE TABLE IF NOT EXISTS " + table + "("
            + "id integer NOT NULL,"
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
            + "PRIMARY KEY (id)"
            + ")";
                
                int id = rs.getInt("id");
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
                Illness ill_type = Illness.valueOf(illness);

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
