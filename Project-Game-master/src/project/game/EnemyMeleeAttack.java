package project.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;

/** A locked, dodgeable swing driven by the gameplay clock. */
final class EnemyMeleeAttack {
    private static final long WINDUP = 280, SWING = 150, RECOVERY = 350;
    private long started = -1, nextAttack, time;
    private double angle;

    boolean isActive() { return started >= 0; }

    private double reach(Enemy enemy, Player player) {
        return enemy.size * 0.25 + player.size * 0.20 + 18;
    }

    int update(Enemy enemy, Player player, long now, boolean casting) {
        time = now;
        if (enemy.hp <= 0 || enemy.isBat()) { started = -1; return 0; }
        if (started >= 0) {
            long previousAge = lastAge;
            long age = now - started;
            lastAge = age;
            if (age >= WINDUP + SWING + RECOVERY) started = -1;
            if (previousAge < WINDUP && age >= WINDUP && age < WINDUP + SWING) {
                double dx = player.x - enemy.x, dy = player.y - enemy.y;
                double difference = Math.atan2(Math.sin(Math.atan2(dy, dx) - angle),
                        Math.cos(Math.atan2(dy, dx) - angle));
                if (Math.hypot(dx, dy) <= reach(enemy, player) && Math.abs(difference) <= Math.toRadians(65)) {
                    return enemy.damage;
                }
            }
            return 0;
        }
        if (!casting && now >= nextAttack && Math.hypot(player.x - enemy.x, player.y - enemy.y) <= reach(enemy, player)) {
            started = now;
            lastAge = 0;
            angle = Math.atan2(player.y - enemy.y, player.x - enemy.x);
            nextAttack = now + switch (enemy.type) {
                case FAST, BERSERK, SHADOW -> 900;
                case TANK, BOSS -> 1300;
                default -> 1100;
            };
        }
        return 0;
    }

    private long lastAge;

    double offset() {
        if (started < 0) return 0;
        double age = time - started;
        if (age < WINDUP) return -8 * Math.sin(age / WINDUP * Math.PI / 2);
        if (age < WINDUP + SWING) return 16 * Math.sin((age - WINDUP) / SWING * Math.PI);
        return 0;
    }

    double offsetX() { return Math.cos(angle) * offset(); }
    double offsetY() { return Math.sin(angle) * offset(); }

    double squash() {
        return started >= 0 && time - started < WINDUP ? 1 - 0.10 * (time - started) / WINDUP : 1;
    }

    void draw(Graphics2D graphics, Enemy enemy, double cameraX, double cameraY) {
        if (started < 0 || enemy.hp <= 0) return;
        long age = time - started;
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(enemy.x - cameraX, enemy.y - cameraY);
        if (age < WINDUP) {
            // Small anticipation mark stays above the monster, away from its hit area.
            g.setColor(new Color(255, 190, 90));
            g.fillRect(-2, -enemy.size / 2 - 14, 4, 8);
            g.fillRect(-2, -enemy.size / 2 - 3, 4, 3);
        } else if (age < WINDUP + SWING) {
            double progress = (age - WINDUP) / (double) SWING;
            double radius = enemy.size * 0.25 + 18;
            g.rotate(angle);
            int alpha = (int) (220 * (1 - progress));
            g.setColor(new Color(255, 195, 115, alpha));
            g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(-radius, -radius, radius * 2, radius * 2,
                    -65 + progress * 35, 110, Arc2D.OPEN));
            g.setColor(new Color(255, 245, 215, alpha));
            g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(-radius, -radius, radius * 2, radius * 2,
                    -65 + progress * 35, 110, Arc2D.OPEN));
        }
        g.dispose();
    }
}
