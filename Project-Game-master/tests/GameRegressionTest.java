package project.game;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;

/** Run with tests/run-tests.ps1; does not open a window. */
public final class GameRegressionTest {
    private static int checks;
    private static final JPanel INPUT = new JPanel();

    private static Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object get(Object target, String name) throws Exception {
        return field(target, name).get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        field(target, name).set(target, value);
    }

    private static void call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }

    private static KeyEvent key(JPanel panel, int code) {
        return new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED);
    }

    private static MouseEvent click(JPanel panel, int x, int y, int button) {
        return new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, button);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        LevelManager manager = new LevelManager();
        Weapon weapon = new GunWeapon(0);
        manager.gainXP(50, null, Arrays.asList(weapon), Collections.emptyList());
        check(manager.getLevel() == 4 && (int) get(manager, "xp") == 3, "Large XP must process all thresholds");
        for (int i = 0; i < 3; i++) {
            check(manager.isLevelUping(), "Every gained level needs an upgrade");
            manager.handleKeyPress(key(INPUT, KeyEvent.VK_1), null);
        }
        check(weapon.getLevel() == 3 && !manager.isLevelUping(), "Three levels must grant three upgrades");

        manager = new LevelManager();
        weapon = new GunWeapon(0);
        manager.gainXP(10, null, Arrays.asList(weapon), Collections.emptyList());
        Object firstOffer = ((List<?>) get(manager, "offeredUpgrades")).get(0);
        manager.gainXP(15, null, Arrays.asList(weapon), Collections.emptyList());
        check(((List<?>) get(manager, "offeredUpgrades")).get(0) == firstOffer, "New XP must preserve current offer");
        manager.handleKeyPress(key(INPUT, KeyEvent.VK_1), null);
        manager.handleKeyPress(key(INPUT, KeyEvent.VK_1), null);
        check(weapon.getLevel() == 2 && !manager.isLevelUping(), "Queued XP must retain both upgrades");

        for (int initialLevel : new int[]{4, 5}) {
            manager = new LevelManager();
            weapon = new GunWeapon(initialLevel);
            manager.gainXP(50, null, Arrays.asList(weapon), Collections.emptyList());
            if (manager.isLevelUping()) manager.handleKeyPress(key(INPUT, KeyEvent.VK_1), null);
            check(weapon.getLevel() == 5 && !manager.isLevelUping(), "Maxed pool must resume automatically");
        }
        for (int row = 0; row < 5; row++) {
            manager = new LevelManager();
            List<Weapon> weapons = Arrays.asList(new SwordWeapon(0), new GarlicWeapon(0), new GunWeapon(0),
                    new LaserWeapon(0), new FireStaffWeapon(0));
            manager.gainXP(10, null, weapons, Collections.emptyList());
            Weapon selected = (Weapon) ((List<?>) get(manager, "offeredUpgrades")).get(row);
            manager.handleMousePress(click(INPUT, 450, 200 + row * 82 + 30, MouseEvent.BUTTON3), null, 900);
            manager.handleMousePress(click(INPUT, 160, 230, MouseEvent.BUTTON1), null, 900);
            manager.handleMousePress(click(INPUT, 450, 280, MouseEvent.BUTTON1), null, 900);
            check(manager.isLevelUping(), "Right click, outside click and gap must do nothing");
            manager.handleMousePress(click(INPUT, 450, 200 + row * 82 + 30, MouseEvent.BUTTON1), null, 900);
            check(selected.getLevel() == 1 && !manager.isLevelUping(), "Mouse row " + row);
        }
        Player player = new Player(0, 0);
        player.hp = 20;
        PassiveItem armor = new PassiveItem(PassiveItem.ItemType.ARMOR);
        manager = new LevelManager();
        manager.gainXP(10, player, Collections.emptyList(), Arrays.asList(armor));
        manager.handleMousePress(click(INPUT, 450, 230, MouseEvent.BUTTON1), player, 900);
        check(armor.getLevel() == 1 && player.maxHp == 100 && player.hp == 50, "Passive effect and level healing");

        Enemy tank = new Enemy(80, 0, EnemyType.TANK, 1);
        int hp = tank.hp;
        List<VisualEffect> effects = new ArrayList<>();
        new FireStaffWeapon(1).update(player, Arrays.asList(tank), effects, 10000);
        check(effects.size() == 1 && tank.hp < hp, "Fireball must damage tank on impact");
        Enemy dead = new Enemy(10, 0, EnemyType.NORMAL, 1);
        dead.hp = 0;
        Enemy alive = new Enemy(120, 0, EnemyType.TANK, 1);
        GunWeapon gun = new GunWeapon(1);
        check(gun.findClosestEnemy(player, Arrays.asList(dead, alive)) == alive, "Target selection excludes dead enemies");
        hp = alive.hp;
        for (int i = 0; i < 15; i++) gun.update(player, Arrays.asList(dead, alive), effects, 10000 + i * 16);
        check(alive.hp < hp && dead.hp == 0, "Bullets pass through dead enemies");

        GarlicWeapon garlic = new GarlicWeapon(1);
        Enemy edge = new Enemy(91, 0, EnemyType.NORMAL, 1);
        Enemy outside = new Enemy(92, 0, EnemyType.NORMAL, 1);
        hp = outside.hp;
        garlic.update(player, Arrays.asList(edge, outside), effects, 10000);
        check(edge.hp < hp && outside.hp == hp, "Garlic damage boundary includes enemy size");
        BufferedImage auraImage = new BufferedImage(220, 220, BufferedImage.TYPE_INT_ARGB);
        var auraGraphics = auraImage.createGraphics();
        garlic.draw(auraGraphics, player, -110, -110);
        auraGraphics.dispose();
        check(garlic.getName().equals("Aurafarming"), "Aura skill uses its new name");
        BufferedImage auraSheet = javax.imageio.ImageIO.read(GameRegressionTest.class.getResource(
                "/project/game/weapons/Aurafarming/Effect-Aurafarming.png"));
        for (int frameIndex = 0; frameIndex < 5; frameIndex++) {
            garlic.update(player, Collections.emptyList(), effects, 10000 + frameIndex * 100);
            auraImage = new BufferedImage(220, 220, BufferedImage.TYPE_INT_ARGB);
            auraGraphics = auraImage.createGraphics();
            garlic.draw(auraGraphics, player, -110, -110);
            auraGraphics.dispose();
            BufferedImage expectedAura = new BufferedImage(220, 220, BufferedImage.TYPE_INT_ARGB);
            var expectedGraphics = expectedAura.createGraphics();
            int frameWidth = auraSheet.getWidth() / 4;
            int drawHeight = 112 * auraSheet.getHeight() / frameWidth;
            expectedGraphics.drawImage(auraSheet.getSubimage(frameIndex % 4 * frameWidth, 0,
                    frameWidth, auraSheet.getHeight()), 54, 110 + player.getRenderSize() / 2 - drawHeight, 112, drawHeight, null);
            expectedGraphics.dispose();
            check(Arrays.equals(auraImage.getRGB(0, 0, 220, 220, null, 0, 220),
                    expectedAura.getRGB(0, 0, 220, 220, null, 0, 220)),
                    "Aura uses supplied frame, correct scale and looping at frame " + frameIndex);
        }

        VampireSurvivorsGame game = new VampireSurvivorsGame();
        player = (Player) get(game, "player");
        ((List<Weapon>) get(game, "weapons")).clear();
        player.hp = 1;
        Enemy killer = new Enemy(0, 0, EnemyType.NORMAL, 1);
        killer.updateMeleeAttack(player, (long) get(game, "gameTimeMillis") - 280);
        ((List<Enemy>) get(game, "enemies")).add(killer);
        ((List<Gem>) get(game, "gems")).add(new Gem(0, 0, 10, false));
        call(game, "updateGame");
        manager = (LevelManager) get(game, "levelManager");
        check((boolean) get(game, "gameOver") && player.hp == 0 && !manager.isLevelUping(), "Death must stop XP collection");
        manager.gainXP(10, player, Arrays.asList(new GunWeapon(0)), Collections.emptyList());
        check(!manager.isLevelUping() && player.hp == 0, "XP cannot revive a dead player");
        game.keyPressed(key(game, KeyEvent.VK_R));
        check(!(boolean) get(game, "gameOver") && ((Player) get(game, "player")).hp == 80, "Restart after death");
        check((long) get(game, "lastSpawnTime") == (long) get(game, "gameTimeMillis"), "Restart resets spawn clock");
        check((double) get(game, "cameraX") == -450 && (double) get(game, "cameraY") == -337.5, "Restart centers camera");

        List<Gem> gems = (List<Gem>) get(game, "gems");
        gems.add(new Gem(0, 0, 10, false));
        gems.add(new Gem(0, 0, 15, false));
        call(game, "updateGame");
        check(gems.size() == 1 && ((LevelManager) get(game, "levelManager")).getLevel() == 2,
                "XP collection stops immediately when upgrade selection opens");
        game.keyPressed(key(game, KeyEvent.VK_1));
        call(game, "updateGame");
        check(gems.isEmpty() && ((LevelManager) get(game, "levelManager")).getLevel() == 3,
                "Remaining gem is collected after selection");
        game.keyPressed(key(game, KeyEvent.VK_1));
        call(game, "initGame");

        HolyWaterWeapon water = null;
        for (Weapon item : (List<Weapon>) get(game, "weapons")) {
            if (item instanceof HolyWaterWeapon) water = (HolyWaterWeapon) item;
        }
        water.levelUp();
        Enemy justiceTarget = new Enemy(80, 0, EnemyType.NORMAL, 1);
        justiceTarget.hp = 1000;
        ((List<Enemy>) get(game, "enemies")).add(justiceTarget);
        call(game, "updateGame");
        check(((List<?>) get(water, "strikes")).size() == 1, "Justice sword spawns before pause");

        manager = (LevelManager) get(game, "levelManager");
        weapon = new GunWeapon(0);
        manager.gainXP(10, null, Arrays.asList(weapon), Collections.emptyList());
        long time = (long) get(game, "gameTimeMillis");
        set(game, "lastUpdateNanos", System.nanoTime() - 61_000_000_000L);
        call(game, "updateGame");
        check((long) get(game, "gameTimeMillis") == time, "Upgrade pause freezes game clock");
        // Simulate more time since the final paused update before selecting.
        set(game, "lastUpdateNanos", System.nanoTime() - 61_000_000_000L);
        MouseEvent event = click(game, 450, 230, MouseEvent.BUTTON1);
        for (var listener : game.getMouseListeners()) listener.mousePressed(event);
        call(game, "updateGame");
        check(weapon.getLevel() == 1 && (long) get(game, "elapsedSeconds") == 0
                && (long) get(game, "lastBossSpawnMinute") == 0, "Mouse resume excludes paused time and boss spawn");
        check(((List<?>) get(water, "strikes")).size() == 1, "Justice sword duration and cooldown freeze during menu");
        set(game, "lastUpdateNanos", System.nanoTime() - 1_100_000_000L);
        call(game, "updateGame");
        check((long) get(game, "elapsedSeconds") == 1, "Active time still advances");

        manager.gainXP(15, null, Arrays.asList(weapon), Collections.emptyList());
        set(game, "lastUpdateNanos", System.nanoTime() - 61_000_000_000L);
        time = (long) get(game, "gameTimeMillis");
        game.keyPressed(key(game, KeyEvent.VK_1));
        call(game, "updateGame");
        check(weapon.getLevel() == 2 && (long) get(game, "gameTimeMillis") - time < 1000,
                "Keyboard resume also excludes paused time");

        // Run frame rendering concurrently with restart input under the same game lock.
        final VampireSurvivorsGame sharedGame = game;
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread frames = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    synchronized (sharedGame) {
                        call(sharedGame, "updateGame");
                        call(sharedGame, "renderToBuffer");
                    }
                }
            } catch (Throwable error) { failure.set(error); }
        });
        frames.start();
        for (int i = 0; i < 10; i++) {
            synchronized (game) {
                set(game, "gameOver", true);
                game.keyPressed(key(game, KeyEvent.VK_R));
            }
        }
        frames.join();
        if (failure.get() != null) throw new AssertionError("Concurrent restart/render", failure.get());
        check(true, "Concurrent restart and frames complete without collection errors");

        for (Player.CharacterType character : Player.CharacterType.values()) {
            for (String map : new String[]{"forest", "desert", "night", "lava"}) {
                game = new VampireSurvivorsGame(character, "/project/game/Map/" + map + ".jpg");
                call(game, "updateGame");
                call(game, "renderToBuffer");
                check(get(game, "dbImage") instanceof BufferedImage, "Render " + character + "/" + map);
            }
        }
        System.out.println("PASS: " + checks + " regression checks");
    }
}
