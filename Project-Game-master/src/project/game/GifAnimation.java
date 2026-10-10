package project.game;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

final class GifAnimation {
    private final BufferedImage[] frames;
    private final int[] frameEndTimes;
    private final int durationMillis;

    private GifAnimation(BufferedImage[] frames, int[] frameEndTimes, int durationMillis) {
        this.frames = frames;
        this.frameEndTimes = frameEndTimes;
        this.durationMillis = durationMillis;
    }

    static GifAnimation load(String resourcePath) {
        var resource = GifAnimation.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Unable to find GIF animation: " + resourcePath);
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(resource.openStream())) {
            if (input == null) {
                throw new IllegalStateException("Unable to open GIF animation: " + resourcePath);
            }
            ImageReader reader = ImageIO.getImageReaders(input).next();
            try {
                reader.setInput(input, false, false);
                int frameCount = reader.getNumImages(true);
                if (frameCount < 1) {
                    throw new IllegalStateException("GIF animation has no frames: " + resourcePath);
                }

                int canvasWidth = 0;
                int canvasHeight = 0;
                IIOMetadata streamMetadata = reader.getStreamMetadata();
                if (streamMetadata != null) {
                    Node descriptor = findNode(
                            streamMetadata.getAsTree(streamMetadata.getNativeMetadataFormatName()),
                            "LogicalScreenDescriptor");
                    canvasWidth = getIntAttribute(descriptor, "logicalScreenWidth", 0);
                    canvasHeight = getIntAttribute(descriptor, "logicalScreenHeight", 0);
                }

                List<FrameData> frameData = new ArrayList<>(frameCount);
                for (int index = 0; index < frameCount; index++) {
                    BufferedImage image = reader.read(index);
                    IIOMetadata metadata = reader.getImageMetadata(index);
                    Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());
                    Node descriptor = findNode(root, "ImageDescriptor");
                    int x = getIntAttribute(descriptor, "imageLeftPosition", 0);
                    int y = getIntAttribute(descriptor, "imageTopPosition", 0);
                    Node control = findNode(root, "GraphicControlExtension");
                    int delay = getIntAttribute(control, "delayTime", 10) * 10;
                    String disposal = getStringAttribute(control, "disposalMethod", "none");
                    canvasWidth = Math.max(canvasWidth, x + image.getWidth());
                    canvasHeight = Math.max(canvasHeight, y + image.getHeight());
                    frameData.add(new FrameData(image, x, y, Math.max(20, delay), disposal));
                }

                BufferedImage canvas = new BufferedImage(
                        canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                List<BufferedImage> frames = new ArrayList<>(frameCount);
                List<Integer> frameEndTimes = new ArrayList<>(frameCount);
                int totalDuration = 0;
                BufferedImage previousCanvas = null;
                FrameData previousFrame = null;

                for (FrameData frame : frameData) {
                    if (previousFrame != null) {
                        if ("restoreToBackgroundColor".equals(previousFrame.disposal)) {
                            Graphics2D clear = canvas.createGraphics();
                            clear.setComposite(AlphaComposite.Clear);
                            clear.fillRect(previousFrame.x, previousFrame.y,
                                    previousFrame.image.getWidth(), previousFrame.image.getHeight());
                            clear.dispose();
                        } else if ("restoreToPrevious".equals(previousFrame.disposal)
                                && previousCanvas != null) {
                            canvas = copyImage(previousCanvas);
                        }
                    }

                    if ("restoreToPrevious".equals(frame.disposal)) {
                        previousCanvas = copyImage(canvas);
                    } else {
                        previousCanvas = null;
                    }

                    Graphics2D graphics = canvas.createGraphics();
                    if (frame.image.getWidth() == canvasWidth && frame.image.getHeight() == canvasHeight) {
                        graphics.drawImage(frame.image, 0, 0, null);
                    } else {
                        graphics.drawImage(frame.image, frame.x, frame.y, null);
                    }
                    graphics.dispose();

                    frames.add(copyImage(canvas));
                    totalDuration += frame.delayMillis;
                    frameEndTimes.add(totalDuration);
                    previousFrame = frame;
                }

                int[] endTimes = new int[frameEndTimes.size()];
                for (int index = 0; index < endTimes.length; index++) {
                    endTimes[index] = frameEndTimes.get(index);
                }
                return new GifAnimation(frames.toArray(new BufferedImage[0]), endTimes, totalDuration);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load GIF animation: " + resourcePath, exception);
        }
    }

    BufferedImage frameAt(long elapsedMillis) {
        int time = (int) Math.max(0, Math.min(elapsedMillis, durationMillis - 1L));
        for (int index = 0; index < frameEndTimes.length; index++) {
            if (time < frameEndTimes[index]) {
                return frames[index];
            }
        }
        return frames[frames.length - 1];
    }

    int getDurationMillis() {
        return durationMillis;
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static Node findNode(Node node, String name) {
        if (node == null) {
            return null;
        }
        if (name.equals(node.getNodeName())) {
            return node;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node match = findNode(child, name);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static int getIntAttribute(Node node, String name, int fallback) {
        String value = getStringAttribute(node, name, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Invalid GIF metadata attribute " + name + ": " + value, exception);
        }
    }

    private static String getStringAttribute(Node node, String name, String fallback) {
        if (node == null) {
            return fallback;
        }
        NamedNodeMap attributes = node.getAttributes();
        Node attribute = attributes == null ? null : attributes.getNamedItem(name);
        return attribute == null ? fallback : attribute.getNodeValue();
    }

    private static final class FrameData {
        private final BufferedImage image;
        private final int x;
        private final int y;
        private final int delayMillis;
        private final String disposal;

        private FrameData(BufferedImage image, int x, int y, int delayMillis, String disposal) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.delayMillis = delayMillis;
            this.disposal = disposal;
        }
    }
}
