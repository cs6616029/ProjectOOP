package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

final class GameHudText {
    static void drawSkills(Graphics2D graphics, String summary, int x, int y, int width) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setFont(GameStartScreen.pixelFont(18));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int cursor = x;
        for (String skill : summary.split("\\s*\\|\\s*")) {
            if (skill.isBlank()) continue;
            int textWidth = g.getFontMetrics().stringWidth(skill);
            if (cursor > x && cursor + textWidth > x + width) {
                cursor = x;
                y += 20;
            }
            g.setColor(new Color(5, 10, 13, 210));
            g.drawString(skill, cursor + 1, y + 1);
            g.setColor(new Color(219, 226, 208));
            g.drawString(skill, cursor, y);
            cursor += textWidth + 20;
        }
        g.dispose();
    }
}
