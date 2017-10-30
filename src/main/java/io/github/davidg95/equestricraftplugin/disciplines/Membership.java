/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.disciplines;

import java.io.Serializable;
import java.util.UUID;

/**
 *
 * @author David
 */
public class Membership implements Serializable {

    private final UUID player;
    private final Discipline discipline;

    public Membership(UUID player, Discipline discipline) {
        this.player = player;
        this.discipline = discipline;
    }

    public UUID getPlayer() {
        return player;
    }

    public Discipline getDiscipline() {
        return discipline;
    }
}
