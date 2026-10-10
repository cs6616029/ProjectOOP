package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class GarlicWeapon extends Weapon {
    private final long baseDmgInterval = 300; // คูลดาวน์การทำดาเมจ (0.3 วินาที)
    private long lastDmgTime = 0;

    public GarlicWeapon(int level) {
        super("Garlic", "Creates a protective damage aura around the player", level, 10, 4);
    }

    @Override
    public String getUpgradeDescription() {
        return super.getUpgradeDescription() + " | " + upgradeStat("Radius", 56, 10);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return; // ถ้าเลเวล 0 จะไม่ทำงาน

        // คำนวณคูลดาวน์ร่วมกับไอเทมเพิ่มความเร็วโจมตี (Gloves)
        long effectiveInterval = (long) (baseDmgInterval * player.attackCooldownMultiplier);

        if (currentTime - lastDmgTime >= effectiveInterval) {
            double auraRadius = getAuraRadius();
            int damage = getDamage();

            for (Enemy enemy : enemies) {
                // เช็คระยะห่างระหว่างผู้เล่นกับศัตรู
                if (Math.hypot(enemy.x - player.x, enemy.y - player.y) <= auraRadius + (enemy.size / 2.0)) {
                    enemy.hp -= damage;
                }
            }
            lastDmgTime = currentTime;
        }
    }

    private int getAuraRadius() {
        return scaleStat(56, 10);
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (level <= 0) return; // ถ้าเลเวล 0 จะไม่วาดวงกระเทียม

        int sx = (int) (player.x - camX);
        int sy = (int) (player.y - camY);
        int auraRadius = getAuraRadius();

        // วาดวงออร่ากระเทียมสีดำ/เทาโปร่งแสงรอบตัวผู้เล่น
        g.setColor(new Color(200, 200, 220, 60)); // วงในโปร่งแสง
        g.fillOval(sx - auraRadius, sy - auraRadius, auraRadius * 2, auraRadius * 2);

        g.setColor(new Color(220, 220, 255, 180)); // เส้นขอบ
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawOval(sx - auraRadius, sy - auraRadius, auraRadius * 2, auraRadius * 2);
        g.setStroke(new java.awt.BasicStroke(1)); // คืนค่าเส้นปกติ
    }
}
