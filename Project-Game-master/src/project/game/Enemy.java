package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

public class Enemy {
    static final int BOSS_BASE_HEALTH = 1000;
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
    private long nextRangedAttack = -1;
    private final List<DamageNumber> pendingDamageNumbers = new ArrayList<>();
    private int damageNumberSequence;
    private final EnemyMeleeAttack meleeAttack = new EnemyMeleeAttack();
    private long statusTime, frozenUntil;
    private long airborneStart = -1, airborneUntil;
    private int frostHits;
    private BufferedImage frozenFrame;
    private static final java.util.Map<BufferedImage, BufferedImage> FROZEN_FRAMES = new java.util.IdentityHashMap<>();

    void updateStatus(long time) { statusTime = time; }

    boolean isFrozen() { return hp > 0 && statusTime < frozenUntil; }

    boolean isAirborne() { return hp > 0 && airborneStart >= 0 && statusTime < airborneUntil; }

    private boolean isImmobilized() { return isFrozen() || isAirborne(); }

    void applyAirborne(long time) {
        updateStatus(time);
        if (hp <= 0) return;
        if (!isAirborne()) airborneStart = time;
        airborneUntil = time + 750;
        meleeAttack.cancelUntil(airborneUntil + 200);
        if (bossSkill != null) bossSkill.cancelUntil(airborneUntil + 500);
        nextRangedAttack = Math.max(nextRangedAttack, airborneUntil + 500);
    }

    double airborneHeight() {
        if (!isAirborne()) return 0;
        double progress = (statusTime - airborneStart) / (double) (airborneUntil - airborneStart);
        return Math.sin(Math.PI * progress) * (type == EnemyType.BOSS ? 28 : 48);
    }

    void applyFrostHit(long time) {
        updateStatus(time);
        if (hp <= 0) return;
        if (type == EnemyType.BOSS && ++frostHits < 2) return;
        frostHits = 0;
        if (!isFrozen()) frozenFrame = sprite.frameAt(time, 110);
        frozenUntil = time + (type == EnemyType.BOSS ? 1200 : 2000);
        meleeAttack.cancelUntil(frozenUntil + 200);
        if (bossSkill != null) bossSkill.cancelUntil(frozenUntil + 500);
        nextRangedAttack = Math.max(nextRangedAttack, frozenUntil + 500);
    }

