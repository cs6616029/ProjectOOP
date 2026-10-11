package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Enemy projectiles and boss effects use the game's paused clock. */
final class BossAttack {
    static final int SWORD_WAVE_WIDTH = 160;
    static final int SWORD_WAVE_RADIUS = 56;
    static final int OGRE_SLAM_RADIUS = 260;
    static final double OGRE_SLAM_HEIGHT_SCALE = 0.35;
    static final int LIGHTNING_RADIUS = 56;
    private static final int LIGHTNING_FRAME_DURATION = 70;
    private static final int SLAM_FRAME_COUNT = 11;
    private static final int SLAM_FRAME_DURATION = 45;
    private final Enemy owner;
    private final double velocityX, velocityY;
    private final int radius, damage;
    private final Color color;
    private long expiresAt;
    private final boolean slam;
    private final boolean swordWave;
    private final long spawnTime;
    private long lastUpdateTime;
    private boolean finished, impactChecked;
    private boolean dragonFire;
    private boolean lightningStrike;
    private double x, y;

    private BossAttack(Enemy owner, double x, double y, double angle, double speed,
            int radius, int damage, Color color, long time, boolean slam, boolean swordWave) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.velocityX = Math.cos(angle) * speed;
        this.velocityY = Math.sin(angle) * speed;
        this.radius = radius;
        this.damage = damage;
        this.color = color;
        this.slam = slam;
        this.swordWave = swordWave;
        spawnTime = time;
        lastUpdateTime = time;
        expiresAt = time + (slam ? SLAM_FRAME_COUNT * SLAM_FRAME_DURATION : 3500);
    }

    static BossAttack projectile(Enemy owner, double angle, double speed, int radius,
            int damage, Color color, long time) {
        return new BossAttack(owner, owner.x, owner.y, angle, speed, radius, damage, color, time, false, false);
    }

    static BossAttack swordWave(Enemy owner, double angle, long time) {
        return new BossAttack(owner, owner.x, owner.y, angle, 220, SWORD_WAVE_RADIUS, 10, Color.WHITE, time, false, true);
    }

    static BossAttack dragonFire(Enemy owner, double angle, long time) {
        BossAttack fire = projectile(owner, angle, 260, 14, 12, Color.ORANGE, time);
        fire.dragonFire = true;
        return fire;
    }

    static BossAttack lightning(Enemy owner, double groundX, double groundY, long time) {
        BossAttack strike = new BossAttack(owner, groundX, groundY, 0, 0,
                LIGHTNING_RADIUS, 14, Color.MAGENTA, time, false, false);
        strike.lightningStrike = true;
        strike.expiresAt = time + 7 * LIGHTNING_FRAME_DURATION;
        return strike;
    }

    private static final class LightningAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load(
                "/project/game/Monster/Boss/effects/lightning-violet.png", 7);
    }

    private static final class SwordWaveAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load(
                "/project/game/Monster/Boss/effects/Effect_spear.png", 3);
    }

    private static final class DragonFireAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load(
                "/project/game/Monster/Boss/effects/BlueFlame.png", 4);
    }

    private static final class OgreSlamAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load(
                "/project/game/Monster/Boss/effects/Free_Smoke_Fx_Pixel_05.png", SLAM_FRAME_COUNT);
    }

    static BossAttack slam(Enemy owner, double x, double y, long time) {
        return new BossAttack(owner, x, y + owner.size / 2.0, 0, 0,
                OGRE_SLAM_RADIUS, 16, Color.ORANGE, time, true, false);
    }

    int update(Player player, long time) {
        return update(player, time, null);
    }

    int update(Player player, long time, List<VisualEffect> effects) {
        if (owner.hp <= 0 || time >= expiresAt) {
            finished = true;
            return 0;
        }
        if (finished) return 0;
        if (lightningStrike) {
            lastUpdateTime = time;
            if (impactChecked || time - spawnTime < 2 * LIGHTNING_FRAME_DURATION) return 0;
            impactChecked = true;
            double feetY = player.y + player.getRenderSize() / 2.0;
            return Math.hypot(player.x - x, feetY - y) <= radius + player.size * 0.20 ? damage : 0;
        }
        if (slam) {
            lastUpdateTime = time;
            if (impactChecked) return 0;
            impactChecked = true;
            double dx = (player.x - x) / (radius + player.size / 2.0);
            double dy = (player.y + player.getRenderSize() / 2.0 - y)
                    / (radius * OGRE_SLAM_HEIGHT_SCALE + player.size / 2.0);
            return dx * dx + dy * dy <= 1 ? damage : 0;
        }
        double oldX = x, oldY = y;
        double seconds = Math.max(0, time - lastUpdateTime) / 1000.0;
        lastUpdateTime = time;
        x += velocityX * seconds;
        y += velocityY * seconds;
        double playerRadius = owner.isBat() ? player.size * 0.20 : player.size / 2.0;
        if (Line2D.ptSegDist(oldX, oldY, x, y, player.x, player.y) <= radius + playerRadius) {
            finished = true;
            if (dragonFire && effects != null) {
                double dx = x - oldX, dy = y - oldY;
                double lengthSquared = dx * dx + dy * dy;
                double progress = lengthSquared == 0 ? 0 : Math.max(0, Math.min(1,
                        ((player.x - oldX) * dx + (player.y - oldY) * dy) / lengthSquared));
                effects.add(VisualEffect.explosion(oldX + progress * dx, oldY + progress * dy, 70));
            }
            return damage;
        }
        return 0;
    }

    boolean isFinished() { return finished; }

    void draw(Graphics2D graphics, double camX, double camY) {
        if (finished || owner.hp <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        int sx = (int) (x - camX), sy = (int) (y - camY);
        if (lightningStrike) {
            BufferedImage frame = LightningAnimation.SPRITE.frameAt(lastUpdateTime - spawnTime, LIGHTNING_FRAME_DURATION);
            int size = 180;
            g.drawImage(frame, sx - size / 2, sy - size * 7 / 8, size, size, null);
            g.dispose();
            return;
        }
        if (slam) {
            BufferedImage frame = OgreSlamAnimation.SPRITE.frameAt(lastUpdateTime - spawnTime, SLAM_FRAME_DURATION);
            int width = radius * 2;
            int height = (int) Math.round(width * OGRE_SLAM_HEIGHT_SCALE);
            g.drawImage(frame, sx - width / 2, sy - height / 2, width, height, null);
            g.dispose();
            return;
        }
        if (dragonFire) {
            BufferedImage image = DragonFireAnimation.SPRITE.frameAt(lastUpdateTime - spawnTime, 90);
            int width = 64;
            int height = width * image.getHeight() / image.getWidth();
            g.translate(sx, sy);
            // The supplied flame points left; rotate its head toward its velocity.
            g.rotate(Math.atan2(velocityY, velocityX) + Math.PI);
            g.drawImage(image, -width / 2, -height / 2, width, height, null);
            g.dispose();
            return;
        }
        if (swordWave) {
            BufferedImage frame = SwordWaveAnimation.SPRITE.frameAt(lastUpdateTime - spawnTime, 90);
            int width = SWORD_WAVE_WIDTH;
            int height = width * frame.getHeight() / frame.getWidth();
            g.translate(sx, sy);
            g.rotate(Math.atan2(velocityY, velocityX) - Math.PI / 2);
            g.drawImage(frame, -width / 2, -height / 2, width, height, null);
            g.dispose();
            return;
        }
        g.setColor(slam ? new Color(255, 150, 30, 90) : color);
        g.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
        g.setColor(Color.WHITE);
        g.drawOval(sx - radius, sy - radius, radius * 2, radius * 2);
        g.dispose();
    }
}
