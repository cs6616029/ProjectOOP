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
    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final List<BossAttack> bossAttacks = new ArrayList<>();
    private final List<Weapon> weapons = new ArrayList<>();
    private final List<PassiveItem> passiveItems = new ArrayList<>();

    private final LevelManager levelManager = new LevelManager();
    private LevelUpEffect levelUpEffect;
    private BossSpawnAlert bossSpawnAlert;
    private GameOverAnimation gameOverAnimation;
    private VictoryMenuOverlay victoryMenu;
    private boolean victoryHovered;
    private XPPickupEffect xpPickupEffect;
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
    private int pauseHoveredButton = -1;
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
        MouseAdapter pauseHover = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                synchronized (VampireSurvivorsGame.this) {
                    victoryHovered = gameWon && victoryMenuButtonBounds().contains(event.getPoint());
                    boolean overUpgrade = !paused && !gameOver && !gameWon && levelManager.handleMouseMove(event, SCREEN_WIDTH);
                    pauseHoveredButton = !paused ? -1 : resumeButtonBounds().contains(event.getPoint()) ? 0
                            : restartButtonBounds().contains(event.getPoint()) ? 1
                            : mainMenuButtonBounds().contains(event.getPoint()) ? 2 : -1;
                    setCursor(java.awt.Cursor.getPredefinedCursor(pauseHoveredButton >= 0 || overUpgrade || victoryHovered
                            ? java.awt.Cursor.HAND_CURSOR : java.awt.Cursor.DEFAULT_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                synchronized (VampireSurvivorsGame.this) {
                    pauseHoveredButton = -1;
                    victoryHovered = false;
                    levelManager.clearHover();
                    setCursor(java.awt.Cursor.getDefaultCursor());
                }
            }
        };
        addMouseMotionListener(pauseHover);
        addMouseListener(pauseHover);
        initGame();
    }

    private synchronized void initGame() {
        paused = false;
        pauseHoveredButton = -1;
        setCursor(java.awt.Cursor.getDefaultCursor());
        SoundEffects.stop(SoundEffects.Effect.WALKING);
        player = new Player(0, 0, characterType);
        enemies.clear();
        gems.clear();
        visualEffects.clear();
        damageNumbers.clear();
        bossAttacks.clear();
        weapons.clear();
        passiveItems.clear();
        activeKeys.clear();

        levelManager.reset();
        levelUpEffect = null;
        bossSpawnAlert = null;
        gameOverAnimation = null;
        victoryMenu = null;
        victoryHovered = false;
        xpPickupEffect = null;
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
        weapons.add(new TornadoWeapon(0));
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
        damageNumbers.removeIf(number -> {
            number.update();
            return number.isFinished();
        });
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
            enemy.collectDamageNumbers(damageNumbers);

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
                    visualEffects.add(VisualEffect.enemyDeath(enemy.x, enemy.y, enemy.size));
                }
                int gemXp = (enemy.type == EnemyType.BOSS) ? 50 : 5;
                gems.add(new Gem(enemy.x, enemy.y, gemXp, enemy.type == EnemyType.BOSS));
                score += enemy.scoreValue;
                eIt.remove();
                continue;
            }

            enemy.updateStatus(currentTime);
            enemy.updateCombatMovement(player);

            if (enemy.type != EnemyType.BOSS && Math.hypot(enemy.x - player.x, enemy.y - player.y) > 1200) {
                eIt.remove();
                continue;
            }

            enemy.updateBossSkill(player, bossAttacks, currentTime);
            enemy.updateRangedAttack(player, bossAttacks, currentTime);

            // แก้ไขการรับดาเมจ ให้เช็ค Invincibility Frames
            damagePlayer(enemy.updateMeleeAttack(player, currentTime));
            if (gameOver) return;
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
                xpPickupEffect = new XPPickupEffect(System.nanoTime());
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
        damageNumbers.add(DamageNumber.playerHit(player.x, player.y - player.getRenderSize() / 2.0, damage));
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
        bossSpawnAlert = new BossSpawnAlert(gameTimeMillis);
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
        for (Weapon weapon : weapons) {
            if (!(weapon instanceof LaserWeapon)) weapon.draw(dbg, player, cameraX, cameraY);
        }
        for (VisualEffect ve : visualEffects) ve.draw(dbg, cameraX, cameraY);

        player.draw(dbg, cameraX, cameraY);
        for (Weapon weapon : weapons) {
            if (weapon instanceof LaserWeapon) weapon.draw(dbg, player, cameraX, cameraY);
        }
        for (DamageNumber number : damageNumbers) number.draw(dbg, cameraX, cameraY);
        if (xpPickupEffect != null) {
            long pickupTime = System.nanoTime();
            if (gameOver || xpPickupEffect.isFinished(pickupTime)) xpPickupEffect = null;
            else xpPickupEffect.draw(dbg, player, cameraX, cameraY, pickupTime);
        }
        if (bossSpawnAlert != null) {
            if (gameOver || gameWon || bossSpawnAlert.isFinished(gameTimeMillis)) bossSpawnAlert = null;
            else {
                bossSpawnAlert.drawScreenEdges(dbg, SCREEN_WIDTH, SCREEN_HEIGHT, gameTimeMillis);
                bossSpawnAlert.draw(dbg, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 - 100, gameTimeMillis);
            }
        }
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
        if (gameWon) {
            long now = System.nanoTime();
            if (victoryMenu == null) victoryMenu = new VictoryMenuOverlay(now);
            victoryMenu.draw(g, SCREEN_WIDTH, SCREEN_HEIGHT, elapsedSeconds, score, victoryHovered, now);
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        HealthBar.draw(g, 20, 16, player.hp, player.maxHp);
        g.setColor(Color.WHITE);

        levelManager.drawXPBar(g, 20, 64, 296, 43);

        g.setFont(GameStartScreen.pixelFont(24));
        g.drawString("Level: " + levelManager.getLevel(), 330, 32);
        g.drawString("Score: " + score, 330, 50);

        g.setFont(GameStartScreen.pixelFont(18));
        g.setColor(Color.LIGHT_GRAY);
        StringBuilder itemSummary = new StringBuilder();
        for (Weapon w : weapons) {
            if (w.getLevel() > 0) itemSummary.append(w.getName()).append(": Lv.").append(w.getLevel()).append(" | ");
        }
        for (PassiveItem p : passiveItems) {
            if (p.getLevel() > 0) itemSummary.append(p.getName()).append(": Lv.").append(p.getLevel()).append(" | ");
        }
        GameHudText.drawSkills(g, itemSummary.toString(), 20, 126, SCREEN_WIDTH - 40);

        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        g.setFont(GameStartScreen.pixelFont(36));
        g.setColor(Color.YELLOW);
        drawCentered(g, timeStr, SCREEN_WIDTH - 64, 85);

        levelManager.drawLevelUpUI(g, SCREEN_WIDTH, SCREEN_HEIGHT);

        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            if (gameOverAnimation == null) gameOverAnimation = new GameOverAnimation(System.nanoTime());
            gameOverAnimation.draw(g, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 - 128, System.nanoTime());
            g.setColor(Color.WHITE);
            g.setFont(GameStartScreen.pixelFont(28));
            drawCentered(g, "Survived Time: " + timeStr, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
            drawCentered(g, "Final Score: " + score, SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 36);
            drawCentered(g, "Press 'R' to Restart", SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2 + 72);
        } else {
            PauseMenuOverlay.drawButton(g, pauseButtonBounds(), "PAUSE", false, false);
        }
    }

    private void drawPauseMenu(Graphics2D g) {
        PauseMenuOverlay.draw(g, SCREEN_WIDTH, SCREEN_HEIGHT, levelManager.getLevel(), elapsedSeconds, score,
                resumeButtonBounds(), restartButtonBounds(), mainMenuButtonBounds(), pauseHoveredButton);
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
        pauseHoveredButton = -1;
        setCursor(java.awt.Cursor.getDefaultCursor());
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
        return VictoryMenuOverlay.menuButtonBounds(SCREEN_WIDTH, SCREEN_HEIGHT);
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
