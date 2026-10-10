package project.game;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class BossDeathEffectTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static int[] render(VisualEffect effect) {
        int size = effect.radius * 2;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        effect.draw(g, effect.x - effect.radius, effect.y - effect.radius);
        g.dispose();
        return image.getRGB(0, 0, size, size, null, 0, size);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (BossSkill.Kind kind : BossSkill.Kind.values()) {
            VampireSurvivorsGame game = new VampireSurvivorsGame();
            Player player = (Player) get(game, "player");
            ((List<Weapon>) get(game, "weapons")).clear();
            List<Enemy> enemies = (List<Enemy>) get(game, "enemies");
            enemies.clear();
            Enemy boss = new Enemy(player.x + 300, player.y, EnemyType.BOSS, 1, kind);
            boss.hp = 0;
            enemies.add(boss);
            Enemy normal = new Enemy(player.x + 400, player.y, EnemyType.NORMAL, 1);
            normal.hp = 0;
            enemies.add(normal);
            var update = game.getClass().getDeclaredMethod("updateGame");
            update.setAccessible(true);
            update.invoke(game);
            List<VisualEffect> effects = (List<VisualEffect>) get(game, "visualEffects");
            check(effects.size() == 1, kind + " death creates one effect; normal death creates none");
            VisualEffect effect = effects.get(0);
            check(effect.x == boss.x && effect.y == boss.y, "Death effect stays at death position");
            check(effect.radius * 2 >= boss.size, "Explosion covers the boss");
            check(((List<Gem>) get(game, "gems")).size() == 2, "Death rewards preserved");
            update.invoke(game);
            check(effects.size() == 1, "Death effect does not duplicate on next update");
        }

        VisualEffect effect = VisualEffect.bossDeath(100, 200, 250);
        int size = effect.radius * 2;
        int[] order = {1, 6, 2, 3, 4, 5};
        for (int tick = 0; tick < 36; tick++) {
            BufferedImage frame = ImageIO.read(BossDeathEffectTest.class.getResource(
                    "/project/game/effects/boss-death/" + order[tick / 6] + ".png"));
            BufferedImage expected = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            var g = expected.createGraphics();
            g.drawImage(frame, 0, 0, size, size, null);
            g.dispose();
            check(Arrays.equals(render(effect), expected.getRGB(0, 0, size, size, null, 0, size)),
                    "Death animation must match supplied frame at tick " + tick);
            effect.update();
        }
        check(effect.isFinished(), "Death animation plays once and expires");
        check(Arrays.stream(render(effect)).allMatch(pixel -> pixel == 0), "Expired effect draws nothing");
        System.out.println("PASS: all boss death triggers, rewards, six supplied frames and one-shot playback");
    }
}
