package itera.model;

import java.awt.Color;
import java.awt.Graphics;

public class Medicine extends Resource {

    private int healAmount;

    public Medicine(int quantity, int healAmount) {

        super(quantity);

        this.healAmount = healAmount;
    }

    public Medicine(int quantity, int healAmount, int x, int y) {

        super(quantity, x, y);

        this.healAmount = healAmount;
    }

    @Override
    public Medicine inventoryCopy() {
        return new Medicine(quantity, healAmount);
    }

    @Override
    public void use(Character target) {

        if (quantity <= 0 || !(target instanceof Human) || !target.isAlive() || target.health >= 100) {
            return;
        }

        target.health += healAmount;

        if (target.health > 100) {

            target.health = 100;
        }

        quantity--;
    }

    @Override
    public void draw(Graphics g) {

        if (collected) {
            return;
        }

        /* Medicine box */
        g.setColor(Color.WHITE);

        g.fillRect(x, y, size, size);

        g.setColor(Color.RED);

        g.fillRect(x + 5, y + 2, 4, 10);

        g.fillRect(x + 2, y + 5, 10, 4);

        g.setColor(Color.BLACK);

        g.drawRect(x, y, size, size);
    }
}
