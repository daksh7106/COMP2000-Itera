package itera.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class MutantBoss extends Zombie {

    // Boss-specific statistics
    private static final int BOSS_HEALTH = 200;
    private static final int BOSS_DAMAGE = 50;
    private static final double BOSS_SPEED = 2.75;
    private static final int BOSS_SIZE = 40;
    private static final double BOSS_DETECTION_RANGE = 700;

    /**
     * Creates a Mutant Boss at the given position.
     *
     * @param x starting x position
     * @param y starting y position
     */
    public MutantBoss(int x, int y) {

        // First create it as a normal Zombie
        super(x, y);

        // Then replace the normal zombie statistics
        // with the stronger boss statistics
        this.health = BOSS_HEALTH;
        this.speed = BOSS_SPEED;
        this.size = BOSS_SIZE;
        this.detectionRange = BOSS_DETECTION_RANGE;
    }

    /**
     * The boss uses a stronger attack than a normal zombie.
     */
    @Override
    protected boolean performAttack(Human target) {

        return target.receiveZombieHit(BOSS_DAMAGE);
    }

    /**
     * Draws the Mutant Boss.
     */
    @Override
    public void draw(Graphics g) {

        // Draw the boss as a large dark-purple circle
        g.setColor(new Color(100, 0, 100));
        g.fillOval(getX(), getY(), size, size);

        drawHealthBar(g);

        // Display BOSS underneath it
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("BOSS", getX(), getY() + size + 15);
    }

    /**
     * Returns the boss's current health.
     * We will use this later for the boss health bar.
     */
    public int getBossHealth() {

        return health;
    }

    /**
     * Returns the boss's maximum health.
     */
    @Override
    public int getMaxHealth() { return BOSS_HEALTH; }

    public int getMaxBossHealth() {

        return BOSS_HEALTH;
    }
}