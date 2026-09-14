package itera.simulation;

import itera.model.Bloater;
import itera.model.MutantBoss;
import itera.model.Runner;
import itera.model.Stalker;
import itera.model.Vector2D;
import itera.model.Zombie;
import java.util.ArrayList;
import java.util.Random;

public class ZombieWave {

    private int count;

    private boolean bossWave;

    private ArrayList<Vector2D> spawnPoints = new ArrayList<>();

    private Random random = new Random();

    public ZombieWave(int count) {

        this(count, false);
    }

    public ZombieWave(int count, boolean bossWave) {

        this.count = count;

        this.bossWave = bossWave;
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

            int zombieType = random.nextInt(4);

            Zombie zombie;

            if (zombieType == 0) {

                zombie = new Zombie(
                    (int) point.getX(),
                    (int) point.getY()
                );

            } else if (zombieType == 1) {

                zombie = new Runner(
                    (int) point.getX(),
                    (int) point.getY()
                );

            } else if (zombieType == 2) {

                zombie = new Stalker(
                    (int) point.getX(),
                    (int) point.getY()
                );

            } else {

                zombie = new Bloater(
                    (int) point.getX(),
                    (int) point.getY()
                );
            }

            world.addCharacter(zombie);
        }
    }

    public boolean isBossWave() {

        return bossWave;
    }
}