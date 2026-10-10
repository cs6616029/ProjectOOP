package project.game;

final class SpawnBalance {
    static final int MAX_REGULAR_ENEMIES = 90;
    static final long FIRST_BOSS_SECONDS = 75;
    static final long BOSS_REST_SECONDS = 45;

    private SpawnBalance() { }

    static EnemyType typeForRoll(double roll, long seconds) {
        double normalChance, fastChance;
        if (seconds < 30) {
            normalChance = 0.85;
            fastChance = 0.15;
        } else if (seconds < 60) {
            normalChance = 0.75;
            fastChance = 0.20;
        } else if (seconds < 120) {
            normalChance = 0.65;
            fastChance = 0.25;
        } else if (seconds < 180) {
            normalChance = 0.55;
            fastChance = 0.30;
        } else {
            normalChance = 0.50;
            fastChance = 0.30;
        }
        if (roll < normalChance) return EnemyType.NORMAL;
        if (roll < normalChance + fastChance) return EnemyType.FAST;
        return EnemyType.TANK;
    }

    static long intervalMillis(long seconds, int level, boolean bossAlive) {
        long interval = Math.max(500L, 1100L - (long) (Math.min(seconds, 300) * 1.6)
                - Math.min(Math.max(0, level - 1), 20) * 8L);
        return interval + (bossAlive ? 250 : 0);
    }

    static int batchSize(long seconds, boolean bossAlive) {
        if (bossAlive || seconds < 60) return 1;
        return seconds < 180 ? 2 : 3;
    }
}
