package project.game;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

public class SwordWeapon extends Weapon {
    private static final GifAnimation SLASH_ANIMATION =
            GifAnimation.load("/project/game/sword/aFwslc.gif");

    private long lastAttackTime = 0;
    private final long baseCooldown = 1000; // คูลดาวน์พื้นฐาน 1 วินาที
    private boolean isAttacking = false;
    private long attackStartTime = 0;
    private long animationTime = 0;
    private final long attackDuration = SLASH_ANIMATION.getDurationMillis();
    private double attackRadius = 90;
    private double attackAngle;

    public SwordWeapon(int level) {
        super("Sword", "Slashes in an arc in front of the player", level);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        animationTime = currentTime;
        if (level <= 0) return;

        long effectiveCooldown = (long) (baseCooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            attackRadius = 62 + (level * 10);
            int damage = 18 + (level * 10);

            double dirX = player.lastDirX;
            double dirY = player.lastDirY;
            attackAngle = Math.atan2(dirY, dirX);

            for (Enemy enemy : enemies) {
                double ex = enemy.x - player.x;
                double ey = enemy.y - player.y;
                double dist = Math.hypot(ex, ey);

                if (dist <= attackRadius + (enemy.size / 2.0) && dist > 0) {
                    double dot = (ex / dist) * dirX + (ey / dist) * dirY;

                    if (dot >= 0.5) {
                        enemy.hp -= damage;
                    }
                }
            }

            isAttacking = true;
            attackStartTime = currentTime;
            lastAttackTime = currentTime;
        }

        if (isAttacking && currentTime - attackStartTime > attackDuration) {
            isAttacking = false;
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (level <= 0 || !isAttacking) return;

        long elapsed = animationTime - attackStartTime;
        if (elapsed < 0 || elapsed > attackDuration) return;

        BufferedImage frame = SLASH_ANIMATION.frameAt(elapsed);
        double renderWidth = attackRadius * 2.4;
        double renderHeight = renderWidth * frame.getHeight() / frame.getWidth();
        double centerX = player.x - camX + Math.cos(attackAngle) * attackRadius * 0.25;
        double centerY = player.y - camY + Math.sin(attackAngle) * attackRadius * 0.25;

        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(attackAngle);
        transform.scale(renderWidth / frame.getWidth(), renderHeight / frame.getHeight());
        transform.translate(-frame.getWidth() / 2.0, -frame.getHeight() / 2.0);
        g.drawImage(frame, transform, null);
    }
}
