package itera.simulation;

/** Immutable, validated settings chosen before a simulation begins. */
public final class SimulationSettings {

    public static final int DEFAULT_HUMANS = 20;
    public static final int DEFAULT_ZOMBIES = 10;
    public static final int MAX_STARTING_POPULATION = 100;

    private final int startingHumans;
    private final int startingZombies;

    public SimulationSettings(int startingHumans, int startingZombies) {
        if (startingHumans < 1) {
            throw new IllegalArgumentException(
                "At least one human is required to run a simulation.");
        }
        if (startingZombies < 0) {
            throw new IllegalArgumentException(
                "The zombie population cannot be negative.");
        }
        if (startingHumans > MAX_STARTING_POPULATION
                || startingZombies > MAX_STARTING_POPULATION) {
            throw new IllegalArgumentException(
                "Choose no more than " + MAX_STARTING_POPULATION
                    + " humans or zombies so the simulation remains responsive.");
        }

        this.startingHumans = startingHumans;
        this.startingZombies = startingZombies;
    }

    public int getStartingHumans() {
        return startingHumans;
    }

    public int getStartingZombies() {
        return startingZombies;
    }
}
