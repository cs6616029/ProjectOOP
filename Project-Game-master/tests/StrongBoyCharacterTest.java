package project.game;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Timer;

public class StrongBoyCharacterTest {
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
    private static void checkDirection(Player player, int dx, int dy, String spriteField) throws Exception {
        player.move(dx, dy);
        var graphics = new BufferedImage(160, 160, BufferedImage.TYPE_INT_ARGB).createGraphics();
        player.draw(graphics, player.x - 80, player.y - 80);
        BufferedImage frame = (BufferedImage) get(player, "lastWalkingFrame");
        SpriteAnimation sprite = (SpriteAnimation) get(player, spriteField);
        boolean matches = false;
        for (int i = 0; i < 8; i++) if (frame == sprite.frameAt(i, 1)) matches = true;
        check(matches, "Correct animation for movement " + dx + "," + dy);
        check((boolean) get(player, "lastWalkingFrameMirrored") == (dx < 0 && dy == 0),
                "Mirroring only applies to leftward side animation");
        player.move(0, 0);
        player.draw(graphics, player.x - 80, player.y - 80);
        graphics.dispose();
        check(frame == get(player, "lastWalkingFrame"), "Stopping preserves last frame");
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (String resource : new String[]{"Strongman.png", "Strongman-South-East.png", "Strongman-North.png"}) {
            BufferedImage sheet = ImageIO.read(StrongBoyCharacterTest.class.getResource(
                    "/project/game/Monster/Player/" + resource));
            check(sheet != null && sheet.getWidth() / 8 == sheet.getHeight(), "Eight square frames: " + resource);
        }
        Player player = new Player(0, 0, Player.CharacterType.STRONG_BOY);
        check(player.getRenderSize() == 120 && player.size == 80, "Strong Boy is visually 50 percent larger with the original collision size");
        check(new Player(0, 0).getRenderSize() == 100
                && new Player(0, 0, Player.CharacterType.IRON_GUARDIAN).getRenderSize() == 100,
                "Swordsman and Iron Guardian are visually 25 percent larger");
        checkDirection(player, 0, 1, "southWalkSprite");
        checkDirection(player, 0, -1, "northWalkSprite");
        checkDirection(player, -1, -1, "northWalkSprite");
        checkDirection(player, 1, 0, "walkSprite");
        checkDirection(player, -1, 0, "walkSprite");

        Player.CharacterType[] selected = new Player.CharacterType[1];
        GameStartScreen screen = new GameStartScreen((character, map) -> selected[0] = character);
        try {
            var cardMethod = GameStartScreen.class.getDeclaredMethod("playerCard", int.class);
            cardMethod.setAccessible(true);
            Rectangle previous = null, strongCard = null;
            for (int i = 0; i < 3; i++) {
                Rectangle card = (Rectangle) cardMethod.invoke(screen, i);
                check(card.x >= 0 && card.x + card.width <= 800, "Character card fits screen");
                check(previous == null || !previous.intersects(card), "Cards do not overlap");
                previous = card;
                if (i == 2) strongCard = card;
            }
            var click = GameStartScreen.class.getDeclaredMethod("handleClick", int.class, int.class);
            click.setAccessible(true);
            click.invoke(screen, 400, 420);
            click.invoke(screen, strongCard.x + strongCard.width / 2, strongCard.y + 120);
            click.invoke(screen, 680, 540);
            click.invoke(screen, 660, 540);
            check(selected[0] == Player.CharacterType.STRONG_BOY, "Menu starts selected Strong Boy");
        } finally {
            ((Timer) get(screen, "animationTimer")).stop();
        }

        VampireSurvivorsGame game = new VampireSurvivorsGame(selected[0], "/project/game/Map/forest.jpg");
        List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
        Weapon aura = weapons.get(1);
        check(aura.getName().equals("Aurafarming") && aura.getLevel() == 1, "Strong Boy starts with Aurafarming Lv1");
        check(weapons.stream().filter(weapon -> weapon.getLevel() > 0).count() == 1, "Only starting aura is unlocked");
        player = (Player) get(game, "player");
        Enemy target = new Enemy(80, 0, EnemyType.TANK, 1);
        target.hp = 1000;
        aura.update(player, List.of(target), new ArrayList<>(), 10000);
        check(target.hp == 990, "Starting aura deals base damage");
        check(aura.getUpgradeDescription().contains("Radius 84 -> 99"), "Strong Boy aura starts proportionally larger");
        Enemy auraEdge = new Enemy(119, 0, EnemyType.NORMAL, 1);
        Enemy auraOutside = new Enemy(120, 0, EnemyType.NORMAL, 1);
        auraEdge.hp = auraOutside.hp = 1000;
        aura.update(player, List.of(auraEdge, auraOutside), new ArrayList<>(), 11000);
        check(auraEdge.hp == 990 && auraOutside.hp == 1000, "Larger aura damage radius matches its visual width");
        LevelManager manager = (LevelManager) get(game, "levelManager");
        manager.gainXP(10, player, weapons, new ArrayList<>());
        check(((List<?>) get(manager, "offeredUpgrades")).get(0) == aura, "Aurafarming is the fixed primary upgrade");
        manager.handleKeyPress(new KeyEvent(game, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_1, '1'), player);
        check(aura.getLevel() == 2, "Primary aura upgrade works");
        Field gameOver = game.getClass().getDeclaredField("gameOver");
        gameOver.setAccessible(true);
        gameOver.set(game, true);
        game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_R, 'R'));
        check(weapons.get(1).getLevel() == 1 && weapons.get(0).getLevel() == 0 && weapons.get(2).getLevel() == 0,
                "Restart restores Strong Boy aura loadout");
        check(((Player) get(game, "player")).getRenderSize() == 120, "Restart preserves larger Strong Boy");
        System.out.println("PASS: " + checks + " Strong Boy sprite, movement, menu, loadout, primary upgrade and restart checks");
    }
}
