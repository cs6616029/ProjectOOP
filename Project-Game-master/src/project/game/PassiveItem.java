package project.game;

public class PassiveItem {

    public enum ItemType {
        ARMOR("Armor", "+20 Max HP and Heals Player"),
        BOOTS("Boots", "+10% Movement Speed"),
        GLOVES("Gloves", "+15% Attack Speed");

        private final String defaultName;
        private final String defaultDescription;

        ItemType(String name, String description) {
            this.defaultName = name;
            this.defaultDescription = description;
        }
    }

    private final ItemType type;
    private int level = 0;
    private final int maxLevel = 5;

    public PassiveItem(ItemType type) {
        this.type = type;
    }

    public void levelUp(Player player) {
        if (level < maxLevel) {
            level++;
            applyEffect(player);
        }
    }

    private void applyEffect(Player player) {
        switch (type) {
            case ARMOR:
                player.maxHp += 20;
                player.hp = Math.min(player.hp + 20, player.maxHp);
                break;

            case BOOTS:
                player.speed *= 1.10;
                break;

            case GLOVES:
                player.attackCooldownMultiplier *= 0.85;
                break;
        }
    }

    public String getName() {
        return type.defaultName;
    }

    public String getDescription() {
        return type.defaultDescription;
    }

    public int getLevel() {
        return level;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public ItemType getType() {
        return type;
    }
}