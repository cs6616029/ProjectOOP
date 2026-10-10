package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class ShieldWeapon extends Weapon {
    private double currentAngle = 0;
    private final long baseDmgInterval = 250;
    private long lastDmgTime = 0;

    public ShieldWeapon(int level) {
        super("Orb Shield", "Summons orbiting shields around you. Higher levels add more shields.", level);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;

        currentAngle += 0.05;
        if (currentAngle > Math.PI * 2) {
            currentAngle -= Math.PI * 2;
        }

        int shieldCount = level;
        double orbitRadius = 70 + (level * 5);
        int shieldSize = 16;
        int damage = 12 + (level * 6);

        // คำนวณคูลดาวน์ทำดาเมจร่วมกับ Speed Buff ของ Player
        long effectiveInterval = (long) (baseDmgInterval * player.attackCooldownMultiplier);

        if (currentTime - lastDmgTime >= effectiveInterval) {
            for (int i = 0; i < shieldCount; i++) {
                double angle = currentAngle + (i * (2 * Math.PI / shieldCount));
                double shieldX = player.x + Math.cos(angle) * orbitRadius;
                double shieldY = player.y + Math.sin(angle) * orbitRadius;

                for (Enemy enemy : enemies) {
                    if (Math.hypot(enemy.x - shieldX, enemy.y - shieldY) <= (enemy.size / 2.0 + shieldSize / 2.0)) {
                        enemy.hp -= damage;
                    }
                }
            }
            lastDmgTime = currentTime;
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (level <= 0) return;

        int shieldCount = level;
        double orbitRadius = 70 + (level * 5);
        int shieldSize = 16;

        g.setColor(new Color(100, 200, 255));
        for (int i = 0; i < shieldCount; i++) {
            double angle = currentAngle + (i * (2 * Math.PI / shieldCount));
            double shieldX = player.x + Math.cos(angle) * orbitRadius;
            double shieldY = player.y + Math.sin(angle) * orbitRadius;

            int sx = (int) (shieldX - camX);
            int sy = (int) (shieldY - camY);

            g.fillOval(sx - shieldSize / 2, sy - shieldSize / 2, shieldSize, shieldSize);
            g.setColor(Color.WHITE);
            g.drawOval(sx - shieldSize / 2, sy - shieldSize / 2, shieldSize, shieldSize);
            g.setColor(new Color(100, 200, 255));
        }
    }
}