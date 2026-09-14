import itera.model.*;
import itera.simulation.*;
import itera.ui.Main;
import java.util.*;
import java.lang.reflect.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Run with assertions enabled: java -ea -Djava.awt.headless=true ... GameplayChecks. */
public class GameplayChecks {
    static int checks;
    static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    static void at(itera.model.Character c, int x, int y) {
        c.getPosition().setX(x); c.getPosition().setY(y);
    }
    static Object field(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(object);
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
    static void set(Object object, String name, Object value) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); f.set(object, value);
    }
    static void call(Object object, String name) throws Exception {
        Method m = object.getClass().getDeclaredMethod(name); m.setAccessible(true); m.invoke(object);
    }
    public static void main(String[] args) throws Exception {
        PoliceStation police = new PoliceStation(0, 550);
        Hospital hospital = new Hospital(1020, 0);
        ConvenienceStore store = new ConvenienceStore(1020, 550);
        List<Building> buildings = List.of(police, hospital, store);
        SafePoint safe = new SafePoint(0, 0, 180, 180, 10);
        Soldier soldier = new Soldier(500, 600);
        Medic medic = new Medic(600, 200);
        Civilian civilian = new Civilian(750, 650);
        ArrayList<Human> humans = new ArrayList<>(List.of(soldier, medic, civilian));
        for (Human h : humans) h.setEnvironment(buildings, humans);
        check(!soldier.isArmed() && medic.getMedKits() == 0, "Equipment must be collected first");
        check(!police.canEnter(civilian) && !police.canEnter(medic) && police.canEnter(soldier), "Police role access");
        check(!hospital.canEnter(civilian) && !hospital.canEnter(soldier) && hospital.canEnter(medic), "Hospital role access");
        check(store.canEnter(civilian), "Everyone can enter store");
        for (Building b : buildings) {
            for (Resource r : b.getResources()) {
                check(b.contains(r.getX(), r.getY()) && b.contains(r.getX() + 18, r.getY() + 14), "Stock inside building");
            }
            check(b.blocksMovement(b.getX() + 30, b.getY() - 2, 15, true), "Top wall blocks human");
            check(b.blocksMovement(b.getDoorX() - 7, b.getDoorY() - 7, 15, false), "Door blocks unauthorized actor");
            check(!b.blocksMovement(b.getDoorX() - 7, b.getDoorY() - 7, 15, true), "Door permits authorized actor");
        }
        check(safe.blocksHumanMovement(200, 30, 175, 30, 15, true), "SafePoint walls preserved");
        check(!safe.blocksHumanMovement(185, 80, 179, 80, 15, true), "SafePoint emergency entrance preserved");
        check(safe.wouldZombieEnter(179,80,18), "SafePoint excludes zombies");
        Civilian shopper = new Civilian(1060,615);
        Resource supply = store.getResources().get(0);
        store.interact(shopper);
        check(!supply.isCollected(), "Full health customer cannot consume store stock");
        shopper.takeDamage(20); store.interact(shopper);
        check(supply.isCollected() && shopper.getHealth() == 100, "Store supply restores health");
        store.interact(shopper);
        check(store.getResources().stream().filter(Resource::isCollected).count() == 1, "Restored customer leaves remaining stock");
        Resource gun = police.getResources().get(0), kit = hospital.getResources().get(0), food = store.getResources().get(1);
        civilian.interact(gun); civilian.interact(kit); civilian.interact(food);
        check(!gun.isCollected() && !kit.isCollected() && !food.isCollected(), "Wrong role/full health cannot collect");
        civilian.takeDamage(40);
        boolean soldierExited = false, medicExited = false;
        for (int i = 0; i < 1400; i++) {
            for (Human h : humans) {
                h.update(1200, 800, new ArrayList<>(), safe);
                for (Building b : buildings) b.interact(h);
            }
            soldierExited |= soldier.isArmed() && !soldier.isSheltered();
            medicExited |= medic.getMedKits() > 0 && !medic.isSheltered();
        }
        check(soldier.isArmed(), "Soldier navigates into police and picks up gun");
        check(medic.getMedKits() > 0, "Medic navigates into hospital and picks up kit");
        check(civilian.getHealth() > 60, "Injured civilian collects and uses supplies or is healed");
        check(soldierExited && medicExited, "Equipped humans exit buildings");
        at(medic, 500, 400); at(civilian, 520, 400); civilian.takeDamage(40);
        int before = civilian.getHealth(), kits = medic.getMedKits(); set(medic, "lastHealTime", 0L);
        medic.heal(civilian);
        check(civilian.getHealth() == Math.min(100, before + 20) && medic.getMedKits() == kits - 1, "Medic consumes kit healing another human");
        medic.heal(civilian); check(medic.getMedKits() == kits - 1, "Healing cooldown");
        at(soldier, 500, 400);
        for (Zombie z : List.of(new Zombie(550,400), new Runner(550,400), new Stalker(550,400), new Bloater(550,400), new MutantBoss(550,400))) {
            Soldier shooter = new Soldier(500,400);
            shooter.setEnvironment(buildings, humans);
            shooter.interact(new Weapon(3,50,3));
            int requiredShots = z instanceof MutantBoss ? 4
                : z instanceof Stalker || z instanceof Bloater ? 3 : 2;
            check(z.getHealth() == requiredShots * 50 && z.getMaxHealth() == z.getHealth(),
                "Zombie health matches type-specific bullet requirement");
            for (int hit = 1; hit <= requiredShots; hit++) {
                if (!shooter.isArmed()) shooter.interact(new Weapon(3,50,3));
                set(shooter,"lastShotTime",0L);
                check(shooter.shoot(z) && z.isAlive() == (hit < requiredShots),
                    "Type-specific shot " + hit + " of " + requiredShots);
                check(z.getHealth() == (requiredShots-hit)*50,"Bullets remove actual health");
                if (hit < requiredShots) check(!shooter.shoot(z),"Shooting cooldown");
            }
        }

        Soldier limited = new Soldier(500,400);
        PoliceStation respawning = new PoliceStation(0,550);
        Weapon pickup = (Weapon) respawning.getResources().get(0);
        limited.interact(pickup);
        long collected = (long) field(pickup,"collectedAt");
        check(limited.getAmmo() == 5 && pickup.isCollected(), "Pickup gives exactly five shots");
        pickup.updateRespawn(collected + 9_999);
        check(pickup.isCollected(), "Gun does not respawn early");
        for (int i = 0; i < 5; i++) {
            set(limited,"lastShotTime",0L);
            check(limited.shoot(new Zombie(550,400)), "Gun fires shot " + (i + 1));
        }
        check(!limited.isArmed() && limited.getAmmo() == 0, "Fifth shot removes gun");
        set(limited,"lastShotTime",0L);
        check(!limited.shoot(new Zombie(550,400)), "No sixth shot");
        pickup.updateRespawn(collected + 10_000);
        check(!pickup.isCollected() && pickup.getAmmo() == 5, "Gun respawns at ten seconds");
        check(!limited.isArmed(), "Respawn does not reload a carried gun");
        limited.interact(pickup);
        check(limited.getAmmo() == 5, "Soldier can collect replacement gun");
        long nextCollection = (long) field(pickup,"collectedAt");
        pickup.updateRespawn(nextCollection + 9_999);
        check(pickup.isCollected(), "Each collection starts a fresh ten-second delay");
        pickup.updateRespawn(nextCollection + 10_000);
        check(!pickup.isCollected() && limited.getAmmo() == 5, "Repeated respawn keeps inventory separate");
        Medic suppliedMedic = new Medic(500,400);
        Civilian patient = new Civilian(520,400);
        Medicine medicinePickup = new Medicine(3,20,1060,75);
        suppliedMedic.interact(medicinePickup);
        patient.takeDamage(40); suppliedMedic.heal(patient);
        check(suppliedMedic.getMedKits() == 2 && medicinePickup.getQuantity() == 3,
            "Using carried medkit does not deplete hospital pickup");
        long medicineTime = (long) field(medicinePickup,"collectedAt");
        medicinePickup.updateRespawn(medicineTime + 29_999);
        check(medicinePickup.isCollected(), "Medkits do not respawn early");
        medicinePickup.updateRespawn(medicineTime + 30_000);
        check(!medicinePickup.isCollected() && suppliedMedic.getMedKits() == 2,
            "Medkit respawn does not refill carried kits");
        Civilian nonMedic = new Civilian(500,400);
        nonMedic.interact(medicinePickup);
        check(!medicinePickup.isCollected(), "Respawned medkit remains medic-only");
        Medic secondMedic = new Medic(500,400); secondMedic.interact(medicinePickup);
        check(secondMedic.getMedKits() == 3 && suppliedMedic.getMedKits() == 2,
            "Another medic collects independent full medkit stock");
        long medicineAgain = (long) field(medicinePickup,"collectedAt");
        medicinePickup.updateRespawn(medicineAgain + 29_999);
        check(medicinePickup.isCollected(), "Medkit recollection resets timer");
        medicinePickup.updateRespawn(medicineAgain + 30_000);
        check(!medicinePickup.isCollected(), "Medkits repeatedly respawn");
        Food foodPickup = new Food(1,20,1060,615);
        Civilian hungry = new Civilian(500,400); hungry.takeDamage(40);
        hungry.interact(foodPickup);
        check(hungry.getHealth() == 80 && foodPickup.getQuantity() == 1,
            "Food consumption leaves full world pickup for respawn");
        long foodTime = (long) field(foodPickup,"collectedAt");
        foodPickup.updateRespawn(foodTime + 29_999);
        check(foodPickup.isCollected(), "Food does not respawn early");
        foodPickup.updateRespawn(foodTime + 30_000);
        check(!foodPickup.isCollected() && hungry.getHealth() == 80,
            "Food respawn does not automatically heal owner");
        nonMedic.interact(foodPickup);
        check(!foodPickup.isCollected(), "Full health human cannot collect respawned food");
        hungry.interact(foodPickup);
        check(hungry.getHealth() == 100 && foodPickup.isCollected(), "Respawned food remains usable");
        long foodAgain = (long) field(foodPickup,"collectedAt");
        foodPickup.updateRespawn(foodAgain + 29_999);
        check(foodPickup.isCollected(), "Food recollection resets timer");
        foodPickup.updateRespawn(foodAgain + 30_000);
        check(!foodPickup.isCollected(), "Food repeatedly respawns");
        at(soldier, 1000, 600);
        Zombie behindWall = new Zombie(1060,600);
        set(soldier,"lastShotTime",0L);
        check(!soldier.shoot(behindWall), "Cannot shoot through building wall");
        // No damage, chase or explosion through shelter walls.
        at(civilian, 1060, 600);
        Zombie zombie = new Zombie(1000, 600); zombie.setBuildings(buildings);
        int hp = civilian.getHealth();
        check(!civilian.receiveZombieHit(20), "Sheltered human rejects melee");
        Bloater bloater = new Bloater(1010,600);
        bloater.explode(new ArrayList<>(List.of(civilian)));
        check(civilian.getHealth() == hp, "Sheltered human rejects explosion");
        for (int i = 0; i < 100; i++) zombie.update(1200,800,new ArrayList<>(List.of(civilian)),safe);
        check(!store.overlaps(zombie.getX(),zombie.getY(),zombie.getSize()), "Zombies stay outside building");
        PoliceStation emptyStation = new PoliceStation(0,550);
        for (Resource resource : emptyStation.getResources()) resource.collect();
        Soldier waiting = new Soldier(60,625);
        waiting.setEnvironment(List.of(emptyStation), List.of(waiting));
        for (int i = 0; i < 100; i++) waiting.update(1200,800,new ArrayList<>(),safe);
        check(!emptyStation.overlaps(waiting.getX(),waiting.getY(),waiting.getSize()) && !waiting.isArmed(),
            "Soldier leaves empty station");
        at(waiting,500,400);
        for (int i = 0; i < 10; i++) waiting.update(1200,800,new ArrayList<>(),safe);
        check(waiting.getX() != 500 || waiting.getY() != 400, "Soldier roams while guns are unavailable");
        Resource replacement = emptyStation.getResources().get(0);
        replacement.updateRespawn((long) field(replacement,"collectedAt") + 10_000);
        for (int i = 0; i < 400 && !waiting.isArmed(); i++) {
            waiting.update(1200,800,new ArrayList<>(),safe);
            emptyStation.interact(waiting);
        }
        check(waiting.isArmed(), "Roaming soldier returns and collects respawned gun");
        Zombie distant = new Zombie(650,400);
        ArrayList<Zombie> targets = new ArrayList<>(List.of(distant));
        for (int i = 0; i < 260; i++) waiting.update(1200,800,targets,safe);
        check(!emptyStation.overlaps(waiting.getX(),waiting.getY(),waiting.getSize())
            && waiting.getPosition().distanceTo(distant.getPosition()) < 180,
            "Armed soldier leaves station and pursues zombie into shooting range");
        Soldier aggressive = new Soldier(500,400); aggressive.interact(new Weapon(3,50,3));
        Zombie nearby = new Zombie(660,400);
        aggressive.update(1200,800,new ArrayList<>(List.of(nearby)),safe);
        check(aggressive.getX() > 500, "Armed soldier approaches instead of fleeing");
        for (int i = 0; i < 3; i++) {
            set(aggressive,"lastShotTime",0L); aggressive.shoot(new Zombie(550,400));
        }
        aggressive.setEnvironment(List.of(emptyStation),List.of(aggressive));
        for (Resource resource : emptyStation.getResources()) resource.collect();
        at(aggressive,500,400);
        for (int i = 0; i < 10; i++) aggressive.update(1200,800,new ArrayList<>(),safe);
        check(!aggressive.isArmed() && (aggressive.getX() != 500 || aggressive.getY() != 400),
            "Empty soldier keeps roaming instead of waiting for unavailable guns");
        Zombie[] strengthened = {new Zombie(500,400), new Runner(500,400), new Stalker(500,400),
            new Bloater(500,400), new MutantBoss(500,400)};
        double[] speeds = {1.875, 2.5, 1.5, 1.0, 2.75};
        int[] damages = {25,25,38,25,50};
        for (int i = 0; i < strengthened.length; i++) {
            check(Math.abs(strengthened[i].getSpeed() - speeds[i]) < 0.0001, "Increased zombie movement speed");
            Human victim = new Human(510,400);
            strengthened[i].update(1200,800,new ArrayList<>(List.of(victim)),safe);
            check(victim.getHealth() == 100 - damages[i], "Increased zombie attack damage");
        }
        Runner burstRunner = new Runner(500,400);
        burstRunner.update(1200,800,new ArrayList<>(List.of(new Human(600,400))),safe);
        check(burstRunner.getSpeed() == 4.0, "Runner burst speed increased");
        Human blastVictim = new Human(510,400);
        new Bloater(500,400).explode(new ArrayList<>(List.of(blastVictim)));
        check(blastVictim.getHealth() == 25, "Bloater explosion deals seventy-five damage");
        Hospital fieldHospital = new Hospital(1020,0);
        Medic fieldMedic = new Medic(1140,120);
        Soldier woundedSoldier = new Soldier(700,350); woundedSoldier.takeDamage(40);
        fieldMedic.setEnvironment(List.of(fieldHospital),List.of(fieldMedic,woundedSoldier));
        fieldMedic.interact(fieldHospital.getResources().get(0));
        ArrayList<Zombie> pressure = new ArrayList<>(List.of(new Zombie(800,300)));
        boolean leftHospital = false;
        for (int i = 0; i < 350 && woundedSoldier.getHealth() < 100; i++) {
            set(fieldMedic,"lastHealTime",0L);
            fieldMedic.update(1200,800,pressure,safe);
            leftHospital |= !fieldHospital.overlaps(fieldMedic.getX(),fieldMedic.getY(),fieldMedic.getSize());
        }
        check(leftHospital && woundedSoldier.getHealth() == 100,
            "Equipped medic leaves hospital and heals wounded soldier despite nearby zombies");
        Medic priorityMedic = new Medic(500,400); priorityMedic.interact(new Medicine(3,20));
        Soldier prioritySoldier = new Soldier(600,400); prioritySoldier.takeDamage(40);
        Civilian priorityCivilian = new Civilian(480,400); priorityCivilian.takeDamage(40);
        priorityMedic.setEnvironment(List.of(),List.of(priorityCivilian,prioritySoldier,priorityMedic));
        priorityMedic.update(1200,800,new ArrayList<>(List.of(new Zombie(550,400))),safe);
        check(priorityMedic.getX() > 500 && priorityCivilian.getHealth() == 60,
            "Medic pursues wounded soldier before a nearer wounded civilian");
        at(prioritySoldier,priorityMedic.getX()+20,priorityMedic.getY());
        priorityMedic.update(1200,800,new ArrayList<>(),safe);
        check(prioritySoldier.getHealth() == 80 && priorityCivilian.getHealth() == 60,
            "Healing prioritizes soldier regardless of companion list order");
        prioritySoldier.takeDamage(1000);
        set(priorityMedic,"lastHealTime",0L);
        at(priorityCivilian,priorityMedic.getX()+20,priorityMedic.getY());
        priorityMedic.update(1200,800,new ArrayList<>(),safe);
        check(priorityCivilian.getHealth() == 80, "Medic treats other humans when no living wounded soldier needs care");
        at(fieldMedic,600,400); at(woundedSoldier,620,400);
        while (fieldMedic.getMedKits() > 0) {
            woundedSoldier.takeDamage(20); set(fieldMedic,"lastHealTime",0L); fieldMedic.heal(woundedSoldier);
        }
        for (int i = 0; i < 400 && fieldMedic.getMedKits() == 0; i++) {
            fieldMedic.update(1200,800,new ArrayList<>(),safe);
            fieldHospital.interact(fieldMedic);
        }
        check(fieldMedic.getMedKits() > 0, "Medic returns for more medkits after exhausting supply");
        BufferedImage bars = new BufferedImage(360,130,BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D barGraphics = bars.createGraphics();
        barGraphics.setColor(java.awt.Color.LIGHT_GRAY); barGraphics.fillRect(0,0,360,130);
        Zombie[] displayed = {new Zombie(30,35), new Runner(90,35), new Stalker(150,35),
            new Bloater(210,35), new MutantBoss(280,35)};
        for (Zombie shown : displayed) {
            int barWidth = Math.max(24,shown.getSize());
            int barLeft = shown.getX() + (shown.getSize()-barWidth)/2;
            shown.draw(barGraphics);
            check(bars.getRGB(barLeft+barWidth-1,shown.getY()-8) == java.awt.Color.GREEN.getRGB(),
                "Every zombie subtype draws a full health bar at its own maximum health");
            shown.receiveGunshot(); at(shown,shown.getX(),90); shown.draw(barGraphics);
            check(bars.getRGB(barLeft+barWidth-1,shown.getY()-8) == java.awt.Color.RED.getRGB()
                && bars.getRGB(barLeft+1,shown.getY()-8) == java.awt.Color.GREEN.getRGB(),
                "Zombie health bar shows lost health after first shot");
            int remainingShots = shown.getHealth() / 50;
            for (int hit = 0; hit < remainingShots; hit++) shown.receiveGunshot();
            check(!shown.isAlive(),"Type-specific health bar reaches zero after required shots");
        }
        barGraphics.dispose();
        if (args.length > 1) ImageIO.write(bars,"png",new File(args[1]));
        Medic criticalMedic = new Medic(500,400); criticalMedic.interact(new Medicine(3,20));
        Soldier criticalPatient = new Soldier(520,400); criticalPatient.takeDamage(40);
        criticalMedic.setEnvironment(List.of(),List.of(criticalMedic,criticalPatient));
        criticalMedic.takeDamage(80);
        criticalMedic.update(1200,800,new ArrayList<>(),safe);
        check(criticalMedic.getHealth() == 40 && criticalMedic.getMedKits() == 2
            && criticalPatient.getHealth() == 60, "Medic at twenty health uses one kit on self before soldier");
        set(criticalMedic,"lastHealTime",0L);
        criticalMedic.update(1200,800,new ArrayList<>(),safe);
        check(criticalMedic.getHealth() == 40 && criticalPatient.getHealth() == 80,
            "Stabilized medic resumes healing soldier instead of self");
        Medic nearlyCritical = new Medic(500,400); nearlyCritical.interact(new Medicine(3,20));
        nearlyCritical.takeDamage(79); nearlyCritical.heal(nearlyCritical);
        check(nearlyCritical.getHealth() == 21 && nearlyCritical.getMedKits() == 3,
            "Medic above twenty health preserves kits for patients");
        nearlyCritical.takeDamage(10); nearlyCritical.heal(nearlyCritical);
        check(nearlyCritical.getHealth() == 31 && nearlyCritical.getMedKits() == 2,
            "Medic below twenty health also stabilizes self");
        Medic unstocked = new Medic(500,400); unstocked.takeDamage(80); unstocked.heal(unstocked);
        check(unstocked.getHealth() == 20,"Empty medic cannot heal without medkit");
        ConvenienceStore exitStore = new ConvenienceStore(1020,550);
        for (Resource resource : exitStore.getResources()) {
            Civilian exiting = new Civilian(resource.getX(),resource.getY());
            exiting.setEnvironment(List.of(exitStore),List.of(exiting));
            int steps = 0;
            while (exitStore.overlaps(exiting.getX(),exiting.getY(),exiting.getSize()) && steps++ < 150)
                exiting.update(1200,800,new ArrayList<>(),safe);
            check(!exitStore.overlaps(exiting.getX(),exiting.getY(),exiting.getSize()),
                "Full-health civilian exits from every shelf without hitting doorway frame");
        }
        Civilian customer = new Civilian(1060,675); customer.takeDamage(20);
        customer.setEnvironment(List.of(exitStore),List.of(customer));
        exitStore.interact(customer);
        check(customer.getHealth() == 100,"Civilian consumes bottom-shelf supply");
        for (int i=0; i<100 && exitStore.overlaps(customer.getX(),customer.getY(),customer.getSize()); i++)
            customer.update(1200,800,new ArrayList<>(),safe);
        check(!exitStore.overlaps(customer.getX(),customer.getY(),customer.getSize()),
            "Civilian exits after using store supply");
        Resource usedSupply = exitStore.getResources().get(6);
        long usedAt = (long) field(usedSupply,"collectedAt");
        usedSupply.updateRespawn(usedAt+29_999);
        check(usedSupply.isCollected(),"Store supply stays unavailable until thirty seconds");
        usedSupply.updateRespawn(usedAt+30_000);
        check(!usedSupply.isCollected() && usedSupply.getQuantity()==1,"Store shelf replenishes after thirty seconds");
        Main panel = new Main(); panel.setSize(1200,800);
        World world = (World) field(panel,"world");
        check(world.getHumans().stream().filter(h -> h instanceof Soldier).count() == 8,
            "Population includes eight soldiers");
        check(world.getHumans().stream().filter(h -> h instanceof Medic).count() == 6
            && world.getHumans().stream().filter(h -> h instanceof Civilian).count() == 6,
            "Population includes six medics and six civilians");
        check(world.getHumans().size() == 20 && world.getZombies().size() == 10,
            "Simulation starts with twenty humans and ten zombies");
        Zombie original = world.getZombies().get(0);
        MutantBoss boss = new MutantBoss(300,30); world.addCharacter(boss);
        int count = world.getZombies().size();
        set(panel,"waveNumber",5); set(panel,"lastWaveTime",0L); call(panel,"updateTimedWaves");
        check(world.getZombies().contains(original) && world.getZombies().contains(boss), "Wave six preserves existing zombies and boss");
        check(world.getZombies().size() == count + 24, "Wave six adds twenty-four zombies");
        check(world.getHumans().size() == 26
            && world.getHumans().stream().filter(h -> h instanceof Soldier).count() == 10
            && world.getHumans().stream().filter(h -> h instanceof Medic).count() == 8
            && world.getHumans().stream().filter(h -> h instanceof Civilian).count() == 8,
            "Normal wave adds two humans of each role");
        call(panel,"updateTimedWaves");
        check(world.getHumans().size() == 26,"No additional reinforcements before next wave");
        set(panel,"waveNumber",9); set(panel,"lastWaveTime",0L); call(panel,"updateTimedWaves");
        check(world.getHumans().size() == 32
            && world.getHumans().stream().filter(h -> h instanceof Soldier).count() == 12
            && world.getHumans().stream().filter(h -> h instanceof Medic).count() == 10
            && world.getHumans().stream().filter(h -> h instanceof Civilian).count() == 10,
            "Boss wave adds two humans of each role");
        set(panel,"lastWaveTime",world.getTime());
        for (int i=0; i<80; i++) call(panel,"updateSimulation");
        BufferedImage image = new BufferedImage(1200,800,BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = image.createGraphics(); panel.paint(graphics); graphics.dispose();
        if (args.length > 0) ImageIO.write(image,"png",new File(args[0]));
        itera.ui.FastForward controls = panel.getFastForward();
        for (java.awt.Component component : controls.getComponents()) {
            if (component instanceof javax.swing.JButton button) {
                button.doClick();
                int speed = Integer.parseInt(button.getText().substring(0,1));
                check(controls.getSpeed() == speed && controls.getDelay() == 30 / speed,
                    "Fast-forward " + speed + "x control works");
            }
        }
        Zombie cleanupSurvivor = new Zombie(600,400); world.addCharacter(cleanupSurvivor);
        boss.takeDamage(10000); world.update();
        check(!world.getZombies().contains(boss) && world.getZombies().contains(cleanupSurvivor),
            "Cleanup removes dead zombies only");
        System.out.println("Passed " + checks + " gameplay checks; Swing simulation updated and rendered successfully.");
    }
}
