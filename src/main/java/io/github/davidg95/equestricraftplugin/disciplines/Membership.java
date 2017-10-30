/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.disciplines;

import java.io.Serializable;
import java.util.UUID;

/**
 * Class which models a membership.
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

    /**
     * The UUID of the player.
     *
     * @return the UUID.
     */
    public UUID getPlayer() {
        return player;
    }

    /**
     * The discipline they are part of.
     *
     * @return the Discipline.
     */
    public Discipline getDiscipline() {
        return discipline;
    }
}
