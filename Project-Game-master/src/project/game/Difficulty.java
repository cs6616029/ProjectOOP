package project.game;

public enum Difficulty {
    NORMAL("NORMAL", "STANDARD CHALLENGE", "SURVIVE AND GROW", 1.0, 1.0, 1.0, 0),
    HARD("HARD", "TOUGHER ENEMIES", "LARGER HORDES", 1.5, 1.25, 0.8, 1),
    VERY_HARD("VERY HARD", "RELENTLESS ENEMIES", "OVERWHELMING HORDES", 2.0, 1.5, 0.65, 2);

    final String label, description, detail;
    private final double healthMultiplier, damageMultiplier, spawnIntervalMultiplier;
    private final int extraEnemies;

    Difficulty(String label, String description, String detail, double healthMultiplier,
            double damageMultiplier, double spawnIntervalMultiplier, int extraEnemies) {
        this.label = label;
        this.description = description;
        this.detail = detail;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.spawnIntervalMultiplier = spawnIntervalMultiplier;
        this.extraEnemies = extraEnemies;
    }

    int health(int base) { return (int) Math.ceil(base * healthMultiplier); }
    int damage(int base) { return (int) Math.ceil(base * damageMultiplier); }
    long spawnInterval(long base) { return Math.max(1, Math.round(base * spawnIntervalMultiplier)); }
    int spawnCount(int base) { return base + extraEnemies; }
}
