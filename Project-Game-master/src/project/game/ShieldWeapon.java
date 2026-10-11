package project.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ShieldWeapon extends Weapon {
    private static final GifAnimation RING_ANIMATION = GifAnimation.load(
            "/project/game/weapons/TenRing/tenring_transparent.gif").trimTransparentMargins();
    private long animationStartTime = -1;
    private long animationTime;
    private double currentAngle = 0;
    private final long baseDmgInterval = 250;
    private final long[] lastDmgTimes = new long[5];
    private final boolean[] touchingRegularEnemy = new boolean[5];
    private static final long BOSS_EXPLOSION_INTERVAL = 750;
    private final Map<Enemy, Long> lastBossExplosions = new IdentityHashMap<>();

    public ShieldWeapon(int level) {
        super("Ten Ring", "Summons fiery rings orbiting around you. Higher levels add more rings.", level, 18, 6);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | Rings " + level + " -> " + Math.min(5, level + 1);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;
        if (animationStartTime < 0) animationStartTime = currentTime;
        animationTime = Math.max(0, currentTime - animationStartTime);

        currentAngle += 0.05;
        if (currentAngle > Math.PI * 2) {
            currentAngle -= Math.PI * 2;
        }

        int shieldCount = level;
        double orbitRadius = scaleStat(75, 5);
        int shieldSize = scaleStat(16, 2);
        int damage = getDamage();
        BufferedImage frame = RING_ANIMATION.frameAt(animationTime % RING_ANIMATION.getDurationMillis());
        double ringHalfWidth = shieldSize;
        double ringHalfHeight = ringHalfWidth * frame.getHeight() / frame.getWidth();
        // Snapshot living targets so simultaneous ring contacts all get effects,
        // even if an earlier ring kills the target during this update.
        List<Enemy> livingEnemies = enemies.stream().filter(enemy -> enemy.hp > 0).toList();
        lastBossExplosions.keySet().removeIf(enemy -> !livingEnemies.contains(enemy));

        // คำนวณคูลดาวน์ทำดาเมจร่วมกับ Speed Buff ของ Player
        long effectiveInterval = (long) (baseDmgInterval * player.attackCooldownMultiplier);

        for (int i = 0; i < shieldCount; i++) {
            double angle = currentAngle + (i * (2 * Math.PI / shieldCount));
            double shieldX = player.x + Math.cos(angle) * orbitRadius;
            double shieldY = player.y + Math.sin(angle) * orbitRadius;
            boolean hitEnemy = false;
            boolean hitRegularEnemy = false;
            boolean bossExplosionReady = false;
            boolean damageReady = currentTime - lastDmgTimes[i] >= effectiveInterval;

            for (Enemy enemy : livingEnemies) {
                double contactX = (enemy.x - shieldX) / (enemy.size / 2.0 + ringHalfWidth);
                double contactY = (enemy.y - shieldY) / (enemy.size / 2.0 + ringHalfHeight);
                if (contactX * contactX + contactY * contactY <= 1) {
                    if (damageReady && enemy.hp > 0) enemy.takeDamage(damage);
                    hitEnemy = true;
                    if (enemy.type == EnemyType.BOSS) {
                        Long lastExplosion = lastBossExplosions.get(enemy);
                        if (lastExplosion == null || currentTime - lastExplosion >= BOSS_EXPLOSION_INTERVAL) {
                            // All rings share the visual cooldown for this boss; damage stays independent.
                            lastBossExplosions.put(enemy, currentTime);
                            bossExplosionReady = true;
                        }
                    } else {
                        hitRegularEnemy = true;
                    }
                }
            }
            if (bossExplosionReady || (hitRegularEnemy && (!touchingRegularEnemy[i] || damageReady))) {
                visualEffects.add(VisualEffect.explosion(shieldX, shieldY, scaleStat(51, 7)));
            }
            if (hitEnemy && damageReady) lastDmgTimes[i] = currentTime;
            touchingRegularEnemy[i] = hitRegularEnemy;
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (level <= 0) return;

        int shieldCount = level;
        double orbitRadius = scaleStat(75, 5);
        int shieldSize = scaleStat(16, 2);

        BufferedImage frame = RING_ANIMATION.frameAt(animationTime % RING_ANIMATION.getDurationMillis());
        int renderWidth = shieldSize * 2;
        int renderHeight = Math.max(1, renderWidth * frame.getHeight() / frame.getWidth());
        for (int i = 0; i < shieldCount; i++) {
            double angle = currentAngle + (i * (2 * Math.PI / shieldCount));
            double shieldX = player.x + Math.cos(angle) * orbitRadius;
            double shieldY = player.y + Math.sin(angle) * orbitRadius;

            int sx = (int) (shieldX - camX);
            int sy = (int) (shieldY - camY);

            g.drawImage(frame, sx - renderWidth / 2, sy - renderHeight / 2,
                    renderWidth, renderHeight, null);
        }
    }
}
