package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Presentation time keeps the victory title moving after gameplay stops. */
final class VictoryAnimation {
    private final long startedAtNanos;

    VictoryAnimation(long startedAtNanos) { this.startedAtNanos = startedAtNanos; }

    private static final class Frames {
        static final BufferedImage[] IMAGES = load();

        private static BufferedImage[] load() {
            var resource = VictoryAnimation.class.getResource("/project/game/effects/you-won.png");
            if (resource == null) throw new IllegalStateException("Missing victory animation");
            try {
                BufferedImage sheet = ImageIO.read(resource);
                // Author's atlas: seven 288x128 cells per row, 42 frames plus padding.
                BufferedImage[] frames = new BufferedImage[42];
                for (int i = 0; i < frames.length; i++) {
                    frames[i] = sheet.getSubimage(i % 7 * 288, i / 7 * 128, 288, 128);
                }
                return frames;
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load victory animation", exception);
            }
        }
    }

    void draw(Graphics2D graphics, int centerX, int topY, long nowNanos) {
        long elapsed = Math.max(0L, nowNanos - startedAtNanos) / 1_000_000L;
        // Hold the completed title rather than letting the atlas fade it away.
        int frame = (int) Math.min(elapsed / 50, 27);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Frames.IMAGES[frame], centerX - 216, topY, 432, 192, null);
        g.dispose();
    }
}
