package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** A one-shot celebration using presentation time, including while gameplay is paused. */
final class LevelUpEffect {
    static final int FRAME_COUNT = 44;
    static final int FRAME_MILLIS = 45;
    static final int DURATION_MILLIS = FRAME_COUNT * FRAME_MILLIS;
    private final long startedAtNanos;

    LevelUpEffect(long startedAtNanos) {
        this.startedAtNanos = startedAtNanos;
    }

    private static final class Frames {
        static final BufferedImage[] IMAGES = load();

        private static BufferedImage[] load() {
            try {
                BufferedImage sheet = ImageIO.read(LevelUpEffect.class.getResource("/project/game/effects/level-up.png"));
                // The supplied atlas has six 320x128 cells per row and unused right padding.
                BufferedImage[] frames = new BufferedImage[FRAME_COUNT];
                for (int i = 0; i < frames.length; i++) {
                    frames[i] = sheet.getSubimage(i % 6 * 320, i / 6 * 128, 320, 128);
                }
                return frames;
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load level-up animation", exception);
            }
        }
    }

    boolean isFinished(long nowNanos) {
        return elapsedMillis(nowNanos) >= DURATION_MILLIS;
    }

    private long elapsedMillis(long nowNanos) {
        return Math.max(0L, nowNanos - startedAtNanos) / 1_000_000L;
    }

    void draw(Graphics2D g, Player player, double cameraX, double cameraY,
            boolean choosingSkill, long nowNanos) {
        if (isFinished(nowNanos)) return;
        BufferedImage frame = Frames.IMAGES[(int) (elapsedMillis(nowNanos) / FRAME_MILLIS)];
        int width = 200;
        int height = 80;
        int bottom = (int) (player.y - cameraY) - player.getRenderSize() / 2 - 8;
        // Keep the text above the first card while the selection menu covers the character.
        if (choosingSkill) bottom = Math.min(bottom, 196);
        Graphics2D overlay = (Graphics2D) g.create();
        overlay.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        overlay.drawImage(frame, (int) (player.x - cameraX) - width / 2, bottom - height, width, height, null);
        overlay.dispose();
    }
}
