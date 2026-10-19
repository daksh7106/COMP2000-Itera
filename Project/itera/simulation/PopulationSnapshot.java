package itera.simulation;

import itera.model.human.Human;
import java.util.*;
import java.util.stream.Collectors;

/** Immutable values collected on the simulation thread, with no live entity references. */
public final class PopulationSnapshot {
    public record RoleSummary(long count, long wounded, double averageHealth) { }

    private final Map<String, RoleSummary> roles;

    public PopulationSnapshot(List<Human> humans) {
        Map<String, RoleSummary> collected = humans.stream()
            .filter(Human::isAlive)
            .collect(Collectors.groupingBy(human -> human.getClass().getSimpleName(),
                TreeMap::new,
                Collectors.collectingAndThen(Collectors.toList(), group -> {
                    IntSummaryStatistics health = group.stream()
                        .collect(Collectors.summarizingInt(Human::getHealth));
                    long wounded = group.stream().filter(human -> human.getHealth() < 100).count();
                    return new RoleSummary(health.getCount(), wounded, health.getAverage());
                })));
        roles = Collections.unmodifiableMap(collected);
    }

    public Map<String, RoleSummary> getRoles() { return roles; }
}
