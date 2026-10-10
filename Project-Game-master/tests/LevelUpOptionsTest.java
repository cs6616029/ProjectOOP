package project.game;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class LevelUpOptionsTest {
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
    private static KeyEvent key(int code) {
        return new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED);
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (Player.CharacterType character : Player.CharacterType.values()) {
            VampireSurvivorsGame game = new VampireSurvivorsGame(character, "/project/game/Map/forest.jpg");
            LevelManager manager = (LevelManager) get(game, "levelManager");
            List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
            Weapon primary = weapons.get(character == Player.CharacterType.SURVIVOR ? 0 : 2);
            manager.gainXP(50, null, weapons, new ArrayList<>());
            for (int choice = 0; choice < 3; choice++) {
                List<?> offers = (List<?>) get(manager, "offeredUpgrades");
                check(offers.size() == 5 && offers.get(0) == primary, "Primary slot: " + character);
                check(offers.stream().distinct().count() == 5, "Options must be unique");
                BufferedImage image = new BufferedImage(900, 675, BufferedImage.TYPE_INT_ARGB);
                var graphics = image.createGraphics();
                manager.drawLevelUpUI(graphics, 900, 675);
                graphics.dispose();
                Method boundsMethod = LevelManager.class.getDeclaredMethod("getUpgradeBounds", int.class, int.class);
                boundsMethod.setAccessible(true);
                Rectangle bounds = (Rectangle) boundsMethod.invoke(manager, 4, 900);
                check(bounds.y + bounds.height < image.getHeight(), "Fifth option must fit the window");
                manager.handleKeyPress(key(KeyEvent.VK_1), null);
            }
            check(primary.getLevel() == 4 && !manager.isLevelUping(), "Queued primary upgrades complete");
        }
        for (int code : new int[]{KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5}) {
            LevelManager manager = new LevelManager();
            List<Weapon> weapons = List.of(new SwordWeapon(0), new GunWeapon(0), new LaserWeapon(0),
                    new FireStaffWeapon(0), new GarlicWeapon(0));
            manager.gainXP(10, null, weapons, new ArrayList<>());
            int index = code >= KeyEvent.VK_NUMPAD1 ? code - KeyEvent.VK_NUMPAD1 : code - KeyEvent.VK_1;
            Weapon selected = (Weapon) ((List<?>) get(manager, "offeredUpgrades")).get(index);
            manager.handleKeyPress(key(code), null);
            check(selected.getLevel() == 1 && !manager.isLevelUping(), "Keyboard selection " + code);
        }
        LevelManager manager = new LevelManager();
        Weapon primary = new SwordWeapon(5);
        List<Weapon> weapons = List.of(primary, new GunWeapon(0), new LaserWeapon(0), new FireStaffWeapon(0), new GarlicWeapon(0));
        manager.setPrimaryWeapon(primary);
        manager.gainXP(10, null, weapons, new ArrayList<>());
        check(((List<?>) get(manager, "offeredUpgrades")).get(0) == primary, "Maxed primary stays in reserved slot");
        manager.handleKeyPress(key(KeyEvent.VK_1), null);
        check(manager.isLevelUping() && primary.getLevel() == 5, "Maxed primary does not consume choice");
        manager.handleKeyPress(key(KeyEvent.VK_5), null);
        check(!manager.isLevelUping(), "Other choices remain selectable");
        manager.reset();
        manager.setPrimaryWeapon(primary);
        manager.gainXP(50, null, List.of(primary), new ArrayList<>());
        check(!manager.isLevelUping(), "All maxed upgrades resume automatically");
        System.out.println("PASS: " + checks + " five-option and primary-skill checks");
    }
}
