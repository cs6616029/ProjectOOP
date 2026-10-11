package project.game;

final class FrostHammerAnimation {
    static final SpriteAnimation HAMMER = SpriteAnimation.loadGrid("/project/game/effects/frost-hammer.png", 5, 3);
    static final SpriteAnimation IMPACT = SpriteAnimation.loadGrid("/project/game/effects/frost-impact.png", 5, 3);
    static final int FRAME_MILLIS = 60;
    static final int IMPACT_MILLIS = 8 * FRAME_MILLIS;
    static final int DURATION_MILLIS = 14 * FRAME_MILLIS;
    private FrostHammerAnimation() {}
}
