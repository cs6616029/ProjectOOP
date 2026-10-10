package project.game;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class BossCombatTest {
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

    private static void call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        BufferedImage blueSheet = javax.imageio.ImageIO.read(BossCombatTest.class.getResource(
                "/project/game/Monster/Boss/effects/BlueFlame.png"));
        Enemy previewDragon = new Enemy(0, 0, EnemyType.BOSS, 1, BossSkill.Kind.DRAGON);
        Player distantPlayer = new Player(5000, 5000);
        BufferedImage smokeSheet = javax.imageio.ImageIO.read(BossCombatTest.class.getResource(
                "/project/game/Monster/Boss/effects/Free_Smoke_Fx_Pixel_05.png"));
        BossAttack smoke = BossAttack.slam(previewDragon, 0, 0, 1000);
        for (int frameIndex = 0; frameIndex < 11; frameIndex++) {
            smoke.update(distantPlayer, 1000 + frameIndex * 45);
            BufferedImage actual = new BufferedImage(420, 420, BufferedImage.TYPE_INT_ARGB);
            var graphics = actual.createGraphics();
            smoke.draw(graphics, -210, -210);
            graphics.dispose();
            BufferedImage expected = new BufferedImage(420, 420, BufferedImage.TYPE_INT_ARGB);
            graphics = expected.createGraphics();
            int height = 400 * smokeSheet.getHeight() / 64;
            graphics.drawImage(smokeSheet.getSubimage(frameIndex * 64, 0, 64, smokeSheet.getHeight()),
                    10, 210 - height / 2, 400, height, null);
            graphics.dispose();
            check(java.util.Arrays.equals(actual.getRGB(0, 0, 420, 420, null, 0, 420),
                    expected.getRGB(0, 0, 420, 420, null, 0, 420)), "Ogre smoke plays supplied frame " + frameIndex);
        }
        smoke.update(distantPlayer, 1495);
        check(smoke.isFinished(), "Ogre smoke finishes once after all eleven frames");
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            BossAttack flame = BossAttack.dragonFire(previewDragon, angle, 1000);
            for (int frameIndex = 0; frameIndex < 5; frameIndex++) {
                flame.update(distantPlayer, 1000 + frameIndex * 90);
                BufferedImage actual = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
                var graphics = actual.createGraphics();
                flame.draw(graphics, (double) get(flame, "x") - 80, (double) get(flame, "y") - 80);
                graphics.dispose();
                BufferedImage expected = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
                graphics = expected.createGraphics();
                graphics.translate(80, 80);
                graphics.rotate(Math.atan2(Math.sin(angle), Math.cos(angle)) + Math.PI);
                int frameWidth = blueSheet.getWidth() / 4;
                int height = 64 * blueSheet.getHeight() / frameWidth;
                graphics.drawImage(blueSheet.getSubimage(frameIndex % 4 * frameWidth, 0,
                        frameWidth, blueSheet.getHeight()), -32, -height / 2, 64, height, null);
                graphics.dispose();
                check(java.util.Arrays.equals(actual.getRGB(0, 0, 160, 160, null, 0, 160),
                        expected.getRGB(0, 0, 160, 160, null, 0, 160)),
                        "Blue flame frame and loop for direction " + direction + ", frame " + frameIndex);
            }
        }
        for (BossSkill.Kind kind : BossSkill.Kind.values()) {
            Enemy boss = new Enemy(0, 0, EnemyType.BOSS, 1, kind);
            Player target = new Player(300, 0);
            List<BossAttack> attacks = new ArrayList<>();
            boss.updateBossSkill(target, attacks, 1000);
            boss.updateBossSkill(target, attacks, 3500);
            check(attacks.isEmpty(), kind + " must warn before attacking");
            boss.updateBossSkill(target, attacks, 3500 + kind.windup - 1);
            check(attacks.isEmpty(), kind + " cannot attack before windup ends");
            double oldX = boss.x;
            boss.update(target.x, target.y);
            check(boss.x == oldX, kind + " must hold position during windup");
            BufferedImage warning = new BufferedImage(900, 675, BufferedImage.TYPE_INT_ARGB);
            var g = warning.createGraphics();
            boss.drawBossWarning(g, -450, -337);
            g.dispose();
            check(warning.getRGB(460, 337) != 0, kind + " must draw warning area");
            long release = 3500 + kind.windup;
            boss.updateBossSkill(target, attacks, release);
            switch (kind) {
                case CHARGER:
                    boss.updateBossSkill(target, attacks, release + 400);
                    check(boss.x > 200 && Math.abs(boss.y) < 0.001 && boss.getContactDamage() == 14,
                            "Charge follows locked direction with increased contact damage");
                    target.y = 400;
                    boss.updateBossSkill(target, attacks, release + 500);
                    check(Math.abs(boss.y) < 0.001, "Charge must not home onto dodging player");
                    boss.updateBossSkill(target, attacks, release + 901);
                    check(boss.getContactDamage() == 7, "Charge damage resets after dash");
                    break;
                case DRAGON:
                    check(attacks.size() == 3, "Dragon fires three projectiles");
                    check(attacks.get(1).update(target, release + 1200) == 12,
                            "Dragon's central fireball hits a stationary target");
                    target.y = 500;
                    check(attacks.get(0).update(target, release + 1200) == 0, "Dragon fire can be dodged");
                    break;
                case OGRE:
                    check(attacks.size() == 1, "Ogre creates one slam");
                    check(attacks.get(0).update(target, release) == 0, "Outside slam is safe");
                    BossAttack slam = BossAttack.slam(boss, 0, 0, release);
                    target.x = 150;
                    check(slam.update(target, release) == 16 && slam.update(target, release + 10) == 0,
                            "Slam hits once inside its radius");
                    break;
                case SKELETON:
                    check(attacks.size() == 8, "Skeleton fires eight radial sword waves");
                    BufferedImage waves = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
                    var waveGraphics = waves.createGraphics();
                    for (BossAttack wave : attacks) wave.draw(waveGraphics, -100, -100);
                    waveGraphics.dispose();
                    check(java.util.Arrays.stream(waves.getRGB(0, 0, 200, 200, null, 0, 200))
                            .anyMatch(pixel -> (pixel >>> 24) != 0), "Attached sword-wave sprite renders in all directions");
                    target.x = 0;
                    target.y = 300;
                    check(attacks.get(2).update(target, release + 1400) == 10,
                            "Skeleton has a damaging vertical sword wave");
                    break;
            }
            int count = attacks.size();
            boss.updateBossSkill(target, attacks, release + 1500);
            check(attacks.size() == count, kind + " respects cooldown");
        }

        Enemy owner = new Enemy(0, 0, EnemyType.BOSS, 1, BossSkill.Kind.DRAGON);
        Player target = new Player(100, 0);
        BossAttack bolt = BossAttack.projectile(owner, 0, 500, 10, 12, java.awt.Color.ORANGE, 1000);
        check(bolt.update(target, 1400) == 12 && bolt.isFinished(), "Fast projectile swept collision prevents tunneling");
        check(bolt.update(target, 1410) == 0, "A projectile cannot hit twice");
        bolt = BossAttack.projectile(owner, 0, 500, 10, 12, java.awt.Color.ORANGE, 1000);
        owner.hp = 0;
        check(bolt.update(target, 1200) == 0 && bolt.isFinished(), "Dead boss attacks disappear");
        owner.hp = 100;
        BossAttack wave = BossAttack.swordWave(owner, 0, 1000);
        target.x = 200;
        target.y = 130;
        check(wave.update(target, 1900) == 0, "Player can dodge outside sword-wave width");
        wave = BossAttack.swordWave(owner, 0, 1000);
        target.y = 80;
        check(wave.update(target, 1900) == 10, "Sword-wave collision matches its larger width");
        bolt = BossAttack.projectile(owner, 0, 500, 10, 12, java.awt.Color.ORANGE, 1000);
        check(bolt.update(target, 4500) == 0 && bolt.isFinished(), "Missed projectiles expire");

        for (Player.CharacterType character : Player.CharacterType.values()) {
            VampireSurvivorsGame game = new VampireSurvivorsGame(character, "/project/game/Map/forest.jpg");
            List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
            int swordLevel = weapons.stream().filter(w -> w instanceof SwordWeapon).findFirst().get().getLevel();
            int laserLevel = weapons.stream().filter(w -> w instanceof LaserWeapon).findFirst().get().getLevel();
            check(swordLevel == (character == Player.CharacterType.SURVIVOR ? 1 : 0), "Starting sword: " + character);
            check(laserLevel == (character == Player.CharacterType.IRON_GUARDIAN ? 1 : 0), "Starting laser: " + character);
            if (character == Player.CharacterType.IRON_GUARDIAN) {
                Player player = (Player) get(game, "player");
                Enemy enemy = new Enemy(200, 0, EnemyType.TANK, 1);
                int hp = enemy.hp;
                weapons.stream().filter(w -> w instanceof LaserWeapon).findFirst().get()
                        .update(player, List.of(enemy), new ArrayList<>(), 10000);
                check(enemy.hp < hp, "Iron Guardian starting laser deals damage");
            }
            List<BossAttack> hazards = (List<BossAttack>) get(game, "bossAttacks");
            Player player = (Player) get(game, "player");
            long now = (long) get(game, "gameTimeMillis");
            hazards.add(BossAttack.slam(owner, player.x, player.y, now));
            hazards.add(BossAttack.slam(owner, player.x, player.y, now));
            call(game, "updateGame");
            check(player.hp == 64, "Simultaneous boss hits share invincibility frames");
            LevelManager levels = (LevelManager) get(game, "levelManager");
            levels.gainXP(10, player, weapons, new ArrayList<>());
            int hp = player.hp;
            long pausedTime = (long) get(game, "gameTimeMillis");
            set(game, "lastUpdateNanos", System.nanoTime() - 10_000_000_000L);
            call(game, "updateGame");
            check((long) get(game, "gameTimeMillis") == pausedTime && player.hp == hp,
                    "Upgrade menu pauses boss combat");
            check(hazards.size() == 2, "Pause does not advance attack expiry");
            set(game, "gameOver", true);
            game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_R, 'R'));
            check(hazards.isEmpty(), "Restart removes boss attacks");
            check(weapons.stream().filter(w -> w instanceof LaserWeapon).findFirst().get().getLevel() == laserLevel,
                    "Restart restores starting loadout");
        }
        System.out.println("PASS: " + checks + " boss combat and starting weapon checks");
    }
}
