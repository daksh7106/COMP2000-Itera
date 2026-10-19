package itera.model.human.treatment;

import itera.model.human.*;
import java.util.Comparator;

/** Prioritize lowest health regardless of role; use distance for equal health. */
public final class MostInjuredStrategy implements TreatmentStrategy {
    @Override
    public Comparator<Human> priorityFor(Medic medic) {
        return Comparator.comparingInt(Human::getHealth)
            .thenComparingDouble(human -> medic.getPosition().distanceTo(human.getPosition()));
    }
}
