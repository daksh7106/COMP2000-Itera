package itera.model;

import java.awt.Graphics;

public abstract class Character {

    protected java.util.function.LongSupplier timeSource = System::currentTimeMillis;

    public void setTimeSource(java.util.function.LongSupplier timeSource) {
        this.timeSource = java.util.Objects.requireNonNull(timeSource);
    }

    protected long now() { return timeSource.getAsLong(); }


    protected int health;
    protected double speed;
    protected Vector2D position;
    protected int size;

    public Character(int health, double speed, double x, double y, int size) {

        this.health = health;
        this.speed = speed;
        this.position = new Vector2D(x, y);
        this.size = size;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void move(Vector2D direction) {

        position.add(direction.getX() * speed, direction.getY() * speed);
    }

    public void takeDamage(int amount) {

        health -= amount;

        if (health < 0) {
            health = 0;
        }
    }

    public int getHealth() {
        return health;
    }

    public int getX() {
        return (int) position.getX();
    }

    public int getY() {
        return (int) position.getY();
    }

    public Vector2D getPosition() {
        return position;
    }

    public double getSpeed() {
        return speed;
    }

    public int getSize() {
        return size;
    }

    public abstract void draw(Graphics g);
}
