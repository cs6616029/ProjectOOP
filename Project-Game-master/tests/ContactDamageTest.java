package project.game;

import java.lang.reflect.Field;
import java.util.List;

public class ContactDamageTest {
    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (Player.CharacterType character : Player.CharacterType.values()) {
            for (EnemyType type : new EnemyType[]{EnemyType.NORMAL, EnemyType.FAST, EnemyType.TANK, EnemyType.BOSS}) {
                BossSkill.Kind[] kinds = type == EnemyType.BOSS ? BossSkill.Kind.values() : new BossSkill.Kind[]{null};
                for (BossSkill.Kind kind : kinds) {
                ISEKAISurvivors game = new ISEKAISurvivors(character, "/project/game/Map/forest.jpg");
                Player player = (Player) get(game, "player");
                ((List<Weapon>) get(game, "weapons")).clear();
                List<Enemy> enemies = (List<Enemy>) get(game, "enemies");
                enemies.clear();
                Enemy enemy = new Enemy(player.x, player.y, type, 1, kind);
                enemy.hp = enemy.maxHp = 1000;
                if (type == EnemyType.FAST) {
                    Field sprites = Enemy.class.getDeclaredField("FAST_SPRITES");
                    sprites.setAccessible(true);
                    Field sprite = Enemy.class.getDeclaredField("sprite");
                    sprite.setAccessible(true);
                    sprite.set(enemy, ((SpriteAnimation[]) sprites.get(null))[0]);
                }
                enemy.speed = 0;
                double reach = enemy.size * 0.25 + player.size * 0.20;
                enemy.x = player.x + reach + 1;
                check(enemy.getBounds().intersects(player.getBounds()), "Old transparent margins would overlap");
                check(!enemy.isTouching(player), "Separated bodies must not touch: " + type);
                enemies.add(enemy);
                var update = game.getClass().getDeclaredMethod("updateGame");
                update.setAccessible(true);
                int hp = player.hp;
                update.invoke(game);
                check(player.hp == hp, "No premature contact damage: " + type);
                enemy.x = player.x + reach - 1;
                Field clock = game.getClass().getDeclaredField("activeTimeNanos");
                clock.setAccessible(true);
                long preparationNanos = enemy.isSkeletonBoss() ? 460_000_000L : 280_000_000L;
                clock.setLong(game, clock.getLong(game) + preparationNanos);
                update.invoke(game);
                check(player.hp == hp - enemy.damage, "Close swing deals damage after preparation: " + type);
                update.invoke(game);
                check(player.hp == hp - enemy.damage, "Contact respects invincibility: " + type);
                enemy.x = player.x + reach * 0.8;
                enemy.y = player.y + reach * 0.8;
                check(!enemy.isTouching(player), "Diagonal rectangle corners do not cause hits");
                enemy.x = player.x;
                enemy.y = player.y;
                check(enemy.isTouching(player), "Overlapping bodies always touch");
                enemy.hp = 0;
                check(!enemy.isTouching(player), "Dead enemies cannot deal contact damage");
                }
            }
        }
        System.out.println("PASS: body contact boundaries, diagonal dodging, damage and invincibility for all three characters, enemy types and four boss kinds");
    }
}
