/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.disciplines;

import io.github.davidg95.equestricraftplugin.EquestriCraftPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Controller for discipline membership.
 *
 * @author David
 */
public class DisciplinesController {

    private List<Membership> memberships;

    private static final String DISCIPLINES_FILE = "disciplines.dat";

    private static final double FIRST_DISCIPLINE = 1000;
    private static final double SECOND_DISCIPLINE = 5000;
    private static final double REFUND_PERCENT = 50;

    public DisciplinesController() {
        this.memberships = new LinkedList<>();
        load();
    }

    /**
     * check are players membership. Will return 1000 if they are not in a
     * discipline, 5000 if they are in one, -1 if they are already in the
     * discipline they wish to join, and -2 if they are already in 2
     * disciplines.
     *
     * @param p the player.
     * @param d the discipline.
     * @return the result.
     */
    public double checkMembership(Player p, Discipline d) {
        int count = 0;
        for (Membership m : memberships) {
            if (m.getPlayer().equals(p.getUniqueId())) {
                if (d == null) {
                    count++;
                } else {
                    if (m.getDiscipline().equals(d)) {
                        return -1;
                    } else {
                        count++;
                    }
                }
            }
        }
        if (count == 2) {
            return -2;
        }
        save();
        if (count == 0) {
            return FIRST_DISCIPLINE;
        } else if (count == 1) {
            return SECOND_DISCIPLINE;
        }
        return -2;
    }

    /**
     * Add a player to a discipline.
     *
     * @param p the player.
     * @param d the discipline.
     */
    public void addMembership(Player p, Discipline d) {
        memberships.add(new Membership(p.getUniqueId(), d));
        save();
    }

    public double removeMembership(Player p, Discipline d) {
        boolean found = false;
        for (int i = 0; i < memberships.size(); i++) {
            Membership m = memberships.get(i);
            if (m.getPlayer().equals(p.getUniqueId())) {
                if (m.getDiscipline().equals(d)) {
                    memberships.remove(i);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            return -1;
        }
        double v = checkMembership(p, null);
        return v / 100 * REFUND_PERCENT;
    }

    /**
     * Get a players discipline membership.
     *
     * @param p the player.
     * @return the disciplines they are in.
     */
    public List<Discipline> getMemberships(Player p) {
        List<Discipline> ds = new LinkedList<>();
        for (Membership m : memberships) {
            if (m.getPlayer().equals(p.getUniqueId())) {
                ds.add(m.getDiscipline());
            }
        }
        return ds;
    }

    /**
     * Get all memberships.
     *
     * @return the memberships as a list.
     */
    public List<Membership> getAll() {
        return memberships;
    }

    /**
     * Get players in a discipline.
     *
     * @param d the discipline.
     * @return a list of players.
     */
    public List<Player> getDisciplineMembers(Discipline d) {
        List<Player> players = new LinkedList<>();
        for (Membership m : memberships) {
            if (m.getDiscipline().equals(d)) {
                players.add(Bukkit.getPlayer(m.getPlayer()));
            }
        }
        return players;
    }

    /**
     * Clear all memberships.
     */
    public void reset() {
        memberships.clear();
        save();
    }

    public void reset(Discipline d) {
        for (int i = 0; i < memberships.size(); i++) {
            Membership m = memberships.get(i);
            if (m.getDiscipline().equals(d)) {
                memberships.remove(i);
            }
        }
    }

    /**
     * Persist memberships to file.
     */
    public final void save() {
        final File file = new File(DISCIPLINES_FILE);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
        } catch (IOException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        }
        try (OutputStream os = new FileOutputStream(DISCIPLINES_FILE)) {
            final ObjectOutputStream oo = new ObjectOutputStream(os);
            oo.writeObject(memberships);
        } catch (FileNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Load memberships from file
     */
    private void load() {
        try (InputStream is = new FileInputStream(DISCIPLINES_FILE)) {
            final ObjectInputStream oi = new ObjectInputStream(is);
            memberships = (List<Membership>) oi.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            EquestriCraftPlugin.plugin.getLogger().log(Level.SEVERE, null, ex);
        }
    }
}
