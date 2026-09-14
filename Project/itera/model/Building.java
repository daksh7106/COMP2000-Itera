package itera.model;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;

public abstract class Building {

    protected int capacity;

    protected int x;
    protected int y;

    protected int width;
    protected int height;

    protected String name;

    protected ArrayList<Resource> stock = new ArrayList<>();

    public Building(int capacity, int x, int y, int width, int height, String name) {

        this.capacity = capacity;

        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;

        this.name = name;
    }

    public boolean isSecure() {
        return true;
    }

    public boolean contains(int objectX, int objectY) {

        return objectX >= x && objectX <= x + width && objectY >= y && objectY <= y + height;
    }

    /**
     * Removes the next resource from the building's stock.
     *
     * @return the next resource, or {@code null} when the stock is empty
     */
    public Resource loot() {

        for (int i = 0; i < stock.size(); i++) {
            if (!stock.get(i).isCollected()) return stock.remove(i);
        }
        return null;
    }

    /**
     * Gives one stocked resource to a human when an item is available.
     *
     * @param human the human receiving the resource
     */
    public void interact(Human human) {
        if (!canEnter(human) || !isInside(human)) return;
        for (Resource resource : stock) {
            if (!resource.isCollected() && resource.isNear(human) && human.canCollect(resource)) {
                human.interact(resource);
                break;
            }
        }
    }

    public boolean canEnter(Human human) {
        return human.isAlive() && (!(this instanceof PoliceStation) || human instanceof Soldier)
            && (!(this instanceof Hospital) || human instanceof Medic);
    }

    public java.util.List<Resource> getResources() {
        return java.util.Collections.unmodifiableList(stock);
    }

    public boolean isInside(Character character) {
        return character.getX() >= x + 4 && character.getY() >= y + 4
            && character.getX() + character.getSize() <= x + width - 4
            && character.getY() + character.getSize() <= y + height - 4;
    }

    public boolean overlaps(double px, double py, int size) {
        return px + size > x && px < x + width && py + size > y && py < y + height;
    }

    public int getDoorX() { return x == 0 ? x + width : x; }
    public int getDoorY() { return y + height / 2; }

    public boolean blocksMovement(double px, double py, int size, boolean allowed) {
        if (!overlaps(px, py, size)) return false;
        if (!allowed) return true;
        // Solid top, bottom and back walls, with a 50-pixel entrance facing the world.
        if (py < y + 4 || py + size > y + height - 4) return true;
        if (x == 0 ? px < x + 4 : px + size > x + width - 4) return true;
        boolean crossesDoorWall = x == 0 ? px + size > x + width - 4 : px < x + 4;
        return crossesDoorWall && (py < getDoorY() - 25 || py + size > getDoorY() + 25);
    }

    public Vector2D destination(Human human, Resource resource) {
        int side = x == 0 ? 1 : -1;
        double outsideX = getDoorX() + (side > 0 ? 24 : -24 - human.getSize());
        double doorY = getDoorY() - human.getSize() / 2.0;
        if (isInside(human)) {
            if (resource != null) return new Vector2D(resource.getX(), resource.getY());
            // Align with the door inside the building before crossing its wall.
            // A diagonal route from a corner shelf can collide with the door frame.
            if (Math.abs(human.getY() - doorY) > 1)
                return new Vector2D(human.getPosition().getX(), doorY);
            return new Vector2D(outsideX, doorY);
        }
        // Align outside the wall before approaching the entrance.
        if (Math.abs(human.getY() - doorY) > 2) {
            if (side > 0 ? human.getX() < outsideX - 2 : human.getX() > outsideX + 2)
                return new Vector2D(outsideX, human.getY());
            return new Vector2D(outsideX, doorY);
        }
        if (resource == null) return new Vector2D(outsideX, doorY);
        return new Vector2D(getDoorX() - side * 30 - human.getSize() / 2.0, doorY);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getName() {
        return name;
    }

    public void draw(Graphics g) {

        drawBuildingBody(g);

        g.setColor(Color.BLACK);

        java.awt.Graphics2D walls = (java.awt.Graphics2D) g.create();
        walls.setStroke(new java.awt.BasicStroke(4));
        walls.drawLine(x, y, x + width, y);
        walls.drawLine(x, y + height, x + width, y + height);
        int backX = x == 0 ? x : x + width;
        walls.drawLine(backX, y, backX, y + height);
        walls.drawLine(getDoorX(), y, getDoorX(), getDoorY() - 25);
        walls.drawLine(getDoorX(), getDoorY() + 25, getDoorX(), y + height);
        walls.dispose();

        g.setColor(Color.BLACK);

        g.setFont(new Font("Arial", Font.BOLD, 14));

        g.drawString(name, x + 10, y + 22);

        g.setFont(new Font("Arial", Font.PLAIN, 11));

        g.drawString("Stock: " + stock.stream().filter(r -> !r.isCollected()).count(), x + 10, y + 42);
    }

    protected abstract void drawBuildingBody(Graphics g);
}
