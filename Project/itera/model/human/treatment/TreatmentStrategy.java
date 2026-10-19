package itera.model.human.treatment;

import itera.model.human.Human;
import itera.model.human.Medic;
import java.util.Comparator;

/** Strategy for ranking eligible patients; the medic retains safety checks. */
@FunctionalInterface
public interface TreatmentStrategy {
    Comparator<Human> priorityFor(Medic medic);
}
