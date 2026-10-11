package project.game;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class SwordOfJusticeTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static List<?> strikes(SwordOfJusticeWeapon skill) throws Exception {
        Field field = SwordOfJusticeWeapon.class.getDeclaredField("strikes");
        field.setAccessible(true);
        return (List<?>) field.get(skill);
    }
    private static Enemy enemy(double x) {
        Enemy enemy = new Enemy(x, 0, EnemyType.NORMAL, 1);
        enemy.hp = 1000;
        return enemy;
    }
    private static BufferedImage draw(SwordOfJusticeWeapon skill, Player player) {
        BufferedImage image = new BufferedImage(400, 500, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        skill.draw(graphics, player, -20, -350);
        graphics.dispose();
        return image;
    }
    public static void main(String[] args) throws Exception {
        Player player = new Player(0, 0);
        SwordOfJusticeWeapon skill = new SwordOfJusticeWeapon(1);
        check(skill.getName().equals("Sword of Justice"), "New skill name reaches HUD and upgrades");
        skill.update(player, List.of(), new ArrayList<>(), 10000);
        check(strikes(skill).isEmpty(), "No random ground pools or casts without enemies");
        Enemy target = enemy(180);
        List<VisualEffect> effects = new ArrayList<>();
        skill.update(player, List.of(target), effects, 10000);
        BufferedImage sheet = ImageIO.read(SwordOfJusticeTest.class.getResource("/project/game/effects/sword-of-justice.png"));
        for (int frame = 0; frame < 13; frame++) {
            skill.update(player, List.of(target), effects, 10000 + frame * 64);
            check(target.hp == (frame < 5 ? 1000 : 988), "Damage occurs once at the impact frame");
            check(effects.size() == (frame < 5 ? 0 : 1), "One nova starts exactly at impact, without repeating");
            BufferedImage actual = draw(skill, player);
            BufferedImage expected = new BufferedImage(400, 500, BufferedImage.TYPE_INT_ARGB);
            var graphics = expected.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            double progress = Math.min(1, frame * 64 / 320.0);
            int y = (int) Math.round(17.5 + 350 - 150 * (1 - progress * progress) - 192);
            if (frame < 5) graphics.drawImage(sheet.getSubimage(frame * 64, 0, 64, 128),
                    152, y, 248, y + 192, 0, 128, 64, 0, null);
            graphics.dispose();
            check(Arrays.equals(actual.getRGB(0, 0, 400, 500, null, 0, 400),
                    expected.getRGB(0, 0, 400, 500, null, 0, 400)), "Supplied frame and downward position: " + frame);
        }
        skill.update(player, List.of(target), new ArrayList<>(), 10832);
        check(strikes(skill).isEmpty(), "Strike completes without looping");
        BufferedImage novaSheet = ImageIO.read(SwordOfJusticeTest.class.getResource("/project/game/effects/holy-nova.png"));
        VisualEffect nova = effects.get(0);
        check(nova.x == 180 && nova.y == 17.5 && nova.radius == 60, "Nova is anchored at the blade impact with the damage radius");
        for (int tick = 0; tick < 40; tick++) {
            check(!nova.isFinished(), "Nova remains active through its final frame");
            BufferedImage actual = new BufferedImage(400, 500, BufferedImage.TYPE_INT_ARGB);
            var graphics = actual.createGraphics();
            nova.draw(graphics, -20, -350);
            graphics.dispose();
            BufferedImage expected = new BufferedImage(400, 500, BufferedImage.TYPE_INT_ARGB);
            graphics = expected.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(novaSheet.getSubimage(tick / 4 * 128, 0, 128, 64), 140, 337, 120, 60, null);
            graphics.dispose();
            check(Arrays.equals(actual.getRGB(0, 0, 400, 500, null, 0, 400),
                    expected.getRGB(0, 0, 400, 500, null, 0, 400)), "Supplied Holy Nova frame and aspect ratio: " + tick);
            nova.update();
        }
        check(nova.isFinished(), "All ten Nova frames complete once");
        for (int level = 1; level <= 5; level++) {
            skill = new SwordOfJusticeWeapon(level);
            Enemy first = enemy(80), second = enemy(350), far = enemy(600), dead = enemy(10);
            dead.hp = 0;
            List<Enemy> enemies = List.of(far, second, dead, first);
            skill.update(player, enemies, new ArrayList<>(), 10000);
            check(strikes(skill).size() == (level < 3 ? 1 : 2), "Distinct nearest living targets at each level");
            skill.update(player, enemies, new ArrayList<>(), 10319);
            check(first.hp == 1000 && second.hp == 1000, "No damage during fall");
            skill.update(player, enemies, new ArrayList<>(), 10320);
            check(first.hp == 1000 - skill.getDamage(), "First target hit");
            check(second.hp == (level < 3 ? 1000 : 1000 - skill.getDamage()), "Second sword unlocked at level three");
            check(far.hp == 1000 && dead.hp == 0, "Distant and dead enemies unaffected");
            skill.update(player, enemies, new ArrayList<>(), 12499);
            check(strikes(skill).isEmpty(), "No cast before cooldown");
            skill.update(player, enemies, new ArrayList<>(), 12500);
            check(!strikes(skill).isEmpty(), "Next cast on cooldown");
        }
        skill = new SwordOfJusticeWeapon(1);
        target = enemy(180);
        skill.update(player, List.of(target), new ArrayList<>(), 10000);
        skill.update(player, List.of(target), new ArrayList<>(), 10064);
        BufferedImage paused = draw(skill, player);
        skill.update(player, List.of(target), new ArrayList<>(), 10064);
        check(Arrays.equals(paused.getRGB(0, 0, 400, 500, null, 0, 400),
                draw(skill, player).getRGB(0, 0, 400, 500, null, 0, 400)), "Paused clock freezes descent");
        target.x = 400;
        skill.update(player, List.of(target), new ArrayList<>(), 10320);
        check(target.hp == 1000, "Moving away dodges the locked strike");
        skill = new SwordOfJusticeWeapon(1);
        target = enemy(180);
        Enemy nearby = enemy(230);
        skill.update(player, List.of(target, nearby), new ArrayList<>(), 10000);
        skill.update(player, List.of(target, nearby), new ArrayList<>(), 10320);
        check(target.hp == 988 && nearby.hp == 988, "Single impact damages nearby enemies in blast radius");
        skill.update(player, List.of(target, nearby), new ArrayList<>(), 10500);
        check(target.hp == 988 && nearby.hp == 988, "No lingering pool damage");
        System.out.println("PASS: Sword of Justice points down, Nova plays all ten frames once at impact, damage timing, target count, cooldown, pause, dodge and area damage");
    }
}
