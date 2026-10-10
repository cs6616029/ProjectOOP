package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class Player {
    private static final int WALK_FRAME_COUNT = 8;

    public enum CharacterType {
        SURVIVOR(
                "Adventurer",
                "/project/game/Monster/Player/ass center.png",
                "/project/game/Monster/Player/ass walk.png",
                "/project/game/Monster/Player/ass south walk.png"),
        IRON_GUARDIAN(
                "Iron Guardian",
                "/project/game/Monster/Player/ironmancenter.png",
                "/project/game/Monster/Player/ironmanwalk.png",
                null);

        private final String displayName;
        private final String idleSpriteResource;
        private final String walkSpriteResource;
        private final String southWalkSpriteResource;

        CharacterType(String displayName, String idleSpriteResource, String walkSpriteResource,
                String southWalkSpriteResource) {
            this.displayName = displayName;
            this.idleSpriteResource = idleSpriteResource;
            this.walkSpriteResource = walkSpriteResource;
            this.southWalkSpriteResource = southWalkSpriteResource;
        }

        public String getDisplayName() {
            return displayName;
        }

        String getIdleSpriteResource() {
            return idleSpriteResource;
        }

        String getWalkSpriteResource() {
            return walkSpriteResource;
        }
    }

    private final SpriteAnimation idleSprite;
    private final SpriteAnimation walkSprite;
    private final SpriteAnimation southWalkSprite;

    public double x, y;
    public int size = 80;
    public double speed = 4.2;
    public int hp = 80;
    public int maxHp = 80;

    public double lastDirX = 1.0;
    public double lastDirY = 0.0;
    private boolean moving;
    
    public double attackCooldownMultiplier = 1.0;

    // เพิ่มตัวแปรเช็คการอมตะชั่วขณะเพื่อแก้ Bug โดนชนทีเดียวตาย
    public long lastDamageTime = 0;
    public long gameTimeMillis = System.currentTimeMillis();
    public final long invincibilityDuration = 450; // 0.45 วินาที

    public Player(double x, double y) {
        this(x, y, CharacterType.SURVIVOR);
    }

    public Player(double x, double y, CharacterType characterType) {
        this.x = x;
        this.y = y;
        idleSprite = SpriteAnimation.load(characterType.getIdleSpriteResource(), WALK_FRAME_COUNT);
        walkSprite = SpriteAnimation.load(characterType.getWalkSpriteResource(), WALK_FRAME_COUNT);
        southWalkSprite = characterType.southWalkSpriteResource == null ? walkSprite
                : SpriteAnimation.load(characterType.southWalkSpriteResource, WALK_FRAME_COUNT);
    }

    public void move(int dx, int dy) {
        moving = dx != 0 || dy != 0;
        if (dx != 0 || dy != 0) {
            double len = Math.hypot(dx, dy);
            lastDirX = dx / len;
            lastDirY = dy / len;

            if (dx != 0 && dy != 0) {
                double diagSpeed = speed / Math.sqrt(2);
                x += dx * diagSpeed;
                y += dy * diagSpeed;
            } else {
                x += dx * speed;
                y += dy * speed;
            }
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
    }

    public void draw(Graphics2D g, double camX, double camY) {
        int screenX = (int) (x - camX);
        int screenY = (int) (y - camY);
        boolean isInvincible = (gameTimeMillis - lastDamageTime) < invincibilityDuration;
        long currentTime = System.currentTimeMillis();
        boolean walkingSouth = moving && lastDirY > 0 && southWalkSprite != walkSprite;
        SpriteAnimation sprite = !moving ? idleSprite : (walkingSouth ? southWalkSprite : walkSprite);
        BufferedImage frame = sprite.frameAt(currentTime, moving ? 100 : 180);
        boolean flash = isInvincible && (gameTimeMillis / 100) % 2 == 0;
        if (lastDirX < 0 && !walkingSouth) {
            g.drawImage(frame, screenX + size / 2, screenY - size / 2,
                    screenX - size / 2, screenY + size / 2,
                    0, 0, frame.getWidth(), frame.getHeight(), null);
        } else {
            g.drawImage(frame, screenX - size / 2, screenY - size / 2,
                    size, size, null);
        }
        if (flash) {
            g.setColor(new Color(255, 0, 0, 110));
            g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        }
    }
}
