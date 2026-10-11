package project.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

final class BossSkill {
    enum Kind {
        CHARGER("White Mage", "LIGHTNING", 2500, 650),
        DRAGON("Dragon", "FIRE SPREAD", 2200, 600),
        OGRE("Ogre", "GROUND SLAM", 2800, 750),
        SKELETON("Skeleton", "SWORD WAVE", 2300, 600);

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
    private long nextAttack = -1, releaseTime;
    private boolean warning;
    private double originX, originY, angle;
    private int ogreFootOffset;
    private double lightningX, lightningY;

    BossSkill(Kind kind) { this.kind = kind; }

    boolean isCharging() { return false; }
    boolean isWindingUp() { return warning; }

    void update(Enemy boss, Player player, List<BossAttack> attacks, long time) {
        if (boss.hp <= 0) return;
        if (nextAttack < 0) {
            nextAttack = time + 1200;
        }
        if (warning) {
            if (time < releaseTime) return;
            warning = false;
            nextAttack = time + kind.cooldown;
            switch (kind) {
                case CHARGER:
                    attacks.add(BossAttack.lightning(boss, lightningX, lightningY, time));
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
            ogreFootOffset = boss.size / 2;
            lightningX = player.x;
            lightningY = player.y + player.getRenderSize() / 2.0;
            angle = Math.atan2(player.y - boss.y, player.x - boss.x);
        }
    }

    void drawWarning(Graphics2D graphics, double camX, double camY) {
        if (!warning) return;
        Graphics2D g = (Graphics2D) graphics.create();
        int sx = (int) (originX - camX), sy = (int) (originY - camY);
        g.setColor(new Color(255, 80, 40, 65));
        if (kind == Kind.CHARGER) {
            int targetX = (int) (lightningX - camX), targetY = (int) (lightningY - camY);
            int radius = BossAttack.LIGHTNING_RADIUS;
            g.setColor(new Color(190, 80, 255, 65));
            g.fillOval(targetX - radius, targetY - radius, radius * 2, radius * 2);
            g.setColor(new Color(230, 130, 255));
            g.setStroke(new BasicStroke(3));
            g.drawOval(targetX - radius, targetY - radius, radius * 2, radius * 2);
        } else if (kind == Kind.OGRE) {
            int radius = BossAttack.OGRE_SLAM_RADIUS;
            int halfHeight = (int) Math.round(radius * BossAttack.OGRE_SLAM_HEIGHT_SCALE);
            int groundY = sy + ogreFootOffset;
            g.fillOval(sx - radius, groundY - halfHeight, radius * 2, halfHeight * 2);
            g.setColor(Color.ORANGE);
            g.setStroke(new BasicStroke(3));
            g.drawOval(sx - radius, groundY - halfHeight, radius * 2, halfHeight * 2);
        } else {
            int rays = kind == Kind.SKELETON ? 8 : (kind == Kind.DRAGON ? 3 : 1);
            g.setStroke(new BasicStroke(kind == Kind.SKELETON ? BossAttack.SWORD_WAVE_RADIUS * 2 : 6));
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
