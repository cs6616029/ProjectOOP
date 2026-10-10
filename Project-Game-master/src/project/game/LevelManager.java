package project.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelManager {
    private static final int MAX_OPTIONS = 5;
    private Weapon primaryWeapon;
    private int level = 1;
    private int xp = 0;
    private int xpToNextLevel = 10;
    private boolean isLevelUping = false;
    private int pendingUpgrades;
    private final List<Weapon> availableWeapons = new ArrayList<>();
    private final List<PassiveItem> availablePassiveItems = new ArrayList<>();

    // เก็บรายการตัวเลือก 3 ชิ้นที่สุ่มได้ในรอบนั้นๆ
    private final List<Object> offeredUpgrades = new ArrayList<>();

    public LevelManager() {
        reset();
    }

    public synchronized void reset() {
        this.level = 1;
        this.xp = 0;
        this.xpToNextLevel = 10;
        this.isLevelUping = false;
        this.offeredUpgrades.clear();
        pendingUpgrades = 0;
        availableWeapons.clear();
        availablePassiveItems.clear();
        primaryWeapon = null;
    }

    public synchronized void setPrimaryWeapon(Weapon weapon) {
        primaryWeapon = weapon;
    }

    private boolean canUpgrade(Object upgrade) {
        return upgrade instanceof Weapon ? ((Weapon) upgrade).getLevel() < 5
                : ((PassiveItem) upgrade).getLevel() < ((PassiveItem) upgrade).getMaxLevel();
    }

    public synchronized void gainXP(int amount, Player player, List<Weapon> weapons, List<PassiveItem> passiveItems) {
        if (amount <= 0 || (player != null && player.hp <= 0)) return;
        availableWeapons.clear();
        availableWeapons.addAll(weapons);
        availablePassiveItems.clear();
        availablePassiveItems.addAll(passiveItems);
        xp += amount;
        while (xp >= xpToNextLevel) {
            level++;
            xp -= xpToNextLevel;
            xpToNextLevel = (int) (xpToNextLevel * 1.5);

            if (player != null) {
                player.hp = Math.min(player.hp + 10, player.maxHp);
            }

            pendingUpgrades++;
        }
        if (!isLevelUping && pendingUpgrades > 0) offerNextUpgrade();
    }

    private void offerNextUpgrade() {
        generateUpgradeOptions(availableWeapons, availablePassiveItems);
        isLevelUping = pendingUpgrades > 0 && offeredUpgrades.stream().anyMatch(this::canUpgrade);
        if (!isLevelUping) {
            pendingUpgrades = 0;
            offeredUpgrades.clear();
        }
    }

    // ฟังก์ชันสุ่มตัวเลือก 3 ชิ้น
    private void generateUpgradeOptions(List<Weapon> weapons, List<PassiveItem> passiveItems) {
        offeredUpgrades.clear();
        List<Object> pool = new ArrayList<>();
        if (primaryWeapon != null && weapons.contains(primaryWeapon)) {
            offeredUpgrades.add(primaryWeapon);
        }

        // กรองเอาเฉพาะอาวุธที่ยังอัปเกรดไม่เต็ม
        for (Weapon w : weapons) {
            if (w != primaryWeapon && w.getLevel() < 5) {
                pool.add(w);
            }
        }

        // กรองเอาเฉพาะ PassiveItem ที่ยังอัปเกรดไม่เต็ม
        for (PassiveItem p : passiveItems) {
            if (p.getLevel() < p.getMaxLevel()) {
                pool.add(p);
            }
        }

        // สุ่มสลับรายการใน pool
        Collections.shuffle(pool);

        // ดึงมา 3 ชิ้น (หรือเท่าที่มีหากเหลือไม่ถึง 3)
        int count = Math.min(MAX_OPTIONS - offeredUpgrades.size(), pool.size());
        for (int i = 0; i < count; i++) {
            offeredUpgrades.add(pool.get(i));
        }
    }

    public synchronized boolean handleKeyPress(KeyEvent e, Player player) {
        if (!isLevelUping) return false;

        if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_5) {
            selectUpgrade(e.getKeyCode() - KeyEvent.VK_1, player);
        } else if (e.getKeyCode() >= KeyEvent.VK_NUMPAD1 && e.getKeyCode() <= KeyEvent.VK_NUMPAD5) {
            selectUpgrade(e.getKeyCode() - KeyEvent.VK_NUMPAD1, player);
        }

        return true;
    }

    public synchronized boolean handleMousePress(MouseEvent e, Player player, int screenWidth) {
        if (!isLevelUping) return false;

        if (e.getButton() == MouseEvent.BUTTON1) {
            for (int i = 0; i < offeredUpgrades.size(); i++) {
                if (getUpgradeBounds(i, screenWidth).contains(e.getPoint())) {
                    selectUpgrade(i, player);
                    break;
                }
            }
        }
        return true;
    }

    private Rectangle getUpgradeBounds(int index, int screenWidth) {
        return new Rectangle(screenWidth / 2 - 280, 200 + index * 82, 560, 72);
    }

    private void selectUpgrade(int index, Player player) {
        if (index >= 0 && index < offeredUpgrades.size()) {
            Object selected = offeredUpgrades.get(index);
            if (!canUpgrade(selected)) return;

            if (selected instanceof Weapon) {
                ((Weapon) selected).levelUp();
            } else if (selected instanceof PassiveItem) {
                ((PassiveItem) selected).levelUp(player);
            }

            pendingUpgrades--;
            if (pendingUpgrades > 0) {
                offerNextUpgrade();
            } else {
                isLevelUping = false;
                offeredUpgrades.clear();
            }
        }
    }

    public synchronized void drawLevelUpUI(Graphics2D g, int screenWidth, int screenHeight) {
        if (!isLevelUping) return;

        // ฉากหลังดำโปร่งแสง
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, screenWidth, screenHeight);

        // หัวข้อ Level Up
        g.setColor(Color.YELLOW);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString("LEVEL UP!", screenWidth / 2 - 100, 95);

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(Color.WHITE);
        String instruction = "Click an item or press 1-5 to upgrade";
        g.drawString(instruction, screenWidth / 2 - g.getFontMetrics().stringWidth(instruction) / 2, 120);

        // แสดงผลตัวเลือก 3 ชิ้น
        for (int i = 0; i < offeredUpgrades.size(); i++) {
            Object upgrade = offeredUpgrades.get(i);
            Rectangle bounds = getUpgradeBounds(i, screenWidth);
            int boxY = bounds.y;

            String name = "";
            String desc = "";
            int currentLv = 0;
            boolean isWeapon = (upgrade instanceof Weapon);
            boolean isPrimary = upgrade == primaryWeapon;
            boolean upgradeable = canUpgrade(upgrade);

            if (isWeapon) {
                Weapon w = (Weapon) upgrade;
                name = w.getName();
                desc = w.getUpgradeDescription();
                currentLv = w.getLevel();
            } else {
                PassiveItem p = (PassiveItem) upgrade;
                name = p.getName();
                desc = p.getDescription();
                currentLv = p.getLevel();
            }

            // กรอบตัวเลือก
            g.setColor(upgradeable ? new Color(35, 39, 46) : new Color(25, 27, 30));
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g.setColor(!upgradeable ? Color.GRAY : (isPrimary ? Color.YELLOW : (isWeapon ? Color.CYAN : Color.GREEN)));
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

            // ชื่อและเลเวล
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String levelText = upgradeable ? " (Lv." + (currentLv + 1) + ")" : " (MAX)";
            g.drawString("[" + (i + 1) + "] " + name + levelText + (isPrimary ? "  [MAIN]" : ""),
                    bounds.x + 20, boxY + 30);

            // คำอธิบาย
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(upgradeable ? desc : "Maximum level reached. Choose another upgrade.", bounds.x + 20, boxY + 55);
        }
    }

    public synchronized void drawXPBar(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, y, width, height);
        g.setColor(Color.CYAN);
        g.fillRect(x, y, (int) (width * ((double) xp / xpToNextLevel)), height);
        g.setColor(Color.WHITE);
        g.drawRect(x, y, width, height);
    }

    public synchronized int getLevel() { return level; }
    public synchronized boolean isLevelUping() { return isLevelUping; }
}
