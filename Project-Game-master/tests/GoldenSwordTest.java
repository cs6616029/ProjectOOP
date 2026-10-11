package project.game;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class GoldenSwordTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Object get(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Player p = new Player(0, 0);
        List<PassiveItem> passives = new ArrayList<>();
        for (PassiveItem.ItemType type : PassiveItem.ItemType.values()) passives.add(new PassiveItem(type));
        SwordWeapon sword = new SwordWeapon(5);
        Weapon gun = new GunWeapon(5), fire = new FireStaffWeapon(4);
        List<Weapon> loadout = List.of(sword, gun, fire, new TornadoWeapon(0));
        check(SwordWeapon.hasMaxedSword(loadout), "Only sword must be maxed; other attacks and stats can be incomplete");
        check(SwordWeapon.hasMaxedSword(List.of(sword)), "Maxed sword alone unlocks gold slash");
        check(!SwordWeapon.hasMaxedSword(List.of(new SwordWeapon(4), gun)), "Level four sword remains normal");
        check(!SwordWeapon.hasMaxedSword(List.of(gun)), "Other maxed weapons do not unlock gold slash");
        List<Enemy> surrounding = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Enemy enemy = new Enemy(Math.cos(i * Math.PI / 4) * 100, Math.sin(i * Math.PI / 4) * 100, EnemyType.NORMAL, 1);
            enemy.hp = 1000; surrounding.add(enemy);
        }
        Enemy outside = new Enemy(300, 0, EnemyType.NORMAL, 1); outside.hp = 1000; surrounding.add(outside);
        sword.setGoldenSlash(true);
        sword.update(p, surrounding, new ArrayList<>(), 10000);
        sword.update(p, surrounding, new ArrayList<>(), 10060);
        for (int i = 0; i < 8; i++) check(surrounding.get(i).hp == 932, "Golden sword hits all eight directions exactly once");
        check(outside.hp == 1000, "Golden sword preserves reach limit");
        SwordWeapon normal = new SwordWeapon(4);
        Enemy behind = new Enemy(-100, 0, EnemyType.NORMAL, 1); behind.hp = 1000;
        normal.update(p, List.of(behind), new ArrayList<>(), 10000);
        check(behind.hp == 1000, "Normal sword still uses forward arc");
        for (double speed : new double[] {1, .5, .7225}) {
            p.attackCooldownMultiplier = speed;
            SwordWeapon alternating = new SwordWeapon(5); alternating.setGoldenSlash(true);
            Enemy front = new Enemy(100, 0, EnemyType.NORMAL, 1); front.hp = 1000;
            Enemy rear = new Enemy(-100, 0, EnemyType.NORMAL, 1); rear.hp = 1000;
            long interval = (long) (1000 * speed);
            for (int swing = 0; swing < 6; swing++) {
                long time = 10000 + swing * interval;
                alternating.update(p, List.of(front, rear), new ArrayList<>(), time);
                check((boolean) get(alternating, "goldenAttack") == (swing % 2 == 0), "Gold and normal attacks alternate with attack speed");
                check(front.hp == 1000 - 68 * (swing + 1), "Forward target takes each swing once");
                check(rear.hp == 1000 - 68 * (swing / 2 + 1), "Rear target takes only gold attacks");
                alternating.update(p, List.of(front, rear), new ArrayList<>(), time + interval - 1);
                check(front.hp == 1000 - 68 * (swing + 1), "Attack cannot trigger before cooldown");
            }
            alternating.update(p, List.of(front, rear), new ArrayList<>(), 20000);
            check((boolean) get(alternating, "goldenAttack"), "Gold ready after a long gap following normal swing");
            alternating.update(p, List.of(front, rear), new ArrayList<>(), 30000);
            check(!(boolean) get(alternating, "goldenAttack"), "Even after a long gap, normal swing follows gold");
        }
        BufferedImage sheet = ImageIO.read(GoldenSwordTest.class.getResource("/project/game/effects/golden-sword-slash.png"));
        p.attackCooldownMultiplier = 1;
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            p.lastDirX = Math.cos(angle); p.lastDirY = Math.sin(angle);
            sword = new SwordWeapon(5); sword.setGoldenSlash(true);
            Enemy target = new Enemy(Math.cos(angle) * 80, Math.sin(angle) * 80, EnemyType.TANK, 1); target.hp = 1000;
            for (int frame = 0; frame < 5; frame++) {
                sword.update(p, List.of(target), new ArrayList<>(), 10000 + frame * 60);
                check(target.hp == 932, "Golden effect preserves sword damage and hits only once");
                BufferedImage actual = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
                var g = actual.createGraphics(); sword.draw(g, p, -250, -250); g.dispose();
                BufferedImage expected = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
                g = expected.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                double width = 122 * 2.4;
                for (int arc = 0; arc < 4; arc++) {
                    double a = Math.atan2(p.lastDirY, p.lastDirX) + arc * Math.PI / 2 + frame * 60 / 300.0 * Math.PI * 2;
                    AffineTransform t = new AffineTransform();
                    t.translate(250 + Math.cos(a) * 122 * .25, 250 + Math.sin(a) * 122 * .25);
                    t.rotate(a); t.scale(width / 64, width / 64); t.translate(-32, -32);
                    g.drawImage(sheet.getSubimage(frame * 64, 0, 64, 64), t, null);
                }
                g.dispose();
                check(Arrays.equals(actual.getRGB(0, 0, 500, 500, null, 0, 500), expected.getRGB(0, 0, 500, 500, null, 0, 500)),
                        "Five supplied gold frames render in all eight directions");
            }
            sword.update(p, List.of(target), new ArrayList<>(), 10300);
            check(!(boolean) get(sword, "isAttacking"), "Golden animation completes without looping");
        }
        for (Player.CharacterType character : Player.CharacterType.values()) {
            ISEKAISurvivors game = new ISEKAISurvivors(character, "/project/game/Map/forest.jpg");
            List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
            weapons.clear(); SwordWeapon main = new SwordWeapon(5);
            weapons.add(main);
            var update = game.getClass().getDeclaredMethod("updateGame"); update.setAccessible(true); update.invoke(game);
            check((boolean) get(main, "goldenSlash") == (character == Player.CharacterType.SURVIVOR),
                    "Gold sword unlock is exclusive to Swordman");
            var reset = game.getClass().getDeclaredMethod("initGame"); reset.setAccessible(true); reset.invoke(game);
            check(!SwordWeapon.hasMaxedSword((List<Weapon>) get(game, "weapons")),
                    "Restart resets the golden unlock requirements");
        }
        System.out.println("PASS: Sword-only max level unlock, rotating gold frames, damage in all directions once, reach limit, normal arc, character restriction and restart");
    }
}
