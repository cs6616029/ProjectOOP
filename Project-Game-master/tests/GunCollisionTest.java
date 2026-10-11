package project.game;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class GunCollisionTest {
    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Enemy enemy(double x, double y) {
        Enemy enemy = new Enemy(x, y, EnemyType.NORMAL, 1);
        enemy.hp = 1000;
        enemy.size = 2;
        return enemy;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            double dx = Math.cos(angle), dy = Math.sin(angle);
            Player player = new Player(0, 0);
            GunWeapon gun = new GunWeapon(1);
            Enemy target = enemy(dx * 300, dy * 300);
            List<VisualEffect> effects = new ArrayList<>();
            gun.update(player, List.of(target), effects, 10000);
            List<?> bullets = (List<?>) get(gun, "bullets");
            check(bullets.size() == 2, "Two-bullet burst is preserved");
            bullets.remove(1);
            Object bullet = bullets.get(0);
            Field speed = bullet.getClass().getDeclaredField("speed");
            speed.setAccessible(true);
            speed.setDouble(bullet, 120);
            Enemy front = enemy(dx * 40, dy * 40);
            Enemy behind = enemy(dx * 80, dy * 80);
            Enemy dead = enemy(dx * 20, dy * 20);
            dead.hp = 0;
            Enemy missed = enemy(dx * 40 - dy * 20, dy * 40 + dx * 20);
            List<Enemy> enemies = List.of(behind, dead, missed, front, target);
            gun.update(player, enemies, effects, 10016);
            check(front.hp == 986 && behind.hp == 1000 && target.hp == 1000 && missed.hp == 1000,
                    "Bullet must hit first living enemy along its path regardless of list order");
            check(bullets.isEmpty() && effects.size() == 1, "Bullet is consumed after one impact");
            check(Math.abs(effects.get(0).x - dx * 35) < 0.00001
                            && Math.abs(effects.get(0).y - dy * 35) < 0.00001,
                    "Impact appears at first contact rather than the end of the movement");
            gun.update(player, enemies, effects, 10032);
            check(behind.hp == 1000 && effects.size() == 1, "Consumed bullet cannot hit another enemy");
        }
        System.out.println("PASS: non-piercing bullets, nearest contact, swept collisions and eight firing directions");
    }
}
