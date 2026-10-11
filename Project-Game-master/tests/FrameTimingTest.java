package project.game;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

public class FrameTimingTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 0.00001, message + ": " + actual + " vs " + expected);
    }
    private static Field field(String name) throws Exception {
        Field f = ISEKAISurvivors.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        SoundEffects.setSoundVolume(0);
        SoundEffects.setMusicVolume(0);
        for (int fps : new int[]{15, 30, 60, 120}) {
            Player p = new Player(0, 0);
            for (int i = 0; i < fps; i++) p.move(1, 0, 1.0 / fps);
            near(p.x, 252, "Same distance at " + fps + " FPS");
            p.x = p.y = 0;
            for (int i = 0; i < fps; i++) p.move(1, -1, 1.0 / fps);
            near(Math.hypot(p.x, p.y), 252, "Normalized diagonal at " + fps);
        }
        Player p = new Player(0, 0);
        for (double dt : new double[]{.01, .04, .20, .50, .25}) p.move(-1, 0, dt);
        near(p.x, -252, "Uneven frames preserve distance");
        p.move(1, 0, 0);
        near(p.x, -252, "Zero elapsed time does not move");
        p.speed *= 1.5;
        p.move(1, 0, 1);
        near(p.x, 126, "Movement upgrade scales timed movement");

        // Verify the gameplay update actually passes elapsed time into movement.
        ISEKAISurvivors game = new ISEKAISurvivors();
        Player controlled = (Player) field("player").get(game);
        ((Set<Integer>) field("activeKeys").get(game)).add(KeyEvent.VK_D);
        Method update = ISEKAISurvivors.class.getDeclaredMethod("updateGame");
        update.setAccessible(true);
        double startX = controlled.x;
        long startTime = field("activeTimeNanos").getLong(game);
        field("lastUpdateNanos").setLong(game, System.nanoTime() - 100_000_000L);
        update.invoke(game);
        double elapsed = (field("activeTimeNanos").getLong(game) - startTime) / 1e9;
        near(controlled.x - startX, controlled.speed * 60 * elapsed, "Game uses actual elapsed movement time");

        for (boolean skeleton : new boolean[]{false, true}) {
            for (long age : new long[]{500, 1000}) {
                Enemy e = new Enemy(0, 0, skeleton ? EnemyType.BOSS : EnemyType.NORMAL,
                        1, skeleton ? BossSkill.Kind.SKELETON : BossSkill.Kind.OGRE);
                p = new Player(30, 0);
                check(e.updateMeleeAttack(p, 1000) == 0, "Windup does not hit");
                check(e.updateMeleeAttack(p, 1000 + age) == e.damage, "Skipped swing still hits");
                check(e.updateMeleeAttack(p, 1000 + age) == 0, "No repeated damage at same clock");
                check(e.updateMeleeAttack(p, 1001 + age) == 0, "No repeated damage next update");
            }
        }
        for (int scenario = 0; scenario < 4; scenario++) {
            Enemy e = new Enemy(0, 0, EnemyType.NORMAL, 1, BossSkill.Kind.OGRE);
            p = new Player(30, 0);
            e.updateMeleeAttack(p, 1000);
            if (scenario == 0) p.x = 500;
            if (scenario == 1) p.x = -30;
            if (scenario == 2) e.hp = 0;
            if (scenario == 3) e.applyFrostHit(1100);
            check(e.updateMeleeAttack(p, 1500) == 0, "Dodge/death/freeze still prevents late hit: " + scenario);
            p.x = 30;
            check(e.updateMeleeAttack(p, 1501) == 0, "Miss cannot retry its hit");
        }
        System.out.println("PASS: frame-independent movement, real game integration, delayed melee impact, single hits, dodges, death and freeze");
    }
}
