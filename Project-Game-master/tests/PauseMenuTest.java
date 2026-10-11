package project.game;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PauseMenuTest {
    private static int checks;

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    private static Object get(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
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
        Method updateGame = ISEKAISurvivors.class.getDeclaredMethod("updateGame");
        updateGame.setAccessible(true);
        Method renderToBuffer = ISEKAISurvivors.class.getDeclaredMethod("renderToBuffer");
        renderToBuffer.setAccessible(true);

        check(!(boolean) get(game, "paused"), "Game starts unpaused");
        game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED));
        check((boolean) get(game, "paused"), "Escape pauses the game");
        long elapsedBeforePause = (long) get(game, "activeTimeNanos");
        updateGame.invoke(game);
        check((long) get(game, "activeTimeNanos") == elapsedBeforePause, "Game time remains frozen while paused");
        renderToBuffer.invoke(game);
        int pausedPanelColor = ((BufferedImage) get(game, "dbImage")).getRGB(280, 180);

        click(game, 450, 289);
        check(!(boolean) get(game, "paused"), "Resume button unpauses the game");
        renderToBuffer.invoke(game);
        check(((BufferedImage) get(game, "dbImage")).getRGB(280, 180) != pausedPanelColor,
                "Pause menu panel is rendered");
        click(game, 796, 32);
        check((boolean) get(game, "paused"), "Pause button pauses the game");
        click(game, 450, 369);
        check(!(boolean) get(game, "paused"), "Restart button resumes the game");
        check((long) get(game, "activeTimeNanos") == 0, "Restart resets elapsed game time");

        click(game, 796, 32);
        click(game, 450, 449);
        check(returnedToMenu[0], "Main menu button invokes the menu callback");
        check(!(boolean) get(game, "running"), "Returning to the menu stops the game thread");
        System.out.println("PASS: " + checks + " pause menu checks");
    }
}
