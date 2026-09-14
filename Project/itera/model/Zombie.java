package itera.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;

public class Zombie extends Character {

    /** Standard police-station bullets deal 50 health damage. */
    public void receiveGunshot() {
        if (isAlive()) takeDamage(50);
    }

    private java.util.List<Building> buildings = java.util.List.of();
    public void setBuildings(java.util.List<Building> buildings) { this.buildings = buildings; }

    protected double detectionRange = 500;

    protected static final int DAMAGE = 25;

    protected static final double ATTACK_DISTANCE = 20;

    public Zombie(int x, int y) {

        super(100, 1.875, x, y, 18);
    }

    /**
     * Moves towards or attacks the nearest eligible human for one simulation step.
     *
     * @param worldWidth the width of the simulation area
     * @param worldHeight the height of the simulation area
     * @param humans the humans available as possible targets
     * @param safePoint the protected area that this zombie cannot enter
     * @return the human killed by this zombie's direct attack, or {@code null}
     *         if no human died
     */
    public Human update(int worldWidth, int worldHeight, ArrayList<Human> humans, SafePoint safePoint) {

        if (!isAlive()) {
            return null;
        }

        Human target = findClosestHuman(humans);

        if (target == null) {
            return null;
        }

        if (target.isSheltered()) {
            return null;
        }

        double distance = position.distanceTo(target.getPosition());

        if (distance > detectionRange) {
            return null;
        }

        if (distance <= ATTACK_DISTANCE) {

            boolean attacked = performAttack(target);

            if (attacked && !target.isAlive()) {

                return target;
            }

            return null;
        }

        double previousX = position.getX();

        double previousY = position.getY();

        chase(target);

        double nextX = position.getX();

        double nextY = position.getY();

        if (safePoint.wouldZombieEnter(nextX, nextY, size)
            || buildings.stream().anyMatch(b -> b.overlaps(nextX, nextY, size))) {

            position.setX(previousX);

            position.setY(previousY);

            return null;
        }

        keepInsideWorld(worldWidth, worldHeight);

        return null;
    }

    protected Human findClosestHuman(ArrayList<Human> humans) {

        Human closest = null;

        double closestDistance = Double.MAX_VALUE;

        for (Human human : humans) {

            if (!human.isAlive()) {
                continue;
            }

            if (human.isSheltered()) {
                continue;
            }

            double distance = position.distanceTo(human.getPosition());

            if (distance < closestDistance) {

                closestDistance = distance;

                closest = human;
            }
        }

        return closest;
    }

    protected boolean performAttack(Human target) {

        return target.receiveZombieHit(DAMAGE);
    }

    public void attack(Character target) {

        target.takeDamage(DAMAGE);
    }

    public void chase(Character target) {

        double directionX = target.getX() - position.getX();

        double directionY = target.getY() - position.getY();

        double distance = Math.sqrt(directionX * directionX + directionY * directionY);

        if (distance > 0) {

            position.add(directionX / distance * speed, directionY / distance * speed);
        }
    }

    protected void keepInsideWorld(int width, int height) {

        if (position.getX() < 0) {
            position.setX(0);
        }

        if (position.getX() > width - size) {

            position.setX(width - size);
        }

        if (position.getY() < 0) {
            position.setY(0);
        }

        if (position.getY() > height - size) {

            position.setY(height - size);
        }
    }

    @Override
    public void draw(Graphics g) {

        g.setColor(Color.RED);

        g.fillOval(getX(), getY(), size, size);

        drawTypeLabel(g, "Z");
    }

    public int getMaxHealth() { return 100; }

    protected void drawHealthBar(Graphics g) {
        int width = Math.max(24, size);
        int barX = getX() + (size - width) / 2;
        int barY = getY() - 9;
        int remaining = (int) Math.round(width * Math.max(0, Math.min(1.0,
            health / (double) getMaxHealth())));
        g.setColor(Color.RED);
        g.fillRect(barX, barY, width, 4);
        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, remaining, 4);
    }

    protected void drawTypeLabel(Graphics g, String label) {
        drawHealthBar(g);

        g.setColor(Color.BLACK);

        g.setFont(new Font("Arial", Font.BOLD, 10));

        g.drawString(label, getX() - 5, getY() + size + 12);
    }
}
