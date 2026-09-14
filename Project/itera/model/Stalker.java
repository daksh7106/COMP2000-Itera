package itera.model;

import java.awt.Color;
import java.awt.Graphics;

public class Stalker extends Zombie {
    private int stealth = 100;

    private static final int AMBUSH_DAMAGE = 38;

    public Stalker(int x, int y) {
        super(x, y);
        health = 150;
        speed = 1.5;
    }

    @Override
    public int getMaxHealth() { return 150; }

    public void ambush(Character target) {
        if (target instanceof Human human) {
            human.receiveZombieHit(AMBUSH_DAMAGE);
        } else {
            target.takeDamage(AMBUSH_DAMAGE);
        }
    }

    @Override
    protected boolean performAttack(Human target) {
        int healthBefore = target.getHealth();
        ambush(target);

        return target.getHealth() < healthBefore;
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.MAGENTA);
        g.fillOval(getX(), getY(), size, size);
        drawTypeLabel(g, "S");
    }
}
