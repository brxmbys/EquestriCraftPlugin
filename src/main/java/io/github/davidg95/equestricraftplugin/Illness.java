/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

/**
 * Enum for illnesses.
 *
 * @author David
 */
public enum Illness {
    Laminitis,
    Colic,
    EquineInfluenza,
    MudFever,
    SkinCancer,
    RainRot,
    WhiteLineDisease, //Farrier
    Strangles,
    GrassSickness,
    MareReproductiveLoss, //Mares only
    HoofWallSeparation, //Farrier
    WobblerDisease,
    WestNileFever,
    TripleE,
    EquineShivers,
    BrokenLegs,
    RecurrentAirwayObstruction,
    Heaves,
    DegenerativeJointDisease,
    KidneyInflammation,
    Trauma,
    Concussion,
    Corns, //Farrier
    Curb,
    BrokenBloodVessels;

    /**
     * Get a random illness.
     *
     * @return an Illness.
     */
    public static Illness randomIllness() {
        final int index = (int) (Math.random() * Illness.values().length);

        return Illness.values()[index];
    }

    /**
     * Outputs the name of the illness with the space between the words.
     *
     * @return the horse illness as a String.
     */
    @Override
    public String toString() {
        String name = this.name();
        for (int i = 0; i < name.length(); i++) {
            if (i > 0) {
                if (Character.isUpperCase(name.charAt(i)) && Character.isLowerCase(name.charAt(i - 1))) {
                    final String first = name.substring(0, i);
                    final String last = name.substring(i);
                    name = first + " " + last;
                }
            }
        }
        return name;
    }
}
