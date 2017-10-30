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
import org.bukkit.entity.Player;

/**
 *
 * @author David
 */
public class DisciplinesController {

    private List<Membership> memberships;

    private static final String DISCIPLINES_FILE = "disciplines.dat";

    public DisciplinesController() {
        this.memberships = new LinkedList<>();
        load();
    }

    public double addMembership(Player p, Discipline d) {
        int count = 0;
        for (Membership m : memberships) {
            if (m.getPlayer().equals(p.getUniqueId())) {
                if (m.getDiscipline().equals(d)) {
                    return -1;
                } else {
                    count++;
                }
            }
        }
        if (count == 2) {
            return -2;
        }
        memberships.add(new Membership(p.getUniqueId(), d));
        save();
        if (count == 0) {
            return 1000;
        } else if (count == 1) {
            return 5000;
        }
        return -2;
    }

    public List<Discipline> getMemberships(Player p) {
        List<Discipline> ds = new LinkedList<>();
        for (Membership m : memberships) {
            if (m.getPlayer().equals(p.getUniqueId())) {
                ds.add(m.getDiscipline());
            }
        }
        return ds;
    }

    public List<Membership> getAll() {
        return memberships;
    }

    public void reset() {
        memberships.clear();
        save();
    }

    /**
     * Persist horses to file.
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
     * Load horses from file
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
