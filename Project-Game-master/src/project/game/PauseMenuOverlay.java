package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Pause presentation shares the start menu's pixel font and warm gold palette. */
final class PauseMenuOverlay {
    private static final Color GOLD = new Color(241, 197, 113);
    private static final Color WHITE = new Color(247, 235, 214);
    private static final Color MUTED = new Color(158, 164, 157);
    private static final Color PANEL = new Color(16, 22, 25);

    static void draw(Graphics2D graphics, int width, int height, int level, long seconds, int score,
            Rectangle resume, Rectangle restart, Rectangle menu, int hovered) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(new Color(5, 10, 13, 200));
        g.fillRect(0, 0, width, height);
        int x = width / 2 - 204, y = 128, panelWidth = 408, panelHeight = 428;
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRect(x + 7, y + 8, panelWidth, panelHeight);
        g.setColor(PANEL);
        g.fillRect(x, y, panelWidth, panelHeight);
        g.setColor(new Color(97, 84, 57));
        g.drawRect(x, y, panelWidth, panelHeight);
        g.setColor(new Color(241, 197, 113, 55));
        g.drawRect(x + 4, y + 4, panelWidth - 8, panelHeight - 8);
        g.setColor(GOLD);
        for (int cornerX : new int[]{x, x + panelWidth - 20}) {
            for (int cornerY : new int[]{y, y + panelHeight - 20}) {
                g.fillRect(cornerX, cornerY == y ? cornerY : cornerY + 18, 20, 2);
                g.fillRect(cornerX == x ? cornerX : cornerX + 18, cornerY, 2, 20);
            }
        }
        center(g, "TAKE A BREATHER", 14, MUTED, width / 2, 164);
        center(g, "PAUSED", 46, GOLD, width / 2, 213);
        g.setColor(new Color(241, 197, 113, 60));
        g.fillRect(x + 64, 228, panelWidth - 128, 1);
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
        center(g, "LV " + level + "   /   " + time + "   /   SCORE " + score, 14, WHITE, width / 2, 247);
        drawButton(g, resume, "RESUME  >", true, hovered == 0);
        drawButton(g, restart, "RESTART", false, hovered == 1);
        drawButton(g, menu, "< MAIN MENU", false, hovered == 2);
        center(g, "YOUR ADVENTURE IS WAITING", 12, MUTED, width / 2, 508);
        center(g, "ESC TO RESUME", 14, GOLD, width / 2, 535);
        g.dispose();
    }

    static void drawButton(Graphics2D graphics, Rectangle bounds, String label, boolean primary, boolean hovered) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(bounds.x, bounds.y + 4, bounds.width, bounds.height);
        g.setColor(primary ? (hovered ? new Color(255, 219, 150) : GOLD)
                : (hovered ? new Color(46, 56, 49) : new Color(22, 30, 30)));
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(primary ? new Color(255, 231, 172) : hovered ? GOLD : new Color(81, 94, 83));
        g.drawRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4);
        int fontSize = bounds.height < 40 ? 20 : 24;
        g.setFont(GameStartScreen.pixelFont(fontSize));
        int baseline = bounds.y + (bounds.height - g.getFontMetrics().getHeight()) / 2 + g.getFontMetrics().getAscent();
        center(g, label, fontSize, primary ? PANEL : WHITE, bounds.x + bounds.width / 2, baseline);
        g.dispose();
    }

    private static void center(Graphics2D g, String text, int size, Color color, int x, int baseline) {
        g.setFont(GameStartScreen.pixelFont(size));
        g.setColor(color);
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }
}
