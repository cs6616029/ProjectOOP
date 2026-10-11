package project.game;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuraCooldownTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static boolean visible(AurafarmingWeapon aura, Player player) {
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics(); aura.draw(g, player, -200, -200); g.dispose();
        return Arrays.stream(image.getRGB(0, 0, 400, 400, null, 0, 400)).anyMatch(pixel -> (pixel >>> 24) != 0);
    }
    public static void main(String[] args) {
        Player p = new Player(0, 0);
        for (int level = 1; level <= 5; level++) {
            AurafarmingWeapon aura = new AurafarmingWeapon(level);
            Enemy target = new Enemy(40, 0, EnemyType.TANK, 1); target.hp = 1000;
            List<VisualEffect> effects = new ArrayList<>();
            int duration = 5000 + (level - 1) * 1000;
            for (int tick = 0; tick < duration / 200; tick++) aura.update(p, List.of(target), effects, 10000 + tick * 200);
            check(target.hp == 1000 - duration / 200 * level && visible(aura, p), "Aura burns through the full duration at each level");
            int hp = target.hp, count = effects.size();
            aura.update(p, List.of(target), effects, 10000 + duration);
            check(target.hp == hp && effects.size() == count && !visible(aura, p), "Cooldown hides aura and blocks damage and explosions");
            for (int repeat = 0; repeat < 5; repeat++) aura.update(p, List.of(target), effects, 11000 + duration);
            aura.update(p, List.of(target), effects, 10000 + duration * 2 - 1);
            check(target.hp == hp && !visible(aura, p), "Paused clock and cooldown cannot reactivate the aura early");
            aura.update(p, List.of(target), effects, 10000 + duration * 2);
            check(target.hp == hp - level && visible(aura, p), "Aura restarts after the full level-specific cooldown");
            aura.update(p, List.of(target), effects, 10000 + duration * 2);
            check(target.hp == hp - level, "Reactivation hits once per gameplay timestamp");
        }
        AurafarmingWeapon fast = new AurafarmingWeapon(1);
        p.attackCooldownMultiplier = .5;
        Enemy target = new Enemy(40, 0, EnemyType.TANK, 1); target.hp = 1000;
        List<VisualEffect> effects = new ArrayList<>();
        fast.update(p, List.of(target), effects, 10000);
        fast.update(p, List.of(target), effects, 15000);
        fast.update(p, List.of(target), effects, 17499);
        check(target.hp == 999 && !visible(fast, p), "Attack speed shortens rest without skipping it");
        fast.update(p, List.of(target), effects, 17500);
        check(target.hp == 998 && visible(fast, p), "Attack speed halves the five-second cooldown");
        AurafarmingWeapon locked = new AurafarmingWeapon(0);
        locked.update(p, List.of(target), effects, 14000);
        check(target.hp == 998 && !visible(locked, p), "Locked aura does not activate");
        System.out.println("PASS: Aura active duration, resting cooldown, hidden visuals, damage ticks, pause, level scaling and attack speed");
    }
}
