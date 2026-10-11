package project.game;

import java.awt.image.BufferedImage;

/** The projectile holds frame zero; impact plays the complete strip. */
final class BatAttackAnimation {
    static final int FRAME_COUNT = 10;
    static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/bat-attack.png", FRAME_COUNT);
    static final BufferedImage PROJECTILE = cropProjectile(SPRITE.frameAt(0, 1));

    private static BufferedImage cropProjectile(BufferedImage frame) {
        int left = frame.getWidth(), top = frame.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                if ((frame.getRGB(x, y) >>> 24) != 0) {
                    left = Math.min(left, x); top = Math.min(top, y);
                    right = Math.max(right, x); bottom = Math.max(bottom, y);
                }
            }
        }
        if (right < left) throw new IllegalStateException("Bat projectile frame is empty");
        return frame.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }
}
