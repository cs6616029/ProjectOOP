package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

final class UpgradeMenuOverlay {
    private static final Color GOLD = new Color(241, 197, 113);
    private static final Color WHITE = new Color(247, 235, 214);
    private static final Color MUTED = new Color(158, 164, 157);
    private static final Color PANEL = new Color(16, 22, 25);

    static void drawBackground(Graphics2D graphics, int width, int height, int level) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(new Color(5, 10, 13, 210));
        g.fillRect(0, 0, width, height);
        int x = width / 2 - 306;
        g.setColor(PANEL);
        g.fillRect(x, 36, 612, 602);
        g.setColor(new Color(97, 84, 57));
        g.drawRect(x, 36, 612, 602);
        g.setColor(GOLD);
        g.fillRect(x, 36, 22, 2);
        g.fillRect(x + 590, 636, 22, 2);
        center(g, "LEVEL UP", 46, GOLD, width / 2, 88);
        center(g, "LEVEL " + level + "  /  CHOOSE YOUR NEXT POWER", 14, WHITE, width / 2, 112);
        center(g, "CLICK A CARD OR PRESS 1 - 5", 13, MUTED, width / 2, 622);
        g.dispose();
    }

    static void drawCard(Graphics2D graphics, Rectangle r, int index, String name, String description,
            int level, boolean primary, boolean weapon, boolean available, boolean hovered) {
        Graphics2D g = (Graphics2D) graphics.create();
        Color accent = !available ? new Color(93, 99, 95) : primary ? GOLD
                : weapon ? new Color(120, 192, 193) : new Color(145, 186, 125);
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRect(r.x + 3, r.y + 4, r.width, r.height);
        g.setColor(hovered ? new Color(37, 45, 40) : primary && available ? new Color(37, 35, 27) : new Color(22, 29, 30));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(hovered ? GOLD : accent);
        g.drawRect(r.x, r.y, r.width, r.height);
        g.fillRect(r.x, r.y, 3, r.height);
        g.setColor(available && primary ? GOLD : new Color(43, 53, 48));
        g.fillRect(r.x + 14, r.y + 14, 44, 44);
        center(g, Integer.toString(index + 1), 28, primary && available ? PANEL : accent, r.x + 36, r.y + 44);
        String title = name.toUpperCase() + "  " + (available ? "LV " + level + " > " + (level + 1) : "MAX");
        text(g, title, 22, available ? WHITE : MUTED, r.x + 76, r.y + 29);
        String detail = available ? description : "Maximum level reached. Choose another power.";
        text(g, detail, 16, MUTED, r.x + 76, r.y + 55);
        String badge = !available ? "MAX" : primary ? "MAIN" : weapon ? "SKILL" : "PASSIVE";
        g.setColor(accent);
        int badgeWidth = 128, badgeHeight = 32;
        int badgeX = r.x + r.width - badgeWidth - 12, badgeY = r.y + 7;
        g.fillRect(badgeX, badgeY, badgeWidth, badgeHeight);
        g.setFont(GameStartScreen.pixelFont(28));
        int badgeBaseline = badgeY + (badgeHeight - g.getFontMetrics().getHeight()) / 2
                + g.getFontMetrics().getAscent();
        center(g, badge, 28, PANEL, badgeX + badgeWidth / 2, badgeBaseline);
        g.dispose();
    }

    private static void text(Graphics2D g, String text, int size, Color color, int x, int y) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setFont(GameStartScreen.pixelFont(size));
        g.setColor(color);
        g.drawString(text, x, y);
    }

    private static void center(Graphics2D g, String text, int size, Color color, int x, int y) {
        g.setFont(GameStartScreen.pixelFont(size));
        text(g, text, size, color, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
