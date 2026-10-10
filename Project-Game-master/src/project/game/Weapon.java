package project.game;

import java.awt.Graphics2D;
import java.util.List;

public abstract class Weapon {
    protected int level;
    protected String name;
    protected String description;

    public Weapon(String name, String description, int level) {
        this.name = name;
        this.description = description;
        this.level = level;
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
