/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin.disciplines;

/**
 * Enum for the disciplines.
 *
 * @author David
 */
public enum Discipline {
    PoleBending,
    BarrelRacing,
    WesternPlesure,
    HuntSeat,
    ShowJumping,
    Dressage,
    CrossCountry,
    Racing,
    SteepleChase;

    /**
     * Outputs the name of the horse breed with the space between the words.
     *
     * @return the horse breed name as a String.
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
