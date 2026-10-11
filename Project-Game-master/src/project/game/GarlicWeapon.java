package project.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class GarlicWeapon extends Weapon {
    private static final SpriteAnimation AURA_ANIMATION = SpriteAnimation.load(
            "/project/game/weapons/Aurafarming/Effect-Aurafarming.png", 4);
    private final long baseDmgInterval = 200;
    private long lastDmgTime;
    private long animationStartTime = -1;
    private long animationTime;
    private final int baseRadius;
    private final int radiusPerLevel;

    public GarlicWeapon(int level) {
        this(level, 1.0);
    }

    public GarlicWeapon(int level, double sizeScale) {
        super("Aurafarming", "Slowly burns nearby enemies with a golden aura", level, 1, 1);
        baseRadius = (int) Math.round(56 * sizeScale);
        radiusPerLevel = (int) Math.round(10 * sizeScale);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Radius", baseRadius, radiusPerLevel);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;
        if (animationStartTime < 0) animationStartTime = currentTime;
        animationTime = Math.max(0, currentTime - animationStartTime);

        long effectiveInterval = (long) (baseDmgInterval * player.attackCooldownMultiplier);
        if (currentTime - lastDmgTime >= effectiveInterval) {
            double auraRadius = getAuraRadius();
            int damage = getDamage();
            for (Enemy enemy : enemies) {
                if (enemy.hp > 0 && Math.hypot(enemy.x - player.x, enemy.y - player.y) <= auraRadius + enemy.size / 2.0) {
                    enemy.takeDamage(damage);
                    visualEffects.add(VisualEffect.explosion(enemy.x, enemy.y, scaleStat(51, 7)));
                }
            }
            lastDmgTime = currentTime;
        }
    }

    private int getAuraRadius() {
        return scaleStat(baseRadius, radiusPerLevel);
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (level <= 0) return;
        int sx = (int) (player.x - camX);
        int sy = (int) (player.y - camY);
        int auraRadius = getAuraRadius();
        BufferedImage frame = AURA_ANIMATION.frameAt(animationTime, 100);
        int width = auraRadius * 2;
        int height = width * frame.getHeight() / frame.getWidth();
        // Anchor the flame base at the player's feet. The player is drawn in front.
        g.drawImage(frame, sx - auraRadius, sy + player.getRenderSize() / 2 - height, width, height, null);
    }
}
