package project.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FireStaffWeapon extends Weapon {
    private long animationTime;
    private long lastAttackTime = 0;
    private final long cooldown = 1800;
    private final List<Fireball> fireballs = new ArrayList<>();

    public FireStaffWeapon(int level) {
        super("Fire Staff", "Launches a fireball at nearest enemy that explodes on hit", level, 22, 7);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Blast", 51, 7);
    }

    private static class Fireball {
        double x, y, dirX, dirY;
        double speed = 7.0;
        int damage = 1;
        int explosionRadius = 70;
        int renderWidth;
        final long spawnedAt;

        Fireball(double x, double y, double dirX, double dirY, int damage, int radius, int renderWidth, long spawnedAt) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.damage = damage;
            this.explosionRadius = radius;
            this.renderWidth = renderWidth;
            this.spawnedAt = spawnedAt;
        }
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;
        animationTime = currentTime;

        long effectiveCooldown = (long) (cooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            Enemy target = findClosestEnemy(player, enemies);
            if (target != null) {
                double dx = target.x - player.x;
                double dy = target.y - player.y;
                double len = Math.hypot(dx, dy);

                if (len > 0 && len <= 500) {
                    int dmg = getDamage();
                    int radius = scaleStat(51, 7);
                    fireballs.add(new Fireball(player.x, player.y, dx / len, dy / len, dmg, radius, scaleStat(56, 6), currentTime));
                    SoundEffects.play(SoundEffects.Effect.FLAME);
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
                double fireRadius = f.renderWidth * 0.30;
                if (enemy.hp > 0 && Math.hypot(enemy.x - f.x, enemy.y - f.y) < enemy.size / 2.0 + fireRadius) {
                    exploded = true;
                    break;
                }
            }

            if (exploded) {
                // ทำดาเมจเป็นวงระเบิด
                for (Enemy enemy : enemies) {
                    if (enemy.hp > 0 && Math.hypot(enemy.x - f.x, enemy.y - f.y) <= f.explosionRadius + enemy.size / 2.0) {
                        enemy.takeDamage(f.damage);
                    }
                }
                // เอฟเฟกต์วงกลมระเบิด
                visualEffects.add(VisualEffect.fireStaffHit(f.x, f.y, f.explosionRadius, Math.atan2(f.dirY, f.dirX)));
                SoundEffects.play(SoundEffects.Effect.EXPLOSION);
                fIt.remove();
            } else if (Math.hypot(f.x - player.x, f.y - player.y) > 900) {
                fIt.remove();
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        for (Fireball f : fireballs) {
            int sx = (int) Math.round(f.x - camX);
            int sy = (int) Math.round(f.y - camY);
            int width = f.renderWidth;
            BufferedImage frame = FireStaffAnimation.FLIGHT.frameAt(Math.max(0, animationTime - f.spawnedAt), 80);
            int height = width * frame.getHeight() / frame.getWidth();
            Graphics2D fireGraphics = (Graphics2D) g.create();
            fireGraphics.translate(sx, sy);
            fireGraphics.rotate(Math.atan2(f.dirY, f.dirX));
            fireGraphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            // The supplied fire head is at x=48 of its 64px frame; keep it on the hit position.
            fireGraphics.drawImage(frame, -width * 3 / 4, -height / 2, width, height, null);
            fireGraphics.dispose();
        }
    }
}
