package project.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
    private static final BossSkill.Kind[] BOSS_SEQUENCE = BossSkill.Kind.values();
    private Thread gameThread;
    private volatile boolean running = false;
    private final int FPS = 60;
    private final long TARGET_TIME = 1000 / FPS;

    private BufferedImage dbImage;
    private Graphics2D dbg;
    private final BufferedImage mapBackground;
    private final Player.CharacterType characterType;
    private final Runnable onReturnToMenu;

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
    private boolean gameWon;
    private boolean paused;
    private int bossesDefeated;
    private int score = 0;

    public VampireSurvivorsGame() {
        this(Player.CharacterType.SURVIVOR, "/project/game/Map/forest.jpg");
    }

    public VampireSurvivorsGame(Player.CharacterType characterType, String mapBackgroundResource) {
        this(characterType, mapBackgroundResource, () -> {});
    }

    public VampireSurvivorsGame(Player.CharacterType characterType, String mapBackgroundResource,
            Runnable onReturnToMenu) {
        this.characterType = characterType;
        this.onReturnToMenu = onReturnToMenu;
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
                    if (gameWon) {
                        handleVictoryClick(e);
                    } else if (paused) {
                        handlePauseClick(e);
                    } else if (!gameOver && !gameWon && pauseButtonBounds().contains(e.getPoint())) {
                        setPaused(true);
                        SoundEffects.play(SoundEffects.Effect.MENU_CLICK);
                    } else if (!gameOver && !gameWon && levelManager.handleMousePress(e, player, SCREEN_WIDTH)
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
                    SoundEffects.stop(SoundEffects.Effect.WALKING);
                }
            }
        });
        initGame();
    }

    private synchronized void initGame() {
        paused = false;
        SoundEffects.stop(SoundEffects.Effect.WALKING);
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
        gameWon = false;
        bossesDefeated = 0;
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
        if (gameOver || gameWon || paused || levelManager.isLevelUping()) {
            SoundEffects.stop(SoundEffects.Effect.WALKING);
            return;
        }

        activeTimeNanos += deltaNanos;
        gameTimeMillis = gameStartTime + activeTimeNanos / 1_000_000L;
        player.gameTimeMillis = gameTimeMillis;
        elapsedSeconds = (gameTimeMillis - gameStartTime) / 1000;

        int dx = 0, dy = 0;
        if (activeKeys.contains(KeyEvent.VK_W) || activeKeys.contains(KeyEvent.VK_UP)) dy -= 1;
        if (activeKeys.contains(KeyEvent.VK_S) || activeKeys.contains(KeyEvent.VK_DOWN)) dy += 1;
        if (activeKeys.contains(KeyEvent.VK_A) || activeKeys.contains(KeyEvent.VK_LEFT)) dx -= 1;
        if (activeKeys.contains(KeyEvent.VK_D) || activeKeys.contains(KeyEvent.VK_RIGHT)) dx += 1;

        if (dx == 0 && dy == 0) SoundEffects.stop(SoundEffects.Effect.WALKING);
        else SoundEffects.loop(SoundEffects.Effect.WALKING);
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

        if (bossesDefeated < BOSS_SEQUENCE.length && !hasBossOnField()
                && elapsedSeconds >= nextBossSpawnSeconds) {
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
                    SoundEffects.play(SoundEffects.Effect.BOSS_DEATH);
                    visualEffects.add(VisualEffect.bossDeath(enemy.x, enemy.y, enemy.size));
                    bossesDefeated++;
                    if (bossesDefeated == BOSS_SEQUENCE.length) {
                        gameWon = true;
                        activeKeys.clear();
                        SoundEffects.stop(SoundEffects.Effect.WALKING);
                    } else {
                        nextBossSpawnSeconds = elapsedSeconds + SpawnBalance.BOSS_REST_SECONDS;
                    }
                } else {
                    SoundEffects.play(SoundEffects.Effect.ENEMY_DEATH);
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
                SoundEffects.play(SoundEffects.Effect.PICKUP);
                gIt.remove();
                if (levelManager.isLevelUping()) {
                    activeKeys.clear();
                    SoundEffects.stop(SoundEffects.Effect.WALKING);
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
        if (damage <= 0 || gameOver || gameWon
                || gameTimeMillis - player.lastDamageTime < player.invincibilityDuration) return;
        player.hp = Math.max(0, player.hp - damage);
        player.lastDamageTime = gameTimeMillis;
        if (player.hp == 0) {
            gameOver = true;
            activeKeys.clear();
            SoundEffects.stop(SoundEffects.Effect.WALKING);
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

        Enemy boss = new Enemy(ex, ey, EnemyType.BOSS, levelManager.getLevel(),
                BOSS_SEQUENCE[bossesDefeated]);
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
        if (level > displayedLevel) {
            levelUpEffect = new LevelUpEffect(presentationTime);
            SoundEffects.play(SoundEffects.Effect.LEVEL_UP);
        }
        displayedLevel = level;
        if (levelUpEffect != null) {
            if (gameOver || levelUpEffect.isFinished(presentationTime)) levelUpEffect = null;
            else levelUpEffect.draw(dbg, player, cameraX, cameraY, levelManager.isLevelUping(), presentationTime);
        }
        if (paused) drawPauseMenu(dbg);
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

        if (gameOver || gameWon) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            g.setColor(gameWon ? new Color(105, 235, 145) : Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            drawCentered(g, gameWon ? "YOU WIN!" : "GAME OVER", SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 - 45);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            drawCentered(g, "Survived Time: " + timeStr, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
            drawCentered(g, "Final Score: " + score, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 36);
            if (gameWon) {
                drawPauseButton(g, victoryMenuButtonBounds(), "Main Menu");
            } else {
                drawCentered(g, "Press 'R' to Restart", SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 72);
            }
        } else {
            g.setColor(new Color(8, 17, 31, 210));
            g.fillRoundRect(SCREEN_WIDTH - 108, 16, 88, 34, 10, 10);
            g.setColor(Color.WHITE);
            g.drawRoundRect(SCREEN_WIDTH - 108, 16, 88, 34, 10, 10);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            drawCentered(g, "PAUSE", SCREEN_WIDTH - 64, 38);
        }
    }

    private void drawPauseMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        g.setColor(new Color(8, 17, 31, 245));
        g.fillRoundRect(270, 165, 360, 365, 24, 24);
        g.setColor(new Color(255, 199, 74));
        g.setStroke(new java.awt.BasicStroke(2));
        g.drawRoundRect(270, 165, 360, 365, 24, 24);
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        drawCentered(g, "PAUSED", SCREEN_WIDTH / 2, 225);
        drawPauseButton(g, resumeButtonBounds(), "Resume");
        drawPauseButton(g, restartButtonBounds(), "Restart");
        drawPauseButton(g, mainMenuButtonBounds(), "Main Menu");
    }

    private void drawPauseButton(Graphics2D g, Rectangle bounds, String label) {
        g.setColor(new Color(29, 50, 75));
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);
        g.setColor(Color.WHITE);
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        drawCentered(g, label, bounds.x + bounds.width / 2, bounds.y + 37);
    }

    private void handlePauseClick(MouseEvent event) {
        if (event.getButton() != MouseEvent.BUTTON1) return;
        if (resumeButtonBounds().contains(event.getPoint())) {
            setPaused(false);
            SoundEffects.play(SoundEffects.Effect.MENU_CLICK);
        } else if (restartButtonBounds().contains(event.getPoint())) {
            initGame();
            SoundEffects.play(SoundEffects.Effect.MENU_CLICK);
        } else if (mainMenuButtonBounds().contains(event.getPoint())) {
            returnToMenu();
        }
    }

    private void handleVictoryClick(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON1
                && victoryMenuButtonBounds().contains(event.getPoint())) {
            returnToMenu();
        }
    }

    private void returnToMenu() {
        running = false;
        SoundEffects.stop(SoundEffects.Effect.WALKING);
        SoundEffects.play(SoundEffects.Effect.MENU_CLICK);
        onReturnToMenu.run();
    }

    private void setPaused(boolean paused) {
        this.paused = paused;
        activeKeys.clear();
        SoundEffects.stop(SoundEffects.Effect.WALKING);
        lastUpdateNanos = System.nanoTime();
    }

    private Rectangle pauseButtonBounds() {
        return new Rectangle(SCREEN_WIDTH - 108, 16, 88, 34);
    }

    private Rectangle resumeButtonBounds() {
        return new Rectangle(310, 260, 280, 58);
    }

    private Rectangle restartButtonBounds() {
        return new Rectangle(310, 340, 280, 58);
    }

    private Rectangle mainMenuButtonBounds() {
        return new Rectangle(310, 420, 280, 58);
    }

    private Rectangle victoryMenuButtonBounds() {
        return new Rectangle(310, SCREEN_HEIGHT / 2 + 95, 280, 58);
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
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !gameOver && !gameWon) {
            setPaused(!paused);
            SoundEffects.play(SoundEffects.Effect.MENU_CLICK);
            return;
        }
        if (paused) return;
        if (gameOver || gameWon) {
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
