package project.game;

import java.awt.Graphics2D;
import java.util.List;

public abstract class Weapon {
    protected int level;
    protected String name;
    protected String description;
    private final int baseDamage;
    private final int damagePerLevel;

    public Weapon(String name, String description, int level, int baseDamage, int damagePerLevel) {
        this.name = name;
        this.description = description;
        this.level = level;
        this.baseDamage = baseDamage;
        this.damagePerLevel = damagePerLevel;
    }

    public abstract void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime);
    public abstract void draw(Graphics2D g, Player player, double camX, double camY);

    public void levelUp() {
        if (level < 5) {
            level++;
        }
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getDamage() { return statAtLevel(baseDamage, damagePerLevel, level); }

    protected int scaleStat(int base, int perLevel) {
        return statAtLevel(base, perLevel, level);
    }

    private int statAtLevel(int base, int perLevel, int atLevel) {
        return atLevel <= 0 ? 0 : base + (atLevel - 1) * perLevel;
    }

    protected String upgradeStat(String label, int base, int perLevel) {
        return label + " " + statAtLevel(base, perLevel, level) + " -> "
                + statAtLevel(base, perLevel, Math.min(5, level + 1));
    }

    public String getUpgradeDescription() {
        return upgradeStat("Damage", baseDamage, damagePerLevel);
    }
    public int getLevel() { return level; }

    protected Enemy findClosestEnemy(Player player, List<Enemy> enemies) {
        Enemy closest = null;
        double minDistance = Double.MAX_VALUE;

        for (Enemy enemy : enemies) {
            if (enemy.hp <= 0) continue;
            double dist = Math.hypot(enemy.x - player.x, enemy.y - player.y);
            if (dist < minDistance) {
                minDistance = dist;
                closest = enemy;
            }
        }
        return closest;
    }
}
