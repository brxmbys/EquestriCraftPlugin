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
}
