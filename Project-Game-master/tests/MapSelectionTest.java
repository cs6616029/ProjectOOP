package project.game;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

public class MapSelectionTest {
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
    private static void click(GameStartScreen screen, int x, int y) {
        MouseEvent event = new MouseEvent(screen, MouseEvent.MOUSE_CLICKED, 0, 0,
                x * 900 / 800, y * 675 / 600, 1, false, MouseEvent.BUTTON1);
        for (var listener : screen.getMouseListeners()) listener.mouseClicked(event);
    }
    public static void main(String[] args) throws Exception {
        String[] resources = {"forest", "desert", "night", "lava"};
        for (int index = 0; index < resources.length; index++) {
            String[] selected = new String[1];
            GameStartScreen screen = new GameStartScreen((character, map) -> selected[0] = map);
            try {
                screen.setSize(900, 675);
                click(screen, 400, 420);
                click(screen, 680, 540);
                var cardMethod = GameStartScreen.class.getDeclaredMethod("mapCard", int.class);
                cardMethod.setAccessible(true);
                List<Rectangle> cards = new ArrayList<>();
                for (int i = 0; i < resources.length; i++) {
                    Rectangle card = (Rectangle) cardMethod.invoke(screen, i);
                    check(card.x >= 0 && card.x + card.width <= 800 && card.y >= 150 && card.y + card.height < 520,
                            "Map card fits between header and buttons");
                    check(cards.stream().noneMatch(previous -> previous.intersects(card)), "Map cards do not overlap");
                    cards.add(card);
                }
                Rectangle card = cards.get(index);
                click(screen, card.x + card.width / 2, card.y + card.height / 2);
                check((int) get(screen, "selectedMap") == index, "Mouse selects map " + resources[index]);
                BufferedImage preview = new BufferedImage(900, 675, BufferedImage.TYPE_INT_ARGB);
                var graphics = preview.createGraphics();
                screen.paint(graphics);
                graphics.dispose();
                if (args.length > 0 && index == resources.length - 1) javax.imageio.ImageIO.write(preview, "png", new java.io.File(args[0]));
                click(screen, 660, 540);
                check(selected[0].equals("/project/game/Map/" + resources[index] + ".jpg"), "Start uses selected map");
                VampireSurvivorsGame game = new VampireSurvivorsGame(Player.CharacterType.STRONG_BOY, selected[0]);
                var render = game.getClass().getDeclaredMethod("renderToBuffer");
                render.setAccessible(true);
                render.invoke(game);
                check(get(game, "dbImage") instanceof BufferedImage, "Selected map renders in game");
            } finally {
                ((Timer) get(screen, "animationTimer")).stop();
            }
        }
        System.out.println("PASS: " + checks + " four-map layout, mouse selection, start and gameplay rendering checks");
    }
}
