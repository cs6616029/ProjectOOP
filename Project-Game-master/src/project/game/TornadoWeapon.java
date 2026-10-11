package project.game;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TornadoWeapon extends Weapon {
    static final long ORBIT_MILLIS = 1200, FLIGHT_MILLIS = 2000, FADE_MILLIS = 320;
    static final double ORBIT_RADIUS = 105, SPEED = 400;
    private long lastAttackTime, animationTime;
    private final List<Storm> storms = new ArrayList<>();

    private static final class Animation {
        static final SpriteAnimation SPRITE = SpriteAnimation.loadFrames(
                "/project/game/effects/tornado/001.png", "/project/game/effects/tornado/002.png",
                "/project/game/effects/tornado/003.png", "/project/game/effects/tornado/004.png",
                "/project/game/effects/tornado/005.png", "/project/game/effects/tornado/006.png",
                "/project/game/effects/tornado/007.png", "/project/game/effects/tornado/008.png",
                "/project/game/effects/tornado/009.png");
    }

    private static final class Storm {
        final long born;
        final double phase;
        final int damage;
        final Set<Enemy> hit = Collections.newSetFromMap(new IdentityHashMap<>());
        double x, y, dx, dy;
        long updated, fadingAt = -1;
        boolean launched;
        Storm(long time, double phase, int damage) {
            born = updated = time; this.phase = phase; this.damage = damage;
        }
    }

    public TornadoWeapon(int level) {
        super("Tornado", "Orbits once, then hunts enemies and lifts them into the air", level, 16, 5);
    }

    int getTornadoCount() { return level <= 0 ? 0 : Math.min(3, 1 + (level - 1) / 2); }

    @Override
    public String getUpgradeDescription() {
        int next = Math.min(3, 1 + Math.min(4, level) / 2);
        return super.getUpgradeDescription() + " | Tornadoes " + getTornadoCount() + " -> " + next;
    }

    private Enemy target(Storm storm, List<Enemy> enemies) {
        Enemy closest = null;
        double distance = 650;
        for (Enemy enemy : enemies) {
            double d = Math.hypot(enemy.x - storm.x, enemy.y - storm.y);
            if (enemy.hp > 0 && !storm.hit.contains(enemy) && d < distance) { distance = d; closest = enemy; }
        }
        return closest;
    }

    private void hit(Storm storm, Enemy enemy, long time) {
        storm.hit.add(enemy);
        enemy.takeDamage(storm.damage);
        enemy.applyAirborne(time);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> effects, long time) {
        if (level <= 0) return;
        animationTime = time;
        if (storms.isEmpty() && time - lastAttackTime >= (long) (4500 * player.attackCooldownMultiplier)
                && findClosestEnemy(player, enemies) != null) {
            int count = getTornadoCount();
            for (int i = 0; i < count; i++) storms.add(new Storm(time, Math.PI * 2 * i / count, getDamage()));
            lastAttackTime = time;
        }
        Iterator<Storm> iterator = storms.iterator();
        while (iterator.hasNext()) {
            Storm storm = iterator.next();
            long age = time - storm.born;
            if (storm.fadingAt >= 0) {
                if (time - storm.fadingAt >= FADE_MILLIS) iterator.remove();
                continue;
            }
            if (age < ORBIT_MILLIS) {
                double angle = storm.phase + Math.PI * 2 * age / ORBIT_MILLIS;
                storm.x = player.x + Math.cos(angle) * ORBIT_RADIUS;
                storm.y = player.y + Math.sin(angle) * ORBIT_RADIUS;
                for (Enemy enemy : enemies) {
                    if (enemy.hp > 0 && !storm.hit.contains(enemy)
                            && Math.hypot(enemy.x - storm.x, enemy.y - storm.y) <= 22 + enemy.size * 0.25) hit(storm, enemy, time);
                }
            } else {
                if (!storm.launched) {
                    storm.x = player.x + Math.cos(storm.phase) * ORBIT_RADIUS;
                    storm.y = player.y + Math.sin(storm.phase) * ORBIT_RADIUS;
                    storm.dx = player.lastDirX; storm.dy = player.lastDirY;
                    if (Math.hypot(storm.dx, storm.dy) == 0) storm.dx = 1;
                    double directionLength = Math.hypot(storm.dx, storm.dy);
                    storm.dx /= directionLength; storm.dy /= directionLength;
                    storm.launched = true;
                }
                Enemy target = target(storm, enemies);
                if (target != null) {
                    double distance = Math.hypot(target.x - storm.x, target.y - storm.y);
                    if (distance > 0) { storm.dx = (target.x - storm.x) / distance; storm.dy = (target.y - storm.y) / distance; }
                }
                double dt = Math.max(0, time - Math.max(storm.updated, storm.born + ORBIT_MILLIS)) / 1000.0;
                double oldX = storm.x, oldY = storm.y;
                storm.x += storm.dx * SPEED * dt; storm.y += storm.dy * SPEED * dt;
                Enemy first = null; double contact = Double.POSITIVE_INFINITY;
                double vx = storm.x - oldX, vy = storm.y - oldY, length2 = vx * vx + vy * vy;
                for (Enemy enemy : enemies) {
                    if (enemy.hp <= 0 || storm.hit.contains(enemy)) continue;
                    double r = 22 + enemy.size * 0.25;
                    if (Line2D.ptSegDist(oldX, oldY, storm.x, storm.y, enemy.x, enemy.y) > r) continue;
                    double projection = length2 == 0 ? 0 : ((enemy.x - oldX) * vx + (enemy.y - oldY) * vy) / length2;
                    double perpendicular2 = Math.pow(enemy.x - oldX - projection * vx, 2)
                            + Math.pow(enemy.y - oldY - projection * vy, 2);
                    double entry = length2 == 0 ? 0 : Math.max(0, projection - Math.sqrt(Math.max(0, r * r - perpendicular2) / length2));
                    if (entry < contact) { contact = entry; first = enemy; }
                }
                if (first != null) {
                    storm.x = oldX + vx * contact; storm.y = oldY + vy * contact;
                    hit(storm, first, time); storm.fadingAt = time;
                } else if (age >= ORBIT_MILLIS + FLIGHT_MILLIS) storm.fadingAt = time;
            }
            storm.updated = time;
        }
    }

    @Override
    public void draw(Graphics2D graphics, Player player, double camX, double camY) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (Storm storm : storms) {
            long frameTime = storm.fadingAt >= 0 ? 5 * 80 + animationTime - storm.fadingAt
                    : (animationTime - storm.born) % (5 * 80);
            int screenX = (int) Math.round(storm.x - camX);
            int screenY = (int) Math.round(storm.y - camY);
            double opacity = storm.fadingAt < 0 ? 1
                    : Math.max(0, 1 - (animationTime - storm.fadingAt) / (double) FADE_MILLIS);
            g.setColor(new Color(0, 0, 0, (int) (85 * opacity)));
            g.fillOval(screenX - 26, screenY + 9, 52, 14);
            g.drawImage(Animation.SPRITE.frameAt(frameTime, 80),
                    screenX - 48, screenY - 76, 96, 96, null);
        }
        g.dispose();
    }
}
