package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

final class VictoryMenuOverlay {
    private static final Color GREEN = new Color(154, 214, 102);
    private static final Color GOLD = new Color(241, 197, 113);
    private static final Color WHITE = new Color(247, 235, 214);
    private static final Color MUTED = new Color(158, 174, 160);
    private final long startedAtNanos;
    private final VictoryAnimation title;

    VictoryMenuOverlay(long startedAtNanos) {
        this.startedAtNanos = startedAtNanos;
        title = new VictoryAnimation(startedAtNanos);
    }

    static Rectangle menuButtonBounds(int width, int height) {
        return new Rectangle(width / 2 - 164, height / 2 + 106, 328, 58);
    }

    void draw(Graphics2D graphics, int width, int height, long seconds, int score, boolean hovered, long nowNanos) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(new Color(5, 12, 12, 210));
        g.fillRect(0, 0, width, height);
        long elapsed = Math.max(0, nowNanos - startedAtNanos) / 1_000_000;
        for (int i = 0; i < 22; i++) {
            int px = (i * 173 + 41) % width;
            int py = height - (int) ((i * 97L + elapsed / 24) % height);
            g.setColor(i % 3 == 0 ? new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 110)
                    : new Color(GREEN.getRed(), GREEN.getGreen(), GREEN.getBlue(), 90));
            g.fillRect(px, py, i % 3 == 0 ? 4 : 2, i % 3 == 0 ? 4 : 2);
        }
        int x = width / 2 - 250, y = height / 2 - 244;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x + 8, y + 8, 500, 488);
        g.setColor(new Color(14, 24, 22));
        g.fillRect(x, y, 500, 488);
        g.setColor(new Color(72, 99, 72));
        g.drawRect(x, y, 500, 488);
        g.setColor(new Color(154, 214, 102, 45));
        g.drawRect(x + 5, y + 5, 490, 478);
        g.setColor(GREEN);
        for (int cx : new int[]{x, x + 480}) for (int cy : new int[]{y, y + 468}) {
            g.fillRect(cx, cy == y ? cy : cy + 18, 20, 2);
            g.fillRect(cx == x ? cx : cx + 18, cy, 2, 20);
        }
        center(g, "RUN COMPLETE", 18, MUTED, width / 2, y + 42);
        title.draw(g, width / 2, y + 36, nowNanos);
        center(g, "ALL FOUR BOSSES DEFEATED", 18, GREEN, width / 2, y + 192);
        g.setColor(new Color(154, 214, 102, 60));
        g.fillRect(x + 34, y + 212, 432, 1);
        stat(g, x + 34, y + 232, "TIME SURVIVED", String.format("%02d:%02d", seconds / 60, seconds % 60));
        stat(g, x + 258, y + 232, "FINAL SCORE", Integer.toString(score));
        PauseMenuOverlay.drawButton(g, menuButtonBounds(width, height), "< MAIN MENU", true, hovered);
        center(g, "R / PLAY AGAIN", 18, MUTED, width / 2, y + 452);
        g.dispose();
    }

    private static void stat(Graphics2D g, int x, int y, String label, String value) {
        g.setColor(new Color(21, 34, 29));
        g.fillRect(x, y, 208, 90);
        g.setColor(new Color(49, 70, 53));
        g.drawRect(x, y, 208, 90);
        center(g, label, 16, MUTED, x + 104, y + 27);
        center(g, value, 40, WHITE, x + 104, y + 64);
    }

    private static void center(Graphics2D g, String text, int size, Color color, int x, int baseline) {
        g.setFont(GameStartScreen.pixelFont(size));
        g.setColor(color);
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }
}
