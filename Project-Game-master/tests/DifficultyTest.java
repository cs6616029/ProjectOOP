package project.game;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

public class DifficultyTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static Field field(Object object, String name) throws Exception {
        Field f=object.getClass().getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static Object get(Object object, String name) throws Exception { return field(object,name).get(object); }
    private static Method method(Object object,String name,Class<?>... types) throws Exception {
        Method m=object.getClass().getDeclaredMethod(name,types); m.setAccessible(true); return m;
    }
    private static void action(GameStartScreen screen,String name) { screen.getActionMap().get(name).actionPerformed(null); }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        SoundEffects.setSoundVolume(0); SoundEffects.setMusicVolume(0);
        SwingUtilities.invokeAndWait(() -> {
            for (int index=0;index<3;index++) {
                AtomicInteger launches=new AtomicInteger();
                Difficulty expected=Difficulty.values()[index];
                GameStartScreen screen=new GameStartScreen((hero,map,difficulty)-> {
                    check(hero==Player.CharacterType.STRONG_BOY,"Selected hero reaches game");
                    check(map.endsWith("lava.jpg"),"Selected map reaches game");
                    check(difficulty==expected,"Selected difficulty reaches game");
                    launches.incrementAndGet();
                });
                try {
                    screen.setSize(900,675);
                    action(screen,"continue"); action(screen,"next"); action(screen,"next");
                    action(screen,"continue"); action(screen,"next"); action(screen,"next"); action(screen,"next");
                    action(screen,"continue");
                    check(launches.get()==0,"Map continue waits for difficulty");
                    check((int)get(screen,"selectedDifficulty")==0,"Default difficulty is Normal");
                    action(screen,"previous");
                    check((int)get(screen,"selectedDifficulty")==2,"Difficulty keyboard selection wraps");
                    action(screen,"next");
                    for(int i=0;i<index;i++) action(screen,"next");
                    action(screen,"back"); action(screen,"continue");
                    check((int)get(screen,"selectedDifficulty")==index,"Back preserves difficulty");
                    Rectangle card=(Rectangle)method(screen,"playerCard",int.class).invoke(screen,index);
                    int x=(card.x+card.width/2)*900/800, y=(card.y+card.height/2)*675/600;
                    MouseEvent click=new MouseEvent(screen,MouseEvent.MOUSE_CLICKED,0,0,x,y,1,false,MouseEvent.BUTTON1);
                    for(var listener:screen.getMouseListeners()) listener.mouseClicked(click);
                    check((int)get(screen,"selectedDifficulty")==index,"Scaled mouse selection works");
                    BufferedImage image=new BufferedImage(900,675,BufferedImage.TYPE_INT_ARGB);
                    var g=image.createGraphics(); screen.paint(g); g.dispose();
                    if(args.length>0 && index==2) javax.imageio.ImageIO.write(image,"png",new java.io.File(args[0]));
                    action(screen,"continue"); action(screen,"continue");
                    check(launches.get()==1,"Difficulty launches exactly once");
                } catch(Exception e) { throw new RuntimeException(e); }
                finally { screen.removeNotify(); }
            }
        });
        for(Difficulty difficulty:Difficulty.values()) {
            ISEKAISurvivors game=new ISEKAISurvivors(Player.CharacterType.SURVIVOR,"/project/game/Map/forest.jpg",difficulty);
            List<Enemy> enemies=(List<Enemy>)get(game,"enemies");
            method(game,"spawnEnemyAroundPlayer").invoke(game);
            check(enemies.size()==difficulty.spawnCount(1),"Difficulty changes starting horde size");
            for(Enemy e:enemies) {
                int base=new Enemy(0,0,e.type,1).hp;
                check(e.hp==difficulty.health(base)&&e.maxHp==e.hp,"Enemy health and bar scale once");
            }
            for(int i=0;i<4;i++) {
                try { field(game,"bossesDefeated").setInt(game,i); } catch(NoSuchFieldException ignored) { }
                enemies.clear(); method(game,"spawnBossAroundPlayer",long.class).invoke(game,50L);
                Enemy boss=enemies.get(0);
                check(boss.hp==difficulty.health(1000*(i+1))&&boss.maxHp==boss.hp,"Boss order and difficulty combine");
            }
            Player player=(Player)get(game,"player");
            method(game,"damagePlayer",int.class).invoke(game,10);
            check(player.hp==80-difficulty.damage(10),"All incoming attack damage uses difficulty once");
            method(game,"damagePlayer",int.class).invoke(game,10);
            check(player.hp==80-difficulty.damage(10),"Invincibility still prevents repeat damage");
            method(game,"initGame").invoke(game);
            check(get(game,"difficulty")==difficulty,"Restart retains difficulty");
            method(game,"updateGame").invoke(game);
            check((long)get(game,"spawnInterval")==difficulty.spawnInterval(1100),"Actual spawn interval reflects difficulty");
            enemies.clear();
            for(int i=0;i<89;i++) enemies.add(new Enemy(1000,1000,EnemyType.NORMAL,1));
            method(game,"spawnEnemyAroundPlayer").invoke(game);
            check(enemies.size()==90,"Enemy cap holds on every difficulty");
        }
        check(Difficulty.NORMAL.health(1000)==1000&&Difficulty.NORMAL.damage(10)==10,"Normal preserves current balance");
        System.out.println("PASS: three difficulty pages, keyboard/mouse/back/launch, enemy and boss health, incoming damage, spawn timing/count/cap and restart");
    }
}
