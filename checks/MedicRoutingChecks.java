package itera.checks;

import itera.model.Building;
import itera.model.Hospital;
import itera.model.Medicine;
import itera.model.PoliceStation;
import itera.model.SafePoint;
import itera.model.human.Medic;
import itera.model.human.Soldier;
import itera.simulation.World;
import java.util.ArrayList;
import java.util.List;

/** Checks that medics reach outdoor targets without crossing building walls. */
public final class MedicRoutingChecks {
    private static final int WIDTH = 700;
    private static final int HEIGHT = 600;
    private static final SafePoint SAFE_POINT = new SafePoint(0, 0, 180, 120, 10);

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void tick(World world, Medic medic, Building building) {
        world.advanceTime(30);
        medic.setEmergencySupplyDrops(world.getActiveEmergencySupplyDrops());
        medic.update(WIDTH, HEIGHT, new ArrayList<>(), SAFE_POINT);
        world.update();

        double x = medic.getPosition().getX();
        double y = medic.getPosition().getY();
        check(x >= 0 && x <= WIDTH - medic.getSize()
            && y >= 0 && y <= HEIGHT - medic.getSize(), "Medic remains on the map");
        check(!building.blocksMovement(x, y, medic.getSize(), building.canEnter(medic)),
            "Medic never moves through a building wall");
    }

    private static void reachesPatientAcrossStation() {
        World world = new World();
        Building station = new PoliceStation(0, 200);
        Medic medic = new Medic(80, 150);
        Soldier patient = new Soldier(80, 410);
        patient.takeDamage(40);
        world.addCharacter(medic);
        world.addCharacter(patient);
        medic.addResource(new Medicine(3, 20));
        medic.setEnvironment(List.of(station), List.of(medic, patient));

        for (int i = 0; i < 180 && patient.getHealth() == 60; i++) {
            tick(world, medic, station);
        }
        check(patient.getHealth() == 80, "Medic detours around station and heals patient");
    }

    private static void reachesDropAcrossStation() {
        World world = new World();
        Building station = new PoliceStation(0, 200);
        Medic medic = new Medic(80, 150);
        world.addCharacter(medic);
        medic.setEnvironment(List.of(station), List.of(medic));
        world.spawnEmergencySupplyDrop(80, 410, 10_000);

        for (int i = 0; i < 180 && medic.getMedKits() == 0; i++) {
            tick(world, medic, station);
        }
        check(medic.getMedKits() == 3, "Medic detours around station and collects the drop");
    }

    private static void leavesHospitalThroughDoorForDrop() {
        World world = new World();
        Building hospital = new Hospital(WIDTH - 180, 0);
        Medic medic = new Medic(600, 25);
        world.addCharacter(medic);
        medic.setEnvironment(List.of(hospital), List.of(medic));
        world.spawnEmergencySupplyDrop(350, 25, 10_000);

        for (int i = 0; i < 180 && medic.getMedKits() == 0; i++) {
            tick(world, medic, hospital);
        }
        check(medic.getMedKits() == 3, "Indoor medic exits through door to collect the drop");
    }

    public static void main(String[] args) {
        reachesPatientAcrossStation();
        reachesDropAcrossStation();
        leavesHospitalThroughDoorForDrop();
        System.out.println("Passed 3 medic routing scenarios.");
    }
}
