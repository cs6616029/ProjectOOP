package project.game;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

public class SpawnBalanceTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private static Object get(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target, value);
    }
    private static void call(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name); m.setAccessible(true); m.invoke(target);
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        int[] times = {0, 30, 60, 120, 180};
        double[][] expected = {{.85,.15,0},{.75,.20,.05},{.65,.25,.10},{.55,.30,.15},{.50,.30,.20}};
        Random random = new Random(42);
        for (int stage = 0; stage < times.length; stage++) {
            int[] counts = new int[3];
            for (int sample = 0; sample < 50000; sample++) {
                counts[SpawnBalance.typeForRoll(random.nextDouble(), times[stage]).ordinal()]++;
            }
            for (int type = 0; type < 3; type++) {
                check(Math.abs(counts[type] / 50000.0 - expected[stage][type]) < .01,
                        "Spawn distribution at " + times[stage] + " seconds, type " + type);
            }
        }
        long previous = Long.MAX_VALUE;
        for (int second = 0; second <= 600; second++) {
            long interval = SpawnBalance.intervalMillis(second, 10, false);
            check(interval <= previous && interval >= 500, "Smooth bounded spawn interval");
            previous = interval;
        }
        check(SpawnBalance.batchSize(59, false) == 1 && SpawnBalance.batchSize(60, false) == 2
                && SpawnBalance.batchSize(180, false) == 3, "Batch size grows by survival time");
        check(SpawnBalance.batchSize(600, true) == 1
                && SpawnBalance.intervalMillis(600, 20, true) > SpawnBalance.intervalMillis(600, 20, false),
                "Boss fights reduce ordinary spawn pressure");
        VampireSurvivorsGame game = new VampireSurvivorsGame();
        List<Enemy> enemies = (List<Enemy>) get(game, "enemies");
        for (int i = 0; i < 89; i++) enemies.add(new Enemy(800, 0, EnemyType.NORMAL, 1));
        set(game, "elapsedSeconds", 300L);
        call(game, "spawnEnemyAroundPlayer");
        call(game, "spawnEnemyAroundPlayer");
        check(enemies.size() == 90, "Regular enemy cap respected even for batches");
        call(game, "initGame");
        set(game, "lastUpdateNanos", System.nanoTime() - 75_000_000_000L);
        call(game, "updateGame");
        check(enemies.stream().filter(e -> e.type == EnemyType.BOSS).count() == 1, "First boss arrives at 75 seconds");
        set(game, "lastUpdateNanos", System.nanoTime() - 100_000_000_000L);
        call(game, "updateGame");
        check(enemies.stream().filter(e -> e.type == EnemyType.BOSS).count() == 1, "No overlapping bosses");
        Enemy boss = enemies.stream().filter(e -> e.type == EnemyType.BOSS).findFirst().get();
        boss.hp = 0;
        call(game, "updateGame");
        check(enemies.stream().noneMatch(e -> e.type == EnemyType.BOSS), "Dead boss does not cause immediate replacement");
        long elapsed = (long) get(game, "elapsedSeconds");
        check((long) get(game, "nextBossSpawnSeconds") == elapsed + 45, "45-second break after boss defeat");
        System.out.println("PASS: " + checks + " spawn balance checks");
    }
}
