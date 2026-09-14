package itera.model;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class Soldier extends Human {
    private static final double SHOOT_RANGE = 180;
    private static final long SHOOT_COOLDOWN = 700;
    private long lastShotTime;
    private long flashUntil;
    private int shotX;
    private int shotY;

    public Soldier(int x, int y) { super(x, y); }

    @Override
    public void update(int width, int height, ArrayList<Zombie> zombies, SafePoint safePoint) {
        super.update(width, height, zombies, safePoint);
        if (!isAlive() || isSheltered() || !isArmed()) return;
        Zombie target = findNearestZombie(zombies);
        if (target != null && position.distanceTo(target.getPosition()) <= SHOOT_RANGE
                && clearShot(target)) shoot(target);
    }

    @Override
    protected boolean seekResources() {
        for (Building building : buildings) {
            if (!(building instanceof PoliceStation)) continue;
            if (isArmed()) {
                // Leave through the entrance before pursuing a target.
                if (!building.overlaps(getX(), getY(), size)) return false;
                moveTowards(building.destination(this, null));
                return true;
            }
            Resource pickup = null;
            for (Resource resource : building.getResources()) {
                if (canCollect(resource)) { pickup = resource; break; }
            }
            if (pickup == null && !building.overlaps(getX(), getY(), size)) {
                // No guns available: roam outside until a pickup respawns.
                if (dx == 0 && dy == 0) chooseRandomDirection();
                return false;
            }
            // If stock ran out while entering, leave through the doorway.

            moveTowards(building.destination(this, pickup));
            return true;
        }
        return super.seekResources();
    }

    @Override
    protected boolean pursueTarget(ArrayList<Zombie> zombies) {
        if (!isArmed()) return false;
        Zombie target = findNearestZombie(zombies);
        if (target == null) return false;
        if (position.distanceTo(target.getPosition()) <= SHOOT_RANGE * 0.8 && clearShot(target)) {
            dx = 0;
            dy = 0;
        } else {
            moveTowards(target.getPosition());
        }
        return true;
    }

    public boolean shoot(Character target) {
        Weapon weapon = findUsableWeapon();
        long now = now();
        if (!isAlive() || isSheltered() || target == null || !target.isAlive() || weapon == null
                || position.distanceTo(target.getPosition()) > SHOOT_RANGE || !clearShot(target)
                || now - lastShotTime < SHOOT_COOLDOWN) return false;
        if (!weapon.fire(target)) return false;
        if (!weapon.canFire()) inventory.remove(weapon);
        lastShotTime = now;
        flashUntil = now + 160;
        shotX = target.getX() + target.getSize() / 2;
        shotY = target.getY() + target.getSize() / 2;
        return true;
    }

    private Weapon findUsableWeapon() {
        for (Resource resource : inventory)
            if (resource instanceof Weapon weapon && weapon.canFire()) return weapon;
        return null;
    }

    public boolean isArmed() { return findUsableWeapon() != null; }
    public int getAmmo() {
        Weapon weapon = findUsableWeapon();
        return weapon == null ? 0 : weapon.getAmmo();
    }

    @Override
    public void draw(Graphics g) {
        super.draw(g);
        drawTypeLabel(g, "S");
        if (isArmed()) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(getX() + 9, getY() + 6, 16, 5);
            g.fillRect(getX() + 12, getY() + 9, 4, 6);
        }
        if (now() < flashUntil) {
            g.setColor(Color.ORANGE);
            g.fillOval(getX() + 22, getY() + 3, 9, 9);
            g.setColor(new Color(230, 170, 0));
            g.drawLine(getX() + 25, getY() + 8, shotX, shotY);
        }
    }
}
