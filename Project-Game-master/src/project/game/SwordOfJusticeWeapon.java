package project.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Retains the existing weapon slot while replacing the pool with a falling holy sword. */
public class SwordOfJusticeWeapon extends Weapon {
    private static final long COOLDOWN = 2500;
    private static final int FRAME_MILLIS = 64;
    private static final int IMPACT_MILLIS = 5 * FRAME_MILLIS;
    private static final int DURATION_MILLIS = 13 * FRAME_MILLIS;
    private long lastAttackTime;
    private long animationTime;
    private final List<JusticeStrike> strikes = new ArrayList<>();

    private static final class Animation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/sword-of-justice.png", 13);
    }

    public SwordOfJusticeWeapon(int level) {
        super("Sword of Justice", "Summons holy swords above enemies that plunge down and strike once", level, 12, 4);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Radius", 60, 10);
    }

    private static final class JusticeStrike {
        final double x, y, groundY;
        final int radius, damage;
        final long spawnedAt;
        boolean impacted;

        JusticeStrike(Enemy target, int radius, int damage, long time) {
            x = target.x;
            y = target.y;
            groundY = target.y + target.size * 0.25;
            this.radius = radius;
            this.damage = damage;
            spawnedAt = time;
        }
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> effects, long currentTime) {
        if (level <= 0) return;
        animationTime = currentTime;
        long effectiveCooldown = (long) (COOLDOWN * player.attackCooldownMultiplier);
        if (currentTime - lastAttackTime >= effectiveCooldown) {
            List<Enemy> targets = new ArrayList<>();
            for (Enemy enemy : enemies) {
                if (enemy.hp > 0 && Math.hypot(enemy.x - player.x, enemy.y - player.y) <= 500) targets.add(enemy);
            }
            targets.sort(Comparator.comparingDouble(enemy -> Math.hypot(enemy.x - player.x, enemy.y - player.y)));
            int count = Math.min(1 + level / 3, targets.size());
            for (int i = 0; i < count; i++) {
                strikes.add(new JusticeStrike(targets.get(i), scaleStat(60, 10), getDamage(), currentTime));
            }
            if (count > 0) {
                SoundEffects.play(SoundEffects.Effect.SWORD_OF_JUSTICE);
                lastAttackTime = currentTime;
            }
        }
        Iterator<JusticeStrike> iterator = strikes.iterator();
        while (iterator.hasNext()) {
            JusticeStrike strike = iterator.next();
            long age = currentTime - strike.spawnedAt;
            if (!strike.impacted && age >= IMPACT_MILLIS) {
                strike.impacted = true;
                effects.add(VisualEffect.holyNova(strike.x, strike.groundY, strike.radius));
                for (Enemy enemy : enemies) {
                    if (enemy.hp > 0 && Math.hypot(enemy.x - strike.x, enemy.y - strike.y)
                            <= strike.radius + enemy.size * 0.25) enemy.takeDamage(strike.damage);
                }
            }
            if (age >= DURATION_MILLIS) iterator.remove();
        }
    }

    @Override
    public void draw(Graphics2D graphics, Player player, double cameraX, double cameraY) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (JusticeStrike strike : strikes) {
            long age = Math.max(0, animationTime - strike.spawnedAt);
            if (age >= IMPACT_MILLIS) continue;
            double progress = Math.min(1, age / (double) IMPACT_MILLIS);
            double lift = 150 * (1 - progress * progress);
            int width = (int) Math.round(strike.radius * 1.6), height = width * 2;
            int x = (int) Math.round(strike.x - cameraX) - width / 2;
            int y = (int) Math.round(strike.groundY - cameraY - lift - height);
            var frame = Animation.SPRITE.frameAt(age, FRAME_MILLIS);
            // Flip the source vertically so the blade tip leads the downward plunge.
            g.drawImage(frame, x, y, x + width, y + height,
                    0, frame.getHeight(), frame.getWidth(), 0, null);
        }
        g.dispose();
    }
}
