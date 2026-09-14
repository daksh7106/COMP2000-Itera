package itera.model;

import java.awt.Color;
import java.awt.Graphics;

public class Weapon extends Resource {

    @Override
    protected long getRespawnDelay() { return 10_000; }

    @Override
    public Weapon inventoryCopy() {
        return new Weapon(quantity, damage, durability);
    }

    private int damage;
    private int durability;

    public Weapon(int quantity, int damage, int durability) {

        super(quantity);

        this.damage = damage;

        this.durability = durability;
    }

    public Weapon(int quantity, int damage, int durability, int x, int y) {

        super(quantity, x, y);

        this.damage = damage;

        this.durability = durability;
    }

    public boolean canFire() {

        return quantity > 0 && durability > 0;
    }

    /**
     * Fires at a target when the weapon is usable. A successful shot consumes
     * one unit of ammunition and durability.
     *
     * @param target the character receiving the weapon damage
     * @return {@code true} when a shot was fired, or {@code false} when the
     *         target is null or the weapon cannot fire
     */
    public boolean fire(Character target) {

        if (target == null || !target.isAlive() || !canFire()) {

            return false;
        }

        target.takeDamage(damage);

        durability--;

        quantity--;

        return true;
    }

    public int getDamage() {
        return damage;
    }

    public int getAmmo() {
        return quantity;
    }

    public int getDurability() {
        return durability;
    }

    public void addAmmo(int amount) {

        if (amount > 0) {

            quantity += amount;
        }
    }

    @Override
    public void use(Character target) {

        fire(target);
    }

    @Override
    public void draw(Graphics g) {

        if (collected) {
            return;
        }

        g.setColor(Color.DARK_GRAY);

        g.fillRect(x, y, 18, 6);

        g.fillRect(x + 10, y + 5, 5, 8);
    }
}
