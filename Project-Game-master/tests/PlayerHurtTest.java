package project.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;

public class PlayerHurtTest {
    private static Object get(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static BufferedImage render(Player p, long time) {
        p.gameTimeMillis = time;
        BufferedImage image = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        p.draw(g, 0, 0);
        g.dispose();
        return image;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (Player.CharacterType type : Player.CharacterType.values()) {
            Player p = new Player(120, 120, type);
            p.lastDamageTime = 1000;
            BufferedImage normal = render(p, 1450), hurt = render(p, 1000), blink = render(p, 1100);
            int changed = 0;
            for (int y = 0; y < 240; y++) for (int x = 0; x < 240; x++) {
                int a = normal.getRGB(x, y), b = hurt.getRGB(x, y);
                check((a >>> 24) == (b >>> 24), "Tint must preserve silhouette and shadow: " + type);
                check(a == blink.getRGB(x, y), "Blink returns original sprite: " + type);
                if (a != b) {
                    changed++;
                    check(((b >>> 16) & 255) > ((b >>> 8) & 255), "Hurt pixels are red");
                }
            }
            check(changed > 100, "Visible red tint: " + type);
        }
        VampireSurvivorsGame game = new VampireSurvivorsGame();
        Player player = (Player) get(game, "player");
        Field clock = game.getClass().getDeclaredField("gameTimeMillis");
        clock.setAccessible(true);
        clock.setLong(game, 1000);
        player.lastDamageTime = 0;
        var hit = game.getClass().getDeclaredMethod("damagePlayer", int.class);
        hit.setAccessible(true);
        List<DamageNumber> numbers = (List<DamageNumber>) get(game, "damageNumbers");
        hit.invoke(game, 7);
        check(player.hp == 73 && numbers.size() == 1 && numbers.get(0).damage == 7, "Accepted hit shows damage");
        hit.invoke(game, 9);
        hit.invoke(game, 0);
        check(player.hp == 73 && numbers.size() == 1, "Blocked hits show no damage");
        clock.setLong(game, 1450);
        hit.invoke(game, 100);
        check(player.hp == 0 && numbers.size() == 2, "Lethal hit shows damage");
        hit.invoke(game, 5);
        check(numbers.size() == 2, "No extra damage after death");
        System.out.println("PASS: player damage numbers, invincibility, lethal hits and red silhouette for all characters");
    }
}
