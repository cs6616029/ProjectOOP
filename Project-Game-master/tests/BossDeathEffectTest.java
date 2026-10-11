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
            check(effects.size() == 2, kind + " death and normal death each create one effect");
            VisualEffect effect = effects.get(0);
            check(effect.x == boss.x && effect.y == boss.y, "Death effect stays at death position");
            check(effect.radius * 2 >= boss.size, "Explosion covers the boss");
            check(((List<Gem>) get(game, "gems")).size() == 2, "Death rewards preserved");
            update.invoke(game);
            check(effects.size() == 2, "Death effects do not duplicate on next update");
            check(effects.get(1).x == normal.x && effects.get(1).y == normal.y,
                    "Regular death effect stays at the enemy death position");
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
        BufferedImage sheet = ImageIO.read(BossDeathEffectTest.class.getResource("/project/game/effects/enemy-death.png"));
        for (EnemyType type : new EnemyType[] {EnemyType.NORMAL, EnemyType.FAST, EnemyType.TANK}) {
            VampireSurvivorsGame game = new VampireSurvivorsGame();
            ((List<Weapon>) get(game, "weapons")).clear();
            List<Enemy> enemies = (List<Enemy>) get(game, "enemies");
            Enemy enemy = new Enemy(300, 0, type, 1);
            enemy.hp = 0;
            enemies.add(enemy);
            var update = game.getClass().getDeclaredMethod("updateGame");
            update.setAccessible(true);
            update.invoke(game);
            List<VisualEffect> effects = (List<VisualEffect>) get(game, "visualEffects");
            check(effects.size() == 1 && enemies.isEmpty(), "Each regular enemy dies with exactly one effect");
            effect = effects.get(0);
            size = effect.radius * 2;
            for (int tick = 0; tick < 27; tick++) {
                BufferedImage expected = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                var g = expected.createGraphics();
                g.drawImage(sheet.getSubimage(tick / 3 * 96, 0, 96, 96), 0, 0, size, size, null);
                g.dispose();
                check(Arrays.equals(render(effect), expected.getRGB(0, 0, size, size, null, 0, size)),
                        "Regular death animation matches the nine supplied frames");
                effect.update();
            }
            check(effect.isFinished() && Arrays.stream(render(effect)).allMatch(pixel -> pixel == 0),
                    "Regular death effect finishes after one playback");
        }
        System.out.println("PASS: normal, fast and tank death triggers and all nine yellow explosion frames");
        System.out.println("PASS: all boss death triggers, rewards, six supplied frames and one-shot playback");
    }
}
