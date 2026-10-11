package project.game;

import java.awt.Font;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class MenuNavigationTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object get(GameStartScreen screen, String name) throws Exception {
        Field field = GameStartScreen.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(screen);
    }

    private static void action(GameStartScreen screen, String name) {
        screen.getActionMap().get(name).actionPerformed(null);
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (int hero = 0; hero < 3; hero++) {
                for (int map = 0; map < 4; map++) {
                    AtomicInteger starts = new AtomicInteger();
                    Player.CharacterType[] chosenHero = new Player.CharacterType[1];
                    String[] chosenMap = new String[1];
                    GameStartScreen screen = new GameStartScreen((character, resource) -> {
                        starts.incrementAndGet();
                        chosenHero[0] = character;
                        chosenMap[0] = resource;
                    });
                    try {
                        check(((Font) get(screen, "PIXEL_FONT")).getFontName().equals("BoldPixels"), "Attached font loads without fallback");
                        action(screen, "continue");
                        for (int i = 0; i < hero; i++) action(screen, "next");
                        action(screen, "continue");
                        for (int i = 0; i < map; i++) action(screen, "next");
                        action(screen, "back");
                        check((int) get(screen, "selectedCharacter") == hero, "Back preserves selected hero");
                        action(screen, "continue");
                        check((int) get(screen, "selectedMap") == map, "Back preserves selected map");
                        action(screen, "continue");
                        action(screen, "continue");
                        action(screen, "back");
                        action(screen, "next");
                        check(starts.get() == 1, "Repeated input cannot launch game twice");
                        check(chosenHero[0] == Player.CharacterType.values()[hero], "Launch receives chosen hero");
                        check(chosenMap[0].equals("/project/game/Map/" + new String[]{"forest", "desert", "night", "lava"}[map] + ".jpg"), "Launch receives chosen map");
                        check(!((Timer) get(screen, "animationTimer")).isRunning(), "Launch stops menu animation timer");
                    } catch (Exception exception) {
                        throw new RuntimeException(exception);
                    } finally {
                        screen.removeNotify();
                    }
                }
            }
        });
        System.out.println("PASS: BoldPixels loading, keyboard selection for all 12 hero/map combinations, back navigation and single launch");
    }
}
