package itera.model;

import java.awt.Graphics;

public abstract class Resource {

    protected java.util.function.LongSupplier timeSource = System::currentTimeMillis;

    public void setTimeSource(java.util.function.LongSupplier timeSource) {
        this.timeSource = java.util.Objects.requireNonNull(timeSource);
    }

    protected long now() { return timeSource.getAsLong(); }


    private static final long RESPAWN_DELAY = 30_000;
    private long collectedAt;

    protected int quantity;

    protected int x;
    protected int y;

    protected int size = 14;

    protected boolean collected = false;

    public Resource(int quantity) {

        this.quantity = quantity;

        this.x = -100;
        this.y = -100;
    }

    public Resource(int quantity, int x, int y) {

        this.quantity = quantity;

        this.x = x;
        this.y = y;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isCollected() {
        return collected;
    }

    public boolean isNear(Character character) {

        double differenceX = character.getX() - x;

        double differenceY = character.getY() - y;

        double distance = Math.sqrt(differenceX * differenceX + differenceY * differenceY);

        return distance <= 25;
    }

    public void collect() {
        if (!collected) {
            collected = true;
            collectedAt = now();
        }
    }

    protected long getRespawnDelay() { return RESPAWN_DELAY; }

    /** Restore a world pickup after its simulation-time respawn delay. */
    public void updateRespawn(long now) {
        if (collected && x >= 0 && y >= 0 && now - collectedAt >= getRespawnDelay()) {
            collected = false;
        }
    }

    /** Keep carried/consumed supplies separate from the respawning world pickup. */
    public abstract Resource inventoryCopy();

    public abstract void use(Character target);

    public abstract void draw(Graphics g);
}
