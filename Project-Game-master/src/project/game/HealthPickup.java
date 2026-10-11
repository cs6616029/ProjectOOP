package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** A boss reward that stays on the ground until an injured player collects it. */
final class HealthPickup {
    static final int HEAL_AMOUNT = 30;
    private static final SpriteAnimation ANIMATION =
            SpriteAnimation.load("/project/game/effects/boss-health.png", 23);
    final double x, y;
    private final long born;
    private long collectedAt = -1;

    HealthPickup(double x, double y, long time) {
        this.x = x; this.y = y; born = time;
    }

    boolean update(Player player, long time) {
        if (collectedAt >= 0) return time - collectedAt >= 12 * 50;
        if (player.hp > 0 && player.hp < player.maxHp
                && new Rectangle((int) x - 12, (int) y - 12, 24, 24).intersects(player.getBounds())) {
            player.hp = Math.min(player.maxHp, player.hp + HEAL_AMOUNT);
            collectedAt = time;
            SoundEffects.play(SoundEffects.Effect.PICKUP);
        }
        return false;
    }

    void draw(Graphics2D graphics, double camX, double camY, long time) {
        long age = Math.max(0, time - born);
        int frame = collectedAt >= 0 ? 11 + (int) Math.min(11, Math.max(0, time - collectedAt) / 50)
                : age < 300 ? (int) (age / 60) : 4 + (int) ((age - 300) / 100 % 7);
        int sx = (int) Math.round(x - camX), sy = (int) Math.round(y - camY);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (collectedAt < 0) {
            g.setColor(new Color(0, 0, 0, 75));
            g.fillOval(sx - 20, sy + 9, 40, 14);
            g.setColor(new Color(0, 0, 0, 115));
            g.fillOval(sx - 14, sy + 12, 28, 8);
        }
        g.drawImage(ANIMATION.frameAt(frame * 50L, 50), sx - 48, sy - 48, 96, 96, null);
        g.dispose();
    }
}
