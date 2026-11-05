package itera.model.human;

import itera.model.*;
import itera.model.Character;
import itera.model.zombie.*;
import itera.simulation.EmergencySupplyDrop;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

public class Medic extends Human {
    private itera.model.human.treatment.TreatmentStrategy treatmentStrategy =
        new itera.model.human.treatment.SoldiersFirstStrategy();

    public void setTreatmentStrategy(itera.model.human.treatment.TreatmentStrategy strategy) {
        treatmentStrategy = Objects.requireNonNull(strategy, "Treatment strategy is required");
    }

    public itera.model.human.treatment.TreatmentStrategy getTreatmentStrategy() {
        return treatmentStrategy;
    }

    private long lastHealTime;
    private long healingUntil;
    private int movementWidth;
    private int movementHeight;

    // The world supplies a current snapshot each simulation tick.
    private java.util.List<EmergencySupplyDrop> emergencySupplyDrops = java.util.List.of();

    public void setEmergencySupplyDrops(java.util.List<EmergencySupplyDrop> drops) {
        emergencySupplyDrops = java.util.List.copyOf(
            Objects.requireNonNull(drops, "Supply drops are required"));
    }

    public Medic(int x, int y) { super(x, y); }

    public int getMedKits() {
        int count = 0;
        for (Resource resource : inventory)
            if (resource instanceof Medicine) count += resource.getQuantity();
        return count;
    }

    public void heal(Character target) {
        long now = now();
        boolean self = target == this;
        if (!isAlive() || !(target instanceof Human) || !target.isAlive()
                || self && health > 20 || !self && health <= 20
                || target.getHealth() >= 100 || position.distanceTo(target.getPosition()) > 35
                || !self && !clearShot(target) || now - lastHealTime < 1000) return;
        for (Resource resource : inventory) {
            if (resource instanceof Medicine && resource.getQuantity() > 0) {
                resource.use(target);
                lastHealTime = now;
                healingUntil = now + 300;
                return;
            }
        }
    }

    @Override
    public void update(int width, int height, ArrayList<Zombie> zombies, SafePoint safePoint) {
        movementWidth = width;
        movementHeight = height;
        // Stabilize at critical health before resuming treatment of soldiers.
        if (health <= 20) heal(this);
        super.update(width, height, zombies, safePoint);
        Human patient = findPatient();
        if (patient != null) heal(patient);
    }

    /** Select only living, wounded patients outside shelter; never treat self here. */
    public Optional<Human> selectPatient(java.util.List<Human> candidates) {
        return candidates.stream()
            .filter(human -> human != this && human.isAlive())
            .filter(human -> human.getHealth() < 100 && !human.isSheltered())
            .min(treatmentStrategy.priorityFor(this));
    }

    private Human findPatient() {
        return selectPatient(companions).orElse(null);
    }

    @Override
    protected boolean seekResources() {
        // Once equipped, leave any building before heading to a patient.
        if (getMedKits() > 0) {
            return leaveBuilding();
        }

        // Prioritise a nearby active emergency drop when this medic has no kits.
        EmergencySupplyDrop nearestDrop = null;
        double nearestDistance = Double.MAX_VALUE;
        for (EmergencySupplyDrop drop : emergencySupplyDrops) {
            if (!drop.isActive(now()) || !canCollect(drop.getMedicine())) {
                continue;
            }
            double distance = position.distanceTo(
                new Vector2D(drop.getX(), drop.getY()));
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestDrop = drop;
            }
        }
        if (nearestDrop != null) {
            if (!leaveBuilding()) {
                moveTowardsOutdoorTarget(new Vector2D(nearestDrop.getX(), nearestDrop.getY()));
            }
            return true;
        }

        // If there is no active emergency drop, use the hospital as before.
        for (Building building : buildings) {
            if (!(building instanceof Hospital)) continue;
            Resource pickup = null;
            for (Resource resource : building.getResources()) {
                if (canCollect(resource)) { pickup = resource; break; }
            }
            if (pickup == null && !building.overlaps(getX(), getY(), size)) {
                if (dx == 0 && dy == 0) chooseRandomDirection();
                return false;
            }
            moveTowards(building.destination(this, pickup));
            return true;
        }
        return false;
    }

    private boolean leaveBuilding() {
        for (Building building : buildings) {
            if (building.overlaps(position.getX(), position.getY(), size)) {
                moveTowards(building.destination(this, null));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean pursueTarget(ArrayList<Zombie> zombies) {
        if (getMedKits() == 0) return false;
        Human patient = findPatient();
        if (patient == null) return false;
        // Medical duty takes precedence over the normal roaming/fleeing behaviour.
        if (position.distanceTo(patient.getPosition()) <= 30 && clearShot(patient)) {
            dx = 0;
            dy = 0;
        } else {
            moveTowardsOutdoorTarget(patient.getPosition());
        }
        return true;
    }

    /** Steer around a building via one or two corners when a straight path is blocked. */
    private void moveTowardsOutdoorTarget(Vector2D destination) {
        // Movement coordinates are the top-left of the medic, so expand the
        // obstacle to account for the medic's full footprint. A tiny inset
        // treats exact edge contact as clear, like Building.overlaps().
        double inset = 0.001;
        List<Rectangle2D> obstacles = new ArrayList<>(buildings.size());
        for (Building building : buildings) {
            obstacles.add(new Rectangle2D.Double(building.getX() - size + inset,
                building.getY() - size + inset, building.getWidth() + size - 2 * inset,
                building.getHeight() + size - 2 * inset));
        }
        if (clearPath(position, destination, obstacles)) {
            moveTowards(destination);
            return;
        }

        List<Vector2D> corners = new ArrayList<>();
        for (Rectangle2D obstacle : obstacles) {
            for (double x : new double[] {obstacle.getMinX() - 1, obstacle.getMaxX() + 1}) {
                for (double y : new double[] {obstacle.getMinY() - 1, obstacle.getMaxY() + 1}) {
                    if (x >= 0 && x <= movementWidth - size && y >= 0 && y <= movementHeight - size) {
                        corners.add(new Vector2D(x, y));
                    }
                }
            }
        }

        Vector2D waypoint = destination;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Vector2D entry : corners) {
            double firstLeg = position.distanceTo(entry);
            if (firstLeg <= inset || !clearPath(position, entry, obstacles)) continue;
            for (Vector2D exit : corners) {
                double distance = firstLeg + entry.distanceTo(exit) + exit.distanceTo(destination);
                if (distance < bestDistance && clearPath(entry, exit, obstacles)
                        && clearPath(exit, destination, obstacles)) {
                    bestDistance = distance;
                    waypoint = entry;
                }
            }
        }
        moveTowards(waypoint);
    }

    private static boolean clearPath(Vector2D from, Vector2D to, List<Rectangle2D> obstacles) {
        for (Rectangle2D obstacle : obstacles) {
            if (obstacle.intersectsLine(from.getX(), from.getY(), to.getX(), to.getY())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        drawTypeLabel(g, "M");
        if (getMedKits() > 0) {
            g.setColor(Color.WHITE);
            g.fillRect(getX() + 12, getY() + 5, 9, 9);
            g.setColor(Color.RED);
            g.drawLine(getX() + 16, getY() + 6, getX() + 16, getY() + 12);
            g.drawLine(getX() + 13, getY() + 9, getX() + 19, getY() + 9);
        }
        if (now() < healingUntil) {
            g.setColor(Color.GREEN.darker());
            g.drawString("+20", getX(), getY() - 12);
        }
    }
}
