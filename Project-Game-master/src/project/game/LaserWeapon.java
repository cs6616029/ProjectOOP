package project.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class LaserWeapon extends Weapon {
    private static final double TRAVEL_SPEED = 900; // Pixels per second of gameplay time.
    private static final long IMPACT_HOLD_MILLIS = 100;
    private static final double BEAM_LENGTH = 72;
    private long lastAttackTime;
    private final long cooldown = 800;
    private boolean isShooting, stopped;
    private long lastTravelTime, stoppedAt;
    private double startX, startY, endX, endY, dirX, dirY, traveled, range;
    private int shotDamage, beamWidth, impactRadius;

    public LaserWeapon(int level) {
        super("Laser", "Fires a traveling laser that stops at the first enemy", level, 26, 8);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Range", 288, 28);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> effects, long currentTime) {
        if (level <= 0) return;
        if (isShooting && stopped && currentTime - stoppedAt >= IMPACT_HOLD_MILLIS) isShooting = false;
        long effectiveCooldown = (long) (cooldown * player.attackCooldownMultiplier);
        if (!isShooting && currentTime - lastAttackTime >= effectiveCooldown) {
            Enemy target = findClosestEnemy(player, enemies);
            if (target != null) {
                double chestX = player.x, chestY = player.y - player.getRenderSize() * 0.04;
                double dx = target.x - chestX, dy = target.y - chestY;
                double distance = Math.hypot(dx, dy);
                double shotRange = scaleStat(288, 28);
                if (distance <= shotRange + target.size / 2.0) {
                    dirX = distance == 0 ? 1 : dx / distance;
                    dirY = distance == 0 ? 0 : dy / distance;
                    double muzzleOffset = Math.min(player.getRenderSize() * 0.18, distance * 0.25);
                    startX = endX = chestX + dirX * muzzleOffset;
                    startY = endY = chestY + dirY * muzzleOffset;
                    range = shotRange;
                    traveled = 0;
                    shotDamage = getDamage();
                    beamWidth = scaleStat(4, 2);
                    impactRadius = scaleStat(51, 7);
                    lastTravelTime = currentTime;
                    stopped = false;
                    isShooting = true;
                    SoundEffects.play(SoundEffects.Effect.LASER);
                }
            }
            lastAttackTime = currentTime;
        }
        if (!isShooting || stopped) return;

        double nextDistance = Math.min(range, traveled + TRAVEL_SPEED * Math.max(0, currentTime - lastTravelTime) / 1000.0);
        lastTravelTime = currentTime;
        double nextX = startX + dirX * nextDistance, nextY = startY + dirY * nextDistance;
        Enemy firstHit = null;
        double earliestContact = Double.POSITIVE_INFINITY;
        for (Enemy enemy : enemies) {
            if (enemy.hp <= 0) continue;
            double contact = contactTime(endX, endY, nextX, nextY, enemy);
            if (contact < earliestContact) {
                earliestContact = contact;
                firstHit = enemy;
            }
        }
        if (firstHit != null) {
            endX += (nextX - endX) * earliestContact;
            endY += (nextY - endY) * earliestContact;
            firstHit.takeDamage(shotDamage);
            effects.add(VisualEffect.laserHit(firstHit.x, firstHit.y, impactRadius));
            stopped = true;
            stoppedAt = currentTime;
        } else {
            endX = nextX;
            endY = nextY;
            traveled = nextDistance;
            if (traveled >= range) {
                stopped = true;
                stoppedAt = currentTime;
            }
        }
    }

    private double contactTime(double x1, double y1, double x2, double y2, Enemy enemy) {
        double radius = enemy.size / 2.0 + beamWidth / 2.0 + 4;
        double ox = x1 - enemy.x, oy = y1 - enemy.y;
        double outside = ox * ox + oy * oy - radius * radius;
        if (outside <= 0) return 0;
        double dx = x2 - x1, dy = y2 - y1;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) return Double.POSITIVE_INFINITY;
        double projection = ox * dx + oy * dy;
        double discriminant = projection * projection - lengthSquared * outside;
        if (discriminant < 0) return Double.POSITIVE_INFINITY;
        double time = (-projection - Math.sqrt(discriminant)) / lengthSquared;
        return time >= 0 && time <= 1 ? time : Double.POSITIVE_INFINITY;
    }

    @Override
    public void draw(Graphics2D graphics, Player player, double camX, double camY) {
        if (level <= 0 || !isShooting || (startX == endX && startY == endY)) return;
        Graphics2D g = (Graphics2D) graphics.create();
        double length = Math.hypot(endX - startX, endY - startY);
        double tailDistance = Math.max(0, length - BEAM_LENGTH);
        int x1 = (int) Math.round(startX + dirX * tailDistance - camX);
        int y1 = (int) Math.round(startY + dirY * tailDistance - camY);
        int x2 = (int) Math.round(endX - camX), y2 = (int) Math.round(endY - camY);
        g.setColor(new Color(255, 40, 35, 100));
        g.setStroke(new BasicStroke(beamWidth + 4, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.drawLine(x1, y1, x2, y2);
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(beamWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.drawLine(x1, y1, x2, y2);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(1, beamWidth / 2f), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.drawLine(x1, y1, x2, y2);
        g.dispose();
    }
}
