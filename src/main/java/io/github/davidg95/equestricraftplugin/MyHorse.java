/*
 * EquestriCraftPlugin for equestricraft.serv.nu.
 */
package io.github.davidg95.equestricraftplugin;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;

/**
 *
 * @author David
 */
public class MyHorse implements Serializable {

    private static final long serialVersionUID = 1L;

    private long vaccinationTime;
    private int gender;
    private UUID uuid;
    private long lastEat;
    private long lastDrink;
    private long illSince;
    private long wellSince;
    private long lastBreed;
    private boolean defecateSinceEat;
    private HorseBreed[] breedArr;
    private volatile long birthTime;
    private Personality[] personality;
    private int dieat;
    private Illness illness;
    private boolean shoed;
    private int trainingLevel;
    private boolean ignored;

    /**
     * The length of time a vaccination will last.
     */
    public static long VACCINATION_DURATION = 2419200000L; //Four weeks.
    /**
     * The length of time a horse can go without drinking before getting sick.
     */
    public static long DRINK_LIMIT = 604800000L; //One week.
    /**
     * The length of time a horse can go without eating before getting sick.
     */
    public static long EAT_LIMIT = 604800000; //One week.

    /**
     * Indicates the horses gender is a stallion. Value = 1.
     */
    public static final int STALLION = 1;
    /**
     * Indicates the horses gender is a mare. Value = 2.
     */
    public static final int MARE = 2;
    /**
     * Indicates the horses gender is a gelding. Value = 3.
     */
    public static final int GELDING = 3;

    public MyHorse(Horse h) {
        this.gender = MyHorse.generateRandomGender();
        this.vaccinationTime = 0;
        this.lastEat = getCurrentTime();
        this.lastDrink = getCurrentTime();
        this.illSince = 0;
        this.wellSince = getCurrentTime();
        this.lastBreed = getCurrentTime();
        this.defecateSinceEat = true;
        this.uuid = h.getUniqueId();
        this.breedArr = new HorseBreed[1];
        this.breedArr[0] = HorseBreed.randomType();
        if (h.getVariant() == Variant.DONKEY) {
            breedArr[0] = HorseBreed.Donkey;
        } else if (h.getVariant() == Variant.MULE) {
            breedArr[0] = HorseBreed.Mule;
        }
        this.birthTime = getCurrentTime();
        Personality p1 = Personality.randomType();
        Personality p2;
        while (true) {
            p2 = Personality.randomType();
            if (!p1.equals(p2)) {
                break;
            }
        }
        this.personality = new Personality[]{p1, p2};
        this.dieat = randomDieAt();
        this.illness = null;
        h.setBaby();
        h.setAgeLock(true);
        shoed = false;
        trainingLevel = 1;
        ignored = false;
    }

    public MyHorse(long vaccinationTime, int gender, UUID uuid, long lastEat, long lastDrink, long illSince, long wellSince, long lastBreed, boolean defecate, HorseBreed breed[], long birth, Personality person[], long dieat, Illness illness, boolean shoed, int trainingLevel, boolean ignored) {
        this.vaccinationTime = vaccinationTime;
        this.gender = gender;
        this.uuid = uuid;
        this.lastEat = lastEat;
        this.lastDrink = lastDrink;
        this.illSince = illSince;
        this.wellSince = wellSince;
        this.lastBreed = lastBreed;
        this.defecateSinceEat = defecate;
        this.breedArr = breed;
        this.birthTime = birth;
        this.personality = person;
        this.dieat = (int) dieat;
        this.illness = illness;
        this.shoed = shoed;
        this.trainingLevel = trainingLevel;
        this.ignored = ignored;
    }

    public static long getCurrentTime() {
        return new Date().getTime();
    }

    public void checkData() {
        if (breedArr == null) {
            HorseBreed br = HorseBreed.randomType();
            breedArr = new HorseBreed[]{br, br};
        }
        if (breedArr.length == 0) {
            HorseBreed br = HorseBreed.randomType();
            breedArr = new HorseBreed[]{br, br};
        }
        if (breedArr.length == 1) {
            HorseBreed br = breedArr[0];
            breedArr = new HorseBreed[]{br, br};
        }
        if (breedArr.length == 2) {
            if (breedArr[1] == null) {
                breedArr[1] = breedArr[0];
            }
        }

        if (personality == null) {
            personality = new Personality[]{Personality.randomType(), Personality.randomType()};
        }
    }

    /**
     * Generate a random month between the age of 25 and 35.
     *
     * @return in between 300 and 420
     */
    private static int randomDieAt() {
        double r = Math.random();
        return (int) (r * 120) + 300;
    }

    /**
     * Get the UUID of the horse.
     *
     * @return the horses UUID.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Method to generate a random gender. Either MyHorse.STALLION or
     * MyHorse.MARE.
     *
     * @return the gender.
     */
    private static int generateRandomGender() {
        final double r = Math.random();
        if (r < 0.5) {
            return STALLION;
        } else {
            return MARE;
        }
    }

    public int getGender() {
        return gender;
    }

