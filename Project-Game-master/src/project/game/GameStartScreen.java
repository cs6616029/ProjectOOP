package project.game;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class GameStartScreen extends JPanel {
    private static final int DESIGN_WIDTH = 800;
    private static final int DESIGN_HEIGHT = 600;
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 675;

    private enum Page {
        START,
        PLAYER,
        MAP
    }

    private static final Color GOLD = new Color(255, 199, 74);
    private static final Color PANEL = new Color(8, 17, 31, 225);

    private static final Player.CharacterType[] CHARACTERS = Player.CharacterType.values();
    private static final MapOption[] MAPS = {
        new MapOption("Enchanted Forest", "/project/game/Map/forest.jpg"),
        new MapOption("Desert", "/project/game/Map/desert.jpg"),
        new MapOption("Fantasy Realm", "/project/game/Map/fantasy.jpg")
    };

    private final BiConsumer<Player.CharacterType, String> onGameStart;
    private final BufferedImage startBackground =
            loadImage("/project/game/startBG/23815429.png");
    private final SpriteAnimation[] characterPreviews = new SpriteAnimation[CHARACTERS.length];
    private final BufferedImage[] mapPreviews = new BufferedImage[MAPS.length];
    private final Timer animationTimer;

    private Page page = Page.START;
    private int selectedCharacter;
    private int selectedMap;
    private int hoveredIndex = -1;
    private boolean primaryButtonHovered;
    private boolean backButtonHovered;

    public GameStartScreen(BiConsumer<Player.CharacterType, String> onGameStart) {
        this.onGameStart = onGameStart;
        setPreferredSize(new java.awt.Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setFocusable(true);

        for (int index = 0; index < CHARACTERS.length; index++) {
            characterPreviews[index] = SpriteAnimation.load(CHARACTERS[index].getIdleSpriteResource(), 8);
        }
        for (int index = 0; index < MAPS.length; index++) {
            mapPreviews[index] = loadImage(MAPS[index].resource);
        }

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                handleClick(toDesignX(event.getX()), toDesignY(event.getY()));
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                updateHover(toDesignX(event.getX()), toDesignY(event.getY()));
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        animationTimer = new Timer(100, event -> {
            if (page == Page.PLAYER) {
                repaint();
            }
        });
        animationTimer.start();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.scale((double) getWidth() / DESIGN_WIDTH, (double) getHeight() / DESIGN_HEIGHT);

        drawBackground(g);
        if (page == Page.START) {
            drawStartPage(g);
        } else if (page == Page.PLAYER) {
            drawPlayerPage(g);
        } else {
            drawMapPage(g);
        }
        g.dispose();
    }

    private void drawBackground(Graphics2D g) {
        g.drawImage(startBackground, 0, 0, DESIGN_WIDTH, DESIGN_HEIGHT, null);
        g.setPaint(new GradientPaint(0, 0, new Color(5, 12, 25, 120),
                0, DESIGN_HEIGHT, new Color(3, 8, 17, 220)));
        g.fillRect(0, 0, DESIGN_WIDTH, DESIGN_HEIGHT);
    }

    private void drawStartPage(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(105, 95, 590, 390, 28, 28);

        g.setColor(GOLD);
        g.setFont(new Font("SansSerif", Font.BOLD, 17));
        drawCentered(g, "SURVIVE THE NIGHT", DESIGN_WIDTH / 2, 170);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 50));
        drawCentered(g, "VAMPIRE", DESIGN_WIDTH / 2, 235);
        drawCentered(g, "SURVIVORS", DESIGN_WIDTH / 2, 290);
        g.setColor(new Color(223, 230, 242));
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        drawCentered(g, "Choose a hero, choose a map, survive the night!", DESIGN_WIDTH / 2, 333);
        drawPrimaryButton(g, new Rectangle(295, 390, 210, 62), "Start Game");
        g.setColor(new Color(225, 230, 240, 210));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        drawCentered(g, "Move with W A S D or the arrow keys", DESIGN_WIDTH / 2, 475);
    }

    private void drawPlayerPage(Graphics2D g) {
        drawHeader(g, "Choose Your Hero", "Select a hero to begin your adventure");
        for (int index = 0; index < CHARACTERS.length; index++) {
            Rectangle card = playerCard(index);
            boolean selected = index == selectedCharacter;
            drawCard(g, card, selected, hoveredIndex == index);
            g.setColor(GOLD);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            drawCentered(g, CHARACTERS[index].getDisplayName(), card.x + card.width / 2, card.y + 42);

            BufferedImage preview = characterPreviews[index].frameAt(System.currentTimeMillis(), 180);
            int frameWidth = preview.getWidth();
            int frameHeight = preview.getHeight();
            int previewHeight = 135;
            int previewWidth = Math.max(70, previewHeight * frameWidth / frameHeight);
            g.drawImage(preview, card.x + (card.width - previewWidth) / 2,
                    card.y + 62, previewWidth, previewHeight, null);
            g.setColor(new Color(218, 226, 239));
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            String details = index == 0 ? "A fearless adventurer" : "A mighty armored guardian";
            drawCentered(g, details, card.x + card.width / 2, card.y + 220);
            if (selected) {
                drawBadge(g, card.x + card.width / 2 - 42, card.y + 226, "SELECTED");
            }
        }
        drawSecondaryButton(g, new Rectangle(48, 520, 130, 48), "Back", backButtonHovered);
        drawPrimaryButton(g, new Rectangle(622, 520, 130, 48), "Next");
    }

    private void drawMapPage(Graphics2D g) {
        drawHeader(g, "Choose Your Map", "Select where your survival begins");
        int cardWidth = 210;
        int cardHeight = 265;
        int gap = 25;
        int startX = (DESIGN_WIDTH - (cardWidth * MAPS.length + gap * (MAPS.length - 1))) / 2;
        for (int index = 0; index < MAPS.length; index++) {
            Rectangle card = new Rectangle(startX + index * (cardWidth + gap), 200, cardWidth, cardHeight);
            boolean selected = index == selectedMap;
            drawCard(g, card, selected, hoveredIndex == index);
            int imageX = card.x + 8;
            int imageY = card.y + 8;
            int imageWidth = card.width - 16;
            int imageHeight = 185;
            g.drawImage(mapPreviews[index], imageX, imageY, imageWidth, imageHeight, null);
            g.setColor(new Color(4, 10, 20, 185));
            g.fillRect(imageX, imageY + imageHeight - 34, imageWidth, 34);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            drawCentered(g, MAPS[index].name, card.x + card.width / 2, card.y + 221);
            if (selected) {
                drawBadge(g, card.x + card.width / 2 - 42, card.y + 231, "SELECTED");
            }
        }
        drawSecondaryButton(g, new Rectangle(48, 520, 130, 48), "Back", backButtonHovered);
        drawPrimaryButton(g, new Rectangle(570, 520, 182, 48), "Start Adventure");
    }

    private void drawHeader(Graphics2D g, String title, String subtitle) {
        g.setColor(new Color(5, 13, 26, 190));
        g.fillRect(0, 0, DESIGN_WIDTH, 150);
        g.setColor(GOLD);
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        drawCentered(g, title, DESIGN_WIDTH / 2, 62);
        g.setColor(new Color(220, 229, 242));
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        drawCentered(g, subtitle, DESIGN_WIDTH / 2, 98);
        g.setColor(new Color(255, 255, 255, 160));
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString(page == Page.PLAYER ? "01 / 02" : "02 / 02", DESIGN_WIDTH - 82, 38);
    }

    private void drawCard(Graphics2D g, Rectangle card, boolean selected, boolean hovered) {
        g.setColor(selected ? new Color(29, 50, 75, 245) : PANEL);
        g.fillRoundRect(card.x, card.y, card.width, card.height, 18, 18);
        g.setColor(selected ? GOLD : (hovered ? new Color(161, 184, 213) : new Color(100, 122, 151)));
        g.setStroke(new java.awt.BasicStroke(selected ? 3f : 1.2f));
        g.drawRoundRect(card.x, card.y, card.width, card.height, 18, 18);
    }

    private void drawPrimaryButton(Graphics2D g, Rectangle bounds, String label) {
        g.setColor(primaryButtonHovered ? new Color(255, 217, 117) : GOLD);
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 16, 16);
        g.setColor(new Color(39, 31, 20));
        g.setFont(new Font("SansSerif", Font.BOLD, 19));
        drawCentered(g, label, bounds.x + bounds.width / 2, bounds.y + 39);
    }

    private void drawSecondaryButton(Graphics2D g, Rectangle bounds, String label, boolean hovered) {
        g.setColor(hovered ? new Color(62, 83, 110, 240) : new Color(19, 31, 49, 220));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);
        g.setColor(new Color(214, 224, 239));
        g.setStroke(new java.awt.BasicStroke(1.2f));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        drawCentered(g, label, bounds.x + bounds.width / 2, bounds.y + 30);
    }

    private void drawBadge(Graphics2D g, int x, int y, String label) {
        g.setColor(new Color(255, 199, 74, 45));
        g.fillRoundRect(x, y, 84, 24, 12, 12);
        g.setColor(GOLD);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        drawCentered(g, label, x + 42, y + 17);
    }

    private Rectangle playerCard(int index) {
        return new Rectangle(165 + index * 270, 185, 220, 280);
    }

    private void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
    }

    private void handleClick(int x, int y) {
        if (page == Page.START) {
            if (new Rectangle(295, 390, 210, 62).contains(x, y)) {
                page = Page.PLAYER;
            }
        } else if (page == Page.PLAYER) {
            for (int index = 0; index < CHARACTERS.length; index++) {
                if (playerCard(index).contains(x, y)) {
                    selectedCharacter = index;
                    break;
                }
            }
            if (new Rectangle(48, 520, 130, 48).contains(x, y)) {
                page = Page.START;
            } else if (new Rectangle(622, 520, 130, 48).contains(x, y)) {
                page = Page.MAP;
            }
        } else {
            int cardWidth = 210;
            int gap = 25;
            int startX = (DESIGN_WIDTH - (cardWidth * MAPS.length + gap * (MAPS.length - 1))) / 2;
            for (int index = 0; index < MAPS.length; index++) {
                Rectangle card = new Rectangle(startX + index * (cardWidth + gap), 200, cardWidth, 265);
                if (card.contains(x, y)) {
                    selectedMap = index;
                    break;
                }
            }
            if (new Rectangle(48, 520, 130, 48).contains(x, y)) {
                page = Page.PLAYER;
            } else if (new Rectangle(570, 520, 182, 48).contains(x, y)) {
                MapOption map = MAPS[selectedMap];
                animationTimer.stop();
                onGameStart.accept(CHARACTERS[selectedCharacter], map.resource);
            }
        }
        updateHover(x, y);
        repaint();
    }

    private void updateHover(int x, int y) {
        hoveredIndex = -1;
        primaryButtonHovered = false;
        backButtonHovered = false;
        if (page == Page.PLAYER) {
            for (int index = 0; index < CHARACTERS.length; index++) {
                if (playerCard(index).contains(x, y)) {
                    hoveredIndex = index;
                }
            }
            primaryButtonHovered = new Rectangle(622, 520, 130, 48).contains(x, y);
            backButtonHovered = new Rectangle(48, 520, 130, 48).contains(x, y);
        } else if (page == Page.MAP) {
            int cardWidth = 210;
            int gap = 25;
            int startX = (DESIGN_WIDTH - (cardWidth * MAPS.length + gap * (MAPS.length - 1))) / 2;
            for (int index = 0; index < MAPS.length; index++) {
                Rectangle card = new Rectangle(startX + index * (cardWidth + gap), 200, cardWidth, 265);
                if (card.contains(x, y)) {
                    hoveredIndex = index;
                }
            }
            primaryButtonHovered = new Rectangle(570, 520, 182, 48).contains(x, y);
            backButtonHovered = new Rectangle(48, 520, 130, 48).contains(x, y);
        } else {
            primaryButtonHovered = new Rectangle(295, 390, 210, 62).contains(x, y);
        }
        setCursor(Cursor.getPredefinedCursor(
                hoveredIndex >= 0 || primaryButtonHovered || backButtonHovered
                        ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    private int toDesignX(int x) {
        return x * DESIGN_WIDTH / Math.max(1, getWidth());
    }

    private int toDesignY(int y) {
        return y * DESIGN_HEIGHT / Math.max(1, getHeight());
    }

    private static BufferedImage loadImage(String resourcePath) {
        try {
            var resource = GameStartScreen.class.getResource(resourcePath);
            if (resource == null) {
                throw new IllegalStateException("Unable to find menu image: " + resourcePath);
            }
            BufferedImage image = ImageIO.read(resource);
            if (image == null) {
                throw new IllegalStateException("Unsupported menu image: " + resourcePath);
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load menu image: " + resourcePath, exception);
        }
    }

    private static final class MapOption {
        private final String name;
        private final String resource;

        private MapOption(String name, String resource) {
            this.name = name;
            this.resource = resource;
        }
    }
}
