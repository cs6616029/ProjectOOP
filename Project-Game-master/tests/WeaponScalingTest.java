package project.game;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class WeaponScalingTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
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
    public static void main(String[] args) throws Exception {
        int[] base = {28,10,26,14,22,32,12,18};
        int[] growth = {10,4,8,4,7,10,4,6};
        Player player = new Player(0,0);
        for (int kind = 0; kind < 8; kind++) {
            Weapon locked = weapon(kind, 0);
            Enemy target = new Enemy(80,0,EnemyType.TANK,1);
            target.hp = 10000;
            locked.update(player,List.of(target),new ArrayList<>(),10000);
            check(target.hp == 10000 && locked.getDamage() == 0, "Locked skill must not deal damage");
            Weapon upgraded = weapon(kind, 0);
            for (int level = 1; level <= 5; level++) {
                upgraded.levelUp();
                int expected = base[kind] + (level - 1) * growth[kind];
                check(upgraded.getDamage() == expected, "Upgrade scaling for " + upgraded.getName() + " Lv." + level);
                Weapon current = weapon(kind, level);
                target = new Enemy(80,0,EnemyType.NORMAL,1);
                target.hp = 10000;
                if (kind == 4) target.size = 150;
                if (kind == 7) target.size = 1000;
                List<VisualEffect> effects = new ArrayList<>();
                int hits = kind == 3 ? 2 : (kind == 7 ? level : (kind == 6 ? 1 + level / 3 : 1));
                if (kind == 6) {
                    current.update(player, new ArrayList<>(), effects, 10000);
                    for (Object zone : (List<?>) get(current, "zones")) {
                        set(zone, "x", 80.0);
                        set(zone, "y", 0.0);
                    }
                    current.update(player,List.of(target),effects,10200);
                } else {
                    current.update(player,List.of(target),effects,10000);
                    if (kind == 2) current.update(player,List.of(target),effects,10100);
                    if (kind == 3) {
                        for (int tick = 1; tick < 8; tick++) current.update(player,List.of(target),effects,10000 + tick * 16);
                    }
                }
                check(10000 - target.hp == expected * hits, "Real combat damage for " + current.getName() + " Lv." + level);
                check(current.getUpgradeDescription().contains(Integer.toString(expected)), "Upgrade UI shows current damage");
            }
            upgraded.levelUp();
            check(upgraded.getLevel() == 5, "Skill remains capped at level five");
        }
        Enemy edge = new Enemy(110,0,EnemyType.NORMAL,1);
        Enemy outside = new Enemy(125,0,EnemyType.NORMAL,1);
        new SwordWeapon(1).update(player,List.of(edge,outside),new ArrayList<>(),10000);
        check(edge.hp <= 0 && outside.hp > 0, "Reduced sword range preserves nearby hits");
        System.out.println("PASS: " + checks + " skill base values, level growth and combat damage checks");
    }
}
