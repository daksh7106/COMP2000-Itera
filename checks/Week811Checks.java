package itera.checks;

import itera.model.*;
import itera.model.human.*;
import itera.model.human.treatment.*;
import itera.model.zombie.*;
import itera.simulation.*;
import itera.ui.Main;
import java.util.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** Standalone deterministic checks; no external test library or display needed. */
public final class Week811Checks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void rejects(Class<? extends Throwable> expected, Runnable action) {
        try { action.run(); }
        catch (Throwable error) {
            check(expected.isInstance(error), "Expected " + expected + ", got " + error);
            return;
        }
        throw new AssertionError("Expected " + expected);
    }
    private static <T extends Human> T injured(T human, int damage) {
        human.takeDamage(damage);
        return human;
    }
    private static <T extends Component> T component(Container parent, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = component(container, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Medic medic = new Medic(0, 0);
        Soldier far = injured(new Soldier(100, 0), 10);
        Soldier near = injured(new Soldier(20, 0), 10);
        Civilian critical = injured(new Civilian(10, 0), 90);
        List<Human> patients = List.of(far, critical, near);
        check(medic.selectPatient(patients).orElseThrow() == near, "Default soldiers then distance");
        medic.setTreatmentStrategy(new MostInjuredStrategy());
        check(medic.selectPatient(patients).orElseThrow() == critical, "Lowest health first");
        Civilian tied = injured(new Civilian(5, 0), 90);
        check(medic.selectPatient(List.of(critical, tied)).orElseThrow() == tied, "Health tie uses distance");
        Soldier dead = injured(new Soldier(1, 0), 100);
        Civilian full = new Civilian(1, 0);
        Civilian sheltered = injured(new Civilian(50, 50), 95);
        sheltered.setEnvironment(List.of(new ConvenienceStore(0, 0)), List.of());
        check(sheltered.isSheltered(), "Fixture is inside a building");
        medic.takeDamage(10);
        check(medic.selectPatient(List.of(medic, dead, full, sheltered)).isEmpty(), "Exclude self, dead, healthy and sheltered");
        check(medic.selectPatient(List.of()).isEmpty(), "Empty candidate list");
        medic.setTreatmentStrategy(m -> Comparator.comparingInt(Human::getHealth).reversed());
        check(medic.selectPatient(List.of(near, critical)).orElseThrow() == near, "Lambda can supply ranking behaviour");
        rejects(NullPointerException.class, () -> medic.setTreatmentStrategy(null));

        World world = new World();
        world.addCharacter(medic);
        TreatmentStrategy policy = new MostInjuredStrategy();
        world.setTreatmentStrategy(policy);
        check(medic.getTreatmentStrategy() == policy, "Existing medic gets policy");
        Medic reinforcement = new Medic(0, 0);
        world.addCharacter(reinforcement);
        check(reinforcement.getTreatmentStrategy() == policy, "New medic gets policy");
        rejects(NullPointerException.class, () -> world.setTreatmentStrategy(null));
        world.addCharacter(near);
        world.addCharacter(dead);
        world.addCharacter(critical);
        world.addCharacter(new Zombie(0, 0));
        check(world.findCharacters(Human.class, Human::isAlive).size() == 4, "Typed living filter");
        check(world.findCharacters(Soldier.class, Human::isAlive).size() == 1, "Supertype method reference predicate");
        check(world.findCharacters(Human.class, h -> h.getHealth() < 20 && h.isAlive()).size() == 1, "Composed lambda filter");
        world.getHumans().clear();
        check(world.getHumans().size() == 5, "Result list mutations cannot clear world");
        rejects(NullPointerException.class, () -> world.findCharacters(Human.class, null));
        rejects(NullPointerException.class, () -> world.findCharacters(null, h -> true));

        PopulationSnapshot snapshot = world.populationSnapshot();
        check(snapshot.getRoles().get("Soldier").count() == 1, "Dead soldier excluded");
        check(snapshot.getRoles().get("Medic").wounded() == 1, "Wounded count");
        check(snapshot.getRoles().get("Medic").averageHealth() == 95, "Mean health");
        check(new ArrayList<>(snapshot.getRoles().keySet()).equals(List.of("Civilian", "Medic", "Soldier")), "Stable alphabetical display");
        medic.takeDamage(20);
        check(snapshot.getRoles().get("Medic").averageHealth() == 95, "Snapshot detached from live health");
        rejects(UnsupportedOperationException.class, () -> snapshot.getRoles().clear());
        check(new World().populationSnapshot().getRoles().isEmpty(), "Empty snapshot");

        World waves = new World();
        Zombie survivor = new Zombie(2, 2);
        waves.addCharacter(survivor);
        ZombieWave runners = new ZombieWave(3, false, Runner::new);
        runners.addSpawnPoint(new Vector2D(40, 60));
        waves.spawnWave(runners);
        check(waves.getZombies().size() == 4 && waves.getZombies().contains(survivor), "Wave preserves survivors");
        check(waves.findCharacters(Runner.class, z -> z.getX() == 40 && z.getY() == 60).size() == 3, "Injected constructor gets coordinates");
        ZombieWave boss = new ZombieWave(10, true, Runner::new);
        boss.addSpawnPoint(new Vector2D(20, 30));
        waves.spawnWave(boss);
        check(waves.findCharacters(MutantBoss.class, z -> true).size() == 1 && waves.getZombies().size() == 5, "Boss remains a single boss");
        waves.spawnWave(new ZombieWave(3));
        check(waves.getZombies().size() == 5, "No spawn points means no additions");
        rejects(IllegalArgumentException.class, () -> new ZombieWave(-1));
        rejects(NullPointerException.class, () -> new ZombieWave(1, false, null));
        ZombieWave missing = new ZombieWave(1, false, (x, y) -> null);
        missing.addSpawnPoint(new Vector2D(1, 1));
        rejects(NullPointerException.class, () -> missing.trigger(waves));
        Class<?>[] types = {Zombie.class, Runner.class, Stalker.class, Bloater.class};
        for (int i = 0; i < types.length; i++)
            check(ZombieSpawns.atType(i, 9, 8).getClass() == types[i], "Initial type rotation " + i);
        Random random = new Random(42);
        for (int i = 0; i < 30; i++) {
            Zombie zombie = ZombieSpawns.random(random, 9, 8);
            check(Arrays.asList(types).contains(zombie.getClass()) && zombie.getX() == 9 && zombie.getY() == 8, "Random spawn family and coordinates");
        }

        Medic healer = injured(new Medic(0, 0), 80);
        World healingWorld = new World();
        healingWorld.addCharacter(healer);
        healer.interact(new Medicine(3, 20, 0, 0));
        Soldier patient = injured(new Soldier(10, 0), 50);
        healer.heal(patient);
        check(patient.getHealth() == 50, "Critical medic must self-heal first");
        healer.heal(healer);
        check(healer.getHealth() == 40 && healer.getMedKits() == 2, "Critical self-healing consumes one use");
        healer.heal(patient);
        check(patient.getHealth() == 50, "Healing cooldown preserved");
        healingWorld.advanceTime(1000);
        healer.heal(patient);
        check(patient.getHealth() == 70 && healer.getMedKits() == 1, "Heal after simulation second");

        SwingUtilities.invokeAndWait(() -> {
            try {
                Main simulation = new Main(new SimulationSettings(20, 10));
                JPanel controls = simulation.getSimulationControls();
                JComboBox<?> choice = component(controls, JComboBox.class);
                check(choice != null && choice.getItemCount() == 2, "Both choices visible");
                var field = Main.class.getDeclaredField("world");
                field.setAccessible(true);
                World uiWorld = (World) field.get(simulation);
                check(uiWorld.getHumans().size() == 20 && uiWorld.getZombies().size() == 10, "Default population unchanged");
                choice.setSelectedIndex(1);
                check(uiWorld.findCharacters(Medic.class, m -> m.getTreatmentStrategy() instanceof MostInjuredStrategy).size() == 6, "UI applies selected strategy");
                var spawn = Main.class.getDeclaredMethod("addHumanReinforcements");
                spawn.setAccessible(true);
                spawn.invoke(simulation);
                check(uiWorld.findCharacters(Medic.class, m -> m.getTreatmentStrategy() instanceof MostInjuredStrategy).size() == 8, "Actual reinforcement path inherits selection");
                check(uiWorld.getHumans().size() == 26, "Six human reinforcements retained");
                choice.setSelectedIndex(0);
                check(uiWorld.findCharacters(Medic.class, m -> m.getTreatmentStrategy() instanceof SoldiersFirstStrategy).size() == 8, "Switch back applies to all medics");
                simulation.setSize(1200, 800);
                var update = Main.class.getDeclaredMethod("updateSimulation");
                update.setAccessible(true);
                for (int i = 0; i < 20; i++) update.invoke(simulation);
                var label = Main.class.getDeclaredField("populationSummary");
                label.setAccessible(true);
                check(((JLabel) label.get(simulation)).getText().contains("wounded"), "Live summary refreshed");
                var graphics = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_RGB).createGraphics();
                simulation.paint(graphics);
                graphics.dispose();
                check(true, "Simulation renders headlessly after updates");
            } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
        });
        System.out.println("Passed " + checks + " checks.");
    }
}
