package itera.simulation;

import itera.model.*;
import itera.model.zombie.*;
import java.util.*;

public class ZombieWave {

    private int count;

    private boolean bossWave;

    private ArrayList<Vector2D> spawnPoints = new ArrayList<>();

    private final java.util.function.BiFunction<Integer, Integer, Zombie> spawnZombie;

    public ZombieWave(int count) {

        this(count, false);
    }

    public ZombieWave(int count, boolean bossWave) {

        this(count, bossWave, defaultSpawner());
    }

    /** Supply creation behaviour for experiments without rewriting wave timing. */
    public ZombieWave(int count, boolean bossWave,
            java.util.function.BiFunction<Integer, Integer, Zombie> spawnZombie) {
        if (count < 0) throw new IllegalArgumentException("Wave count cannot be negative");
        this.count = count;
        this.bossWave = bossWave;
        this.spawnZombie = Objects.requireNonNull(spawnZombie, "Zombie spawner is required");
    }

    private static java.util.function.BiFunction<Integer, Integer, Zombie> defaultSpawner() {
        Random random = new Random();
        return (x, y) -> ZombieSpawns.random(random, x, y);
    }

    public void addSpawnPoint(Vector2D point) {

        spawnPoints.add(point);
    }

    public void trigger(World world) {

        if (spawnPoints.isEmpty()) {
            return;
        }

        /*
         * Boss waves spawn one MutantBoss.
         */
        if (bossWave) {

            Vector2D point = spawnPoints.get(0);

            MutantBoss boss = new MutantBoss(
                (int) point.getX(),
                (int) point.getY()
            );

            world.addCharacter(boss);

            return;
        }

        /*
         * Normal waves spawn a random mixture
         * of the existing zombie types.
         */
        for (int i = 0; i < count; i++) {

            Vector2D point = spawnPoints.get(i % spawnPoints.size());

            Zombie zombie = Objects.requireNonNull(spawnZombie.apply(
                (int) point.getX(), (int) point.getY()), "Spawner returned no zombie");

            world.addCharacter(zombie);
        }
    }

    public boolean isBossWave() {

        return bossWave;
    }
}