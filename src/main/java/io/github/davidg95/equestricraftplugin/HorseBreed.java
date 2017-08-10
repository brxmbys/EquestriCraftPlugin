/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

/**
 *
 * @author David
 */
public enum HorseBreed {
    AkhalTeke,
    AmericanCreamDraft,
    AmericanIndianHorse,
    AmericanPaintHorse,
    AmericanQuarterHorse,
    AmericanSaddlebred,
    AmericanWarmblood,
    Andalusian,
    Arabian,
    Appaloosa,
    AustrianWarmblood,
    AztecaHorse,
    BarbHorse,
    BelgianHorse,
    BlackForestHorse,
    BrazilianSportHorse,
    Brumby,
    CamargueHorse,
    CaspianHorse,
    ClevelandBay,
    Clydesdale,
    CroatianColdblood,
    CurlyHorse,
    DanishWarmblood,
    DutchHeavyDraft,
    DutchWarmblood,
    FjordHorse,
    FrenchTrotter,
    FriesianHorse,
    FriesianSport,
    GypsyVanner,
    HackneyHorse,
    Haflinger,
    Hanoverian,
    Icelandic,
    IrishDraught,
    IrishSport,
    Kathiawari,
    Kladruber,
    Lipizzaner,
    Lusitano,
    MissourieFoxTrotter,
    Morab,
    MorganHorse,
    Mustang,
    NormanCob,
    NorthSwedishHorse,
    Oldenburg,
    PasoFino,
    Percheron,
    RockyMountainHorse,
    SelleFrançais,
    Shirehorse,
    SpottedSaddleHorse,
    Standardbred,
    SwedishWarmblood,
    TennesseeWalking,
    Thoroughbred,
    Trakhener,
    WelshPony,
    Westphalian;

    /**
     * Get a random horse breed.
     *
     * @return a HorseBreed.
     */
    public static HorseBreed randomType() {
        final int index = (int) (Math.random() * HorseBreed.values().length);

        return HorseBreed.values()[index];
    }

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
