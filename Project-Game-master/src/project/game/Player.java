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
                "/project/game/Monster/Player/ass south walk.png",
                "/project/game/Monster/Player/ass north walk.png",
                2),
        IRON_GUARDIAN(
                "Iron Guardian",
                "/project/game/Monster/Player/ironmancenter.png",
                "/project/game/Monster/Player/ironmanwalk.png",
                "/project/game/Monster/Player/ironmancenter.png",
                "/project/game/Monster/Player/ironman north walk.png",
                2),
        STRONG_BOY(
                "Strong Boy",
                "/project/game/Monster/Player/Strongman.png",
                "/project/game/Monster/Player/Strongman-South-East.png",
                "/project/game/Monster/Player/Strongman.png",
                "/project/game/Monster/Player/Strongman-North.png",
                2);

        private final String displayName;
        private final String idleSpriteResource;
        private final String walkSpriteResource;
        private final String southWalkSpriteResource;
        private final String northWalkSpriteResource;
        private final int idleFrameIndex;

        CharacterType(String displayName, String idleSpriteResource, String walkSpriteResource,
                String southWalkSpriteResource, String northWalkSpriteResource, int idleFrameIndex) {
            this.displayName = displayName;
            this.idleSpriteResource = idleSpriteResource;
            this.walkSpriteResource = walkSpriteResource;
            this.southWalkSpriteResource = southWalkSpriteResource;
            this.northWalkSpriteResource = northWalkSpriteResource;
            this.idleFrameIndex = idleFrameIndex;
        }

        public String getDisplayName() {
            return displayName;
        }

        int getPrimaryWeaponIndex() {
            return switch (this) {
                case SURVIVOR -> 0;
                case IRON_GUARDIAN -> 2;
                case STRONG_BOY -> 1;
            };
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
    private final SpriteAnimation northWalkSprite;
    private final int idleFrameIndex;
    private final double spriteScale;
    private BufferedImage lastWalkingFrame;
    private boolean lastWalkingFrameMirrored;

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
        spriteScale = characterType == CharacterType.STRONG_BOY ? 1.5 : 1.0;
        idleSprite = SpriteAnimation.load(characterType.getIdleSpriteResource(), WALK_FRAME_COUNT);
        walkSprite = SpriteAnimation.load(characterType.getWalkSpriteResource(), WALK_FRAME_COUNT);
        southWalkSprite = characterType.southWalkSpriteResource == null ? walkSprite
                : SpriteAnimation.load(characterType.southWalkSpriteResource, WALK_FRAME_COUNT);
        northWalkSprite = characterType.northWalkSpriteResource == null ? walkSprite
                : SpriteAnimation.load(characterType.northWalkSpriteResource, WALK_FRAME_COUNT);
        idleFrameIndex = characterType.idleFrameIndex;
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

    int getRenderSize() {
        return (int) Math.round(size * spriteScale);
    }

    public void draw(Graphics2D g, double camX, double camY) {
        int screenX = (int) (x - camX);
        int screenY = (int) (y - camY);
        int renderSize = getRenderSize();
        boolean isInvincible = (gameTimeMillis - lastDamageTime) < invincibilityDuration;
        long currentTime = System.currentTimeMillis();
        boolean walkingSouth = moving && lastDirY > 0 && southWalkSprite != walkSprite;
        boolean walkingNorth = moving && lastDirY < 0 && northWalkSprite != walkSprite;
        BufferedImage frame;
        boolean mirrorFrame;
        if (moving) {
            SpriteAnimation sprite = walkingNorth ? northWalkSprite : (walkingSouth ? southWalkSprite : walkSprite);
            frame = sprite.frameAt(currentTime, 100);
            mirrorFrame = lastDirX < 0 && !walkingSouth && !walkingNorth;
            lastWalkingFrame = frame;
            lastWalkingFrameMirrored = mirrorFrame;
        } else if (lastWalkingFrame != null) {
            frame = lastWalkingFrame;
            mirrorFrame = lastWalkingFrameMirrored;
        } else {
            frame = idleSprite.frameAt(idleFrameIndex, 1);
            mirrorFrame = false;
        }
        boolean flash = isInvincible && (gameTimeMillis / 100) % 2 == 0;
        if (mirrorFrame) {
            g.drawImage(frame, screenX + renderSize / 2, screenY - renderSize / 2,
                    screenX - renderSize / 2, screenY + renderSize / 2,
                    0, 0, frame.getWidth(), frame.getHeight(), null);
        } else {
            g.drawImage(frame, screenX - renderSize / 2, screenY - renderSize / 2,
                    renderSize, renderSize, null);
        }
        if (flash) {
            g.setColor(new Color(255, 0, 0, 110));
            g.fillOval(screenX - renderSize / 2, screenY - renderSize / 2, renderSize, renderSize);
        }
    }
}
