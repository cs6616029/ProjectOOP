package project.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

final class BossSkill {
    enum Kind {
        CHARGER("Warlord", "CHARGE", 4500, 1000),
        DRAGON("Dragon", "FIRE SPREAD", 4000, 900),
        OGRE("Ogre", "GROUND SLAM", 5000, 1100),
        SKELETON("Skeleton", "SWORD WAVE", 4200, 1000);

        final String name, attackName;
        final long cooldown, windup;
        Kind(String name, String attackName, long cooldown, long windup) {
            this.name = name;
            this.attackName = attackName;
            this.cooldown = cooldown;
            this.windup = windup;
        }
    }

    final Kind kind;
    private long nextAttack = -1, releaseTime, dashUntil;
    private boolean warning, charging;
    private double originX, originY, angle;
    private long lastUpdate;

    BossSkill(Kind kind) { this.kind = kind; }

    boolean isCharging() { return charging; }
    boolean isWindingUp() { return warning; }

    void update(Enemy boss, Player player, List<BossAttack> attacks, long time) {
        if (boss.hp <= 0) return;
        if (nextAttack < 0) {
            nextAttack = time + 2500;
            lastUpdate = time;
        }
        double seconds = Math.max(0, time - lastUpdate) / 1000.0;
        lastUpdate = time;
        if (charging) {
            if (time >= dashUntil) charging = false;
            else {
                boss.x += Math.cos(angle) * 540 * seconds;
                boss.y += Math.sin(angle) * 540 * seconds;
            }
            return;
        }
        if (warning) {
            if (time < releaseTime) return;
            warning = false;
            nextAttack = time + kind.cooldown;
            switch (kind) {
                case CHARGER:
                    charging = true;
                    dashUntil = time + 900;
                    break;
                case DRAGON:
                    for (int i = -1; i <= 1; i++) {
                        attacks.add(BossAttack.dragonFire(boss, angle + i * 0.24, time));
                    }
                    break;
                case OGRE:
                    attacks.add(BossAttack.slam(boss, originX, originY, time));
                    break;
                case SKELETON:
                    for (int i = 0; i < 8; i++) {
                        attacks.add(BossAttack.swordWave(boss, angle + i * Math.PI / 4, time));
                    }
                    break;
            }
        } else if (time >= nextAttack && Math.hypot(player.x - boss.x, player.y - boss.y) <= 750) {
            warning = true;
            releaseTime = time + kind.windup;
            originX = boss.x;
            originY = boss.y;
            angle = Math.atan2(player.y - boss.y, player.x - boss.x);
        }
    }

    void drawWarning(Graphics2D graphics, double camX, double camY) {
        if (!warning) return;
        Graphics2D g = (Graphics2D) graphics.create();
        int sx = (int) (originX - camX), sy = (int) (originY - camY);
        g.setColor(new Color(255, 80, 40, 65));
        if (kind == Kind.OGRE) {
            g.fillOval(sx - 200, sy - 200, 400, 400);
            g.setColor(Color.ORANGE);
            g.setStroke(new BasicStroke(3));
            g.drawOval(sx - 200, sy - 200, 400, 400);
        } else {
            int rays = kind == Kind.SKELETON ? 8 : (kind == Kind.DRAGON ? 3 : 1);
            g.setStroke(new BasicStroke(kind == Kind.CHARGER ? 250
                    : (kind == Kind.SKELETON ? BossAttack.SWORD_WAVE_RADIUS * 2 : 6)));
            for (int i = 0; i < rays; i++) {
                double rayAngle = angle + (kind == Kind.SKELETON ? i * Math.PI / 4
                        : (kind == Kind.DRAGON ? (i - 1) * 0.24 : 0));
                g.drawLine(sx, sy, sx + (int) (Math.cos(rayAngle) * 550),
                        sy + (int) (Math.sin(rayAngle) * 550));
            }
        }
        g.setColor(Color.YELLOW);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString(kind.attackName + "!", sx - 60, sy - 145);
        g.dispose();
    }
}
