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
        return loadGrid(resourcePath, frameCount, 1);
    }

    static SpriteAnimation loadGrid(String resourcePath, int columns, int rows) {
        try {
            BufferedImage sheet = ImageIO.read(SpriteAnimation.class.getResource(resourcePath));
            if (sheet == null || columns < 1 || rows < 1 || sheet.getWidth() % columns != 0
                    || sheet.getHeight() % rows != 0) {
                throw new IllegalArgumentException("Invalid sprite sheet: " + resourcePath);
            }

            int frameWidth = sheet.getWidth() / columns;
            int frameHeight = sheet.getHeight() / rows;
            BufferedImage[] frames = new BufferedImage[columns * rows];
            for (int index = 0; index < frames.length; index++) {
                frames[index] = sheet.getSubimage((index % columns) * frameWidth,
                        (index / columns) * frameHeight, frameWidth, frameHeight);
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
