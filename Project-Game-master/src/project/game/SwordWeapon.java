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
    private static final long GOLDEN_COOLDOWN = 2000;
    private long lastGoldenAttackTime = -1;
    private static final long STARTING_COOLDOWN = 1500;
    private boolean isAttacking = false;
    private long attackStartTime = 0;
    private long animationTime = 0;
    private final long attackDuration = SLASH_ANIMATION.getDurationMillis();
    private double attackRadius = 90;
    private double attackAngle;
    private boolean goldenSlash, goldenAttack;
    private static final class GoldenAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/golden-sword-slash.png", 5);
    }

    void setGoldenSlash(boolean enabled) { goldenSlash = enabled; }

    static boolean hasMaxedSword(List<Weapon> weapons) {
        return weapons.stream().anyMatch(w -> w instanceof SwordWeapon && w.getLevel() == 5);
    }

    private long currentAttackDuration() { return goldenAttack ? 300 : attackDuration; }

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

        long baseCooldown = level == 1 ? STARTING_COOLDOWN : 1000;
        long effectiveCooldown = (long) (baseCooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            long goldenCooldown = Math.max(1, (long) (GOLDEN_COOLDOWN / 2 * player.attackCooldownMultiplier)) * 2;
            goldenAttack = goldenSlash && !goldenAttack && (lastGoldenAttackTime < 0
                    || currentTime - lastGoldenAttackTime >= goldenCooldown);
            if (goldenAttack) lastGoldenAttackTime = currentTime;
            SoundEffects.play(SoundEffects.Effect.SWORD);
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
                    if (goldenAttack || angleDifference <= SLASH_HALF_ANGLE + edgeAllowance) {
                        enemy.takeDamage(damage);
                        visualEffects.add(VisualEffect.swordHit(enemy.x, enemy.y));
                    }
                }
            }

            isAttacking = true;
            attackStartTime = currentTime;
            lastAttackTime = currentTime;
        }

        if (isAttacking && currentTime - attackStartTime >= currentAttackDuration()) {
            isAttacking = false;
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (level <= 0 || !isAttacking) return;

        long elapsed = animationTime - attackStartTime;
        if (elapsed < 0 || elapsed >= currentAttackDuration()) return;

        BufferedImage frame = goldenAttack ? GoldenAnimation.SPRITE.frameAt(elapsed, 60) : SLASH_ANIMATION.frameAt(elapsed);
        double renderWidth = attackRadius * 2.4;
        double renderHeight = renderWidth * frame.getHeight() / frame.getWidth();
        Graphics2D slash = (Graphics2D) g.create();
        if (goldenAttack) slash.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int arcs = goldenAttack ? 4 : 1;
        for (int i = 0; i < arcs; i++) {
            double angle = attackAngle + i * Math.PI / 2
                    + (goldenAttack ? elapsed / 300.0 * Math.PI * 2 : 0);
            AffineTransform transform = new AffineTransform();
            transform.translate(player.x - camX + Math.cos(angle) * attackRadius * .25,
                    player.y - camY + Math.sin(angle) * attackRadius * .25);
            transform.rotate(angle);
            transform.scale(renderWidth / frame.getWidth(), renderHeight / frame.getHeight());
            transform.translate(-frame.getWidth() / 2.0, -frame.getHeight() / 2.0);
            slash.drawImage(frame, transform, null);
        }
        slash.dispose();
    }
}
