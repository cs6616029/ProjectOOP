package project.game;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

public class SwordWeapon extends Weapon {
    private static final double SLASH_HALF_ANGLE = Math.toRadians(70);
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
        super("Sword", "Slashes in an arc in front of the player", level, 28, 10);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Reach", 82, 10);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        animationTime = currentTime;
        if (level <= 0) return;

        long effectiveCooldown = (long) (baseCooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            attackRadius = scaleStat(82, 10);
            int damage = getDamage();

            double dirX = player.lastDirX;
            double dirY = player.lastDirY;
            attackAngle = Math.atan2(dirY, dirX);

            for (Enemy enemy : enemies) {
                if (enemy.hp <= 0) continue;
                double ex = enemy.x - player.x;
                double ey = enemy.y - player.y;
                double dist = Math.hypot(ex, ey);

                double enemyRadius = enemy.size / 2.0;
                if (dist <= attackRadius + enemyRadius) {
                    double difference = Math.atan2(ey, ex) - attackAngle;
                    double angleDifference = Math.abs(Math.atan2(Math.sin(difference), Math.cos(difference)));
                    double edgeAllowance = dist <= enemyRadius ? Math.PI : Math.asin(enemyRadius / dist);
                    if (angleDifference <= SLASH_HALF_ANGLE + edgeAllowance) {
                        enemy.takeDamage(damage);
                        visualEffects.add(VisualEffect.swordHit(enemy.x, enemy.y));
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
