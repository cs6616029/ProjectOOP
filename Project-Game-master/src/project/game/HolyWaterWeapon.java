package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HolyWaterWeapon extends Weapon {
    private long lastAttackTime = 0;
    private final long cooldown = 2500;
    private final List<HolyZone> zones = new ArrayList<>();

    public HolyWaterWeapon(int level) {
        super("Holy Water", "Throws holy water creating damaging pools on the ground for 2s", level, 12, 4);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Radius", 60, 10);
    }

    private static class HolyZone {
        double x, y;
        int radius;
        int damage;
        long spawnTime;
        long duration = 2000; // อยู่ได้ 2 วินาที (2000ms)
        long lastDmgTime = 0;

        HolyZone(double x, double y, int radius, int damage, long spawnTime) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.damage = damage;
            this.spawnTime = spawnTime;
        }
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;

        long effectiveCooldown = (long) (cooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            int numZones = 1 + (level / 3); // เพิ่มจำนวนขวดตามเลเวล
            for (int i = 0; i < numZones; i++) {
                // สุ่มจุดตกใกล้ๆ ตัวผู้เล่น
                double offsetX = (Math.random() - 0.5) * 300;
                double offsetY = (Math.random() - 0.5) * 300;

                int radius = scaleStat(60, 10);
                int dmg = getDamage();
                zones.add(new HolyZone(player.x + offsetX, player.y + offsetY, radius, dmg, currentTime));
            }
            lastAttackTime = currentTime;
        }

        // อัปเดตโซนน้ำมนต์
        Iterator<HolyZone> zIt = zones.iterator();
        while (zIt.hasNext()) {
            HolyZone zone = zIt.next();

            // ทำดาเมจเป็นช่วงๆ ทุกๆ 0.2 วินาที
            if (currentTime - zone.lastDmgTime >= 200) {
                for (Enemy enemy : enemies) {
                    if (Math.hypot(enemy.x - zone.x, enemy.y - zone.y) <= zone.radius) {
                        enemy.takeDamage(zone.damage);
                    }
                }
                zone.lastDmgTime = currentTime;
            }

            // ครบ 2 วินาทีให้หายไป
            if (currentTime - zone.spawnTime >= zone.duration) {
                zIt.remove();
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        for (HolyZone zone : zones) {
            int sx = (int) (zone.x - camX);
            int sy = (int) (zone.y - camY);

            // วงกลมน้ำมนต์สีฟ้าโปร่งแสง
            g.setColor(new Color(0, 180, 255, 100));
            g.fillOval(sx - zone.radius, sy - zone.radius, zone.radius * 2, zone.radius * 2);
            g.setColor(Color.CYAN);
            g.drawOval(sx - zone.radius, sy - zone.radius, zone.radius * 2, zone.radius * 2);
        }
    }
}
