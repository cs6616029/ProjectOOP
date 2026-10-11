package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.List;

public class LaserWeapon extends Weapon {
    private long lastAttackTime = 0;
    private final long cooldown = 800; // คูลดาวน์พื้นฐาน
    private boolean isShooting = false;
    private long shootStartTime = 0;
    private final long shootDuration = 100; // แสดงเส้นเลเซอร์ 0.1 วินาที

    private double startX, startY, endX, endY;

    public LaserWeapon(int level) {
        super("Laser", "Fires a high-energy laser beam at the nearest enemy", level, 26, 8);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Range", 288, 28);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return; // ถ้า level 0 จะยังไม่ทำงาน

        long effectiveCooldown = (long) (cooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            Enemy target = findClosestEnemy(player, enemies); // ค้นหาศัตรูที่ใกล้ที่สุด
            if (target != null) {
                double dx = target.x - player.x;
                double dy = target.y - player.y;
                double len = Math.hypot(dx, dy);

                double laserRange = scaleStat(288, 28);
                if (len > 0 && len <= laserRange + target.size / 2.0) {
                    startX = player.x;
                    startY = player.y;
                    endX = player.x + (dx / len) * laserRange;
                    endY = player.y + (dy / len) * laserRange;

                    int damage = getDamage();
                    Line2D laserLine = new Line2D.Double(startX, startY, endX, endY);

                    // ทำดาเมจศัตรูทุกตัวที่ตัดผ่านเส้นเลเซอร์
                    for (Enemy enemy : enemies) {
                        if (enemy.hp > 0 && laserLine.ptSegDist(enemy.x, enemy.y) <= enemy.size / 2.0 + scaleStat(4, 2) / 2.0 + 4) {
                            enemy.takeDamage(damage);
                            visualEffects.add(VisualEffect.laserHit(enemy.x, enemy.y, scaleStat(51, 7)));
                        }
                    }

                   

                    isShooting = true;
                    shootStartTime = currentTime;
                    SoundEffects.play(SoundEffects.Effect.LASER);
                }
            }
            lastAttackTime = currentTime;
        }

        if (isShooting && currentTime - shootStartTime > shootDuration) {
            isShooting = false;
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (level <= 0 || !isShooting) return;

        int sx1 = (int) (startX - camX);
        int sy1 = (int) (startY - camY);
        int sx2 = (int) (endX - camX);
        int sy2 = (int) (endY - camY);

        // วาดเส้นเลเซอร์สีแดงพร้อมประกายสีขาวตรงกลาง
        g.setColor(Color.RED);
        g.setStroke(new java.awt.BasicStroke(scaleStat(4, 2)));
        g.drawLine(sx1, sy1, sx2, sy2);

        g.setColor(Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(scaleStat(2, 1)));
        g.drawLine(sx1, sy1, sx2, sy2);

        g.setStroke(new java.awt.BasicStroke(1)); // คืนค่าความหนาเส้น
    }
}
