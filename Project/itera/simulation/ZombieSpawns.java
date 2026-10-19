package itera.simulation;

import itera.model.zombie.*;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

/** Constructor references keep ordinary zombie creation in one place. */
public final class ZombieSpawns {
    private static final List<BiFunction<Integer, Integer, Zombie>> TYPES =
        List.of(Zombie::new, Runner::new, Stalker::new, Bloater::new);

    private ZombieSpawns() { }

    public static Zombie atType(int type, int x, int y) {
        return TYPES.get(Math.floorMod(type, TYPES.size())).apply(x, y);
    }

    public static Zombie random(Random random, int x, int y) {
        return atType(random.nextInt(TYPES.size()), x, y);
    }
}
