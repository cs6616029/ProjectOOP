package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.List;

public class SpearWeapon extends Weapon {
    private long lastAttackTime = 0;
    private final long cooldown = 1200;
    private boolean isStabbing = false;
    private long stabStartTime = 0;
    private final long stabDuration = 150;

    private double startX, startY, endX, endY;

    public SpearWeapon(int level) {
        super("Spear", "Thrusts a narrow straight spear toward the nearest enemy", level, 32, 10);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Range", 138, 18);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;

        long effectiveCooldown = (long) (cooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            Enemy target = findClosestEnemy(player, enemies);
            if (target != null) {
                double dx = target.x - player.x;
                double dy = target.y - player.y;
                double len = Math.hypot(dx, dy);

                double spearLength = scaleStat(138, 18);
                if (len > 0 && len <= spearLength + target.size / 2.0) {
                    startX = player.x;
                    startY = player.y;
                    endX = player.x + (dx / len) * spearLength;
                    endY = player.y + (dy / len) * spearLength;

                    int damage = getDamage();
                    Line2D spearLine = new Line2D.Double(startX, startY, endX, endY);

                    for (Enemy enemy : enemies) {
                        if (spearLine.ptSegDist(enemy.x, enemy.y) <= enemy.size / 2.0 + 5) {
                            enemy.takeDamage(damage);
                        }
                    }

                    isStabbing = true;
                    stabStartTime = currentTime;
                }
            }
            lastAttackTime = currentTime;
        }

        if (isStabbing && currentTime - stabStartTime > stabDuration) {
            isStabbing = false;
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (isStabbing) {
            g.setColor(Color.CYAN);
            int sx1 = (int) (startX - camX);
            int sy1 = (int) (startY - camY);
            int sx2 = (int) (endX - camX);
            int sy2 = (int) (endY - camY);

            g.setStroke(new java.awt.BasicStroke(scaleStat(5, 1)));
            g.drawLine(sx1, sy1, sx2, sy2);
            g.setStroke(new java.awt.BasicStroke(1));
        }
    }
}
