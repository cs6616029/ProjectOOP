package project.game;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;

public final class SkeletonMeleeTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static Enemy boss() { return new Enemy(0, 0, EnemyType.BOSS, 1, BossSkill.Kind.SKELETON); }
    private static EnemyMeleeAttack attack(Enemy enemy) throws Exception {
        var field = Enemy.class.getDeclaredField("meleeAttack"); field.setAccessible(true);
        return (EnemyMeleeAttack) field.get(enemy);
    }
    public static void main(String[] args) throws Exception {
        BufferedImage sheet = ImageIO.read(SkeletonMeleeTest.class.getResource("/project/game/effects/skeleton-melee-slash.png"));
        check(sheet.getWidth() == 256 && sheet.getHeight() == 64, "Supplied slash has four frames");
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            Enemy enemy = boss(); Player player = new Player(150 * Math.cos(angle), 150 * Math.sin(angle));
            check(enemy.updateMeleeAttack(player, 10000) == 0, "Skeleton prepares at dash range");
            enemy.updateCombatMovement(player); check(enemy.x == 0 && enemy.y == 0, "Windup stops walking");
            check(enemy.updateMeleeAttack(player, 10279) == 0 && enemy.x == 0 && enemy.y == 0, "Dodgeable warning precedes dash");
            check(enemy.updateMeleeAttack(player, 10280) == 0, "Dash starts without instant damage");
            check(enemy.updateMeleeAttack(player, 10370) == 0, "Mid-dash does not slash yet");
            double halfDistance = Math.hypot(enemy.x, enemy.y);
            check(halfDistance > 20 && halfDistance < 50, "Boss physically moves gradually toward locked target");
            check(enemy.updateMeleeAttack(player, 10460) == enemy.damage, "Dash ends in one close-range sword hit");
            check(Math.abs(Math.hypot(enemy.x, enemy.y) - halfDistance * 2) < .0001, "Dash reaches its fixed endpoint");
            double x = enemy.x, y = enemy.y;
            for (int frame = 0; frame < 4; frame++) {
                check(enemy.updateMeleeAttack(player, 10460 + frame * 60) == 0, "Slash cannot repeat damage");
                BufferedImage actual = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
                var graphics = actual.createGraphics(); attack(enemy).draw(graphics, enemy, -250, -250); graphics.dispose();
                BufferedImage expected = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
                graphics = expected.createGraphics(); graphics.translate(x + 250, y + 250); graphics.rotate(angle);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                double radius = enemy.size * .25 + 18; int width = (int) Math.round(radius * 2.4);
                graphics.drawImage(sheet.getSubimage(frame * 64, 0, 64, 64),
                        (int) Math.round(radius * .3) - width / 2, -width / 2, width, width, null); graphics.dispose();
                check(Arrays.equals(actual.getRGB(0, 0, 500, 500, null, 0, 500), expected.getRGB(0, 0, 500, 500, null, 0, 500)),
                        "All four supplied slash frames face the dash direction");
            }
            enemy.updateMeleeAttack(player, 11050); check(!attack(enemy).isActive(), "Recovery completes");
            enemy.updateMeleeAttack(player, 11599); check(!attack(enemy).isActive(), "Cooldown prevents immediate repeated dash");
            enemy.updateMeleeAttack(player, 11600); check(attack(enemy).isActive(), "Next dash begins when cooldown ends");
            Enemy dodge = boss(); dodge.updateMeleeAttack(player, 20000);
            player.x = -player.x; player.y = -player.y;
            check(dodge.updateMeleeAttack(player, 20460) == 0, "Player can dodge behind the locked dash");
        }
        for (int interruption = 0; interruption < 3; interruption++) {
            Enemy enemy = boss(); Player player = new Player(150, 0);
            enemy.updateMeleeAttack(player, 10000); enemy.updateMeleeAttack(player, 10370);
            double stoppedX = enemy.x;
            if (interruption == 0) { enemy.applyFrostHit(10370); enemy.applyFrostHit(10370); }
            else if (interruption == 1) enemy.applyAirborne(10370);
            else enemy.hp = 0;
            check(enemy.updateMeleeAttack(player, 10460) == 0 && enemy.x == stoppedX, "Freeze, lift and death cancel dash and damage");
        }
        Enemy casting = boss(); Player player = new Player(150, 0);
        casting.updateBossSkill(player, new ArrayList<>(), 10000);
        casting.updateBossSkill(player, new ArrayList<>(), 11200);
        casting.updateMeleeAttack(player, 11200); check(!attack(casting).isActive(), "Ranged cast cannot overlap melee dash");
        Enemy far = boss(); player.x = 300; far.updateMeleeAttack(player, 10000);
        check(!attack(far).isActive(), "Melee dash has limited acquisition range");
        Enemy ogre = new Enemy(0, 0, EnemyType.BOSS, 1, BossSkill.Kind.OGRE); player.x = 150;
        ogre.updateMeleeAttack(player, 10000); check(!attack(ogre).isActive(), "Other bosses retain their existing melee range");
        System.out.println("PASS: Skeleton windup, gradual physical dash, four slash frames in eight directions, single hit, dodges, recovery, cooldown, interruption and casting exclusion");
    }
}