    private static BufferedImage iceTint(BufferedImage source) {
        return FROZEN_FRAMES.computeIfAbsent(source, frame -> {
            BufferedImage tinted = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < frame.getHeight(); y++) for (int x = 0; x < frame.getWidth(); x++) {
                int pixel = frame.getRGB(x, y);
                int brightness = (((pixel >> 16) & 255) + ((pixel >> 8) & 255) + (pixel & 255)) / 3;
                tinted.setRGB(x, y, (pixel & 0xff000000) | ((brightness / 4) << 16)
                        | ((100 + brightness / 2) << 8) | (170 + brightness / 3));
            }
            return tinted;
        });
    }

    void takeDamage(int amount) {
        if (hp <= 0 || amount <= 0) return;
        hp -= amount;
        // Stagger simultaneous hits so separate skills remain readable.
        int offset = damageNumberSequence++ % 3 - 1;
        pendingDamageNumbers.add(new DamageNumber(x + offset * 18, y - size * 0.25, amount));
    }

    void collectDamageNumbers(List<DamageNumber> numbers) {
        numbers.addAll(pendingDamageNumbers);
        pendingDamageNumbers.clear();
    }

    boolean isBat() {
        return sprite == FAST_SPRITES[1];
    }

    SoundEffects.Effect deathSound() {
        if (type == EnemyType.BOSS) return switch (bossSkill.kind) {
            case DRAGON -> SoundEffects.Effect.DRAGON_DEATH;
            case SKELETON -> SoundEffects.Effect.SKELETON_DEATH;
            default -> SoundEffects.Effect.BOSS_DEATH;
        };
        if (type == EnemyType.TANK) return SoundEffects.Effect.CRAB_DEATH;
        if (sprite == NORMAL_SPRITES[0]) return SoundEffects.Effect.SLIME_DEATH;
        if (sprite == NORMAL_SPRITES[1]) return SoundEffects.Effect.GREEN_MONSTER_DEATH;
        if (sprite == FAST_SPRITES[0]) return SoundEffects.Effect.DOG_DEATH;
        if (sprite == NORMAL_SPRITES[4]) return SoundEffects.Effect.GOBLIN_DEATH;
        return SoundEffects.Effect.ENEMY_DEATH;
    }

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
                this.hp = 30 + playerLevel * 6;
                this.speed = 1.0;
                this.damage = 4;
                this.scoreValue = 40;
                break;
            case BOSS:
                this.size = 250;
                this.hp = BOSS_BASE_HEALTH;
                this.speed = 3.0;
                this.damage = 10;
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
        if (isImmobilized()) return;
        if (meleeAttack.isActive()) return;
        facingLeft = targetX < x;
        if (bossSkill != null && (bossSkill.isCharging() || bossSkill.isWindingUp())) return;
        double angle = Math.atan2(targetY - y, targetX - x);
        double movement = speed;
        if (type == EnemyType.BOSS && Math.hypot(targetX - x, targetY - y) > 200) {
            movement *= 1.65;
        }
        if (isBat()) {
            double distance = Math.hypot(targetX - x, targetY - y);
            if (distance < 160) movement = -speed;
            else if (distance <= 260) movement = 0;
        }
        x += Math.cos(angle) * movement;
        y += Math.sin(angle) * movement;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
    }

    boolean isTouching(Player player) {
        // Sprite canvases include transparent margins, weapons and limbs.
        // Contact damage uses the bodies, rather than the whole image rectangle.
        double enemyBodyRadius = size * 0.25;
        double playerBodyRadius = player.size * 0.20;
        return hp > 0 && Math.hypot(x - player.x, y - player.y)
                <= enemyBodyRadius + playerBodyRadius;
    }

    void updateBossSkill(Player player, List<BossAttack> attacks, long time) {
        updateStatus(time);
        if (isImmobilized()) return;
        if (bossSkill != null && !meleeAttack.isActive()) bossSkill.update(this, player, attacks, time);
    }

    void updateCombatMovement(Player player) {
        if (isImmobilized()) return;
        if (!meleeAttack.isActive()) facingLeft = player.x < x;
        if (!isBat() && Math.hypot(x - player.x, y - player.y) <= size * 0.25 + player.size * 0.20 + 14) return;
        update(player.x, player.y);
    }

    boolean isSkeletonBoss() {
        return type == EnemyType.BOSS && bossSkill.kind == BossSkill.Kind.SKELETON;
    }

    int updateMeleeAttack(Player player, long time) {
        updateStatus(time);
        if (isImmobilized()) return 0;
        return meleeAttack.update(this, player, time, bossSkill != null && bossSkill.isWindingUp());
    }

    void updateRangedAttack(Player player, List<BossAttack> attacks, long time) {
        updateStatus(time);
        if (isImmobilized()) return;
        if (!isBat() || hp <= 0) return;
        // Use gameplay time so pausing also pauses the firing cooldown.
        if (nextRangedAttack < 0) nextRangedAttack = time + 1000;
        if (time < nextRangedAttack || Math.hypot(player.x - x, player.y - y) > 450) return;
        double angle = Math.atan2(player.y - y, player.x - x);
        attacks.add(BossAttack.batEnergy(this, angle, time));
        nextRangedAttack = time + 2000;
    }

    int getContactDamage() {
        return bossSkill != null && bossSkill.isCharging() ? 14 : damage;
    }

    void drawBossWarning(Graphics2D g, double camX, double camY) {
        if (isImmobilized()) return;
        if (bossSkill != null && hp > 0) bossSkill.drawWarning(g, camX, camY);
    }

    public void draw(Graphics2D g, double camX, double camY) {
        if (isAirborne()) {
            g.setColor(new Color(0, 0, 0, 90));
            g.fillOval((int) (x - camX - size * 0.2), (int) (y - camY + size * 0.25),
                    (int) (size * 0.4), (int) (size * 0.12));
        }
        int screenX = (int) (x - camX + meleeAttack.offsetX());
        int screenY = (int) (y - camY + meleeAttack.offsetY() - airborneHeight());
        BufferedImage frame = getSprite().frameAt(meleeAttack.isActive() || isAirborne() ? 0 : System.currentTimeMillis(), 110);
        if (isFrozen()) frame = iceTint(frozenFrame);
        int renderHeight = (int) Math.round(size * meleeAttack.squash());
        int top = screenY + size / 2 - renderHeight;
        if (facingLeft) {
            g.drawImage(frame, screenX + size / 2, top,
                    screenX - size / 2, screenY + size / 2,
                    0, 0, frame.getWidth(), frame.getHeight(), null);
        } else {
            g.drawImage(frame, screenX - size / 2, top,
                    size, renderHeight, null);
        }
        meleeAttack.draw(g, this, camX, camY);

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