    public long getVaccinationTime() {
        return vaccinationTime;
    }

    public boolean isVaccinated() {
        return this.getDurationSinceLastVaccinated() < VACCINATION_DURATION;
    }

    public long getLastEat() {
        return lastEat;
    }

    public void setLastEat(long lastEat) {
        this.lastEat = lastEat;
    }

    public long getLastDrink() {
        return lastDrink;
    }

    public void setLastDrink(long lastDrink) {
        this.lastDrink = lastDrink;
    }

    public long getIllSince() {
        return illSince;
    }

    public long getWellSince() {
        return wellSince;
    }

    public long getLastBreed() {
        return lastBreed;
    }

    /**
     * Sets the current time to the horses last breed time.
     */
    public void setLastBreed() {
        this.lastBreed = getCurrentTime();
    }

    public void allowBreed() {
        this.lastBreed = 0;
    }

    /**
     * Returns the duration in ms since that last vaccination.
     *
     * @return the duration as a long.
     */
    public long getDurationSinceLastVaccinated() {
        return getCurrentTime() - this.vaccinationTime;
    }

    /**
     * Sets the vaccination state of the horse, if they are being vaccinated,
     * the time will be set.
     */
    public void vaccinate() {
        this.vaccinationTime = getCurrentTime();
    }

    /**
     * Sets the last drink time to the current time.
     */
    public void drink() {
        this.lastDrink = getCurrentTime();
    }

    /**
     * Get the thirst state of the horse.
     *
     * @return the thirst state as a boolean.
     */
    public boolean isThirsty() {
        return this.getDurationSinceLastDrink() > DRINK_LIMIT; //Check if the horse is thirsty.
    }

    /**
     * Get the duration the horse has been thirsty for in ms.
     *
     * @return the duration a long.
     */
    public long getThristDuration() {
        return getCurrentTime() - this.lastDrink - DRINK_LIMIT;
    }

    /**
     * Sets the hunger state of the horse.
     */
    public void eat() {
        this.lastEat = getCurrentTime();
        this.defecateSinceEat = false;
    }

    /**
     * Check if the horse has defecated since eating.
     *
     * @return if the horse has defecated since eating.
     */
    public boolean hasDefecate() {
        return this.defecateSinceEat;
    }

    public void setDefecated() {
        this.defecateSinceEat = true;
    }

    /**
     * Get the hunger state of the horse.
     *
     * @return the state as a boolean.
     */
    public boolean isHungry() {
        return this.getDurationSinceLastEat() > EAT_LIMIT; //Check if the horse is thirsty.
    }
    
    public boolean isTooHungry(){
        return getHungerDuration() - EAT_LIMIT > 0;
    }
    
    public boolean isTooThirsty(){
        return getThristDuration() - DRINK_LIMIT > 0;
    }

    /**
     * Get the time the horse has been hungry for in ms.
     *
     * @return the time as a long.
     */
    public long getHungerDuration() {
        return getCurrentTime() - this.lastEat - EAT_LIMIT;
    }

    /**
     * Get the duration since the horse last drank.
     *
     * @return the time since the horse last drank.
     */
    public long getDurationSinceLastDrink() {
        return getCurrentTime() - this.lastDrink;
    }

    /**
     * Get the duration since the horse last ate in ms.
     *
     * @return the time since the horse last ate in ms.
     */
    public long getDurationSinceLastEat() {
        return getCurrentTime() - this.lastEat;
    }

    /**
     * Get the ill state of the horse.
     *
     * @return ill state as a boolean.
     */
    public boolean isSick() {
        return this.illSince > this.wellSince;
    }

    /**
     * Set the sickness of the horse.
     *
     * @param sick the sickness as a boolean.
     */
    public void setSick(boolean sick) {
        if (sick) {
            while (true) {
                this.illness = Illness.randomIllness();
                if (gender == MyHorse.MARE) {
                    if (illness != Illness.MareReproductiveLoss) {
                        this.illSince = getCurrentTime();
                        break;
                    }
                } else {
                    this.illSince = getCurrentTime();
                    break;
                }
            }
        } else {
            this.wellSince = getCurrentTime();
        }
    }

    /**
     * Get the duration the horse has been ill for is ms.
     *
     * @return the duration as a long.
     */
    public long getIllDuration() {
        return getCurrentTime() - this.illSince;
    }

    /**
     * Get the time the horse has been well for in ms.
     *
     * @return the time as a long.
     */
    public long getWellDuration() {
        return getCurrentTime() - this.wellSince;
    }

    /**
     * Get the time since the horse last bred in ms.
     *
     * @return the time as a long.
     */
    public long getDurationSinceLastBreed() {
        return getCurrentTime() - this.lastBreed;
    }

    /**
     * Set the gender of the horse.
     *
     * @param gender the gender.
     */
    public void setGender(int gender) {
        this.gender = gender;
    }

