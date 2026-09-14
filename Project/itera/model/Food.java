package itera.model;

import java.awt.Color;
import java.awt.Graphics;

public class Food extends Resource {

    private int nutrition;

    public Food(int quantity, int nutrition) {

        super(quantity);

        this.nutrition = nutrition;
    }

    public Food(int quantity, int nutrition, int x, int y) {

        super(quantity, x, y);

        this.nutrition = nutrition;
    }

    @Override
    public Food inventoryCopy() {
        return new Food(quantity, nutrition);
    }

    @Override
    public void use(Character target) {

        if (quantity <= 0) {
            return;
        }

        if (target instanceof Human human && human.isAlive() && human.health < 100) {

            human.health = Math.min(100, human.health + nutrition);

            human.stamina += nutrition;

            if (human.stamina > 100) {

                human.stamina = 100;
            }

            quantity--;
        }
    }

    @Override
    public void draw(Graphics g) {

        if (collected) {
            return;
        }

        g.setColor(new Color(230, 170, 60));

        g.fillRect(x, y, size, size);

        g.setColor(Color.BLACK);

        g.drawRect(x, y, size, size);
    }
}
