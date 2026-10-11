package project.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** A short, player-following sparkle animation when an XP gem is collected. */
final class XPPickupEffect {
    private static final int FRAME_COUNT = 24;
    private static final int FRAME_MILLIS = 45;
    private final long startedAtNanos;

    XPPickupEffect(long startedAtNanos) {
        this.startedAtNanos = startedAtNanos;
    }

    private static final class Frames {
        static final BufferedImage[] IMAGES = load();

        private static BufferedImage[] load() {
            try {
                BufferedImage sheet = ImageIO.read(XPPickupEffect.class.getResource("/project/game/effects/xp-pickup.png"));
                // The atlas has five 96x96 cells per row, with padding and an unused last cell.
                BufferedImage[] frames = new BufferedImage[FRAME_COUNT];
                for (int i = 0; i < frames.length; i++) {
                    frames[i] = sheet.getSubimage(i % 5 * 96, i / 5 * 96, 96, 96);
                }
                return frames;
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load XP pickup animation", exception);
            }
        }
    }

    private long elapsedMillis(long nowNanos) {
        return Math.max(0L, nowNanos - startedAtNanos) / 1_000_000L;
    }

    boolean isFinished(long nowNanos) {
        return elapsedMillis(nowNanos) >= FRAME_COUNT * FRAME_MILLIS;
    }

    void draw(Graphics2D g, Player player, double cameraX, double cameraY, long nowNanos) {
        if (isFinished(nowNanos)) return;
        BufferedImage frame = Frames.IMAGES[(int) (elapsedMillis(nowNanos) / FRAME_MILLIS)];
        int size = (int) Math.round(player.getRenderSize() * 1.3);
        g.drawImage(frame, (int) (player.x - cameraX) - size / 2,
                (int) (player.y - cameraY) - size / 2, size, size, null);
    }
}
