package itera.model;

import java.awt.Color;
import java.awt.Graphics;

public class PoliceStation extends Building {

    public PoliceStation(int x, int y) {

        super(10, x, y, 180, 180, "POLICE STATION");

        for (int i = 0; i < 6; i++)
            stock.add(new Weapon(5, 50, 5, x + 40 + (i % 3) * 35, y + 75 + (i / 3) * 40));
    }

    /**
     * Removes a weapon from the police station's stock.
     *
     * @return the next weapon, or {@code null} when none is available
     */
    public Weapon getWeapon() {

        Resource resource = loot();

        if (resource instanceof Weapon weapon) {
            return weapon;
        }

        return null;
    }

    @Override
    protected void drawBuildingBody(Graphics g) {

        g.setColor(new Color(210, 225, 255));

        g.fillRect(x, y, width, height);
    }
}
