package project.game;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.List;
import javax.imageio.ImageIO;

public final class HealthPickupTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static Object get(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static void call(Object object, String name) throws Exception {
        var method = object.getClass().getDeclaredMethod(name); method.setAccessible(true); method.invoke(object);
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        BufferedImage sheet = ImageIO.read(HealthPickupTest.class.getResource("/project/game/effects/boss-health.png"));
        check(sheet.getWidth() == 23 * 128 && sheet.getHeight() == 128, "Supplied heart atlas has 23 frames");
        BufferedImage shadowImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        var shadowGraphics = shadowImage.createGraphics();
        new HealthPickup(0, 0, 10000).draw(shadowGraphics, -64, -64, 10300); shadowGraphics.dispose();
        check((shadowImage.getRGB(64, 84) >>> 24) >= 70 && (shadowImage.getRGB(64, 84) & 0xffffff) == 0,
                "Dropped heart has a visible ground shadow below it");
        Player player = new Player(0, 0); player.hp = 20;
        HealthPickup heart = new HealthPickup(200, 0, 10000);
        check(!heart.update(player, 10100) && player.hp == 20, "Distant heart cannot heal");
        player.x = 200;
        heart.update(player, 10200); check(player.hp == 50, "Walking onto heart heals 30 HP");
        heart.update(player, 10250); check(player.hp == 50, "Pickup cannot heal twice");
        for (int frame = 0; frame < 12; frame++) {
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics(); heart.draw(graphics, 136, -64, 10200 + frame * 50); graphics.dispose();
            BufferedImage expected = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            graphics = expected.createGraphics();
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(sheet.getSubimage((11 + frame) * 128, 0, 128, 128), 16, 16, 96, 96, null); graphics.dispose();
            check(java.util.Arrays.equals(image.getRGB(0, 0, 128, 128, null, 0, 128),
                    expected.getRGB(0, 0, 128, 128, null, 0, 128)), "Pickup displays each supplied burst frame in order");
            check(!heart.update(player, 10200 + frame * 50), "All pickup burst frames finish before removal");
        }
        check(heart.update(player, 10800), "Heart disappears after pickup animation");
        HealthPickup waiting = new HealthPickup(200, 0, 20000);
        player.hp = player.maxHp;
        waiting.update(player, 20500); check(player.hp == player.maxHp, "Full health keeps heart on ground");
        player.hp = player.maxHp - 5;
        waiting.update(player, 20600); check(player.hp == player.maxHp, "Healing clamps to maximum HP");
        HealthPickup dead = new HealthPickup(200, 0, 20000);
        player.hp = 0; dead.update(player, 21000); check(player.hp == 0, "Heart does not revive a dead player");
        for (BossSkill.Kind kind : BossSkill.Kind.values()) {
            ISEKAISurvivors game = new ISEKAISurvivors();
            List<Enemy> enemies = (List<Enemy>) get(game, "enemies");
            ((List<Weapon>) get(game, "weapons")).clear(); enemies.clear();
            Player gp = (Player) get(game, "player"); gp.hp = 20;
            Enemy boss = new Enemy(gp.x + 400, gp.y, EnemyType.BOSS, 1, kind); boss.hp = 0; enemies.add(boss);
            call(game, "updateGame");
            List<HealthPickup> hearts = (List<HealthPickup>) get(game, "healthPickups");
            check(hearts.size() == 1 && hearts.get(0).x == boss.x - 32 && hearts.get(0).y == boss.y,
                    "Each boss drops one heart beside its death position");
            List<Gem> gems = (List<Gem>) get(game, "gems");
            check(gems.size() == 1 && gems.get(0).isBossGem && gems.get(0).xpValue == 50,
                    "Each boss also drops a red 50 EXP gem");
            check(gems.get(0).x == boss.x + 32 && gems.get(0).y == boss.y
                    && gems.get(0).x - hearts.get(0).x == 64, "Heart and red gem stay separated and visible");
            call(game, "updateGame"); check(hearts.size() == 1, "Dead boss cannot duplicate heart drops");
            ((List<Gem>) get(game, "gems")).clear(); gp.x = hearts.get(0).x; gp.y = hearts.get(0).y;
            call(game, "updateGame"); check(gp.hp == 50, "Game collects boss heart and heals player");
            call(game, "initGame"); check(hearts.isEmpty(), "Restart removes old hearts");
            enemies = (List<Enemy>) get(game, "enemies"); enemies.clear();
            Enemy normal = new Enemy(gp.x + 400, gp.y, EnemyType.NORMAL, 1); normal.hp = 0; enemies.add(normal);
            call(game, "updateGame"); check(hearts.isEmpty(), "Regular enemies do not drop boss healing rewards");
        }
        System.out.println("PASS: heart atlas, 30 HP healing, range, max HP, single pickup, full-health retention, all bosses, EXP, restart and normal-enemy exclusion");
    }
}
