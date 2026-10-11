package project.game;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.imageio.ImageIO;

public final class LevelUpEffectTest {
    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        BufferedImage sheet = ImageIO.read(LevelUpEffectTest.class.getResource("/project/game/effects/level-up.png"));
        for (Player.CharacterType type : Player.CharacterType.values()) {
            Player player = new Player(450, 337, type);
            LevelUpEffect effect = new LevelUpEffect(0);
            for (boolean paused : new boolean[] {false, true}) {
                for (int frame = 0; frame < LevelUpEffect.FRAME_COUNT; frame++) {
                    BufferedImage actual = new BufferedImage(900, 675, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = actual.createGraphics();
                    effect.draw(g, player, 0, 0, paused, frame * LevelUpEffect.FRAME_MILLIS * 1_000_000L);
                    g.dispose();
                    int bottom = 337 - player.getRenderSize() / 2 - 8;
                    if (paused) bottom = Math.min(bottom, 196);
                    BufferedImage expected = new BufferedImage(900, 675, BufferedImage.TYPE_INT_ARGB);
                    g = expected.createGraphics();
                    g.drawImage(sheet.getSubimage(frame % 6 * 320, frame / 6 * 128, 320, 128),
                            350, bottom - 80, 200, 80, null);
                    g.dispose();
                    for (int y = 0; y < 675; y++) {
                        for (int x = 350; x < 550; x++) {
                            check(actual.getRGB(x, y) == expected.getRGB(x, y), "Frame or character anchor mismatch");
                            if (paused && y >= 200) check(actual.getRGB(x, y) == 0, "Effect covers skill card");
                        }
                    }
                }
            }
            check(!effect.isFinished(1_979_000_000L) && effect.isFinished(1_980_000_000L), "One-shot duration");
            BufferedImage expired = new BufferedImage(900, 675, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = expired.createGraphics();
            effect.draw(g, player, 0, 0, false, 1_980_000_000L);
            g.dispose();
            check(expired.getRGB(400, 240) == 0, "Animation must not loop");

            ISEKAISurvivors game = new ISEKAISurvivors(type, "/project/game/Map/forest.jpg");
            player = (Player) get(game, "player");
            LevelManager manager = (LevelManager) get(game, "levelManager");
            List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
            List<PassiveItem> items = (List<PassiveItem>) get(game, "passiveItems");
            manager.gainXP(9, player, weapons, items);
            call(game, "renderToBuffer");
            check(get(game, "levelUpEffect") == null, "Sub-threshold XP must not trigger");
            manager.gainXP(1, player, weapons, items);
            call(game, "renderToBuffer");
            Object original = get(game, "levelUpEffect");
            check(original != null && manager.isLevelUping(), "Effect triggers when menu opens");
            long gameplayTime = (long) get(game, "gameTimeMillis");
            Field started = LevelUpEffect.class.getDeclaredField("startedAtNanos");
            started.setAccessible(true);
            started.setLong(original, System.nanoTime() - 500_000_000L);
            call(game, "updateGame");
            call(game, "renderToBuffer");
            check(get(game, "levelUpEffect") == original, "Rendering must not restart effect");
            check((long) get(game, "gameTimeMillis") == gameplayTime, "Animation must not advance gameplay");
            if (args.length > 0 && type == Player.CharacterType.SURVIVOR) {
                started.setLong(original, System.nanoTime() - 810_000_000L);
                call(game, "renderToBuffer");
                ImageIO.write((BufferedImage) get(game, "dbImage"), "png", new java.io.File(args[0]));
            }
            game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_1, '1'));
            call(game, "renderToBuffer");
            check(get(game, "levelUpEffect") == original, "Effect continues after selecting skill");
            started.setLong(original, System.nanoTime() - 2_000_000_000L);
            call(game, "renderToBuffer");
            check(get(game, "levelUpEffect") == null, "Expired effect is removed");
            manager.gainXP(15, player, weapons, items);
            call(game, "renderToBuffer");
            check(get(game, "levelUpEffect") != null, "Next level triggers another effect");
            call(game, "initGame");
            check(get(game, "levelUpEffect") == null, "Restart clears effect");
        }
        System.out.println("Level-up animation passed: all 44 frames, three characters, paused menu, expiry and restart.");
    }
}
