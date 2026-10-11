package project.game;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DamageNumberTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
    private static Weapon weapon(int kind, int level) {
        return switch (kind) {
            case 0 -> new SwordWeapon(level);
            case 1 -> new GarlicWeapon(level);
            case 2 -> new LaserWeapon(level);
            case 3 -> new GunWeapon(level);
            case 4 -> new FireStaffWeapon(level);
            case 5 -> new SpearWeapon(level);
            case 6 -> new HolyWaterWeapon(level);
            default -> new ShieldWeapon(level);
        };
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Player player = new Player(0, 0);
        for (int kind = 0; kind < 8; kind++) {
            for (int level : new int[]{1, 5}) {
                Weapon weapon = weapon(kind, level);
                Enemy target = new Enemy(80, 0, EnemyType.NORMAL, 1);
                target.hp = 10000;
                if (kind == 4) target.size = 150;
                if (kind == 7) target.size = 1000;
                List<VisualEffect> effects = new ArrayList<>();
                if (kind == 5) {
                    weapon.update(player, List.of(target), effects, 10000);
                    weapon.update(player, List.of(target), effects, 10480);
                } else if (kind == 6) {
                    weapon.update(player, List.of(target), effects, 10000);
                    weapon.update(player, List.of(target), effects, 10320);
                } else {
                    weapon.update(player, List.of(target), effects, 10000);
                    if (kind == 2) weapon.update(player, List.of(target), effects, 10100);
                    if (kind == 3) {
                        for (int tick = 1; tick < 8; tick++) weapon.update(player, List.of(target), effects, 10000 + tick * 16);
                    }
                }
                List<DamageNumber> numbers = new ArrayList<>();
                target.collectDamageNumbers(numbers);
                check(!numbers.isEmpty(), "Every skill generates damage numbers: " + weapon.getName());
                check(numbers.stream().allMatch(n -> n.damage == weapon.getDamage()), "Numbers show each hit's scaled damage");
                check(numbers.stream().mapToInt(n -> n.damage).sum() == 10000 - target.hp, "Numbers total actual damage");
                int count = numbers.size();
                target.collectDamageNumbers(numbers);
                check(numbers.size() == count, "Hits are collected once");
            }
        }
        Enemy boss = new Enemy(0, 0, EnemyType.BOSS, 1);
        boss.takeDamage(40);
        List<DamageNumber> numbers = new ArrayList<>();
        boss.collectDamageNumbers(numbers);
        check(numbers.size() == 1 && numbers.get(0).damage == 40, "Boss hits show damage");
        boss.takeDamage(0);
        boss.takeDamage(-10);
        boss.hp = 1;
        boss.takeDamage(28);
        boss.takeDamage(28);
        numbers.clear();
        boss.collectDamageNumbers(numbers);
        check(numbers.size() == 1 && numbers.get(0).damage == 28, "Killing hit shows full skill damage; dead targets generate none");

        DamageNumber number = new DamageNumber(100, 100, 28);
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        number.draw(g, 0, 0);
        check(Arrays.stream(image.getRGB(0, 0, 200, 200, null, 0, 200)).anyMatch(p -> (p >>> 24) != 0), "Number is visible");
        for (int tick = 0; tick < 48; tick++) number.update();
        check(number.isFinished(), "Number expires after 48 gameplay ticks");
        image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        g.dispose();
        g = image.createGraphics();
        number.draw(g, 0, 0);
        g.dispose();
        check(Arrays.stream(image.getRGB(0, 0, 200, 200, null, 0, 200)).allMatch(p -> p == 0), "Expired number does not draw");

        VampireSurvivorsGame game = new VampireSurvivorsGame();
        Player gamePlayer = (Player) get(game, "player");
        List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
        weapons.clear();
        weapons.add(new SwordWeapon(1));
        gamePlayer.lastDirX = 1;
        gamePlayer.lastDirY = 0;
        List<Enemy> enemies = (List<Enemy>) get(game, "enemies");
        enemies.clear();
        Enemy victim = new Enemy(gamePlayer.x + 80, gamePlayer.y, EnemyType.NORMAL, 1);
        enemies.add(victim);
        set(game, "gameTimeMillis", 10000L);
        var update = game.getClass().getDeclaredMethod("updateGame");
        update.setAccessible(true);
        update.invoke(game);
        check(!enemies.contains(victim), "Victim dies in game loop");
        check(((List<DamageNumber>) get(game, "damageNumbers")).stream().anyMatch(n -> n.damage == 28), "Killing hit survives enemy removal");
        var restart = game.getClass().getDeclaredMethod("initGame");
        restart.setAccessible(true);
        restart.invoke(game);
        check(((List<?>) get(game, "damageNumbers")).isEmpty(), "Restart clears floating numbers");
        System.out.println("PASS: all eight skills at levels 1 and 5, hit amounts, boss and killing hits, lifetime and game integration");
    }
}
