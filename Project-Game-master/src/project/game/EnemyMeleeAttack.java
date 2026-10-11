package project.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

/** A locked, dodgeable swing driven by the gameplay clock. */
final class EnemyMeleeAttack {
    private static final long WINDUP = 280, SWING = 150, RECOVERY = 350;
    private static final long SKELETON_DASH = 180, SKELETON_SWING = 240;
    private static final double SKELETON_DASH_REACH = 100;
    private static final class SkeletonAnimation {
        static final SpriteAnimation SLASH = SpriteAnimation.load("/project/game/effects/skeleton-melee-slash.png", 4);
    }
    private long started = -1, nextAttack, time;
    private double angle;
    private boolean skeletonAttack;
    private double dashDistance, dashTraveled;

    boolean isActive() { return started >= 0; }

    void cancelUntil(long time) {
        started = -1;
        nextAttack = Math.max(nextAttack, time);
    }

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
            long hitTime = WINDUP + (skeletonAttack ? SKELETON_DASH : 0);
            long swingTime = skeletonAttack ? SKELETON_SWING : SWING;
            if (skeletonAttack) {
                double traveled = dashDistance * Math.min(1, Math.max(0, (age - WINDUP) / (double) SKELETON_DASH));
                double step = Math.max(0, traveled - dashTraveled);
                enemy.x += Math.cos(angle) * step;
                enemy.y += Math.sin(angle) * step;
                dashTraveled = traveled;
            }
            if (age >= hitTime + swingTime + RECOVERY) started = -1;
            if (previousAge < hitTime && age >= hitTime && age < hitTime + swingTime) {
                if (skeletonAttack) SoundEffects.play(SoundEffects.Effect.SWORD);
                double dx = player.x - enemy.x, dy = player.y - enemy.y;
                double difference = Math.atan2(Math.sin(Math.atan2(dy, dx) - angle),
                        Math.cos(Math.atan2(dy, dx) - angle));
                if (Math.hypot(dx, dy) <= reach(enemy, player) && Math.abs(difference) <= Math.toRadians(65)) {
                    return enemy.damage;
                }
            }
            return 0;
        }
        boolean skeleton = enemy.isSkeletonBoss();
        double distance = Math.hypot(player.x - enemy.x, player.y - enemy.y);
        if (!casting && now >= nextAttack && distance <= reach(enemy, player) + (skeleton ? SKELETON_DASH_REACH : 0)) {
            started = now;
            lastAge = 0;
            angle = Math.atan2(player.y - enemy.y, player.x - enemy.x);
            skeletonAttack = skeleton;
            dashDistance = skeleton ? Math.min(SKELETON_DASH_REACH, Math.max(0, distance - reach(enemy, player) + 10)) : 0;
            dashTraveled = 0;
            nextAttack = now + (skeleton ? 1600 : switch (enemy.type) {
                case FAST, BERSERK, SHADOW -> 900;
                case TANK -> 1300;
                case BOSS -> 900;
                default -> 1100;
            });
        }
        return 0;
    }

    private long lastAge;

    double offset() {
        if (started < 0) return 0;
        double age = time - started;
        if (age < WINDUP) return -8 * Math.sin(age / WINDUP * Math.PI / 2);
        if (skeletonAttack) return 0;
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
        } else if (skeletonAttack) {
            long slashAge = age - WINDUP - SKELETON_DASH;
            g.rotate(angle);
            double radius = enemy.size * .25 + 18;
            if (slashAge >= 0 && slashAge < SKELETON_SWING) {
                int width = (int) Math.round(radius * 2.4);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(SkeletonAnimation.SLASH.frameAt(slashAge, 60),
                        (int) Math.round(radius * .3) - width / 2, -width / 2, width, width, null);
            } else if (slashAge < 0) {
                g.setColor(new Color(255, 239, 182, 150));
                g.setStroke(new BasicStroke(3));
                g.drawLine(-45, -12, -10, -12);
                g.drawLine(-55, 12, -20, 12);
            }
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
