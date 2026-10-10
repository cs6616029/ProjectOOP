package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

/** Boss projectiles and slam effects use the game's paused clock. */
final class BossAttack {
    private final Enemy owner;
    private final double velocityX, velocityY;
    private final int radius, damage;
    private final Color color;
    private final long expiresAt;
    private final boolean slam;
    private long lastUpdateTime;
    private boolean finished, impactChecked;
    private double x, y;

    private BossAttack(Enemy owner, double x, double y, double angle, double speed,
            int radius, int damage, Color color, long time, boolean slam) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.velocityX = Math.cos(angle) * speed;
        this.velocityY = Math.sin(angle) * speed;
        this.radius = radius;
        this.damage = damage;
        this.color = color;
        this.slam = slam;
        lastUpdateTime = time;
        expiresAt = time + (slam ? 450 : 3500);
    }

    static BossAttack projectile(Enemy owner, double angle, double speed, int radius,
            int damage, Color color, long time) {
        return new BossAttack(owner, owner.x, owner.y, angle, speed, radius, damage, color, time, false);
    }

    static BossAttack slam(Enemy owner, double x, double y, long time) {
        return new BossAttack(owner, x, y, 0, 0, 200, 16, Color.ORANGE, time, true);
    }

    int update(Player player, long time) {
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
            return damage;
        }
        return 0;
    }

    boolean isFinished() { return finished; }

    void draw(Graphics2D graphics, double camX, double camY) {
        if (finished || owner.hp <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        int sx = (int) (x - camX), sy = (int) (y - camY);
        g.setColor(slam ? new Color(255, 150, 30, 90) : color);
        g.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
        g.setColor(Color.WHITE);
        g.drawOval(sx - radius, sy - radius, radius * 2, radius * 2);
        g.dispose();
    }
}
