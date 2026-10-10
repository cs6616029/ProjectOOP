package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GunWeapon extends Weapon {
    private long lastAttackTime = 0;
    private final long cooldown = 1000; // ทุกๆ 1 วินาที
    private final List<Bullet> bullets = new ArrayList<>();

    public GunWeapon(int level) {
        super("Gun", "Fires 2 rapid bullets at the nearest enemy", level, 14, 4);
    }

    private static class Bullet {
        double x, y, dirX, dirY;
        double speed = 12.0;
        int damage = 15;
        boolean active = true;

        Bullet(double x, double y, double dirX, double dirY, int damage) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.damage = damage;
        }

        void update() {
            x += dirX * speed;
            y += dirY * speed;
        }
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;

        long effectiveCooldown = (long) (cooldown * player.attackCooldownMultiplier);

        // ยิงกระสุน 2 นัด (นัดแรกทันที, นัดที่สองตามหลังเล็กน้อย)
        if (currentTime - lastAttackTime >= effectiveCooldown) {
            Enemy target = findClosestEnemy(player, enemies);
            if (target != null) {
                double dx = target.x - player.x;
                double dy = target.y - player.y;
                double len = Math.hypot(dx, dy);

                if (len > 0 && len <= 520) {
                    double dirX = dx / len;
                    double dirY = dy / len;
                    int dmg = getDamage();

                    // นัดที่ 1
                    bullets.add(new Bullet(player.x, player.y, dirX, dirY, dmg));
                    
                    // นัดที่ 2 (ยิงเหลื่อมออกมานิดหน่อย)
                    bullets.add(new Bullet(player.x - dirX * 15, player.y - dirY * 15, dirX, dirY, dmg));
                }
            }
            lastAttackTime = currentTime;
        }

        // อัปเดตตำแหน่งกระสุนและตรวจชน
        Iterator<Bullet> bIt = bullets.iterator();
        while (bIt.hasNext()) {
            Bullet b = bIt.next();
            b.update();

            // ชนศัตรู
            for (Enemy enemy : enemies) {
                if (enemy.hp > 0 && Math.hypot(enemy.x - b.x, enemy.y - b.y) < enemy.size / 2.0 + 4) {
                    enemy.hp -= b.damage;
                    b.active = false;
                    break;
                }
            }

            // ลบเมื่อชน หรือ บินไกลเกินไป
            if (!b.active || Math.hypot(b.x - player.x, b.y - player.y) > 800) {
                bIt.remove();
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        g.setColor(Color.YELLOW);
        for (Bullet b : bullets) {
            int sx = (int) (b.x - camX);
            int sy = (int) (b.y - camY);
            g.fillOval(sx - 3, sy - 3, 6, 6);
        }
    }
}
