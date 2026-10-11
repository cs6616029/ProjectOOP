package project.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class AurafarmingWeapon extends Weapon {
    private static final SpriteAnimation AURA_ANIMATION = SpriteAnimation.load(
            "/project/game/weapons/Aurafarming/Effect-Aurafarming.png", 4);
    private final long baseDmgInterval = 200;
    private static final int BASE_DURATION_MILLIS = 5000, DURATION_PER_LEVEL = 1000;
    private long cycleDurationMillis;
    private long activeUntil, nextActivation;
    private boolean active;
    private long lastDmgTime;
    private long animationStartTime = -1;
    private long animationTime;
    private final int baseRadius;
    private final int radiusPerLevel;

    public AurafarmingWeapon(int level) {
        this(level, 1.0);
    }

    public AurafarmingWeapon(int level, double sizeScale) {
        super("Aurafarming", "Slowly burns nearby enemies with a golden aura", level, 1, 1);
        baseRadius = (int) Math.round(56 * sizeScale);
        radiusPerLevel = (int) Math.round(10 * sizeScale);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Radius", baseRadius, radiusPerLevel)
                + " | " + upgradeStat("Active/Rest (s)", 5, 1);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) { active = false; return; }
        long effectiveInterval = Math.max(1, (long) (baseDmgInterval * player.attackCooldownMultiplier));
        if (active && currentTime >= activeUntil) {
            active = false;
            nextActivation = activeUntil + Math.max(1, (long) (cycleDurationMillis * player.attackCooldownMultiplier));
        }
        if (!active) {
            if (currentTime < nextActivation) return;
            active = true;
            animationStartTime = currentTime;
            cycleDurationMillis = scaleStat(BASE_DURATION_MILLIS, DURATION_PER_LEVEL);
            activeUntil = currentTime + cycleDurationMillis;
            lastDmgTime = currentTime - effectiveInterval;
        }
        animationTime = Math.max(0, currentTime - animationStartTime);

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
        if (level <= 0 || !active) return;
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
