package itera.model.human.treatment;

import itera.model.human.*;
import java.util.Comparator;

/** Preserve the original preference for soldiers, then the nearest patient. */
public final class SoldiersFirstStrategy implements TreatmentStrategy {
    @Override
    public Comparator<Human> priorityFor(Medic medic) {
        return Comparator.comparingInt((Human human) -> human instanceof Soldier ? 0 : 1)
            .thenComparingDouble(human -> medic.getPosition().distanceTo(human.getPosition()));
    }
}
