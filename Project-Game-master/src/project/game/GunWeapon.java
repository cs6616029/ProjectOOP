package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GunWeapon extends Weapon {
    private long lastAttackTime = 0;
    private final long cooldown = 1000; // ทุกๆ 1 วินาที
    private final List<Bullet> bullets = new ArrayList<>();

    private static final class BulletAnimation {
        static final BufferedImage[] FRAMES = loadFrames();

        private static BufferedImage[] loadFrames() {
            SpriteAnimation sprite = SpriteAnimation.load("/project/game/weapons/Gun/bullet.png", 22);
            BufferedImage[] frames = new BufferedImage[22];
            for (int i = 0; i < frames.length; i++) {
                BufferedImage frame = sprite.frameAt(i, 1);
                int left = frame.getWidth(), top = frame.getHeight(), right = -1, bottom = -1;
                for (int y = 0; y < frame.getHeight(); y++) {
                    for (int x = 0; x < frame.getWidth(); x++) {
                        if ((frame.getRGB(x, y) >>> 24) != 0) {
                            left = Math.min(left, x);
                            top = Math.min(top, y);
                            right = Math.max(right, x);
                            bottom = Math.max(bottom, y);
                        }
                    }
                }
                frames[i] = right < left ? frame : frame.getSubimage(left, top, right - left + 1, bottom - top + 1);
            }
            return frames;
        }
    }

    public GunWeapon(int level) {
        super("Gun", "Fires 2 rapid bullets at the nearest enemy", level, 14, 4);
    }

    private static class Bullet {
        final ShotBurst burst;
        double x, y, dirX, dirY;
        double speed = 12.0;
        int damage = 15;
        boolean active = true;
        final long spawnedAt;
        long animationTime;

        Bullet(double x, double y, double dirX, double dirY, int damage, long spawnedAt, ShotBurst burst) {
            this.burst = burst;
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.damage = damage;
            this.spawnedAt = spawnedAt;
        }

        void update(long currentTime) {
            animationTime = Math.max(0L, currentTime - spawnedAt);
            x += dirX * speed;
            y += dirY * speed;
        }
    }

    private static class ShotBurst {
        Enemy firstEnemy;
        double impactX, impactY;
    }

    private static double contactTime(double startX, double startY, double endX, double endY, Enemy enemy) {
        double radius = enemy.size / 2.0 + 4;
        double offsetX = startX - enemy.x, offsetY = startY - enemy.y;
        double outside = offsetX * offsetX + offsetY * offsetY - radius * radius;
        if (outside <= 0) return 0;
        double dx = endX - startX, dy = endY - startY;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) return Double.POSITIVE_INFINITY;
        double projection = offsetX * dx + offsetY * dy;
        double discriminant = projection * projection - lengthSquared * outside;
        if (discriminant < 0) return Double.POSITIVE_INFINITY;
        double time = (-projection - Math.sqrt(discriminant)) / lengthSquared;
        return time >= 0 && time <= 1 ? time : Double.POSITIVE_INFINITY;
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
                    ShotBurst burst = new ShotBurst();

                    // นัดที่ 1
                    bullets.add(new Bullet(player.x, player.y, dirX, dirY, dmg, currentTime, burst));
                    
                    // นัดที่ 2 (ยิงเหลื่อมออกมานิดหน่อย)
                    bullets.add(new Bullet(player.x - dirX * 15, player.y - dirY * 15, dirX, dirY, dmg, currentTime, burst));
                }
            }
            lastAttackTime = currentTime;
        }

        // อัปเดตตำแหน่งกระสุนและตรวจชน
        Iterator<Bullet> bIt = bullets.iterator();
        while (bIt.hasNext()) {
            Bullet b = bIt.next();
            double previousX = b.x, previousY = b.y;
            b.update(currentTime);

            // ชนศัตรู
            Enemy firstHit = null;
            double firstContact = Double.POSITIVE_INFINITY;
            // Following bullets keep the first impact as their stop, even after that enemy is removed.
            List<Enemy> collisionTargets = b.burst.firstEnemy == null ? enemies : List.of(b.burst.firstEnemy);
            for (Enemy enemy : collisionTargets) {
                if (enemy.hp > 0 || enemy == b.burst.firstEnemy) {
                    double contact = contactTime(previousX, previousY, b.x, b.y, enemy);
                    if (contact < firstContact) {
                        firstContact = contact;
                        firstHit = enemy;
                    }
                }
            }
            if (b.burst.firstEnemy != null) {
                double stopTime = ((b.burst.impactX - previousX) * b.dirX
                        + (b.burst.impactY - previousY) * b.dirY) / b.speed;
                if (stopTime <= 1 && stopTime < firstContact) {
                    firstHit = null;
                    b.active = false;
                }
            }
            if (firstHit != null) {
                b.x = previousX + (b.x - previousX) * firstContact;
                b.y = previousY + (b.y - previousY) * firstContact;
                if (b.burst.firstEnemy == null) {
                    b.burst.firstEnemy = firstHit;
                    b.burst.impactX = b.x;
                    b.burst.impactY = b.y;
                }
                if (firstHit.hp > 0) {
                    firstHit.takeDamage(b.damage);
                    visualEffects.add(VisualEffect.bulletHit(b.x, b.y));
                }
                b.active = false;
            } else if (b.burst.firstEnemy != null
                    && (b.x - b.burst.impactX) * b.dirX + (b.y - b.burst.impactY) * b.dirY >= 0) {
                // If the first enemy moves away, the rest of the burst still cannot pass the impact point.
                b.active = false;
            }

            // ลบเมื่อชน หรือ บินไกลเกินไป
            if (!b.active || Math.hypot(b.x - player.x, b.y - player.y) > 800) {
                bIt.remove();
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        for (Bullet b : bullets) {
            int sx = (int) (b.x - camX);
            int sy = (int) (b.y - camY);
            BufferedImage frame = BulletAnimation.FRAMES[(int) Math.min(b.animationTime / 50, 21)];
            int width = frame.getWidth() * 2;
            int height = frame.getHeight() * 2;
            Graphics2D spriteGraphics = (Graphics2D) g.create();
            spriteGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            spriteGraphics.translate(sx, sy);
            spriteGraphics.rotate(Math.atan2(b.dirY, b.dirX));
            spriteGraphics.drawImage(frame, -width, -height / 2, width, height, null);
            spriteGraphics.dispose();
        }
    }
}
