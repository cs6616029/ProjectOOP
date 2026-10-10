package project.game;

import java.awt.Color;
import java.awt.Graphics2D;

public class VisualEffect {
    public double x, y;
    public int radius;
    public Color color;
    public int duration; // จำนวนเฟรมที่แสดง
    private int currentFrame = 0;
    private boolean explosion;
    private boolean bossDeath;

    private static final class BossDeathAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.loadFrames(
                "/project/game/effects/boss-death/1.png",
                "/project/game/effects/boss-death/6.png",
                "/project/game/effects/boss-death/2.png",
                "/project/game/effects/boss-death/3.png",
                "/project/game/effects/boss-death/4.png",
                "/project/game/effects/boss-death/5.png");
    }

    public static VisualEffect bossDeath(double x, double y, int bossSize) {
        VisualEffect effect = new VisualEffect(x, y, (int) Math.ceil(bossSize * 0.7), Color.ORANGE, 36);
        effect.bossDeath = true;
        return effect;
    }

    private static final class ExplosionAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.loadGrid(
                "/project/game/effects/explosion.png", 10, 2);
    }

    public static VisualEffect explosion(double x, double y, int radius) {
        VisualEffect effect = new VisualEffect(x, y, radius, Color.ORANGE, 40);
        effect.explosion = true;
        return effect;
    }

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
        if (isFinished()) return;
        int sx = (int) (x - camX);
        int sy = (int) (y - camY);
        if (bossDeath) {
            g.drawImage(BossDeathAnimation.SPRITE.frameAt(currentFrame, 6),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }
        if (explosion) {
            g.drawImage(ExplosionAnimation.SPRITE.frameAt(currentFrame, 2),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }

        // ป้องกันค่า Alpha ติดลบเมื่อเกินเวลา
        int alpha = Math.max(0, Math.min(255, 255 - (currentFrame * 255 / duration)));
        Color fadeColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        g.setColor(fadeColor);
        g.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
    }
}
