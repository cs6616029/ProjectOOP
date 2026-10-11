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

    static SpriteAnimation loadFrames(String... resourcePaths) {
        if (resourcePaths.length == 0) throw new IllegalArgumentException("Animation needs frames");
        BufferedImage[] frames = new BufferedImage[resourcePaths.length];
        for (int i = 0; i < frames.length; i++) {
            var resource = SpriteAnimation.class.getResource(resourcePaths[i]);
            if (resource == null) throw new IllegalStateException("Missing animation frame: " + resourcePaths[i]);
            try {
                frames[i] = ImageIO.read(resource);
                if (frames[i] == null) throw new IllegalStateException("Invalid animation frame: " + resourcePaths[i]);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to load animation frame: " + resourcePaths[i], exception);
            }
        }
        return new SpriteAnimation(frames);
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

    SpriteAnimation normalizeVisibleHeight(int targetHeight, int targetBaseline) {
        int maximumHeight = 0, bottomEdge = 0;
        for (BufferedImage frame : frames) {
            int top = frame.getHeight(), bottom = -1;
            for (int y = 0; y < frame.getHeight(); y++) {
                for (int x = 0; x < frame.getWidth(); x++) {
                    if ((frame.getRGB(x, y) >>> 24) != 0) {
                        top = Math.min(top, y);
                        bottom = Math.max(bottom, y);
                    }
                }
            }
            if (bottom >= top) maximumHeight = Math.max(maximumHeight, bottom - top + 1);
            bottomEdge = Math.max(bottomEdge, bottom + 1);
        }
        if (maximumHeight == 0) return this;
        double scale = targetHeight / (double) maximumHeight;
        BufferedImage[] normalized = new BufferedImage[frames.length];
        for (int i = 0; i < frames.length; i++) {
            BufferedImage frame = frames[i];
            normalized[i] = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            var g = normalized[i].createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.translate(frame.getWidth() / 2.0, targetBaseline);
            g.scale(scale, scale);
            g.drawImage(frame, -frame.getWidth() / 2, -bottomEdge, null);
            g.dispose();
        }
        return new SpriteAnimation(normalized);
    }
}
