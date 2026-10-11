package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/** Keeps the existing upgrade slot for the Frost Hammer replacement. */
public class SpearWeapon extends Weapon {
    private long lastAttackTime, attackStart, animationTime;
    private boolean swinging, impacted;
    private double originX, originY, angle, impactX, impactY;
    private int swingWidth, blastRadius, swingDamage;

    public SpearWeapon(int level) {
        super("Frost Hammer", "Slams an icy hammer and freezes enemies; bosses require two hits", level, 32, 10);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Range", 138, 18)
                + " | " + upgradeStat("Radius", 55, 8);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> effects, long time) {
        if (level <= 0) return;
        animationTime = time;
        if (swinging && time - attackStart >= FrostHammerAnimation.IMPACT_MILLIS && !impacted) {
            impacted = true;
            effects.add(VisualEffect.frostImpact(impactX, impactY, blastRadius));
            for (Enemy enemy : enemies) {
                if (enemy.hp > 0 && Math.hypot(enemy.x - impactX, enemy.y - impactY)
                        <= blastRadius + enemy.size * 0.25) {
                    enemy.takeDamage(swingDamage);
                    enemy.applyFrostHit(time);
                }
            }
        }
        if (swinging && time - attackStart >= FrostHammerAnimation.DURATION_MILLIS) swinging = false;
        if (!swinging && time - lastAttackTime >= (long) (1200 * player.attackCooldownMultiplier)) {
            Enemy target = findClosestEnemy(player, enemies);
            if (target == null || Math.hypot(target.x - player.x, target.y - player.y)
                    > scaleStat(138, 18) + target.size * 0.25) return;
            originX = player.x;
            originY = player.y;
            angle = Math.atan2(target.y - originY, target.x - originX);
            swingWidth = (int) Math.round(scaleStat(138, 18) * 1.25);
            blastRadius = scaleStat(55, 8);
            swingDamage = getDamage();
            impactX = originX + Math.cos(angle) * swingWidth * 0.70;
            impactY = originY + Math.sin(angle) * swingWidth * 0.70;
            attackStart = lastAttackTime = time;
            swinging = true;
            impacted = false;
        }
    }

    @Override
    public void draw(Graphics2D graphics, Player player, double camX, double camY) {
        if (!swinging) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(originX - camX, originY - camY);
        g.rotate(angle);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int height = swingWidth / 2;
        g.drawImage(FrostHammerAnimation.HAMMER.frameAt(animationTime - attackStart,
                FrostHammerAnimation.FRAME_MILLIS), -(int) Math.round(swingWidth * 0.12),
                -(int) Math.round(height * 0.85), swingWidth, height, null);
        g.dispose();
    }
}
