import itera.model.*;
import itera.simulation.World;
import itera.ui.FastForward;
import itera.ui.Main;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;

public class SimulationTimingChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void speed(FastForward controls, int speed) {
        for (java.awt.Component c : controls.getComponents())
            if (c instanceof JButton b && b.getText().equals(speed + "x")) b.doClick();
    }
    public static void main(String[] args) throws Exception {
        for (int multiplier : new int[]{1,2,5}) {
            World world = new World();
            FastForward controls = new FastForward(); speed(controls,multiplier);
            Weapon gun = new Weapon(5,50,5,40,80);
            Medicine kit = new Medicine(3,20,40,80);
            Food food = new Food(1,20,40,80);
            for (Resource r : List.of(gun,kit,food)) { world.addResource(r); r.collect(); }
            long start = world.getTime();
            for (int step=1; step<=1000; step++) {
                world.advanceTime(30);
                for (Resource r : List.of(gun,kit,food)) r.updateRespawn(world.getTime());
                if (step==333) check(gun.isCollected(),"Gun stays hidden before ten simulation seconds");
                if (step==334) check(!gun.isCollected(),"Gun respawns on first step after ten simulation seconds");
                if (step==999) check(kit.isCollected() && food.isCollected(),"Supplies stay hidden before thirty simulation seconds");
            }
            check(world.getTime()-start == 30000 && !kit.isCollected() && !food.isCollected(),
                "Supply respawns after thirty simulation seconds");
            check(1000*controls.getDelay() == 30000/multiplier,
                "Movement and timers advance together at selected speed");
        }
        World world = new World();
        Soldier soldier = new Soldier(500,400); world.addCharacter(soldier);
        soldier.interact(new Weapon(5,50,5));
        Zombie victim = new Zombie(550,400); world.addCharacter(victim);
        check(soldier.shoot(victim),"First shot uses world clock");
        world.advanceTime(699); check(!soldier.shoot(victim),"Shot cooldown before seven hundred ms");
        world.advanceTime(1); check(soldier.shoot(victim),"Shot cooldown expires in simulation time");
        Medic medic = new Medic(500,400); world.addCharacter(medic); medic.interact(new Medicine(3,20));
        Human patient = new Human(520,400); world.addCharacter(patient); patient.takeDamage(60);
        medic.heal(patient);
        world.advanceTime(999); medic.heal(patient); check(patient.getHealth()==60,"Medic cooldown respected");
        world.advanceTime(1); medic.heal(patient); check(patient.getHealth()==80,"Medic cooldown expires in simulation time");
        Human attacked = new Human(500,400); world.addCharacter(attacked);
        attacked.receiveZombieHit(25); world.advanceTime(599);
        check(!attacked.receiveZombieHit(25),"Zombie damage cooldown respected");
        world.advanceTime(1); check(attacked.receiveZombieHit(25),"Zombie damage cooldown uses simulation time");
        Runner runner = new Runner(500,400); world.addCharacter(runner);
        SafePoint safe = new SafePoint(0,0,180,180,10);
        runner.update(1200,800,new ArrayList<>(List.of(new Human(600,400))),safe);
        check(runner.getSpeed()==4.0,"Runner burst begins");
        world.advanceTime(1200); runner.update(1200,800,new ArrayList<>(),safe);
        check(runner.getSpeed()==2.5,"Runner burst expires in simulation time");
        Main panel = new Main(); panel.setSize(1200,800);
        World panelWorld = (World) GameplayChecks.field(panel,"world");
        GameplayChecks.set(panel,"lastWaveTime",panelWorld.getTime());
        int initialZombies = panelWorld.getZombies().size();
        panelWorld.advanceTime(14999); GameplayChecks.call(panel,"updateTimedWaves");
        check(panelWorld.getZombies().size()==initialZombies,"Wave does not arrive early");
        panelWorld.advanceTime(1); GameplayChecks.call(panel,"updateTimedWaves");
        check(panelWorld.getZombies().size()==initialZombies+12 && panelWorld.getHumans().size()==26,
            "Wave and reinforcements arrive at fifteen simulation seconds");
        Weapon pickup = new Weapon(5,50,5,40,80); panelWorld.addResource(pickup); pickup.collect();
        FastForward controls = panel.getFastForward();
        speed(controls,1);
        // 150 frames = 4.5 real seconds at 1x; then 184 frames = 1.104 real seconds at 5x.
        for(int i=0;i<150;i++) panelWorld.advanceTime(30);
        speed(controls,5);
        for(int i=0;i<183;i++) panelWorld.advanceTime(30);
        pickup.updateRespawn(panelWorld.getTime()); check(pickup.isCollected(),"Speed switch preserves elapsed cooldown");
        panelWorld.advanceTime(30); pickup.updateRespawn(panelWorld.getTime());
        check(!pickup.isCollected(),"Existing respawn completes faster after switching to five times speed");
        long beforeStep = panelWorld.getTime(); GameplayChecks.call(panel,"updateSimulation");
        check(panelWorld.getTime()==beforeStep+30,"Simulation update advances shared clock once per movement step");
        System.out.println("Passed " + checks + " simulation timing checks.");
    }
}
