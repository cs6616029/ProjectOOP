package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/** Innate attack, kept outside the upgradeable weapon slots. */
final class StrongBoyPunch {
    static final int DAMAGE = 18, REACH = 85, FRAME_MILLIS = 60, WINDUP = 180,
            IMPACT = WINDUP + FRAME_MILLIS, DURATION = WINDUP + 7 * FRAME_MILLIS;
    private static final class Animation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/strong-boy-punch.png", 7);
    }
    private long lastAttack, started, time;
    private boolean attacking, hit;
    private double x, y, angle;

    void update(Player player, List<Enemy> enemies, long now) {
        time = now;
        player.attackOffsetX = player.attackOffsetY = 0;
        if (player.hp <= 0) { attacking = false; return; }
        if (attacking) { x = player.x; y = player.y; }
        if (attacking && !hit && now - started >= IMPACT) {
            hit = true;
            for (Enemy enemy : enemies) {
                if (enemy.hp <= 0) continue;
                double dx = enemy.x - x - Math.cos(angle) * 26, dy = enemy.y - y - Math.sin(angle) * 26;
                double difference = Math.atan2(Math.sin(Math.atan2(dy, dx) - angle),
                        Math.cos(Math.atan2(dy, dx) - angle));
                if (Math.hypot(dx, dy) <= REACH + enemy.size * 0.25
                        && Math.abs(difference) <= Math.toRadians(65)) enemy.takeDamage(DAMAGE);
            }
        }
        if (attacking && now - started >= DURATION) attacking = false;
        if (attacking) {
            player.attackOffsetX = Math.cos(angle) * offset();
            player.attackOffsetY = Math.sin(angle) * offset();
        }
        if (attacking || player.hp <= 0 || now - lastAttack < Math.max(DURATION, (long) (900 * player.attackCooldownMultiplier))) return;
        Enemy closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Enemy enemy : enemies) {
            double d = Math.hypot(enemy.x - player.x, enemy.y - player.y);
            if (enemy.hp > 0 && d <= REACH + enemy.size * 0.25 && d < distance) { closest = enemy; distance = d; }
        }
        if (closest == null) return;
        x = player.x; y = player.y;
        angle = Math.atan2(closest.y - y, closest.x - x);
        lastAttack = started = now;
        attacking = true; hit = false;
    }

    private double offset() {
        if (!attacking) return 0;
        long age = time - started;
        if (age < WINDUP) return -10 * Math.sin(age / (double) WINDUP * Math.PI / 2);
        if (age < IMPACT) return -10 + 36 * Math.sin((age - WINDUP) / (double) FRAME_MILLIS * Math.PI / 2);
        if (age < IMPACT + 120) return 26 * (1 - (age - IMPACT) / 120.0);
        return 0;
    }

    void draw(Graphics2D graphics, double camX, double camY) {
        if (!attacking || time - started < WINDUP) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x - camX + Math.cos(angle) * offset(), y - camY + Math.sin(angle) * offset());
        g.rotate(angle);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Animation.SPRITE.frameAt(time - started - WINDUP, FRAME_MILLIS), -34, -64, 128, 128, null);
        g.dispose();
    }
}
