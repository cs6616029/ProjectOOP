package project.game;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class BossHealthTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        SoundEffects.setSoundVolume(0);
        SoundEffects.setMusicVolume(0);
        ISEKAISurvivors game = new ISEKAISurvivors();
        Method spawn = ISEKAISurvivors.class.getDeclaredMethod("spawnBossAroundPlayer", long.class);
        spawn.setAccessible(true);
        Method reset = ISEKAISurvivors.class.getDeclaredMethod("initGame");
        reset.setAccessible(true);
        List<Enemy> enemies = (List<Enemy>) field(ISEKAISurvivors.class, "enemies").get(game);
        LevelManager levels = (LevelManager) field(ISEKAISurvivors.class, "levelManager").get(game);
        Field defeated = null;
        try { defeated = field(ISEKAISurvivors.class, "bossesDefeated"); }
        catch (NoSuchFieldException ignored) { /* Older project tracks spawn count. */ }
        for (int run = 0; run < 2; run++) {
            for (int index = 0; index < 4; index++) {
                field(LevelManager.class, "level").setInt(levels, 1 + index * 20);
                if (defeated != null) defeated.setInt(game, index);
                enemies.clear();
                spawn.invoke(game, 10L + index * 50L);
                Enemy boss = enemies.get(0);
                int expected = 1000 * (index + 1);
                check(boss.hp == expected && boss.maxHp == expected,
                        "Boss " + (index + 1) + " must spawn with " + expected + " HP regardless of level/time");
            }
            reset.invoke(game);
        }
        for (BossSkill.Kind kind : BossSkill.Kind.values()) {
            Enemy boss = new Enemy(0, 0, EnemyType.BOSS, 50, kind);
            check(boss.hp == 1000 && boss.maxHp == 1000, "Default boss health is 1000 for " + kind);
        }
        check(new Enemy(0, 0, EnemyType.TANK, 10).hp == 90, "Regular enemy health is unchanged");
        System.out.println("PASS: 1000/2000/3000/4000 boss HP and maxHP, level/time independence, restart and regular enemies");
    }
}
