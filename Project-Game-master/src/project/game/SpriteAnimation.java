package project.game;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

final class SpriteAnimation {
    private final BufferedImage[] frames;

    private SpriteAnimation(BufferedImage[] frames) {
        this.frames = frames;
    }

    static SpriteAnimation load(String resourcePath, int frameCount) {
        try {
            BufferedImage sheet = ImageIO.read(SpriteAnimation.class.getResource(resourcePath));
            if (sheet == null || frameCount < 1 || sheet.getWidth() % frameCount != 0) {
                throw new IllegalArgumentException("Invalid sprite sheet: " + resourcePath);
            }

            int frameWidth = sheet.getWidth() / frameCount;
            BufferedImage[] frames = new BufferedImage[frameCount];
            for (int index = 0; index < frameCount; index++) {
                frames[index] = sheet.getSubimage(index * frameWidth, 0, frameWidth, sheet.getHeight());
            }
            return new SpriteAnimation(frames);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load sprite sheet: " + resourcePath, exception);
        }
    }

    BufferedImage frameAt(long timeMillis, int frameDurationMillis) {
        int frameIndex = (int) ((timeMillis / frameDurationMillis) % frames.length);
        return frames[frameIndex];
    }
}