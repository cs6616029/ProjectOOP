package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Uses presentation time so the title animates after gameplay stops. */
final class GameOverAnimation {
    private final long startedAtNanos;

    GameOverAnimation(long startedAtNanos) {
        this.startedAtNanos = startedAtNanos;
    }

    private static final class Frames {
        static final BufferedImage[] IMAGES = load();

        private static BufferedImage[] load() {
            try {
                BufferedImage sheet = ImageIO.read(GameOverAnimation.class.getResource("/project/game/effects/game-over.png"));
                BufferedImage[] frames = new BufferedImage[44];
                // Four 416x128 cells per row; the supplied atlas includes unused padding.
                for (int i = 0; i < frames.length; i++) {
                    frames[i] = sheet.getSubimage(i % 4 * 416, i / 4 * 128, 416, 128);
                }
                return frames;
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load game-over animation", exception);
            }
        }
    }

    void draw(Graphics2D graphics, int centerX, int topY, long nowNanos) {
        long elapsedMillis = Math.max(0L, nowNanos - startedAtNanos) / 1_000_000L;
        // Hold the complete title before the atlas's fade-out, keeping the death screen readable.
        int frame = (int) Math.min(elapsedMillis / 50, 27);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Frames.IMAGES[frame], centerX - 208, topY, 416, 128, null);
        g.dispose();
    }
}
