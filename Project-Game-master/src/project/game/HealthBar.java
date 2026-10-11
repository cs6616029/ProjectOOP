package project.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Pixel HUD with a clipped fill that preserves the supplied artwork. */
final class HealthBar {
    private static final BufferedImage BACKGROUND = load("UI_StatusBar_Bg.png");
    private static final BufferedImage FILL = load("UI_StatusBar_Fill_HP.png");
    private static final BufferedImage HEART = load("UI_Indicator_Heart.png");
    private static final BufferedImage XP_FILL = createXPFill();
    private static final BufferedImage XP_ICON = loadXPIcon();

    private static BufferedImage createXPFill() {
        BufferedImage blue = new BufferedImage(FILL.getWidth(), FILL.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < FILL.getHeight(); y++) {
            for (int x = 0; x < FILL.getWidth(); x++) {
                int pixel = FILL.getRGB(x, y);
                float[] hsb = Color.RGBtoHSB((pixel >>> 16) & 255, (pixel >>> 8) & 255, pixel & 255, null);
                // Keep the pixel shading and transparency of the original HUD artwork.
                int rgb = Color.HSBtoRGB(0.56f, hsb[1], Math.min(1f, hsb[2] * 1.35f));
                blue.setRGB(x, y, (pixel & 0xff000000) | (rgb & 0x00ffffff));
            }
        }
        return blue;
    }

    private static BufferedImage loadXPIcon() {
        BufferedImage image = SpriteAnimation.load("/project/game/effects/gem-green.png", 1).frameAt(0, 1);
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
        if (right < left) throw new IllegalStateException("Empty EXP gem icon");
        return image.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    private static BufferedImage load(String name) {
        var resource = HealthBar.class.getResource("/project/game/UI/" + name);
        if (resource == null) throw new IllegalStateException("Missing health bar image: " + name);
        try {
            BufferedImage image = ImageIO.read(resource);
            if (image == null) throw new IllegalStateException("Invalid health bar image: " + name);
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load health bar image: " + name, exception);
        }
    }

    static void draw(Graphics2D graphics, int x, int y, int hp, int maxHp) {
        drawStatus(graphics, x, y, hp, maxHp, true);
    }

    static void drawXP(Graphics2D graphics, int x, int y, int xp, int requiredXP, int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.scale(width / 296.0, height / 43.0);
        drawStatus(g, 0, 0, xp, requiredXP, false);
        g.dispose();
    }

    private static void drawStatus(Graphics2D graphics, int x, int y, int value, int maximum, boolean health) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(BACKGROUND, x, y, 296, 43, null);
        int fillX = x + 46, fillY = y + 7;
        double ratio = maximum <= 0 ? 0 : Math.max(0, Math.min(1, (double) value / maximum));
        Graphics2D fillGraphics = (Graphics2D) g.create();
        fillGraphics.clipRect(fillX, fillY, (int) Math.round(239 * ratio), 29);
        fillGraphics.drawImage(health ? FILL : XP_FILL, fillX, fillY, 239, 29, null);
        fillGraphics.dispose();
        if (health) {
            g.drawImage(HEART, x + 6, y + 7, 32, 29, null);
        } else {
            int iconHeight = 29;
            int iconWidth = (int) Math.round(iconHeight * XP_ICON.getWidth() / (double) XP_ICON.getHeight());
            g.drawImage(XP_ICON, x + (41 - iconWidth) / 2, y + 7, iconWidth, iconHeight, null);
        }
        String text = Math.max(0, value) + " / " + Math.max(0, maximum);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        int textX = fillX + (239 - g.getFontMetrics().stringWidth(text)) / 2;
        int baseline = fillY + (29 - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
        g.setColor(new Color(35, 15, 15));
        g.drawString(text, textX + 1, baseline + 1);
        g.setColor(Color.WHITE);
        g.drawString(text, textX, baseline);
        g.dispose();
    }
}
