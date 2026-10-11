package project.game;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class StrongBoyPunchTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Object get(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    private static Enemy enemy(double x, double y) {
        Enemy e = new Enemy(x, y, EnemyType.NORMAL, 1); e.hp = 1000; return e;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Player p = new Player(0, 0, Player.CharacterType.STRONG_BOY);
        BufferedImage sheet = ImageIO.read(StrongBoyPunchTest.class.getResource("/project/game/effects/strong-boy-punch.png"));
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            StrongBoyPunch punch = new StrongBoyPunch();
            Enemy front = enemy(Math.cos(angle) * 80, Math.sin(angle) * 80);
            Enemy back = enemy(-Math.cos(angle) * 90, -Math.sin(angle) * 90);
            Enemy far = enemy(Math.cos(angle) * 150, Math.sin(angle) * 150);
            List<Enemy> enemies = List.of(front, back, far);
            punch.update(p, enemies, 10000);
            punch.update(p, enemies, 10090);
            check(front.hp == 1000 && p.attackOffsetX * Math.cos(angle) + p.attackOffsetY * Math.sin(angle) < -6,
                    "Windup pulls Strong Boy back before the hit");
            for (int frame = 0; frame < 7; frame++) {
                punch.update(p, enemies, 10180 + frame * 60);
                check(front.hp == (frame == 0 ? 1000 : 982), "One punch hit at the second animation frame");
                check(back.hp == 1000 && far.hp == 1000, "Punch respects its direction and reach");
                BufferedImage actual = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
                var g = actual.createGraphics(); punch.draw(g, -200, -200); g.dispose();
                BufferedImage expected = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
                if (frame == 1) check(Math.abs(Math.hypot(p.attackOffsetX, p.attackOffsetY) - 26) < .001,
                        "Strong Boy lunges forward at the damage moment");
                g = expected.createGraphics(); g.translate(200 + p.attackOffsetX, 200 + p.attackOffsetY);
                g.rotate(Math.atan2(front.y, front.x));
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(sheet.getSubimage(frame * 64, 0, 64, 64), -34, -64, 128, 128, null); g.dispose();
                check(Arrays.equals(actual.getRGB(0, 0, 400, 400, null, 0, 400), expected.getRGB(0, 0, 400, 400, null, 0, 400)),
                        "Seven supplied punch frames render in every direction: " + direction + "/" + frame);
            }
            List<DamageNumber> numbers = new ArrayList<>(); front.collectDamageNumbers(numbers);
            check(numbers.size() == 1 && numbers.get(0).damage == 18, "Innate punch reports damage numbers");
            punch.update(p, enemies, 10600); check(!(boolean) get(punch, "attacking") && p.attackOffsetX == 0 && p.attackOffsetY == 0,
                    "Punch completes and returns Strong Boy to his resting position");
            punch.update(p, enemies, 10899); check(!(boolean) get(punch, "attacking"), "Cooldown is respected");
            punch.update(p, enemies, 10900); check((boolean) get(punch, "attacking"), "Punch repeats automatically");
            punch.update(p, enemies, 10900); check(front.hp == 982, "Paused clock does not repeat damage");
        }
        StrongBoyPunch punch = new StrongBoyPunch(); Enemy target = enemy(80, 0);
        p.attackCooldownMultiplier = .5;
        punch.update(p, List.of(target), 10000); punch.update(p, List.of(target), 10600);
        check((long) get(punch, "started") == 10600, "Attack speed passive affects innate punch");
        p.hp = 0; punch.update(p, List.of(target), 10660);
        check(target.hp == 982 && !(boolean) get(punch, "attacking"), "Dead player cannot continue punching");
        for (Player.CharacterType character : Player.CharacterType.values()) {
            VampireSurvivorsGame game = new VampireSurvivorsGame(character, "/project/game/Map/forest.jpg");
            Object innate = get(game, "strongBoyPunch");
            check((innate != null) == (character == Player.CharacterType.STRONG_BOY), "Punch belongs only to Strong Boy");
            List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
            check(weapons.size() == 9 && weapons.stream().filter(w -> w.getLevel() > 0).count() == 1,
                    "Innate punch consumes no upgrade slot or attack skill allowance");
            if (innate != null) {
                Player gp = (Player) get(game, "player");
                weapons.clear();
                List<Enemy> enemies = (List<Enemy>) get(game, "enemies"); enemies.clear();
                Enemy victim = enemy(gp.x + 80, gp.y); enemies.add(victim);
                var update = game.getClass().getDeclaredMethod("updateGame"); update.setAccessible(true); update.invoke(game);
                Field activeTime = game.getClass().getDeclaredField("activeTimeNanos"); activeTime.setAccessible(true);
                activeTime.set(game, (long) activeTime.get(game) + 240_000_000L);
                update.invoke(game);
                check(victim.hp == 982, "Strong Boy punches in the game loop without any upgradeable weapons");
                var restart = game.getClass().getDeclaredMethod("initGame"); restart.setAccessible(true); restart.invoke(game);
                check(get(game, "strongBoyPunch") != innate, "Restart resets innate attack cooldown and animation");
            }
        }
        System.out.println("PASS: innate Strong Boy punch, seven frames, eight directions, damage, cooldown, attack speed, death, slot exclusion and game integration");
    }
}
