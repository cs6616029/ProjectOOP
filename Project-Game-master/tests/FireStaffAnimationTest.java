package project.game;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class FireStaffAnimationTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
    private static BufferedImage source(int frame) throws Exception {
        return ImageIO.read(FireStaffAnimationTest.class.getResource(String.format(
                "/project/game/weapons/Firestaff/FireBall/%03d.png", frame)));
    }
    static void verifyImpact(VisualEffect effect, double angle) throws Exception {
        int size = effect.radius * 2, width = effect.radius * 4;
        for (int tick = 0; tick < 20; tick++) {
            BufferedImage actual = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            var graphics = actual.createGraphics();
            effect.draw(graphics, effect.x - effect.radius, effect.y - effect.radius);
            graphics.dispose();
            BufferedImage expected = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            graphics = expected.createGraphics();
            graphics.translate(effect.radius, effect.radius);
            graphics.rotate(angle);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(source(6 + tick / 4), -width * 3 / 4, -width / 2, width, width, null);
            graphics.dispose();
            check(Arrays.equals(actual.getRGB(0, 0, size, size, null, 0, size),
                    expected.getRGB(0, 0, size, size, null, 0, size)), "Impact frame, position and direction: " + tick);
            effect.update();
        }
        check(effect.isFinished(), "Impact stops after frame 010 without looping");
    }
    public static void main(String[] args) throws Exception {
        Player player = new Player(0, 0);
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            FireStaffWeapon staff = new FireStaffWeapon(1);
            Enemy target = new Enemy(Math.cos(angle) * 400, Math.sin(angle) * 400, EnemyType.NORMAL, 1);
            target.hp = 10000;
            List<VisualEffect> effects = new ArrayList<>();
            for (int index = 0; index < 6; index++) {
                staff.update(player, List.of(target), effects, 10000 + index * 80);
                Object fireball = ((List<?>) get(staff, "fireballs")).get(0);
                double x = (double) get(fireball, "x"), y = (double) get(fireball, "y");
                BufferedImage actual = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
                var graphics = actual.createGraphics();
                staff.draw(graphics, player, x - 80, y - 80);
                graphics.dispose();
                BufferedImage expected = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB);
                graphics = expected.createGraphics();
                graphics.translate(80, 80);
                double shotAngle = Math.atan2((double) get(fireball, "dirY"), (double) get(fireball, "dirX"));
                graphics.rotate(shotAngle);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.drawImage(source(index % 5 + 1), -42, -28, 56, 56, null);
                graphics.dispose();
                check(Arrays.equals(actual.getRGB(0, 0, 160, 160, null, 0, 160),
                        expected.getRGB(0, 0, 160, 160, null, 0, 160)), "Flight frame loop and direction: " + direction + "/" + index);
                check(effects.isEmpty(), "Flying fire does not explode early");
            }
            verifyImpact(VisualEffect.fireStaffHit(0, 0, 51, angle), angle);
        }
        FireStaffWeapon staff = new FireStaffWeapon(1);
        Enemy tank = new Enemy(80, 0, EnemyType.TANK, 1);
        tank.hp = 1000;
        List<VisualEffect> effects = new ArrayList<>();
        staff.update(player, List.of(tank), effects, 10000);
        check(tank.hp == 978 && effects.size() == 1 && (boolean) get(effects.get(0), "fireStaffHit"), "Collision uses the new impact and original damage");
        staff.update(player, List.of(tank), effects, 10001);
        check(tank.hp == 978 && effects.size() == 1, "Fireball damages and bursts once");
        System.out.println("PASS: supplied Fire Staff frames, eight directions, flight loop, impact completion, damage and cooldown");
    }
}
