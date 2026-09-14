package itera.model;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

public class Runner extends Zombie {
    private static final double SPRINT_SPEED = 2.5;
    private static final double BURST_SPEED = 4.0;
    private static final double BURST_RANGE = 150;

    private static final long BURST_DURATION = 1200;
    private static final long BURST_COOLDOWN = 3000;

    private long burstEndTime = 0;
    private long lastBurstTime = 0;

    @SuppressWarnings("this-escape")
    public Runner(int x, int y) {
        super(x, y);
        sprint();
    }

    public void sprint() {
        speed = SPRINT_SPEED;
    }

    @Override
    public Human update(int worldWidth, int worldHeight, ArrayList<Human> humans, SafePoint safePoint) {
        long now = now();
        Human target = findClosestHuman(humans);

        if (target != null) {
            double distance = position.distanceTo(target.getPosition());

            if (distance <= BURST_RANGE && now - lastBurstTime >= BURST_COOLDOWN) {
                burstEndTime = now + BURST_DURATION;
                lastBurstTime = now;
            }
        }

        if (now < burstEndTime) {
            speed = BURST_SPEED;
        } else {
            speed = SPRINT_SPEED;
        }

        return super.update(worldWidth, worldHeight, humans, safePoint);
    }

    @Override
    public void draw(Graphics g) {
        g.setColor(Color.ORANGE);
        g.fillOval(getX(), getY(), size, size);
        drawTypeLabel(g, "R");
    }
}
