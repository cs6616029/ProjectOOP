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
            Weapon primary = weapons.get(switch (character) {
                case SURVIVOR -> 0;
                case IRON_GUARDIAN -> 2;
                case STRONG_BOY -> 1;
            });
            manager.gainXP(50, null, weapons, new ArrayList<>());
            for (int choice = 0; choice < 3; choice++) {
                List<?> offers = (List<?>) get(manager, "offeredUpgrades");
                check(offers.size() == 5 && offers.get(0) == primary, "Primary slot: " + character);
                check(offers.stream().distinct().count() == 5, "Options must be unique");
                check(offers.stream().noneMatch(item -> item != primary &&
                        (item instanceof SwordWeapon || item instanceof GarlicWeapon || item instanceof LaserWeapon)),
                        "Other characters' signature skills never appear: " + character);
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
            Method generate = LevelManager.class.getDeclaredMethod("generateUpgradeOptions", List.class, List.class);
            generate.setAccessible(true);
            for (int roll = 0; roll < 100; roll++) {
                generate.invoke(manager, weapons, List.of());
                List<?> offers = (List<?>) get(manager, "offeredUpgrades");
                check(offers.stream().allMatch(item -> item == primary ||
                        !(item instanceof SwordWeapon || item instanceof GarlicWeapon || item instanceof LaserWeapon)),
                        "Signature exclusion survives repeated random rolls");
            }
            Weapon foreign = weapons.stream().filter(w -> w != primary &&
                    (w instanceof SwordWeapon || w instanceof GarlicWeapon || w instanceof LaserWeapon)).findFirst().orElseThrow();
            List<Object> staleOffers = (List<Object>) get(manager, "offeredUpgrades");
            staleOffers.clear(); staleOffers.add(foreign);
            Method select = LevelManager.class.getDeclaredMethod("selectUpgrade", int.class, Player.class);
            select.setAccessible(true); select.invoke(manager, 0, null);
            check(foreign.getLevel() == 0, "Selection guard rejects foreign signature even in stale options");
            primary.levelUp();
            manager.reset(); manager.setPrimaryWeapon(primary);
            manager.gainXP(10, null, List.of(primary, foreign), List.of());
            check(!manager.isLevelUping(), "Foreign skills do not keep upgrade menu open when own skills are maxed");
            Player player = new Player(0, 0, character);
            List<PassiveItem> passives = List.of(new PassiveItem(PassiveItem.ItemType.ARMOR),
                    new PassiveItem(PassiveItem.ItemType.BOOTS), new PassiveItem(PassiveItem.ItemType.GLOVES));
            manager.reset(); manager.setPrimaryWeapon(primary);
            manager.gainXP(50, player, weapons, passives);
            Weapon gun = weapons.stream().filter(w -> w instanceof GunWeapon).findFirst().orElseThrow();
            Weapon fire = weapons.stream().filter(w -> w instanceof FireStaffWeapon).findFirst().orElseThrow();
            generate.invoke(manager, List.of(primary, gun, fire), List.of());
            List<?> offers = (List<?>) get(manager, "offeredUpgrades");
            select.invoke(manager, offers.indexOf(gun), player);
            check(gun.getLevel() == 1 && weapons.stream().filter(w -> w.getLevel() > 0).count() == 2,
                    "Primary counts as first attack skill");
            generate.invoke(manager, List.of(primary, gun, fire), List.of());
            offers = (List<?>) get(manager, "offeredUpgrades");
            select.invoke(manager, offers.indexOf(fire), player);
            check(fire.getLevel() == 1 && weapons.stream().filter(w -> w.getLevel() > 0).count() == 3,
                    "Third attack skill can be selected");
            offers = (List<?>) get(manager, "offeredUpgrades");
            check(offers.size() == 5 && offers.contains(gun) && offers.contains(fire)
                    && offers.stream().allMatch(item -> item instanceof PassiveItem || item == gun || item == fire),
                    "After three unlocks, queued upgrades offer selected attacks and stats only");
            double cooldown = player.attackCooldownMultiplier;
            select.invoke(manager, offers.indexOf(passives.get(2)), player);
            check(player.attackCooldownMultiplier == cooldown * 0.85 && !manager.isLevelUping(),
                    "Attack speed selection applies and completes queued upgrades");
            manager.gainXP(33, player, weapons, passives);
            offers = (List<?>) get(manager, "offeredUpgrades");
            check(offers.stream().allMatch(item -> item instanceof PassiveItem || item == gun || item == fire),
                    "Future levels exclude new attack skills while keeping selected upgrades");
            int maxHp = player.maxHp;
            select.invoke(manager, offers.indexOf(passives.get(0)), player);
            check(player.maxHp == maxHp + 20, "Health upgrade remains functional");
            manager.gainXP(49, player, weapons, passives);
            offers = (List<?>) get(manager, "offeredUpgrades");
            double speed = player.speed;
            select.invoke(manager, offers.indexOf(passives.get(1)), player);
            check(player.speed == speed * 1.10, "Movement speed upgrade remains functional");
            Weapon fourth = weapons.stream().filter(w -> w instanceof TornadoWeapon).findFirst().orElseThrow();
            staleOffers = (List<Object>) get(manager, "offeredUpgrades"); staleOffers.clear(); staleOffers.add(fourth); staleOffers.add(gun);
            select.invoke(manager, 0, player);
            check(fourth.getLevel() == 0 && gun.getLevel() == 1,
                    "Cap blocks a fourth attack unlock even in stale offers");
            for (Weapon selected : List.of(gun, fire)) {
                while (selected.getLevel() < 5) {
                    manager.gainXP((int) get(manager, "xpToNextLevel"), player, weapons, passives);
                    offers = (List<?>) get(manager, "offeredUpgrades");
                    check(offers.contains(selected), "Selected attack stays upgradeable at the three-skill cap");
                    select.invoke(manager, offers.indexOf(selected), player);
                }
            }
            manager.gainXP((int) get(manager, "xpToNextLevel"), player, weapons, passives);
            offers = (List<?>) get(manager, "offeredUpgrades");
            check(offers.size() == 3 && offers.stream().allMatch(item -> item instanceof PassiveItem),
                    "Only stats remain once all three selected attacks reach level five");
            select.invoke(manager, 0, player);
            for (PassiveItem passive : passives) while (passive.getLevel() < passive.getMaxLevel()) passive.levelUp(player);
            manager.gainXP(100, player, weapons, passives);
            check(!manager.isLevelUping(), "Fully maxed stats do not trap the player in an empty menu");
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
        List<Weapon> weapons = List.of(primary, new GunWeapon(0), new LaserWeapon(0), new FireStaffWeapon(0),
                new GarlicWeapon(0), new SpearWeapon(0), new HolyWaterWeapon(0));
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
        System.out.println("PASS: " + checks + " five-option, character-exclusive skills, selection guards and primary-skill checks");
    }
}
