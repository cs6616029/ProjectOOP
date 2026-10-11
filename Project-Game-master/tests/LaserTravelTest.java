package project.game;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LaserTravelTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Object get(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
    private static double tip(LaserWeapon laser) throws Exception {
        return Math.hypot((double) get(laser, "endX") - (double) get(laser, "startX"),
                (double) get(laser, "endY") - (double) get(laser, "startY"));
    }
    private static Enemy enemy(double x, double y, int hp) {
        Enemy enemy = new Enemy(x, y, EnemyType.NORMAL, 1);
        enemy.hp = hp;
        return enemy;
    }
    public static void main(String[] args) throws Exception {
        Player player = new Player(0, 0);
        LaserWeapon laser = new LaserWeapon(1);
        Enemy front = enemy(180, 0, 1000), back = enemy(270, 0, 1000);
        List<VisualEffect> effects = new ArrayList<>();
        laser.update(player, List.of(back, front), effects, 1000);
        check(tip(laser) == 0 && effects.isEmpty() && front.hp == 1000, "Laser starts at player without instant damage");
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        laser.draw(g, player, -50, -100);
        check(Arrays.stream(image.getRGB(0, 0, 400, 200, null, 0, 400)).allMatch(p -> p == 0), "Full beam does not flash on launch");
        laser.update(player, List.of(back, front), effects, 1020);
        double firstTip = tip(laser);
        check(firstTip > 0 && firstTip < 180 && front.hp == 1000, "Laser extends gradually before contact");
        laser.draw(g, player, -50, -100);
        g.dispose();
        check((image.getRGB(60, 100) >>> 24) != 0 && image.getRGB(250, 100) == 0, "Only traveled beam is drawn");
        laser.update(player, List.of(back, front), effects, 1020);
        check(tip(laser) == firstTip, "Paused gameplay clock freezes travel");
        player.x = 50;
        laser.update(player, List.of(back, front), effects, 1100);
        check((double) get(laser, "startX") == 0 && tip(laser) > firstTip, "Origin and direction stay fixed during flight");
        laser.update(player, List.of(back, front), effects, 1200);
        check(front.hp == 974 && back.hp == 1000 && effects.size() == 1, "Only first enemy is damaged");
        check(Math.abs(tip(laser) - 139) < 0.001, "Beam stops at first contact boundary");
        laser.update(player, List.of(back, front), effects, 1250);
        check(front.hp == 974 && effects.size() == 1, "Hit and effect are not repeated during hold");
        player.x = 0;

        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            Enemy victim = enemy(Math.cos(angle) * 180, Math.sin(angle) * 180, 1);
            Enemy behind = enemy(Math.cos(angle) * 270, Math.sin(angle) * 270, 1000);
            Enemy dead = enemy(Math.cos(angle) * 80, Math.sin(angle) * 80, 0);
            laser = new LaserWeapon(1);
            effects.clear();
            laser.update(player, List.of(behind, dead, victim), effects, 1000);
            laser.update(player, List.of(behind, dead, victim), effects, 1400);
            check(victim.hp <= 0 && behind.hp == 1000 && effects.size() == 1, "Swept first hit works in direction " + direction);
            laser.update(player, List.of(behind), effects, 1500);
            check(behind.hp == 1000 && effects.size() == 1, "Killing hit cannot continue through removed victim");
            laser.update(player, List.of(behind), effects, 1800);
            check(behind.hp == 1000, "Next shot also travels before hitting");
            laser.update(player, List.of(behind), effects, 2200);
            check(behind.hp == 974 && effects.size() == 2, "Next shot can hit next surviving enemy");
        }

        Enemy small = enemy(100, 0, 1000), large = enemy(150, 0, 1000);
        small.size = 10; large.size = 200;
        laser = new LaserWeapon(1); effects.clear();
        laser.update(player, List.of(small, large), effects, 1000);
        laser.update(player, List.of(small, large), effects, 1200);
        check(large.hp == 974 && small.hp == 1000, "Collision order uses body entry, rather than center or list order");

        Enemy dodger = enemy(180, 0, 1000);
        laser = new LaserWeapon(1); effects.clear();
        laser.update(player, List.of(dodger), effects, 1000);
        dodger.y = 1000;
        laser.update(player, List.of(dodger), effects, 1400);
        check(dodger.hp == 1000 && effects.isEmpty(), "Enemy can move out of firing line before impact");
        check(Math.abs(tip(laser) - 288) < 0.001, "Missed shot is capped at skill range");
        laser.update(player, List.of(dodger), effects, 1500);
        check(!(boolean) get(laser, "isShooting"), "Missed beam expires");
        System.out.println("PASS: gradual laser rendering, paused clock, first-contact order, eight directions, killing hits, cooldown, dodging and range");
    }
}
