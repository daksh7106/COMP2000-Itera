package itera.model;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class Medic extends Human {
    private long lastHealTime;
    private long healingUntil;

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
        // Stabilize at critical health before resuming treatment of soldiers.
        if (health <= 20) heal(this);
        super.update(width, height, zombies, safePoint);
        Human patient = findPatient();
        if (patient != null) heal(patient);
    }

    private Human findPatient() {
        Human best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Human human : companions) {
            if (human == this || !human.isAlive() || human.getHealth() >= 100 || human.isSheltered()) continue;
            double distance = position.distanceTo(human.getPosition());
            boolean soldier = human instanceof Soldier;
            boolean bestSoldier = best instanceof Soldier;
            if (best == null || soldier && !bestSoldier
                    || soldier == bestSoldier && distance < bestDistance) {
                best = human;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Override
    protected boolean seekResources() {
        // Once equipped, leave any building before heading to a patient.
        if (getMedKits() > 0) {
            for (Building building : buildings) {
                if (building.overlaps(getX(), getY(), size)) {
                    moveTowards(building.destination(this, null));
                    return true;
                }
            }
            return false;
        }
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
            moveTowards(patient.getPosition());
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
