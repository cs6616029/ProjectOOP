package project.game;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

public class FrostHammerTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Enemy enemy(double x, double y, EnemyType type) {
        Enemy e = new Enemy(x, y, type, 1);
        e.hp = e.maxHp = 1000;
        return e;
    }
    private static BufferedImage render(Enemy e) {
        BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        e.draw(g, e.x - 200, e.y - 200);
        g.dispose();
        return image;
    }
    private static boolean same(BufferedImage a, BufferedImage b) {
        return Arrays.equals(a.getRGB(0, 0, a.getWidth(), a.getHeight(), null, 0, a.getWidth()),
                b.getRGB(0, 0, b.getWidth(), b.getHeight(), null, 0, b.getWidth()));
    }
    private static Object get(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Player p = new Player(0, 0);
        BufferedImage sheet = ImageIO.read(FrostHammerTest.class.getResource("/project/game/effects/frost-hammer.png"));
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            SpearWeapon hammer = new SpearWeapon(1);
            Enemy target = enemy(120 * Math.cos(angle), 120 * Math.sin(angle), EnemyType.NORMAL);
            Enemy outside = enemy(-120 * Math.cos(angle), -120 * Math.sin(angle), EnemyType.NORMAL);
            List<VisualEffect> effects = new ArrayList<>();
            for (int frame = 0; frame < 14; frame++) {
                hammer.update(p, List.of(target, outside), effects, 10000 + frame * 60);
                check(target.hp == (frame < 8 ? 1000 : 968), "Damage on slam only in direction " + direction);
                check(effects.size() == (frame < 8 ? 0 : 1), "One ice impact per swing");
                check(outside.hp == 1000 && !outside.isFrozen(), "Enemies behind the slam stay unaffected");
                BufferedImage actual = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
                var g = actual.createGraphics(); hammer.draw(g, p, -250, -250); g.dispose();
                BufferedImage expected = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
                g = expected.createGraphics(); g.translate(250, 250); g.rotate(angle);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(sheet.getSubimage(frame % 5 * 256, frame / 5 * 128, 256, 128), -21, -73, 173, 86, null);
                g.dispose();
                check(same(actual, expected), "All fourteen supplied hammer frames in eight directions");
            }
            check(target.isFrozen(), "Ordinary enemy freezes on first hit");
            double x = target.x, y = target.y;
            BufferedImage frozen = render(target);
            target.updateStatus(12479); target.updateCombatMovement(p); target.update(500, 500);
            check(target.x == x && target.y == y, "No movement throughout freeze");
            check(same(frozen, render(target)), "Frozen animation holds the same frame");
            target.updateStatus(12480); target.updateCombatMovement(p);
            check(!target.isFrozen() && Math.hypot(target.x - x, target.y - y) > 0, "Movement resumes at thaw time");
        }
        VisualEffect effect = VisualEffect.frostImpact(200, 200, 55);
        BufferedImage ice = ImageIO.read(FrostHammerTest.class.getResource("/project/game/effects/frost-impact.png"));
        for (int tick = 0; tick < 44; tick++) {
            BufferedImage actual = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
            var g = actual.createGraphics(); effect.draw(g, 0, 0); g.dispose();
            BufferedImage expected = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
            g = expected.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int frame = tick / 4;
            g.drawImage(ice.getSubimage(frame % 5 * 256, frame / 5 * 128, 256, 128), 145, 145, 110, 55, null);
            g.dispose(); check(same(actual, expected), "All eleven ice frames play without blank atlas cells"); effect.update();
        }
        check(effect.isFinished(), "Ice explosion ends without looping");
        SpearWeapon largeHammer = new SpearWeapon(5);
        Enemy center = enemy(220, 0, EnemyType.TANK);
        Enemy expandedEdge = enemy(220, 140, EnemyType.NORMAL);
        Enemy outsideBlast = enemy(220, 190, EnemyType.NORMAL);
        List<VisualEffect> largeEffects = new ArrayList<>();
        largeHammer.update(p, List.of(center, expandedEdge, outsideBlast), largeEffects, 10000);
        largeHammer.update(p, List.of(center, expandedEdge, outsideBlast), largeEffects, 10480);
        check((int) get(largeHammer, "swingWidth") == 313, "Level five hammer is visibly larger than level one's 173 pixels");
        check(largeEffects.size() == 1 && largeEffects.get(0).radius == 135, "Ice effect grows with the damage radius");
        check(expandedEdge.hp == 928 && expandedEdge.isFrozen(), "Larger ice blast damages and freezes newly covered enemies");
        check(outsideBlast.hp == 1000 && !outsideBlast.isFrozen(), "Larger blast still respects its outer boundary");
        for (BossSkill.Kind kind : BossSkill.Kind.values()) {
            Enemy boss = new Enemy(120, 0, EnemyType.BOSS, 1, kind); boss.hp = 1000;
            SpearWeapon hammer = new SpearWeapon(1);
            List<VisualEffect> effects = new ArrayList<>();
            hammer.update(p, List.of(boss), effects, 10000);
            hammer.update(p, List.of(boss), effects, 10480);
            check(!boss.isFrozen() && boss.hp == 968, "First hammer hit damages boss without freezing");
            hammer.update(p, List.of(boss), effects, 11200);
            hammer.update(p, List.of(boss), effects, 11680);
            check(boss.isFrozen() && boss.hp == 936, "Second distinct slam freezes every boss kind");
            List<BossAttack> attacks = new ArrayList<>();
            boss.updateBossSkill(p, attacks, 12000); boss.updateCombatMovement(p);
            check(attacks.isEmpty() && boss.x == 120, "Frozen boss cannot move or cast");
            boss.updateStatus(12879); check(boss.isFrozen(), "Boss freeze lasts 1.2 seconds");
            boss.updateStatus(12880); check(!boss.isFrozen(), "Boss thaws on schedule");
            boss.applyFrostHit(13000); check(!boss.isFrozen(), "Boss requires another pair after thawing");
        }
        Enemy melee = enemy(30, 0, EnemyType.TANK);
        melee.updateMeleeAttack(p, 10000); melee.applyFrostHit(10100);
        check(melee.updateMeleeAttack(p, 10280) == 0, "Freeze interrupts an anticipated melee hit");
        BufferedImage frozen = render(melee);
        boolean blue = false;
        for (int pixel : frozen.getRGB(0, 0, 400, 400, null, 0, 400)) if ((pixel >>> 24) != 0) {
            check((pixel & 255) > ((pixel >> 16) & 255), "Frozen sprite is blue while retaining alpha"); blue = true;
        }
        check(blue, "Frozen monster is visible");
        for (BossSkill.Kind kind : BossSkill.Kind.values()) {
            Enemy boss = new Enemy(120, 0, EnemyType.BOSS, 1, kind);
            List<BossAttack> attacks = new ArrayList<>();
            boss.updateBossSkill(p, attacks, 10000); boss.updateBossSkill(p, attacks, 11200);
            boss.applyFrostHit(11201); boss.applyFrostHit(11202);
            boss.updateBossSkill(p, attacks, 12000);
            check(attacks.isEmpty(), "Freeze cancels a pending boss cast");
        }
        Enemy bat;
        do { bat = enemy(120, 0, EnemyType.FAST); } while (!bat.isBat());
        bat.applyFrostHit(10000);
        List<BossAttack> shots = new ArrayList<>(); bat.updateRangedAttack(p, shots, 11500);
        check(shots.isEmpty(), "Frozen bat cannot shoot");
        Enemy dead = enemy(80, 0, EnemyType.NORMAL); dead.hp = 0; dead.applyFrostHit(10000);
        check(!dead.isFrozen(), "Dead enemies are not frozen");
        VampireSurvivorsGame game = new VampireSurvivorsGame();
        Player gp = (Player) get(game, "player");
        List<Enemy> enemies = (List<Enemy>) get(game, "enemies"); enemies.clear();
        ((List<Weapon>) get(game, "weapons")).clear();
        Enemy victim = enemy(gp.x + 100, gp.y, EnemyType.TANK); enemies.add(victim);
        long now = (long) get(game, "gameTimeMillis"); victim.applyFrostHit(now);
        var update = game.getClass().getDeclaredMethod("updateGame"); update.setAccessible(true); update.invoke(game);
        check(victim.isFrozen() && victim.x == gp.x + 100, "Game loop updates status before movement");
        System.out.println("PASS: Frost Hammer frames, eight directions, timed area damage, blue freeze, thaw, boss two-hit rule, interrupted attacks and game integration");
    }
}
