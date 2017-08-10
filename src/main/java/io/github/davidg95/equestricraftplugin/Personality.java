/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

/**
 * Enum for personalities/traits. Note that adding new traits can change the
 * traits of some horses on the server.
 *
 * @author David
 */
public enum Personality {
    Aggressive, Confident, Calm, Anxious, Agile, Clever, Friendly, Gentle, Dominant, Shy, Sassy;

    /**
     * Get a random personality.
     *
     * @return a Personality.
     */
    public static Personality randomType() {
        final int index = (int) (Math.random() * Personality.values().length);

        return Personality.values()[index];
    }

    /**
     * Outputs the name of the personality with the space between the words.
     *
     * @return the personality name as a String.
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
