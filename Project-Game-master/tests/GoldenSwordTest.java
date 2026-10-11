package project.game;

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class GoldenSwordTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Object get(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o);
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Player p = new Player(0, 0);
        List<PassiveItem> passives = new ArrayList<>();
        for (PassiveItem.ItemType type : PassiveItem.ItemType.values()) passives.add(new PassiveItem(type));
        SwordWeapon sword = new SwordWeapon(5);
        Weapon gun = new GunWeapon(5), fire = new FireStaffWeapon(4);
        List<Weapon> loadout = List.of(sword, gun, fire, new TornadoWeapon(0));
        for (PassiveItem passive : passives) for (int i = 0; i < 5; i++) passive.levelUp(p);
        check(!SwordWeapon.hasMaxedLoadout(loadout, passives), "Incomplete selected attack prevents gold slash");
        fire.levelUp();
        check(SwordWeapon.hasMaxedLoadout(loadout, passives), "Three maxed attacks and all maxed stats unlock gold slash");
        check(!SwordWeapon.hasMaxedLoadout(List.of(sword, gun), passives), "Two selected skills are not a complete loadout");
        passives.set(0, new PassiveItem(PassiveItem.ItemType.ARMOR));
        check(!SwordWeapon.hasMaxedLoadout(loadout, passives), "Incomplete health upgrade prevents gold slash");
        BufferedImage sheet = ImageIO.read(GoldenSwordTest.class.getResource("/project/game/effects/golden-sword-slash.png"));
        p.attackCooldownMultiplier = 1;
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            p.lastDirX = Math.cos(angle); p.lastDirY = Math.sin(angle);
            sword = new SwordWeapon(5); sword.setGoldenSlash(true);
            Enemy target = new Enemy(Math.cos(angle) * 80, Math.sin(angle) * 80, EnemyType.TANK, 1); target.hp = 1000;
            for (int frame = 0; frame < 5; frame++) {
                sword.update(p, List.of(target), new ArrayList<>(), 10000 + frame * 60);
                check(target.hp == 932, "Golden effect preserves sword damage and hits only once");
                BufferedImage actual = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
                var g = actual.createGraphics(); sword.draw(g, p, -250, -250); g.dispose();
                BufferedImage expected = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
                g = expected.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                double a = Math.atan2(p.lastDirY, p.lastDirX), width = 122 * 2.4;
                AffineTransform t = new AffineTransform();
                t.translate(250 + Math.cos(a) * 122 * .25, 250 + Math.sin(a) * 122 * .25);
                t.rotate(a); t.scale(width / 64, width / 64); t.translate(-32, -32);
                g.drawImage(sheet.getSubimage(frame * 64, 0, 64, 64), t, null); g.dispose();
                check(Arrays.equals(actual.getRGB(0, 0, 500, 500, null, 0, 500), expected.getRGB(0, 0, 500, 500, null, 0, 500)),
                        "Five supplied gold frames render in all eight directions");
            }
            sword.update(p, List.of(target), new ArrayList<>(), 10300);
            check(!(boolean) get(sword, "isAttacking"), "Golden animation completes without looping");
        }
        for (Player.CharacterType character : Player.CharacterType.values()) {
            VampireSurvivorsGame game = new VampireSurvivorsGame(character, "/project/game/Map/forest.jpg");
            List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
            weapons.clear(); SwordWeapon main = new SwordWeapon(5);
            weapons.add(main); weapons.add(new GunWeapon(5)); weapons.add(new FireStaffWeapon(5));
            Player gp = (Player) get(game, "player");
            for (PassiveItem passive : (List<PassiveItem>) get(game, "passiveItems")) while (passive.getLevel() < 5) passive.levelUp(gp);
            var update = game.getClass().getDeclaredMethod("updateGame"); update.setAccessible(true); update.invoke(game);
            check((boolean) get(main, "goldenSlash") == (character == Player.CharacterType.SURVIVOR),
                    "Gold sword unlock is exclusive to Swordman");
            var reset = game.getClass().getDeclaredMethod("initGame"); reset.setAccessible(true); reset.invoke(game);
            check(!SwordWeapon.hasMaxedLoadout((List<Weapon>) get(game, "weapons"), (List<PassiveItem>) get(game, "passiveItems")),
                    "Restart resets the golden unlock requirements");
        }
        System.out.println("PASS: Golden sword full-loadout requirement, five frames, eight directions, unchanged damage, character restriction and restart");
    }
}
