package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

public class Enemy {
    private static final int WALK_FRAME_COUNT = 8;
    private static final SpriteAnimation[] NORMAL_SPRITES = new SpriteAnimation[] {
        SpriteAnimation.load("/project/game/Monster/Normal/Slime_1.png", WALK_FRAME_COUNT),
        SpriteAnimation.load("/project/game/Monster/Normal/dog.png", WALK_FRAME_COUNT),
        SpriteAnimation.load("/project/game/Monster/Normal/meow.png", WALK_FRAME_COUNT),
        SpriteAnimation.load("/project/game/Monster/Normal/yel.png", WALK_FRAME_COUNT),
        SpriteAnimation.load("/project/game/Monster/Normal/South-East.png", WALK_FRAME_COUNT)
    };
    private static final SpriteAnimation[] FAST_SPRITES = new SpriteAnimation[] {
        SpriteAnimation.load("/project/game/Monster/Fast/South-East_3.png", WALK_FRAME_COUNT),
        SpriteAnimation.load("/project/game/Monster/Fast/South-East_7.png", WALK_FRAME_COUNT)
    };
    private static final SpriteAnimation TANK_SPRITE = SpriteAnimation.load("/project/game/Monster/Tank/South-East_4.png", WALK_FRAME_COUNT);
    private static final SpriteAnimation[] BOSS_SPRITES = new SpriteAnimation[] {
        SpriteAnimation.load("/project/game/Monster/Boss/South-East_10.png", WALK_FRAME_COUNT),
        SpriteAnimation.load("/project/game/Monster/Boss/dragon_1.png", WALK_FRAME_COUNT),
        SpriteAnimation.load("/project/game/Monster/Boss/Ogre.png", WALK_FRAME_COUNT),
        SpriteAnimation.load("/project/game/Monster/Boss/Skeleton.png", WALK_FRAME_COUNT)
    };

    public double x, y;
    public int size, hp, maxHp, damage, scoreValue;
    public double speed;
    public EnemyType type;
    private final SpriteAnimation sprite;
    private boolean facingLeft;
    private final BossSkill bossSkill;

    public Enemy(double x, double y, EnemyType type, int playerLevel) {
        this(x, y, type, playerLevel, null);
    }

    Enemy(double x, double y, EnemyType type, int playerLevel, BossSkill.Kind bossKind) {
        this.x = x;
        this.y = y;
        this.type = type;
        if (type == EnemyType.BOSS) {
            BossSkill.Kind[] kinds = BossSkill.Kind.values();
            bossSkill = new BossSkill(bossKind == null ? kinds[(int) (Math.random() * kinds.length)] : bossKind);
        } else {
            bossSkill = null;
        }
        this.sprite = selectSprite(type);

        switch (type) {
            case FAST:
                this.size = 100;
                this.hp = 3 + (playerLevel / 2);
                this.speed = 3.1;
                this.damage = 3;
                this.scoreValue = 20;
                break;
            case TANK:
                this.size = 150;
                this.hp = 10 + playerLevel * 2;
                this.speed = 1.0;
                this.damage = 4;
                this.scoreValue = 40;
                break;
            case BOSS:
                this.size = 250;
                this.hp = 60 + (playerLevel * 10);
                this.speed = 1.3;
                this.damage = 7;
                this.scoreValue = 240;
                break;
            case NORMAL:
            default:
                this.size = 70;
                this.hp = 4 + (playerLevel / 2);
                this.speed = 1.9;
                this.damage = 3;
                this.scoreValue = 12;
                break;
        }
        this.maxHp = this.hp;
    }

    public void update(double targetX, double targetY) {
        facingLeft = targetX < x;
        if (bossSkill != null && (bossSkill.isCharging() || bossSkill.isWindingUp())) return;
        double angle = Math.atan2(targetY - y, targetX - x);
        x += Math.cos(angle) * speed;
        y += Math.sin(angle) * speed;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
    }

    void updateBossSkill(Player player, List<BossAttack> attacks, long time) {
        if (bossSkill != null) bossSkill.update(this, player, attacks, time);
    }

    int getContactDamage() {
        return bossSkill != null && bossSkill.isCharging() ? 14 : damage;
    }

    void drawBossWarning(Graphics2D g, double camX, double camY) {
        if (bossSkill != null && hp > 0) bossSkill.drawWarning(g, camX, camY);
    }

    public void draw(Graphics2D g, double camX, double camY) {
        int screenX = (int) (x - camX);
        int screenY = (int) (y - camY);
        BufferedImage frame = getSprite().frameAt(System.currentTimeMillis(), 110);
        if (facingLeft) {
            g.drawImage(frame, screenX + size / 2, screenY - size / 2,
                    screenX - size / 2, screenY + size / 2,
                    0, 0, frame.getWidth(), frame.getHeight(), null);
        } else {
            g.drawImage(frame, screenX - size / 2, screenY - size / 2,
                    size, size, null);
        }

        if (type == EnemyType.BOSS) {
            g.setColor(Color.WHITE);
            g.drawString(bossSkill.kind.name, screenX - 30, screenY - size / 2 - 18);
            g.setColor(Color.RED);
            g.fillRect(screenX - 30, screenY - size / 2 - 12, 60, 6);
            g.setColor(Color.GREEN);
            g.fillRect(screenX - 30, screenY - size / 2 - 12, (int) (60 * ((double) hp / maxHp)), 6);
            g.setColor(Color.WHITE);
            g.drawRect(screenX - 30, screenY - size / 2 - 12, 60, 6);
        }
    }

    private SpriteAnimation selectSprite(EnemyType type) {
        switch (type) {
            case FAST:
                return FAST_SPRITES[(int) (Math.random() * FAST_SPRITES.length)];
            case TANK:
                return TANK_SPRITE;
            case BERSERK:
            case SHADOW:
                return FAST_SPRITES[(int) (Math.random() * FAST_SPRITES.length)];
            case BOSS:
                return BOSS_SPRITES[bossSkill.kind.ordinal()];
            case NORMAL:
            default:
                return NORMAL_SPRITES[(int) (Math.random() * NORMAL_SPRITES.length)];
        }
    }

    private SpriteAnimation getSprite() {
        return sprite;
    }
}
