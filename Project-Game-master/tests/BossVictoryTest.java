package project.game;

import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public class BossVictoryTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void click(ISEKAISurvivors game, int x, int y) {
        MouseEvent event = new MouseEvent(game, MouseEvent.MOUSE_PRESSED, 0, 0,
                x, y, 1, false, MouseEvent.BUTTON1);
        for (var listener : game.getMouseListeners()) listener.mousePressed(event);
    }

    public static void main(String[] args) throws Exception {
        boolean[] returnedToMenu = {false};
        ISEKAISurvivors game = new ISEKAISurvivors(
                Player.CharacterType.SURVIVOR, "/project/game/Map/forest.jpg",
                () -> returnedToMenu[0] = true);
        @SuppressWarnings("unchecked")
        List<Enemy> enemies = (List<Enemy>) get(game, "enemies");
        Method spawnBoss = ISEKAISurvivors.class.getDeclaredMethod("spawnBossAroundPlayer", long.class);
        spawnBoss.setAccessible(true);
        Method updateGame = ISEKAISurvivors.class.getDeclaredMethod("updateGame");
        updateGame.setAccessible(true);
        BossSkill.Kind[] sequence = {
            BossSkill.Kind.CHARGER,
            BossSkill.Kind.DRAGON,
            BossSkill.Kind.OGRE,
            BossSkill.Kind.SKELETON
        };

        for (int index = 0; index < sequence.length; index++) {
            spawnBoss.invoke(game, 0L);
            Enemy boss = enemies.stream().filter(enemy -> enemy.type == EnemyType.BOSS)
                    .findFirst().orElseThrow();
            Object skill = get(boss, "bossSkill");
            check(get(skill, "kind") == sequence[index], "Boss " + (index + 1) + " follows the expected sequence");
            boss.x = 5000;
            boss.y = 5000;
            boss.hp = 0;
            set(game, "lastSpawnTime", Long.MAX_VALUE);
            updateGame.invoke(game);
            check((int) get(game, "score") == (index + 1) * boss.scoreValue,
                    "Defeated boss adds to the score");

            boolean expectedVictory = index == sequence.length - 1;
            check((boolean) get(game, "gameWon") == expectedVictory,
                    expectedVictory ? "Final Skeleton defeat wins the game" : "Earlier boss does not end the game");
        }

        check((int) get(game, "score") == 960, "Final score includes all four bosses");
        check(!(boolean) get(game, "gameOver"), "Victory is distinct from game over");
        click(game, 450, 460);
        check(returnedToMenu[0], "Victory screen Main Menu button returns to the start screen");
        check(!(boolean) get(game, "running"), "Victory return stops the game thread");
        System.out.println("PASS: " + checks + " boss victory checks");
    }
}
