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
        check((boolean) get(shots.get(0), "batEnergy"), "Bat uses the supplied energy projectile");
        java.awt.image.BufferedImage flight = new java.awt.image.BufferedImage(80, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var flightGraphics = flight.createGraphics();
        shots.get(0).draw(flightGraphics, bat.x - 40, bat.y - 40);
        flightGraphics.dispose();
        java.awt.image.BufferedImage expectedFlight = new java.awt.image.BufferedImage(80, 80, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var expectedGraphics = expectedFlight.createGraphics();
        expectedGraphics.drawImage(BatAttackAnimation.PROJECTILE, 33, 33, 14, 14, null);
        expectedGraphics.dispose();
        check(java.util.Arrays.equals(flight.getRGB(0, 0, 80, 80, null, 0, 80),
                expectedFlight.getRGB(0, 0, 80, 80, null, 0, 80)), "Flying energy holds the first frame");
        check(shots.get(0).update(player, 1500) == 0, "Damage waits for projectile contact");
        List<VisualEffect> impacts = new ArrayList<>();
        check(shots.get(0).update(player, 2100, impacts) == bat.damage, "Projectile reaches player");
        check(impacts.size() == 1 && (boolean) get(impacts.get(0), "batHit"), "Contact creates the supplied explosion");
        check(shots.get(0).update(player, 2200, impacts) == 0 && impacts.size() == 1, "Projectile damages and explodes only once");
        VisualEffect impact = impacts.get(0);
        var sheet = javax.imageio.ImageIO.read(BatRangedAttackTest.class.getResource("/project/game/effects/bat-attack.png"));
        int cellWidth = sheet.getWidth() / 10;
        for (int tick = 0; tick < 30; tick++) {
            var actual = new java.awt.image.BufferedImage(160, 160, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var actualGraphics = actual.createGraphics();
            impact.draw(actualGraphics, impact.x - 80, impact.y - 80);
            actualGraphics.dispose();
            var expected = new java.awt.image.BufferedImage(160, 160, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var graphics = expected.createGraphics();
            graphics.drawImage(sheet.getSubimage(tick / 3 * cellWidth, 0, cellWidth, sheet.getHeight()), 20, 20, 120, 120, null);
            graphics.dispose();
            check(java.util.Arrays.equals(actual.getRGB(0, 0, 160, 160, null, 0, 160),
                    expected.getRGB(0, 0, 160, 160, null, 0, 160)), "Complete explosion frame at tick " + tick);
            check(!impact.isFinished(), "Explosion lasts through the last frame");
            impact.update();
        }
        check(impact.isFinished(), "Explosion ends without looping");
        bat.updateRangedAttack(player, shots, 2999);
        check(shots.size() == 1, "Two second cooldown");
        bat.updateRangedAttack(player, shots, 3000);
        check(shots.size() == 2, "Second shot fires on cooldown");
        player.y = 100;
        check(shots.get(1).update(player, 4100, impacts) == 0 && impacts.size() == 1, "Dodging causes no hit explosion");
        bat.hp = 0;
        check(shots.get(1).update(player, 4200, impacts) == 0 && shots.get(1).isFinished() && impacts.size() == 1, "Dead bat cancels projectile without explosion");
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

        ISEKAISurvivors game = new ISEKAISurvivors();
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
