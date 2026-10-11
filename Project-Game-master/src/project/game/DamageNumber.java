package project.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/** A hit's damage stays at the impact position and floats upward. */
final class DamageNumber {
    private static final int DURATION = 48;
    final int damage;
    private final double x, y;
    private int age;

    DamageNumber(double x, double y, int damage) {
        this.x = x;
        this.y = y;
        this.damage = damage;
    }

    void update() { age++; }
    boolean isFinished() { return age >= DURATION; }

    void draw(Graphics2D graphics, double cameraX, double cameraY) {
        if (isFinished()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        String text = Integer.toString(damage);
        int sx = (int) Math.round(x - cameraX) - g.getFontMetrics().stringWidth(text) / 2;
        int sy = (int) Math.round(y - cameraY - age * 0.85);
        int alpha = age < 24 ? 255 : Math.max(0, (DURATION - age) * 255 / 24);
        g.setColor(new Color(20, 12, 5, alpha));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) g.drawString(text, sx + dx, sy + dy);
            }
        }
        g.setColor(new Color(255, 230, 105, alpha));
        g.drawString(text, sx, sy);
        g.dispose();
    }
}
