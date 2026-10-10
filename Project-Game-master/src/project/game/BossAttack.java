package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Boss projectiles and slam effects use the game's paused clock. */
final class BossAttack {
    static final int SWORD_WAVE_WIDTH = 160;
    static final int SWORD_WAVE_RADIUS = 56;
    private final Enemy owner;
    private final double velocityX, velocityY;
    private final int radius, damage;
    private final Color color;
    private final long expiresAt;
    private final boolean slam;
    private final boolean swordWave;
    private final long spawnTime;
    private long lastUpdateTime;
    private boolean finished, impactChecked;
    private boolean dragonFire;
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
        expiresAt = time + (slam ? 450 : 3500);
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

    private static final class SwordWaveAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load(
                "/project/game/Monster/Boss/effects/Effect_spear.png", 3);
    }

    static BossAttack slam(Enemy owner, double x, double y, long time) {
        return new BossAttack(owner, x, y, 0, 0, 200, 16, Color.ORANGE, time, true, false);
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
        if (slam) {
            if (impactChecked) return 0;
            impactChecked = true;
            return Math.hypot(player.x - x, player.y - y) <= radius + player.size / 2.0 ? damage : 0;
        }
        double oldX = x, oldY = y;
        double seconds = Math.max(0, time - lastUpdateTime) / 1000.0;
        lastUpdateTime = time;
        x += velocityX * seconds;
        y += velocityY * seconds;
        if (Line2D.ptSegDist(oldX, oldY, x, y, player.x, player.y) <= radius + player.size / 2.0) {
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
