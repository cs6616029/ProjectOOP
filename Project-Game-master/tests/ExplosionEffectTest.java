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
        System.out.println("PASS: explosion sprite frames, fireball impact, dragon impact, one-shot playback and dodging");
    }
}
