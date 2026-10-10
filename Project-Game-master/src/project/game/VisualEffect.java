package project.game;

import java.awt.Color;
import java.awt.Graphics2D;

public class VisualEffect {
    public double x, y;
    public int radius;
    public Color color;
    public int duration; // จำนวนเฟรมที่แสดง
    private int currentFrame = 0;

    public VisualEffect(double x, double y, int radius, Color color, int duration) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.duration = duration;
    }

    public void update() {
        currentFrame++;
    }

    public boolean isFinished() {
        return currentFrame >= duration;
    }

    public void draw(Graphics2D g, double camX, double camY) {
        int sx = (int) (x - camX);
        int sy = (int) (y - camY);

        // ป้องกันค่า Alpha ติดลบเมื่อเกินเวลา
        int alpha = Math.max(0, Math.min(255, 255 - (currentFrame * 255 / duration)));
        Color fadeColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        g.setColor(fadeColor);
        g.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
    }
}