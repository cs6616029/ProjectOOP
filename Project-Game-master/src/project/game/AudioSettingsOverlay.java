package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Pixel panel and slider handles come from the supplied UI atlas. */
final class AudioSettingsOverlay {
    private static final BufferedImage SHEET = loadSheet();
    private static final Color INK = new Color(68, 49, 44);
    private boolean open;
    private int dragging = -1, selected;
    private double grabOffset;

    boolean isOpen() { return open; }
    void open() { open = true; dragging = -1; selected = 0; }
    void close() { open = false; dragging = -1; }

    static Rectangle sliderBounds(int width, int height, int row) {
        return new Rectangle(width / 2 - 48, height / 2 - 63 + row * 80, 220, 40);
    }
    static Rectangle backBounds(int width, int height) {
        return new Rectangle(width / 2 - 100, height / 2 + 110, 200, 42);
    }
    private static Rectangle sliderHitBounds(int width, int height, int row) {
        Rectangle bounds = sliderBounds(width, height, row);
        bounds.grow(28, 14);
        return bounds;
    }
    boolean isInteractive(int x, int y, int width, int height) {
        return backBounds(width, height).contains(x, y)
                || sliderHitBounds(width, height, 0).contains(x, y) || sliderHitBounds(width, height, 1).contains(x, y);
    }
    void mousePressed(int x, int y, int width, int height) {
        if (backBounds(width, height).contains(x, y)) {
            close(); SoundEffects.play(SoundEffects.Effect.MENU_CLICK); return;
        }
        for (int row = 0; row < 2; row++) {
            if (sliderHitBounds(width, height, row).contains(x, y)) {
                dragging = selected = row;
                Rectangle track = sliderBounds(width, height, row);
                double knobCenter = track.x + Math.round(track.width * volume(row));
                grabOffset = Math.abs(x - knobCenter) <= 28 ? x - knobCenter : 0;
                if (grabOffset == 0) setFromMouse(x, width, height);
                return;
            }
        }
    }
    void mouseDragged(int x, int width, int height) { if (dragging >= 0) setFromMouse(x, width, height); }
    void mouseReleased() { dragging = -1; grabOffset = 0; }
    private void setFromMouse(int x, int width, int height) {
        Rectangle track = sliderBounds(width, height, dragging);
        setVolume(dragging, (x - grabOffset - track.x) / (double) track.width);
    }
    private static double volume(int row) { return row == 0 ? SoundEffects.getSoundVolume() : SoundEffects.getMusicVolume(); }
    private static void setVolume(int row, double level) {
        if (row == 0) SoundEffects.setSoundVolume(level); else SoundEffects.setMusicVolume(level);
    }
    void keyPressed(int key) {
        switch (key) {
            case KeyEvent.VK_ESCAPE -> close();
            case KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_TAB -> selected = 1 - selected;
            case KeyEvent.VK_LEFT -> setVolume(selected, volume(selected) - .05);
            case KeyEvent.VK_RIGHT -> setVolume(selected, volume(selected) + .05);
            case KeyEvent.VK_HOME -> setVolume(selected, 0);
            case KeyEvent.VK_END -> setVolume(selected, 1);
        }
    }
    void draw(Graphics2D graphics, int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setColor(new Color(5, 10, 13, 210)); g.fillRect(0, 0, width, height);
        int x = width / 2 - 260, y = height / 2 - 180;
        g.setColor(new Color(0, 0, 0, 130)); g.fillRect(x + 8, y + 8, 520, 390);
        drawPanel(g, x, y, 520, 390);
        text(g, "AUDIO SETTINGS", 30, width / 2, y + 52, true);
        for (int row = 0; row < 2; row++) {
            Rectangle track = sliderBounds(width, height, row);
            double value = volume(row);
            text(g, row == 0 ? "Sound" : "Music", 26, x + 44, track.y + 29, false);
            g.setColor(new Color(98, 73, 60)); g.fillRect(track.x, track.y + 16, track.width, 8);
            g.setColor(new Color(237, 161, 50)); g.fillRect(track.x, track.y + 16, (int) Math.round(track.width * value), 8);
            int knobX = track.x + (int) Math.round(track.width * value) - 20;
            // Plain yellow tile from the supplied atlas is the slider knob.
            g.drawImage(SHEET, knobX, track.y, knobX + 40, track.y + 40, 48, 112, 64, 128, null);
            if (selected == row) { g.setColor(INK); g.drawRect(knobX - 2, track.y - 2, 43, 43); }
            text(g, value == 0 ? "OFF" : Math.round(value * 100) + "%", 16, x + 466, track.y + 27, true);
        }
        text(g, "Sound: effects   Music: map songs", 14, width / 2, y + 261, true);
        Rectangle back = backBounds(width, height);
        PauseMenuOverlay.drawButton(g, back, "< BACK", false, false);
        text(g, "DRAG OR USE ARROWS  /  ESC TO GO BACK", 12, width / 2, y + 367, true);
        g.dispose();
    }

    private static void text(Graphics2D g, String value, int size, int x, int baseline, boolean centered) {
        g.setFont(GameStartScreen.pixelFont(size)); g.setColor(INK);
        g.drawString(value, centered ? x - g.getFontMetrics().stringWidth(value) / 2 : x, baseline);
    }
    private static void drawPanel(Graphics2D g, int x, int y, int width, int height) {
        int[] source = {96, 104, 136, 144};
        int[] sourceY = {80, 88, 120, 128};
        int[] destX = {x, x + 24, x + width - 24, x + width};
        int[] destY = {y, y + 24, y + height - 24, y + height};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            g.drawImage(SHEET, destX[col], destY[row], destX[col + 1], destY[row + 1],
                    source[col], sourceY[row], source[col + 1], sourceY[row + 1], null);
        }
    }
    private static BufferedImage loadSheet() {
        try { return ImageIO.read(AudioSettingsOverlay.class.getResource("/project/game/effects/audio-settings-ui.png")); }
        catch (IOException exception) { throw new IllegalStateException("Unable to load audio settings UI", exception); }
    }
}
