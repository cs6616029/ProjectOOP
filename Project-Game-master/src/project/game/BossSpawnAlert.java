package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Color;
import java.awt.GradientPaint;

/** One-shot boss warning using gameplay time, so menus pause the animation. */
final class BossSpawnAlert {
    static final int FRAME_COUNT = 32;
    static final int FRAME_MILLIS = 75;
    static final int DURATION_MILLIS = FRAME_COUNT * FRAME_MILLIS;
    private final long startedAt;

    BossSpawnAlert(long startedAt) {
        this.startedAt = startedAt;
    }

    private static final class Frames {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/boss-alert.png", FRAME_COUNT);
    }

    boolean isFinished(long time) {
        return time - startedAt >= DURATION_MILLIS;
    }

    void drawScreenEdges(Graphics2D graphics, int width, int height, long time) {
        long age = time - startedAt;
        if (age < 0 || isFinished(time) || width <= 0 || height <= 0) return;
        double fade = Math.min(1.0, (DURATION_MILLIS - age) / 400.0);
        double pulse = 0.65 + 0.35 * Math.cos(age * Math.PI * 2 / 600.0);
        Color edge = new Color(235, 20, 35, (int) (155 * pulse * fade));
        Color clear = new Color(235, 20, 35, 0);
        int depth = Math.max(1, Math.min(110, Math.min(width, height) / 6));
        Graphics2D g = (Graphics2D) graphics.create();
        g.setPaint(new GradientPaint(0, 0, edge, depth, 0, clear));
        g.fillRect(0, 0, depth, height);
        g.setPaint(new GradientPaint(width, 0, edge, width - depth, 0, clear));
        g.fillRect(width - depth, 0, depth, height);
        g.setPaint(new GradientPaint(0, 0, edge, 0, depth, clear));
        g.fillRect(0, 0, width, depth);
        g.setPaint(new GradientPaint(0, height, edge, 0, height - depth, clear));
        g.fillRect(0, height - depth, width, depth);
        g.dispose();
    }

    void draw(Graphics2D graphics, int centerX, int centerY, long time) {
        if (isFinished(time)) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Frames.SPRITE.frameAt(Math.max(0, time - startedAt), FRAME_MILLIS),
                centerX - 216, centerY - 144, 432, 288, null);
        g.dispose();
    }
}
