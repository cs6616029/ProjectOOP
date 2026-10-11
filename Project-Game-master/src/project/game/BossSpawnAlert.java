package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

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

    void draw(Graphics2D graphics, int centerX, int centerY, long time) {
        if (isFinished(time)) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Frames.SPRITE.frameAt(Math.max(0, time - startedAt), FRAME_MILLIS),
                centerX - 216, centerY - 144, 432, 288, null);
        g.dispose();
    }
}
