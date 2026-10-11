package project.game;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class ExplosionEffectTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static int[] render(VisualEffect effect) {
        int size = effect.radius * 2;
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var g = output.createGraphics();
        effect.draw(g, effect.x - effect.radius, effect.y - effect.radius);
        g.dispose();
        return output.getRGB(0, 0, size, size, null, 0, size);
    }

    private static void verifyAnimation(VisualEffect effect) throws Exception {
        BufferedImage sheet = ImageIO.read(ExplosionEffectTest.class.getResource("/project/game/effects/explosion.png"));
        int frameWidth = sheet.getWidth() / 10, frameHeight = sheet.getHeight() / 2;
        int size = effect.radius * 2;
        for (int tick = 0; tick < 40; tick++) {
            int frame = tick / 2;
            BufferedImage expected = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            var g = expected.createGraphics();
            g.drawImage(sheet.getSubimage(frame % 10 * frameWidth, frame / 10 * frameHeight,
                    frameWidth, frameHeight), 0, 0, size, size, null);
            g.dispose();
            check(Arrays.equals(render(effect), expected.getRGB(0, 0, size, size, null, 0, size)),
                    "Explosion frame " + frame + " must match supplied sprite sheet");
            effect.update();
        }
        check(effect.isFinished(), "Explosion finishes after all 20 frames");
        check(Arrays.stream(render(effect)).allMatch(pixel -> pixel == 0), "Expired explosion no longer draws");
    }

    private static void verifyLaserAnimation(VisualEffect effect) throws Exception {
        BufferedImage sheet = ImageIO.read(ExplosionEffectTest.class.getResource("/project/game/effects/laser-hit.png"));
        int size = effect.radius * 2;
        for (int tick = 0; tick < 30; tick++) {
            BufferedImage expected = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            var g = expected.createGraphics();
            g.drawImage(sheet.getSubimage(tick / 3 * 96, 0, 96, 96), 0, 0, size, size, null);
            g.dispose();
            check(Arrays.equals(render(effect), expected.getRGB(0, 0, size, size, null, 0, size)),
                    "Laser impact frame must match supplied sprite sheet");
            effect.update();
        }
        check(effect.isFinished(), "Laser impact plays once through all ten frames");
        check(Arrays.stream(render(effect)).allMatch(pixel -> pixel == 0), "Expired laser impact no longer draws");
    }

    public static void main(String[] args) throws Exception {
        Player player = new Player(0, 0);
        Enemy tank = new Enemy(80, 0, EnemyType.TANK, 1);
        List<VisualEffect> effects = new ArrayList<>();
        new FireStaffWeapon(1).update(player, List.of(tank), effects, 10000);
        check(effects.size() == 1, "Fireball creates one explosion on impact");
        verifyAnimation(effects.get(0));

        effects.clear();
        Enemy dragon = new Enemy(0, 0, EnemyType.BOSS, 1, BossSkill.Kind.DRAGON);
        BossAttack fire = BossAttack.dragonFire(dragon, 0, 1000);
        player.x = 200;
        int damage = fire.update(player, 1900, effects);
        check(damage == 12 && effects.size() == 1, "Dragon fire deals damage and creates one explosion");
        check(Math.abs(effects.get(0).x - player.x) < 1, "Explosion appears at impact instead of overshooting");
        fire.update(player, 1910, effects);
        check(effects.size() == 1, "Consumed dragon fire does not duplicate explosions");
        verifyAnimation(effects.get(0));
        effects.clear();
        fire = BossAttack.dragonFire(dragon, 0, 1000);
        player.y = 300;
        check(fire.update(player, 1900, effects) == 0 && effects.isEmpty(), "Dodged fire does not explode on player");
        player.x = 0;
        player.y = 0;
        ShieldWeapon ring = new ShieldWeapon(1);
        Enemy first = new Enemy(80, 0, EnemyType.NORMAL, 1);
        Enemy second = new Enemy(83, 0, EnemyType.NORMAL, 1);
        Enemy dead = new Enemy(80, 0, EnemyType.NORMAL, 1);
        first.hp = second.hp = 1000;
        dead.hp = 0;
        ring.update(player, List.of(first, second, dead), effects, 10000);
        check(first.hp == 982 && second.hp == 982 && dead.hp == 0,
                "Ten Ring keeps contact damage and skips dead enemies");
        check(effects.size() == 1, "One ring impact creates one explosion even when hitting a crowd");
        check(Math.abs(effects.get(0).x - Math.cos(0.05) * 75) < 0.001
                && Math.abs(effects.get(0).y - Math.sin(0.05) * 75) < 0.001,
                "Ten Ring explosion appears at the ring impact position");
        ring.update(player, List.of(first, second), effects, 10010);
        check(effects.size() == 1 && first.hp == 982, "Cooldown does not duplicate ring hits or explosions");
        verifyAnimation(effects.get(0));
        effects.clear();
        ring.update(player, List.of(dead), effects, 10250);
        check(effects.isEmpty(), "Dead enemies do not trigger ring explosions");
        ring.update(player, List.of(new Enemy(400, 0, EnemyType.NORMAL, 1)), effects, 10500);
        check(effects.isEmpty(), "Missed rings do not explode");
        ring = new ShieldWeapon(2);
        first = new Enemy(80, 0, EnemyType.NORMAL, 1);
        second = new Enemy(-80, 0, EnemyType.NORMAL, 1);
        first.hp = second.hp = 1000;
        ring.update(player, List.of(first), effects, 20000);
        check(effects.size() == 1 && first.hp == 976, "First ring hits on its own cooldown");
        ring.update(player, List.of(first, second), effects, 20010);
        check(effects.size() == 2 && second.hp == 976 && first.hp == 976,
                "Second ring immediately hits and explodes while first ring is on cooldown");
        effects.clear();
        ring = new ShieldWeapon(1);
        first = new Enemy(125, 0, EnemyType.NORMAL, 1);
        first.hp = 1000;
        ring.update(player, List.of(first), effects, 30000);
        check(effects.size() == 1 && first.hp == 982, "Visible outer edge of ring triggers contact");
        effects.clear();
        ring = new ShieldWeapon(5);
        first = new Enemy(0, 0, EnemyType.TANK, 1);
        first.size = 1000;
        first.hp = 1;
        ring.update(player, List.of(first), effects, 40000);
        check(effects.size() == 5, "All five simultaneous contacts explode even on a killing hit");
        effects.clear();
        LaserWeapon laser = new LaserWeapon(1);
        first = new Enemy(100, 0, EnemyType.NORMAL, 1);
        second = new Enemy(200, 0, EnemyType.NORMAL, 1);
        Enemy missed = new Enemy(200, 150, EnemyType.NORMAL, 1);
        first.hp = second.hp = missed.hp = 1000;
        laser.update(player, List.of(first, second, dead, missed), effects, 50000);
        check(effects.isEmpty() && first.hp == 1000, "Traveling laser has no impact at launch");
        laser.update(player, List.of(second, first, dead, missed), effects, 50100);
        check(effects.size() == 1 && first.hp == 974 && second.hp == 1000 && missed.hp == 1000,
                "Laser explodes only on its first live target and skips misses and dead targets");
        check(effects.get(0).x == first.x && effects.get(0).y == first.y,
                "Laser explosion appears at the first hit target");
        laser.update(player, List.of(first, second), effects, 50110);
        check(effects.size() == 1 && first.hp == 974, "Laser does not repeat impact during cooldown");
        verifyLaserAnimation(effects.get(0));
        System.out.println("PASS: explosion sprite frames, fireball impact, dragon impact, one-shot playback and dodging");
        effects.clear();
        ring = new ShieldWeapon(5);
        Enemy boss = new Enemy(0, 0, EnemyType.BOSS, 1);
        boss.hp = 10000;
        ring.update(player, List.of(boss), effects, 60000);
        check(effects.size() == 1 && boss.hp == 9790,
                "Five rings create one boss explosion while all five deal damage");
        ring.update(player, List.of(boss), effects, 60250);
        check(effects.size() == 1 && boss.hp == 9580,
                "Boss explosion cooldown does not slow ring damage");
        ring.update(player, List.of(boss), effects, 60749);
        check(effects.size() == 1 && boss.hp == 9370,
                "Boss impact effects remain limited before 750 milliseconds");
        ring.update(player, List.of(boss), effects, 60750);
        check(effects.size() == 2 && boss.hp == 9370,
                "Boss gets only one new explosion after visual cooldown");
        verifyAnimation(effects.get(1));
        effects.clear();
        GarlicWeapon aura = new GarlicWeapon(1);
        first = new Enemy(40, 0, EnemyType.NORMAL, 1);
        second = new Enemy(0, 40, EnemyType.NORMAL, 1);
        missed = new Enemy(400, 0, EnemyType.NORMAL, 1);
        first.hp = second.hp = missed.hp = 1000;
        aura.update(player, List.of(first, second, dead, missed), effects, 70000);
        check(effects.size() == 2 && first.hp == 990 && second.hp == 990 && missed.hp == 1000 && dead.hp == 0,
                "Aura explodes for every live enemy hit, excluding dead and distant enemies");
        check(effects.get(0).x == first.x && effects.get(0).y == first.y
                && effects.get(1).x == second.x && effects.get(1).y == second.y,
                "Aura explosions occur at hit enemy positions");
        aura.update(player, List.of(first, second), effects, 70010);
        check(effects.size() == 2 && first.hp == 990, "Aura cooldown prevents repeated explosion every frame");
        verifyAnimation(effects.get(0));
        System.out.println("PASS: Ten Ring impacts, crowd hits, cooldown, dead enemies and shared Fire Staff animation");
        System.out.println("PASS: independent rings, visual contact edge, simultaneous killing hits and laser impact explosions");
        System.out.println("PASS: shared boss explosion cooldown preserves all ring damage");
        System.out.println("PASS: Aurafarming impact positions, cooldown and shared Fire Staff explosion frames");
    }
}
