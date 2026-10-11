package project.game;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Gem {
    private static final BufferedImage NORMAL_ICON = loadIcon("/project/game/effects/gem-green.png");
    private static final BufferedImage BOSS_ICON = loadIcon("/project/game/effects/gem-red.png");
    public double x, y;
    public int size, xpValue;
    public boolean isBossGem;

    public Gem(double x, double y, int xpValue, boolean isBossGem) {
        this.x = x;
        this.y = y;
        this.xpValue = xpValue;
        this.isBossGem = isBossGem;
        this.size = isBossGem ? 14 : 8;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
    }

    private static BufferedImage loadIcon(String path) {
        try {
            BufferedImage image = ImageIO.read(Gem.class.getResource(path));
            int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    if ((image.getRGB(x, y) >>> 24) != 0) {
                        left = Math.min(left, x);
                        top = Math.min(top, y);
                        right = Math.max(right, x);
                        bottom = Math.max(bottom, y);
                    }
                }
            }
            if (right < left) throw new IllegalStateException("Empty gem icon: " + path);
            return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load gem icon: " + path, exception);
        }
    }

    public void draw(Graphics2D g, double camX, double camY) {
        int screenX = (int) (x - camX);
        int screenY = (int) (y - camY);

        BufferedImage icon = isBossGem ? BOSS_ICON : NORMAL_ICON;
        int width = size * 2;
        int height = (int) Math.round(width * icon.getHeight() / (double) icon.getWidth());
        Graphics2D spriteGraphics = (Graphics2D) g.create();
        spriteGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int shadowWidth = width + 4;
        int shadowHeight = Math.max(4, width / 3);
        spriteGraphics.setColor(new Color(0, 0, 0, 90));
        spriteGraphics.fillOval(screenX - shadowWidth / 2,
                screenY + height / 2 - shadowHeight / 2, shadowWidth, shadowHeight);
        spriteGraphics.drawImage(icon, screenX - width / 2, screenY - height / 2, width, height, null);
        spriteGraphics.dispose();
    }
}
