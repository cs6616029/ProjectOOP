package project.game;

import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TornadoTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static Object get(Object object, String name) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(object);
    }
    private static double number(Object o, String name) throws Exception { return ((Number) get(o, name)).doubleValue(); }
    private static List<?> storms(TornadoWeapon w) throws Exception { return (List<?>) get(w, "storms"); }
    private static Enemy enemy(double x, double y) {
        Enemy e = new Enemy(x, y, EnemyType.TANK, 1); e.hp = 1000; return e;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Player p = new Player(0, 0);
        Enemy target = enemy(500, 0);
        TornadoWeapon locked = new TornadoWeapon(0);
        locked.update(p, List.of(target), new ArrayList<>(), 10000);
        check(storms(locked).isEmpty(), "Locked skill stays inactive");
        for (int level = 1; level <= 5; level++) {
            TornadoWeapon w = new TornadoWeapon(level);
            w.update(p, List.of(target), new ArrayList<>(), 10000);
            int count = 1 + (level - 1) / 2;
            check(storms(w).size() == count && w.getTornadoCount() == count, "Storm count increases to three");
            for (int step = 0; step < 12; step++) {
                p.x = step * 3; p.y = step * 2;
                w.update(p, List.of(target), new ArrayList<>(), 10000 + step * 100);
                for (int i = 0; i < count; i++) {
                    Object storm = storms(w).get(i);
                    double phase = Math.PI * 2 * i / count + Math.PI * 2 * step / 12;
                    check(Math.abs(number(storm, "x") - p.x - Math.cos(phase) * 105) < .001
                            && Math.abs(number(storm, "y") - p.y - Math.sin(phase) * 105) < .001,
                            "Each storm completes exactly one orbit around moving player");
                    check(!(boolean) get(storm, "launched"), "No launch before full orbit");
                }
            }
            w.update(p, List.of(target), new ArrayList<>(), 11200);
            for (Object storm : storms(w)) check((boolean) get(storm, "launched"), "Launch at one completed revolution");
            Object first = storms(w).get(0);
            double x = number(first, "x"), y = number(first, "y");
            p.x += 300; p.y += 300;
            w.update(p, List.of(target), new ArrayList<>(), 11200);
            check(number(first, "x") == x && number(first, "y") == y, "Paused clock freezes flight and detached storm ignores player movement");
            w.update(p, List.of(target), new ArrayList<>(), 11300);
            check(Math.abs(Math.hypot(number(first, "x") - x, number(first, "y") - y) - 40) < .001,
                    "Storm advances gradually at 400 pixels per second");
            p.x = p.y = 0;
        }
        TornadoWeapon w = new TornadoWeapon(1);
        target = enemy(500, 0);
        w.update(p, List.of(target), new ArrayList<>(), 10000);
        w.update(p, List.of(target), new ArrayList<>(), 11200);
        Enemy front = enemy(220, 0), behind = enemy(300, 0);
        w.update(p, List.of(behind, front, target), new ArrayList<>(), 11700);
        check(front.hp == 984 && front.isAirborne() && behind.hp == 1000, "Swept flight hits first enemy even with a large frame step");
        w.update(p, List.of(front, behind), new ArrayList<>(), 11700);
        check(front.hp == 984, "A storm cannot repeatedly damage the same target");
        double x = front.x, y = front.y;
        front.updateStatus(12075); check(Math.abs(front.airborneHeight() - 48) < .001, "Enemy visibly rises to peak height");
        front.updateCombatMovement(p); check(front.x == x && front.y == y, "Airborne enemy cannot walk");
        check(front.updateMeleeAttack(p, 12075) == 0, "Airborne enemy cannot attack");
        double height = front.airborneHeight(); front.updateStatus(12075);
        check(front.airborneHeight() == height, "Paused gameplay clock freezes lift");
        front.updateStatus(12450); check(!front.isAirborne() && front.airborneHeight() == 0, "Enemy lands after 750 milliseconds");
        front.updateCombatMovement(p); check(front.x != x, "Enemy resumes moving after landing");
        w.update(p, List.of(target), new ArrayList<>(), 12020);
        check(storms(w).isEmpty(), "Storm vanishes after all four fading frames");
        w.update(p, List.of(target), new ArrayList<>(), 14499); check(storms(w).isEmpty(), "No early recast");
        w.update(p, List.of(target), new ArrayList<>(), 14500); check(storms(w).size() == 1, "Recast after cooldown");
        w = new TornadoWeapon(1); Enemy nearby = enemy(105, 0);
        w.update(p, List.of(nearby), new ArrayList<>(), 10000);
        check(nearby.hp == 984 && nearby.isAirborne(), "Orbit can hit nearby enemies");
        for (int tick = 0; tick < 10; tick++) w.update(p, List.of(nearby), new ArrayList<>(), 10000);
        check(nearby.hp == 984, "Orbit damage is once per storm");
        nearby.applyFrostHit(10010); nearby.updateStatus(10300);
        check(nearby.isFrozen() && nearby.isAirborne(), "Freeze and lift coexist");
        w = new TornadoWeapon(1); target = enemy(500, 0);
        w.update(p, List.of(target), new ArrayList<>(), 10000);
        Object storm = storms(w).get(0);
        for (int frame = 0; frame < 5; frame++) {
            w.update(p, List.of(target), new ArrayList<>(), 10000 + frame * 80);
            BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
            var g = image.createGraphics(); w.draw(g, p, -200, -200); g.dispose();
            check(java.util.Arrays.stream(image.getRGB(0, 0, 400, 400, null, 0, 400)).anyMatch(pixel -> (pixel >>> 24) != 0),
                    "All active tornado frames render");
        }
        target.hp = 0;
        w.update(p, List.of(target), new ArrayList<>(), 13200);
        w.update(p, List.of(target), new ArrayList<>(), 13520);
        check(storms(w).isEmpty(), "Missed storms expire even without living targets");
        Enemy boss = new Enemy(0, 0, EnemyType.BOSS, 1); boss.applyAirborne(10000); boss.updateStatus(10375);
        check(boss.isAirborne() && boss.airborneHeight() == 28, "Boss lift uses a smaller visible height");
        List<BossAttack> attacks = new ArrayList<>(); boss.updateBossSkill(p, attacks, 10375);
        check(attacks.isEmpty(), "Lift interrupts boss casts");
        VampireSurvivorsGame game = new VampireSurvivorsGame();
        List<Weapon> weapons = (List<Weapon>) get(game, "weapons");
        check(weapons.stream().anyMatch(weapon -> weapon instanceof TornadoWeapon && weapon.getLevel() == 0), "New skill is registered for upgrades");
        TornadoWeapon offered = new TornadoWeapon(0);
        TornadoWeapon capped = new TornadoWeapon(5);
        p.attackCooldownMultiplier = 0.1;
        Enemy far = enemy(500, 0);
        capped.update(p, List.of(far), new ArrayList<>(), 10000);
        capped.update(p, List.of(far), new ArrayList<>(), 10500);
        check(storms(capped).size() == 3, "Cooldown bonuses cannot exceed three simultaneous tornadoes");
        p.attackCooldownMultiplier = 1;
        LevelManager manager = new LevelManager(); manager.gainXP(10, p, List.of(offered), List.of());
        var select = LevelManager.class.getDeclaredMethod("selectUpgrade", int.class, Player.class); select.setAccessible(true); select.invoke(manager, 0, p);
        check(offered.getLevel() == 1, "Tornado can be unlocked from level up");
        System.out.println("PASS: Tornado count, full moving orbit, gradual homing flight, first contact, one hit, lift and landing, freeze interaction, lifetime, cooldown and upgrade registration");
    }
}
