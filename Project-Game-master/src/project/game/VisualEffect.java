package project.game;

import java.awt.Color;
import java.awt.Graphics2D;

public class VisualEffect {
    public double x, y;
    public int radius;
    public Color color;
    public int duration; // จำนวนเฟรมที่แสดง
    private int currentFrame = 0;
    private boolean explosion;
    private boolean bossDeath;
    private boolean bulletHit;
    private boolean laserHit;
    private boolean enemyDeath;
    private boolean swordHit;
    private boolean batHit;
    private boolean fireStaffHit;
    private boolean holyNova;
    private double fireStaffAngle;

    private static final class HolyNovaAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/holy-nova.png", 10);
    }

    public static VisualEffect holyNova(double x, double y, int radius) {
        VisualEffect effect = new VisualEffect(x, y, radius, new Color(255, 239, 170), 40);
        effect.holyNova = true;
        return effect;
    }

    public static VisualEffect fireStaffHit(double x, double y, int radius, double angle) {
        VisualEffect effect = new VisualEffect(x, y, radius, Color.ORANGE, 20);
        effect.fireStaffHit = true;
        effect.fireStaffAngle = angle;
        return effect;
    }

    public static VisualEffect batHit(double x, double y) {
        VisualEffect effect = new VisualEffect(x, y, 60, new Color(235, 120, 255), BatAttackAnimation.FRAME_COUNT * 3);
        effect.batHit = true;
        return effect;
    }

    private static final class SwordHitAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.loadGrid("/project/game/effects/sword-hit-blood.png", 6, 6);
    }

    public static VisualEffect swordHit(double x, double y) {
        // The blood animation ends at cell 21; later atlas cells are empty or unrelated.
        VisualEffect effect = new VisualEffect(x, y, 50, Color.RED, 44);
        effect.swordHit = true;
        return effect;
    }

    private static final class EnemyDeathAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/enemy-death.png", 9);
    }

    public static VisualEffect enemyDeath(double x, double y, int enemySize) {
        VisualEffect effect = new VisualEffect(x, y, (int) Math.ceil(enemySize * 0.55), Color.YELLOW, 27);
        effect.enemyDeath = true;
        return effect;
    }

    private static final class LaserHitAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/laser-hit.png", 10);
    }

    public static VisualEffect laserHit(double x, double y, int radius) {
        VisualEffect effect = new VisualEffect(x, y, radius, Color.RED, 30);
        effect.laserHit = true;
        return effect;
    }

    private static final class BulletHitAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/bullet-hit.png", 10);
    }

    public static VisualEffect bulletHit(double x, double y) {
        VisualEffect effect = new VisualEffect(x, y, 40, Color.RED, 30);
        effect.bulletHit = true;
        return effect;
    }

    private static final class BossDeathAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.load("/project/game/effects/boss-death.png", 11);
    }

    public static VisualEffect bossDeath(double x, double y, int bossSize) {
        VisualEffect effect = new VisualEffect(x, y, (int) Math.ceil(bossSize * 0.7), Color.ORANGE, 44);
        effect.bossDeath = true;
        return effect;
    }

    private static final class ExplosionAnimation {
        static final SpriteAnimation SPRITE = SpriteAnimation.loadGrid(
                "/project/game/effects/explosion.png", 10, 2);
    }

    public static VisualEffect explosion(double x, double y, int radius) {
        VisualEffect effect = new VisualEffect(x, y, radius, Color.ORANGE, 40);
        effect.explosion = true;
        return effect;
    }

    public VisualEffect(double x, double y, int radius, Color color, int duration) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
        this.duration = duration;
    }

    public void update() {
        currentFrame++;
    }

    public boolean isFinished() {
        return currentFrame >= duration;
    }

    public void draw(Graphics2D g, double camX, double camY) {
        if (isFinished()) return;
        int sx = (int) (x - camX);
        int sy = (int) (y - camY);
        if (holyNova) {
            Graphics2D impact = (Graphics2D) g.create();
            impact.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            impact.drawImage(HolyNovaAnimation.SPRITE.frameAt(currentFrame, 4),
                    sx - radius, sy - radius / 2, radius * 2, radius, null);
            impact.dispose();
            return;
        }
        if (fireStaffHit) {
            Graphics2D impact = (Graphics2D) g.create();
            impact.translate(sx, sy);
            impact.rotate(fireStaffAngle);
            impact.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int width = radius * 4;
            impact.drawImage(FireStaffAnimation.IMPACT.frameAt(currentFrame, 4),
                    -width * 3 / 4, -width / 2, width, width, null);
            impact.dispose();
            return;
        }
        if (batHit) {
            g.drawImage(BatAttackAnimation.SPRITE.frameAt(currentFrame, 3),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }
        if (swordHit) {
            g.drawImage(SwordHitAnimation.SPRITE.frameAt(currentFrame, 2),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }
        if (enemyDeath) {
            g.drawImage(EnemyDeathAnimation.SPRITE.frameAt(currentFrame, 3),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }
        if (laserHit) {
            g.drawImage(LaserHitAnimation.SPRITE.frameAt(currentFrame, 3),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }
        if (bulletHit) {
            g.drawImage(BulletHitAnimation.SPRITE.frameAt(currentFrame, 3),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }
        if (bossDeath) {
            g.drawImage(BossDeathAnimation.SPRITE.frameAt(currentFrame, 4),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }
        if (explosion) {
            g.drawImage(ExplosionAnimation.SPRITE.frameAt(currentFrame, 2),
                    sx - radius, sy - radius, radius * 2, radius * 2, null);
            return;
        }

        // ป้องกันค่า Alpha ติดลบเมื่อเกินเวลา
        int alpha = Math.max(0, Math.min(255, 255 - (currentFrame * 255 / duration)));
        Color fadeColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        g.setColor(fadeColor);
        g.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
    }
}
