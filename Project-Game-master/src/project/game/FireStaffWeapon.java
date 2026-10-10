package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;

public class FireStaffWeapon extends Weapon {
    private static final BufferedImage FIRE_IMAGE = loadFireImage();
    private long lastAttackTime = 0;
    private final long cooldown = 1800;
    private final List<Fireball> fireballs = new ArrayList<>();

    public FireStaffWeapon(int level) {
        super("Fire Staff", "Launches a fireball at nearest enemy that explodes on hit", level);
    }

    private static BufferedImage loadFireImage() {
        String resourcePath = "/project/game/weapons/Firestaff/Fire.png";
        var resource = FireStaffWeapon.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Unable to find fire staff image: " + resourcePath);
        }

        try {
            BufferedImage image = ImageIO.read(resource);
            if (image == null) {
                throw new IllegalStateException("Unsupported fire staff image: " + resourcePath);
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load fire staff image: " + resourcePath, exception);
        }
    }

    private static class Fireball {
        double x, y, dirX, dirY;
        double speed = 7.0;
        int damage = 1;
        int explosionRadius = 70;

        Fireball(double x, double y, double dirX, double dirY, int damage, int radius) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.damage = damage;
            this.explosionRadius = radius;
        }
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

                if (len > 0 && len <= 500) {
                    int dmg = 15 + (level * 7);
                    int radius = 44 + (level * 7);
                    fireballs.add(new Fireball(player.x, player.y, dx / len, dy / len, dmg, radius));
                }
            }
            lastAttackTime = currentTime;
        }

        Iterator<Fireball> fIt = fireballs.iterator();
        while (fIt.hasNext()) {
            Fireball f = fIt.next();
            f.x += f.dirX * f.speed;
            f.y += f.dirY * f.speed;

            boolean exploded = false;
            for (Enemy enemy : enemies) {
                if (enemy.hp > 0 && Math.hypot(enemy.x - f.x, enemy.y - f.y) < enemy.size / 2.0 + 8) {
                    exploded = true;
                    break;
                }
            }

            if (exploded) {
                // ทำดาเมจเป็นวงระเบิด
                for (Enemy enemy : enemies) {
                    if (enemy.hp > 0 && Math.hypot(enemy.x - f.x, enemy.y - f.y) <= f.explosionRadius + enemy.size / 2.0) {
                        enemy.hp -= f.damage;
                    }
                }
                // เอฟเฟกต์วงกลมระเบิด
                visualEffects.add(VisualEffect.explosion(f.x, f.y, f.explosionRadius));
                fIt.remove();
            } else if (Math.hypot(f.x - player.x, f.y - player.y) > 900) {
                fIt.remove();
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        for (Fireball f : fireballs) {
            int sx = (int) (f.x - camX);
            int sy = (int) (f.y - camY);
            int width = 56;
            int height = width * FIRE_IMAGE.getHeight() / FIRE_IMAGE.getWidth();
            Graphics2D fireGraphics = (Graphics2D) g.create();
            fireGraphics.translate(sx, sy);
            fireGraphics.rotate(Math.atan2(f.dirY, f.dirX));
            fireGraphics.drawImage(FIRE_IMAGE, -width / 2, -height / 2, width, height, null);
            fireGraphics.dispose();
        }
    }
}
