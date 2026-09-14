package itera.simulation;

import itera.model.Building;
import itera.model.Character;
import itera.model.Human;
import itera.model.Resource;
import itera.model.SafePoint;
import itera.model.Zombie;
import java.util.ArrayList;

public class World {

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

        for (Character character : characters) {

            if (character instanceof Zombie && character.isAlive()) {

                return false;
            }
        }

        return true;
    }

    public ArrayList<Character> getCharacters() {

        return characters;
    }

    public ArrayList<Human> getHumans() {

        ArrayList<Human> humans = new ArrayList<>();

        for (Character character : characters) {

            if (character instanceof Human human) {

                humans.add(human);
            }
        }

        return humans;
    }

    public ArrayList<Zombie> getZombies() {

        ArrayList<Zombie> zombies = new ArrayList<>();

        for (Character character : characters) {

            if (character instanceof Zombie zombie) {

                zombies.add(zombie);
            }
        }

        return zombies;
    }
}