    /**
     * Get the HorseBreed.
     *
     * @return the HorseBreed.
     */
    public HorseBreed[] getBreed() {
        if (breedArr == null || breedArr.length == 0) {
            throw new NullPointerException("This horse has no breed");
        }
        if (breedArr[0] == null) {
            breedArr[0] = HorseBreed.randomType();
        }
        if (breedArr.length == 1) {
            breedArr = new HorseBreed[]{breedArr[0], breedArr[0]};
        }
        if (breedArr[1] == null) {
            breedArr[1] = breedArr[0];
        }
        return breedArr;
    }

    /**
     * Set the breed of the horse.
     *
     * @param br the HorseBreed.
     */
    public void setBreed(HorseBreed[] br) {
        this.breedArr = br;
    }

    /**
     * Get the time the horse was born in ms.
     *
     * @return the time as a long.
     */
    public long getBirthTime() {
        return birthTime;
    }

    /**
     * Get the duration since the horse was born in ms.
     *
     * @return the duration as a long.
     */
    public long getAge() {
        return getCurrentTime() - birthTime;
    }

    /**
     * Set the age of the horse in ms. This method simply edits the birth time
     * of the horse.
     *
     * @param age the age in ms as a long.
     */
    public void setAge(long age) {
        birthTime = getCurrentTime() - age;
    }

    /**
     * Get the horses age in months.
     *
     * @return the age of the horse in months.
     */
    public double getAgeInMonths() {
        final double days = ((double) getAge() / 1000L / 60L / 60L / 24L);
        final double y = days / 2.5;
        return y;
    }

    /**
     * Set the age of the horse in months, this method changes the birth time.
     *
     * @param months the age in months as an int.
     */
    public void setAgeInMonths(int months) {
        double m = (double) months;
        long age = (long) (m * 2.5) * 24L * 60L * 60L * 1000L;
        setAge(age);
    }

    /**
     * Get the age the horse will die at.
     *
     * @return horse death age as an int.
     */
    public int getDieAt() {
        return dieat;
    }

    /**
     * Set the horses death age.
     *
     * @param dieat the age in months the horse will die at.
     */
    public void setDieat(int dieat) {
        this.dieat = dieat;
    }

    /**
     * Get the horses personality.
     *
     * @return the horses personality.
     */
    public Personality[] getPersonalities() {
        return personality;
    }

    /**
     * Set the horses personality.
     *
     * @param p1 the first personality.
     * @param p2 the second personality.
     */
    public void setPersonalities(Personality p1, Personality p2) {
        personality[0] = p1;
        personality[1] = p2;
    }

    /**
     * Get the horses illness.
     *
     * @return the horses illness.
     */
    public Illness getIllness() {
        return illness;
    }

    public String getIllnessString() {
        if (illness != null) {
            return illness.name();
        } else {
            return null;
        }
    }

    /**
     * Set the horses illness.
     *
     * @param illness the illness.
     */
    public void setIllness(Illness illness) {
        this.illness = illness;
    }

    public boolean isShoed() {
        return shoed;
    }

    public void setShoed(boolean shoed) {
        this.shoed = shoed;
    }

    public int getTrainingLevel() {
        return trainingLevel;
    }

    public void setTrainingLevel(int trainingLevel) {
        this.trainingLevel = trainingLevel;
    }

    public boolean isIgnored() {
        return ignored;
    }

    /**
     * Get a horses rank.
     *
     * @param h the horse to check.
     * @return 1 = Novice, 2 = Intermediate, 3 = Advanced, 4 = Master, 5 =
     * Instructor, 6 = Champion, 7 = Elite, 8 = Olympian.
     */
    public static int getRank(Horse h) {
        final double jump = h.getJumpStrength();
        final double speed = HorseNMS.getSpeed(h) * 10;

        if (jump >= 1.2 || speed >= 8) {
            return 8;
        } else if (jump >= 1.1 || speed >= 7.9) {
            return 7;
        } else if (jump >= 1.09 || speed >= 7) {
            return 6;
        } else if (jump >= 1.05 || speed >= 6.5) {
            return 5;
        } else if (jump >= 0.91 || speed >= 5.9) {
            return 4;
        } else if (jump >= 0.81 || speed >= 5) {
            return 3;
        } else if (jump >= 0.69 || speed >= 4.5) {
            return 2;
        } else if (jump >= 0.68 || speed >= 4) {
            return 1;
        } else {
            return 0;
        }
    }

    public String getInsertValues() {
        return "('"
                + getUuid() + "',"
                + getGender() + ","
                + getVaccinationTime() + ","
                + getLastEat() + ","
                + getLastDrink() + ","
                + getIllSince() + ","
                + getWellSince() + ","
                + getLastBreed() + ","
                + (hasDefecate() ? "1" : "0") + ",'"
                + getBreed()[0].toString() + "','"
                + getBreed()[1].toString() + "',"
                + getBirthTime() + ",'"
                + getPersonalities()[0].toString() + "','"
                + getPersonalities()[1].toString() + "',"
                + getDieAt() + ",'"
                + getIllnessString() + "',"
                + (isShoed() ? "1" : "0") + ","
                + getTrainingLevel() + ")";
    }

    @Override
    public String toString() {
        return uuid.toString();
    }
}
