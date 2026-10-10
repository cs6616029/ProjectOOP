package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Gem {
    public double x, y;
    public int size, xpValue;
    public boolean isBossGem;

    public Gem(double x, double y, int xpValue, boolean isBossGem) {
        this.x = x;
        this.y = y;
        this.xpValue = xpValue;
        this.isBossGem = isBossGem;
        this.size = isBossGem ? 14 : 8;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
    }

    public void draw(Graphics2D g, double camX, double camY) {
        int screenX = (int) (x - camX);
        int screenY = (int) (y - camY);

        g.setColor(isBossGem ? Color.RED : Color.CYAN);
        g.fillRect(screenX - size / 2, screenY - size / 2, size, size);
        g.setColor(Color.WHITE);
        g.drawRect(screenX - size / 2, screenY - size / 2, size, size);
    }
}