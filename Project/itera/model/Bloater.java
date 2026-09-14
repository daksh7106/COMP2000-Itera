package itera.model;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class Bloater extends Zombie {
    private double blastRadius = 60;
    private int blastDamage = 75;

    public Bloater(int x, int y) {
        super(x, y);
        health = 150;
        speed = 1.0;
        size = 22;
    }

    public boolean shouldExplode(ArrayList<Human> humans) {
        if (!isAlive()) {
            return false;
        }

        for (Human human : humans) {
            if (!human.isAlive()) {
                continue;
            }

            if (human.isSheltered()) {
                continue;
            }

            double distance = position.distanceTo(human.getPosition());

            if (distance <= blastRadius) {
                return true;
            }
        }

        return false;
    }

    /**
     * Damages living humans within range, ignores humans in the safe point,
     * and then defeats this bloater.
     *
     * @param humans the humans currently in the simulation
     * @return the humans killed by the explosion
     */
    public ArrayList<Human> explode(ArrayList<Human> humans) {
        ArrayList<Human> killedHumans = new ArrayList<>();

        for (Human human : humans) {
            if (!human.isAlive()) {
                continue;
            }

            if (human.isSheltered()) {
                continue;
            }

            double distance = position.distanceTo(human.getPosition());

            if (distance <= blastRadius) {
                human.takeDamage(blastDamage);

                if (!human.isAlive()) {
                    killedHumans.add(human);
                }
            }
        }

        health = 0;

        return killedHumans;
    }

    @Override
    public int getMaxHealth() { return 150; }

    public double getBlastRadius() {
        return blastRadius;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillOval(getX(), getY(), size, size);
        drawTypeLabel(g, "B");
    }
}
