package project.game;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class BatRangedAttackTest {
    private static Object get(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static Enemy fastEnemy(int spriteIndex) throws Exception {
        Enemy enemy = new Enemy(240, 0, EnemyType.FAST, 1);
        Field sprites = Enemy.class.getDeclaredField("FAST_SPRITES");
        sprites.setAccessible(true);
        Field sprite = Enemy.class.getDeclaredField("sprite");
        sprite.setAccessible(true);
        sprite.set(enemy, ((SpriteAnimation[]) sprites.get(null))[spriteIndex]);
        return enemy;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Player player = new Player(0, 0);
        Enemy bat = fastEnemy(1);
        check(bat.isBat(), "Bat sprite identified");
        bat.update(0, 0);
        check(bat.x == 240, "Bat maintains firing distance");
        bat.x = 100;
        bat.update(0, 0);
        check(bat.x > 100, "Bat retreats when too close");
        bat.x = 500;
        bat.update(0, 0);
        check(bat.x < 500, "Bat approaches from outside firing range");
        bat.x = 240;
        List<BossAttack> shots = new ArrayList<>();
        bat.updateRangedAttack(player, shots, 0);
        bat.updateRangedAttack(player, shots, 999);
        check(shots.isEmpty(), "Initial shot has one second preparation");
        bat.updateRangedAttack(player, shots, 1000);
        check(shots.size() == 1, "Bat fires at range");
        check(shots.get(0).update(player, 1500) == 0, "Damage waits for projectile contact");
        check(shots.get(0).update(player, 2100) == bat.damage, "Projectile reaches player");
        check(shots.get(0).update(player, 2200) == 0, "Projectile damages only once");
        bat.updateRangedAttack(player, shots, 2999);
        check(shots.size() == 1, "Two second cooldown");
        bat.updateRangedAttack(player, shots, 3000);
        check(shots.size() == 2, "Second shot fires on cooldown");
        player.y = 100;
        check(shots.get(1).update(player, 4100) == 0, "Moving sideways dodges straight projectile");
        bat.hp = 0;
        check(shots.get(1).update(player, 4200) == 0 && shots.get(1).isFinished(), "Dead bat cancels projectile");
        bat.updateRangedAttack(player, shots, 5000);
        check(shots.size() == 2, "Dead bats cannot shoot");
        bat.hp = 10;
        bat.x = 451;
        player.y = 0;
        bat.updateRangedAttack(player, shots, 6000);
        check(shots.size() == 2, "No shots outside 450 pixel range");
        Enemy other = fastEnemy(0);
        check(!other.isBat(), "Other fast monster remains melee");
        other.update(0, 0);
        check(other.x < 240, "Other fast monster still chases");
        other.updateRangedAttack(player, shots, 0);
        other.updateRangedAttack(player, shots, 10000);
        check(shots.size() == 2, "Other monsters do not gain bat projectiles");

        VampireSurvivorsGame game = new VampireSurvivorsGame();
        Player gamePlayer = (Player) get(game, "player");
        ((List<Weapon>) get(game, "weapons")).clear();
        List<Enemy> enemies = (List<Enemy>) get(game, "enemies");
        enemies.clear();
        Enemy gameBat = fastEnemy(1);
        gameBat.x = gamePlayer.x + 240;
        gameBat.y = gamePlayer.y;
        enemies.add(gameBat);
        Field nextShot = Enemy.class.getDeclaredField("nextRangedAttack");
        nextShot.setAccessible(true);
        nextShot.setLong(gameBat, 0);
        var update = game.getClass().getDeclaredMethod("updateGame");
        update.setAccessible(true);
        update.invoke(game);
        check(((List<?>) get(game, "bossAttacks")).size() == 1, "Game loop spawns bat attack");
        System.out.println("PASS: bat movement, range, cooldown, projectile contact, dodging, death and game integration");
    }
}
