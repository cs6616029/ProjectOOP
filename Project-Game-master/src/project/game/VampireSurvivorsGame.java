package project.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class VampireSurvivorsGame extends JPanel implements Runnable, KeyListener {

    private static final int SCREEN_WIDTH = 900;
    private static final int SCREEN_HEIGHT = 675;
    private Thread gameThread;
    private volatile boolean running = false;
    private final int FPS = 60;
    private final long TARGET_TIME = 1000 / FPS;

    private BufferedImage dbImage;
    private Graphics2D dbg;
    private final BufferedImage mapBackground;
    private final Player.CharacterType characterType;

    private final Set<Integer> activeKeys = new HashSet<>();

    private double cameraX = 0;
    private double cameraY = 0;

    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Gem> gems = new ArrayList<>();
    private final List<VisualEffect> visualEffects = new ArrayList<>();
    private final List<BossAttack> bossAttacks = new ArrayList<>();
    private final List<Weapon> weapons = new ArrayList<>();
    private final List<PassiveItem> passiveItems = new ArrayList<>();

    private final LevelManager levelManager = new LevelManager();
    private LevelUpEffect levelUpEffect;
    private int displayedLevel = 1;

    private long gameStartTime;
    private long gameTimeMillis;
    private long lastUpdateNanos;
    private long activeTimeNanos;
    private long elapsedSeconds = 0;
    private long lastBossSpawnMinute = 0;
    private long nextBossSpawnSeconds = SpawnBalance.FIRST_BOSS_SECONDS;
    private long lastSpawnTime = 0;
    private long spawnInterval = 1100;

    private boolean gameOver = false;
    private int score = 0;

    public VampireSurvivorsGame() {
        this(Player.CharacterType.SURVIVOR, "/project/game/Map/forest.jpg");
    }

    public VampireSurvivorsGame(Player.CharacterType characterType, String mapBackgroundResource) {
        this.characterType = characterType;
        mapBackground = loadMapBackground(mapBackgroundResource);
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        requestFocus();
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                synchronized (VampireSurvivorsGame.this) {
                    if (!gameOver && levelManager.handleMousePress(e, player, SCREEN_WIDTH)
                            && !levelManager.isLevelUping()) {
                        lastUpdateNanos = System.nanoTime();
                    }
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                synchronized (VampireSurvivorsGame.this) {
                    activeKeys.clear();
                }
            }
        });
        initGame();
    }

    private synchronized void initGame() {
        player = new Player(0, 0, characterType);
        enemies.clear();
        gems.clear();
        visualEffects.clear();
        bossAttacks.clear();
        weapons.clear();
        passiveItems.clear();
        activeKeys.clear();

        levelManager.reset();
        levelUpEffect = null;
        displayedLevel = 1;

        int primaryWeaponIndex = characterType.getPrimaryWeaponIndex();
        weapons.add(new SwordWeapon(primaryWeaponIndex == 0 ? 1 : 0));
        weapons.add(new GarlicWeapon(primaryWeaponIndex == 1 ? 1 : 0,
                player.getRenderSize() / (double) player.size));
        weapons.add(new LaserWeapon(primaryWeaponIndex == 2 ? 1 : 0));
        weapons.add(new GunWeapon(0));
        weapons.add(new FireStaffWeapon(0));
        weapons.add(new SpearWeapon(0));
        weapons.add(new HolyWaterWeapon(0));
        weapons.add(new ShieldWeapon(0));
        levelManager.setPrimaryWeapon(weapons.get(primaryWeaponIndex));

        passiveItems.add(new PassiveItem(PassiveItem.ItemType.ARMOR));
        passiveItems.add(new PassiveItem(PassiveItem.ItemType.BOOTS));
        passiveItems.add(new PassiveItem(PassiveItem.ItemType.GLOVES));

        gameOver = false;
        score = 0;
        spawnInterval = 1100;

        gameStartTime = System.currentTimeMillis();
        gameTimeMillis = gameStartTime;
        lastUpdateNanos = System.nanoTime();
        activeTimeNanos = 0;
        lastSpawnTime = gameTimeMillis;
        player.gameTimeMillis = gameTimeMillis;
        cameraX = player.x - SCREEN_WIDTH / 2.0;
        cameraY = player.y - SCREEN_HEIGHT / 2.0;
        elapsedSeconds = 0;
        lastBossSpawnMinute = 0;
        nextBossSpawnSeconds = SpawnBalance.FIRST_BOSS_SECONDS;
    }

    public synchronized void startGameThread() {
        if (gameThread == null || !running) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        long start, elapsed, wait;

        while (running) {
            start = System.currentTimeMillis();

            synchronized (this) {
                updateGame();
                renderToBuffer();
            }
            drawToScreen();

            elapsed = System.currentTimeMillis() - start;
            wait = TARGET_TIME - elapsed;

            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    running = false;
                }
            }
        }
    }

    private synchronized void updateGame() {
        long nowNanos = System.nanoTime();
        long deltaNanos = Math.max(0L, nowNanos - lastUpdateNanos);
        lastUpdateNanos = nowNanos;
        if (gameOver || levelManager.isLevelUping()) return;

        activeTimeNanos += deltaNanos;
        gameTimeMillis = gameStartTime + activeTimeNanos / 1_000_000L;
        player.gameTimeMillis = gameTimeMillis;
        elapsedSeconds = (gameTimeMillis - gameStartTime) / 1000;

        int dx = 0, dy = 0;
        if (activeKeys.contains(KeyEvent.VK_W) || activeKeys.contains(KeyEvent.VK_UP)) dy -= 1;
        if (activeKeys.contains(KeyEvent.VK_S) || activeKeys.contains(KeyEvent.VK_DOWN)) dy += 1;
        if (activeKeys.contains(KeyEvent.VK_A) || activeKeys.contains(KeyEvent.VK_LEFT)) dx -= 1;
        if (activeKeys.contains(KeyEvent.VK_D) || activeKeys.contains(KeyEvent.VK_RIGHT)) dx += 1;

        player.move(dx, dy);

        cameraX = player.x - (SCREEN_WIDTH / 2.0);
        cameraY = player.y - (SCREEN_HEIGHT / 2.0);

        long currentTime = gameTimeMillis;
        for (Weapon weapon : weapons) {
            weapon.update(player, enemies, visualEffects, currentTime);
        }

        boolean bossAlive = hasLivingBoss();
        spawnInterval = SpawnBalance.intervalMillis(elapsedSeconds, levelManager.getLevel(), bossAlive);
        if (currentTime - lastSpawnTime >= spawnInterval) {
            spawnEnemyAroundPlayer();
            lastSpawnTime = currentTime;
        }

        if (!hasBossOnField() && elapsedSeconds >= nextBossSpawnSeconds) {
            spawnBossAroundPlayer(elapsedSeconds / 60);
            lastBossSpawnMinute = elapsedSeconds;
            nextBossSpawnSeconds = elapsedSeconds + SpawnBalance.FIRST_BOSS_SECONDS;
        }

        Iterator<VisualEffect> vIt = visualEffects.iterator();
        while (vIt.hasNext()) {
            VisualEffect ve = vIt.next();
            ve.update();
            if (ve.isFinished()) vIt.remove();
        }

        Iterator<Enemy> eIt = enemies.iterator();
        while (eIt.hasNext()) {
            Enemy enemy = eIt.next();

            if (enemy.hp <= 0) {
                if (enemy.type == EnemyType.BOSS) {
                    visualEffects.add(VisualEffect.bossDeath(enemy.x, enemy.y, enemy.size));
                    nextBossSpawnSeconds = elapsedSeconds + SpawnBalance.BOSS_REST_SECONDS;
                }
                int gemXp = (enemy.type == EnemyType.BOSS) ? 50 : 5;
                gems.add(new Gem(enemy.x, enemy.y, gemXp, enemy.type == EnemyType.BOSS));
                score += enemy.scoreValue;
                eIt.remove();
                continue;
            }

            enemy.update(player.x, player.y);

            if (enemy.type != EnemyType.BOSS && Math.hypot(enemy.x - player.x, enemy.y - player.y) > 1200) {
                eIt.remove();
                continue;
            }

            enemy.updateBossSkill(player, bossAttacks, currentTime);

            // แก้ไขการรับดาเมจ ให้เช็ค Invincibility Frames
            if (enemy.isTouching(player)) {
                damagePlayer(enemy.getContactDamage());
                if (gameOver) return;
            }
        }

        Iterator<BossAttack> attackIterator = bossAttacks.iterator();
        while (attackIterator.hasNext()) {
            BossAttack attack = attackIterator.next();
            damagePlayer(attack.update(player, currentTime, visualEffects));
            if (attack.isFinished()) attackIterator.remove();
            if (gameOver) return;
        }

        Iterator<Gem> gIt = gems.iterator();
        while (gIt.hasNext()) {
            Gem gem = gIt.next();
            double dist = Math.hypot(player.x - gem.x, player.y - gem.y);

            if (dist < 150) {
                gem.x += (player.x - gem.x) * 0.12;
                gem.y += (player.y - gem.y) * 0.12;
            }

            if (gem.getBounds().intersects(player.getBounds())) {
                levelManager.gainXP(gem.xpValue, player, weapons, passiveItems);
                gIt.remove();
                if (levelManager.isLevelUping()) {
                    activeKeys.clear();
                    return;
                }
            }
        }
    }

    private void spawnEnemyAroundPlayer() {
        int regularCount = 0;
        for (Enemy enemy : enemies) {
            if (enemy.type != EnemyType.BOSS && enemy.hp > 0) regularCount++;
        }
        int spawnCount = Math.min(SpawnBalance.batchSize(elapsedSeconds, hasLivingBoss()),
                SpawnBalance.MAX_REGULAR_ENEMIES - regularCount);

        for (int i = 0; i < spawnCount; i++) {
            double spawnDistance = 660 + Math.random() * 120;
            double angle = Math.random() * Math.PI * 2;

            double ex = player.x + Math.cos(angle) * spawnDistance;
            double ey = player.y + Math.sin(angle) * spawnDistance;

            EnemyType type = getRandomEnemyType();
            enemies.add(new Enemy(ex, ey, type, levelManager.getLevel()));
        }
    }

    private void damagePlayer(int damage) {
        if (damage <= 0 || gameOver || gameTimeMillis - player.lastDamageTime < player.invincibilityDuration) return;
        player.hp = Math.max(0, player.hp - damage);
        player.lastDamageTime = gameTimeMillis;
        if (player.hp == 0) {
            gameOver = true;
            activeKeys.clear();
        }
    }

    private EnemyType getRandomEnemyType() {
        return SpawnBalance.typeForRoll(Math.random(), elapsedSeconds);
    }

    private boolean hasLivingBoss() {
        for (Enemy enemy : enemies) {
            if (enemy.type == EnemyType.BOSS && enemy.hp > 0) return true;
        }
        return false;
    }

    private boolean hasBossOnField() {
        for (Enemy enemy : enemies) {
            if (enemy.type == EnemyType.BOSS) return true;
        }
        return false;
    }

    private void spawnBossAroundPlayer(long bossWave) {
        double spawnDistance = 660 + Math.random() * 60;
        double angle = Math.random() * Math.PI * 2;

        double ex = player.x + Math.cos(angle) * spawnDistance;
        double ey = player.y + Math.sin(angle) * spawnDistance;

        Enemy boss = new Enemy(ex, ey, EnemyType.BOSS, levelManager.getLevel());
        boss.hp += (int) (bossWave * 35 * Enemy.BOSS_HEALTH_MULTIPLIER);
        boss.maxHp = boss.hp;
        enemies.add(boss);
    }

    private synchronized void renderToBuffer() {
        if (dbImage == null) {
            dbImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            dbg = dbImage.createGraphics();
            dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        drawInfiniteMap(dbg);
        for (Enemy enemy : enemies) enemy.drawBossWarning(dbg, cameraX, cameraY);
        for (BossAttack attack : bossAttacks) attack.draw(dbg, cameraX, cameraY);

        for (Gem gem : gems) gem.draw(dbg, cameraX, cameraY);
        for (Enemy enemy : enemies) enemy.draw(dbg, cameraX, cameraY);
        for (Weapon weapon : weapons) weapon.draw(dbg, player, cameraX, cameraY);
        for (VisualEffect ve : visualEffects) ve.draw(dbg, cameraX, cameraY);

        player.draw(dbg, cameraX, cameraY);
        drawUI(dbg);
        long presentationTime = System.nanoTime();
        int level = levelManager.getLevel();
        if (level > displayedLevel) levelUpEffect = new LevelUpEffect(presentationTime);
        displayedLevel = level;
        if (levelUpEffect != null) {
            if (gameOver || levelUpEffect.isFinished(presentationTime)) levelUpEffect = null;
            else levelUpEffect.draw(dbg, player, cameraX, cameraY, levelManager.isLevelUping(), presentationTime);
        }
    }

    private void drawInfiniteMap(Graphics2D g) {
        int tileWidth = mapBackground.getWidth();
        int tileHeight = mapBackground.getHeight();
        int startX = (int) Math.floor(cameraX / tileWidth) * tileWidth;
        int startY = (int) Math.floor(cameraY / tileHeight) * tileHeight;

        for (int x = startX; x < cameraX + SCREEN_WIDTH + tileWidth; x += tileWidth) {
            for (int y = startY; y < cameraY + SCREEN_HEIGHT + tileHeight; y += tileHeight) {
                g.drawImage(mapBackground, (int) (x - cameraX), (int) (y - cameraY), null);
            }
        }
    }

    private static BufferedImage loadMapBackground(String resourcePath) {
        try {
            var resource = VampireSurvivorsGame.class.getResource(resourcePath);
            if (resource == null) {
                throw new IllegalStateException("Unable to find map background: " + resourcePath);
            }

            BufferedImage image = ImageIO.read(resource);
            if (image == null) {
                throw new IllegalStateException("Unsupported map background: " + resourcePath);
            }
            return image;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load map background: " + resourcePath, exception);
        }
    }

    private void drawUI(Graphics2D g) {
        g.setColor(Color.RED);
        g.fillRect(20, 20, 200, 15);
        g.setColor(Color.GREEN);
        g.fillRect(20, 20, (int) (200 * ((double) player.hp / player.maxHp)), 15);
        g.setColor(Color.WHITE);
        g.drawRect(20, 20, 200, 15);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("HP: " + player.hp + "/" + player.maxHp, 25, 32);

        levelManager.drawXPBar(g, 20, 42, 200, 10);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Level: " + levelManager.getLevel(), 230, 32);
        g.drawString("Score: " + score, 230, 50);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.LIGHT_GRAY);
        StringBuilder itemSummary = new StringBuilder();
        for (Weapon w : weapons) {
            if (w.getLevel() > 0) itemSummary.append(w.getName()).append(": Lv.").append(w.getLevel()).append(" | ");
        }
        for (PassiveItem p : passiveItems) {
            if (p.getLevel() > 0) itemSummary.append(p.getName()).append(": Lv.").append(p.getLevel()).append(" | ");
        }
        g.drawString(itemSummary.toString(), 20, 72);

        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.YELLOW);
        drawCentered(g, timeStr, SCREEN_WIDTH / 2, 35);

        levelManager.drawLevelUpUI(g, SCREEN_WIDTH, SCREEN_HEIGHT);

        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            drawCentered(g, "GAME OVER", SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 - 20);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            drawCentered(g, "Survived Time: " + timeStr, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 20);
            drawCentered(g, "Press 'R' to Restart", SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 55);
        }
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baselineY) {
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, baselineY);
    }

    private void drawToScreen() {
        repaint();
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dbImage != null) {
            g.drawImage(dbImage, 0, 0, null);
        }
    }

    @Override
    public synchronized void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) initGame();
            return;
        }
        if (levelManager.handleKeyPress(e, player)) {
            if (!levelManager.isLevelUping()) lastUpdateNanos = System.nanoTime();
            return;
        }

        activeKeys.add(e.getKeyCode());

    }

    @Override
    public synchronized void keyReleased(KeyEvent e) {
        activeKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
