package itera.simulation;

import itera.model.*;
import itera.model.Character;
import itera.model.human.*;
import itera.model.zombie.*;
import java.util.*;
import java.util.stream.Collectors;

public class World {
    private itera.model.human.treatment.TreatmentStrategy treatmentStrategy =
        new itera.model.human.treatment.SoldiersFirstStrategy();

    /** Apply policy to current medics and retain it for future reinforcements. */
    public void setTreatmentStrategy(itera.model.human.treatment.TreatmentStrategy strategy) {
        treatmentStrategy = Objects.requireNonNull(strategy, "Treatment strategy is required");
        getHumans().stream().filter(Medic.class::isInstance).map(Medic.class::cast)
            .forEach(medic -> medic.setTreatmentStrategy(strategy));
    }


    // All in-world timers advance with movement, independent of wall-clock time.
    private long simulationTime = System.currentTimeMillis();

    public long getTime() { return simulationTime; }

    public void advanceTime(long milliseconds) {
        if (milliseconds < 0) throw new IllegalArgumentException("Time cannot go backwards");
        simulationTime += milliseconds;
    }


    private ArrayList<Character> characters = new ArrayList<>();

    private ArrayList<Resource> resources = new ArrayList<>();

    private ArrayList<Building> buildings = new ArrayList<>();

    private ArrayList<SafePoint> safePoints = new ArrayList<>();

    private ArrayList<ZombieWave> waves = new ArrayList<>();

    public void addCharacter(Character character) {

        character.setTimeSource(this::getTime);
        if (character instanceof Medic medic) medic.setTreatmentStrategy(treatmentStrategy);
        characters.add(character);
    }

    public void addResource(Resource resource) {

        resource.setTimeSource(this::getTime);
        resources.add(resource);
    }

    public void addBuilding(Building building) {

        buildings.add(building);
    }

    public void addSafePoint(SafePoint safePoint) {

        safePoints.add(safePoint);
    }

    public void addWave(ZombieWave wave) {

        waves.add(wave);
    }

    public void update() {

        characters.removeIf(character -> !character.isAlive());
    }

    public void spawnWave(ZombieWave wave) {

        wave.trigger(this);
    }

    public boolean isCleared() {

        return characters.stream()
            .noneMatch(character -> character instanceof Zombie && character.isAlive());
    }

    public ArrayList<Character> getCharacters() {

        return characters;
    }

    public ArrayList<Human> getHumans() {

        return characters.stream()
            .filter(Human.class::isInstance)
            .map(Human.class::cast)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public ArrayList<Zombie> getZombies() {

        return characters.stream()
            .filter(Zombie.class::isInstance)
            .map(Zombie.class::cast)
            .collect(Collectors.toCollection(ArrayList::new));
    }
}
