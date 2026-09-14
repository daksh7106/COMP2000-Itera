package itera.model;

import java.awt.Color;
import java.awt.Graphics;

public class ConvenienceStore extends Building {

    public ConvenienceStore(int x, int y) {

        super(15, x, y, 180, 180, "CONVENIENCE STORE");

        for (int i = 0; i < 9; i++)
            stock.add(new Food(1, 20, x + 40 + (i % 3) * 35, y + 65 + (i / 3) * 30));
    }

    /**
     * Removes food from the store's stock.
     *
     * @return the next food item, or {@code null} when none is available
     */
    public Food getFood() {

        Resource resource = loot();

        if (resource instanceof Food food) {
            return food;
        }

        return null;
    }

    @Override
    protected void drawBuildingBody(Graphics g) {

        g.setColor(new Color(255, 245, 200));

        g.fillRect(x, y, width, height);
    }
}
