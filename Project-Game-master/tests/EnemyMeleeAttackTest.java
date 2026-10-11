package project.game;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class EnemyMeleeAttackTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static BufferedImage render(Enemy e) {
        BufferedImage image = new BufferedImage(320, 320, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        e.draw(g, -160, -160);
        g.dispose();
        return image;
    }
    private static Enemy enemy(EnemyType type) {
        return new Enemy(0, 0, type, 1, BossSkill.Kind.OGRE);
    }
    public static void main(String[] args) {
        for (EnemyType type : new EnemyType[]{EnemyType.NORMAL, EnemyType.TANK, EnemyType.BOSS}) {
            Enemy e = enemy(type);
            Player p = new Player(e.size * 0.25 + 20, 0);
            check(e.updateMeleeAttack(p, 1000) == 0, "No damage on touch");
            e.updateCombatMovement(p);
            check(e.x == 0 && e.y == 0, "Preparation stops movement");
            check(e.updateMeleeAttack(p, 1279) == 0, "Warning gives dodge time");
            BufferedImage warning = render(e);
            check(e.updateMeleeAttack(p, 1280) == e.damage, "Swing hits once");
            BufferedImage swing = render(e);
            int changed = 0;
            for (int y = 0; y < 320; y++) for (int x = 0; x < 320; x++)
                if (warning.getRGB(x, y) != swing.getRGB(x, y)) changed++;
            check(changed > 100, "Preparation and slash are visibly distinct");
            check(e.updateMeleeAttack(p, 1280) == 0, "Same clock cannot repeat damage");
            check(e.updateMeleeAttack(p, 1400) == 0, "Swing cannot hit twice");
            check(e.updateMeleeAttack(p, 1800) == 0, "Recovery cannot hurt player");
            check(e.updateMeleeAttack(p, 2400) == 0, "Next swing prepares again");
            check(e.updateMeleeAttack(p, 2680) == e.damage, "New swing can hit");
        }
        for (int direction = 0; direction < 8; direction++) {
            double angle = direction * Math.PI / 4;
            Player p = new Player(30 * Math.cos(angle), 30 * Math.sin(angle));
            Enemy e = enemy(EnemyType.NORMAL);
            e.updateMeleeAttack(p, 1000);
            p.x = -p.x; p.y = -p.y;
            check(e.updateMeleeAttack(p, 1280) == 0, "Dodge behind locked swing in all directions");
        }
        Player p = new Player(30, 0);
        Enemy e = enemy(EnemyType.NORMAL);
        e.updateMeleeAttack(p, 1000);
        p.x = 200;
        check(e.updateMeleeAttack(p, 1280) == 0, "Out-of-range dodge");
        e = enemy(EnemyType.NORMAL);
        p.x = 30;
        e.updateMeleeAttack(p, 1000);
        e.hp = 0;
        check(e.updateMeleeAttack(p, 1280) == 0, "Killed monster cannot swing");
        Enemy boss = enemy(EnemyType.BOSS);
        boss.updateBossSkill(p, new ArrayList<>(), 1000);
        boss.updateBossSkill(p, new ArrayList<>(), 3500);
        check(boss.updateMeleeAttack(p, 3500) == 0 && boss.updateMeleeAttack(p, 3780) == 0,
                "Boss cannot melee during skill warning");
        System.out.println("PASS: melee anticipation, single hit, cooldown, movement, visual phases, dodges and boss casting");
    }
}
