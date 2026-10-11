package project.game;

/** Music lifecycle is separate from short combat sound effects. */
final class MapMusic {
    private final SoundEffects.Effect track;
    private boolean started;

    MapMusic(String map) {
        track = map == null ? null : switch (map) {
            case "/project/game/Map/forest.jpg" -> SoundEffects.Effect.MUSIC_FOREST;
            case "/project/game/Map/desert.jpg" -> SoundEffects.Effect.MUSIC_DESERT;
            case "/project/game/Map/night.jpg" -> SoundEffects.Effect.MUSIC_NIGHT;
            default -> null;
        };
    }

    void start() {
        for (SoundEffects.Effect music : new SoundEffects.Effect[]{SoundEffects.Effect.MUSIC_FOREST,
                SoundEffects.Effect.MUSIC_DESERT, SoundEffects.Effect.MUSIC_NIGHT}) SoundEffects.stop(music);
        started = track != null;
        if (started) SoundEffects.loop(track);
    }

    void pause() { if (started) SoundEffects.stop(track); }
    void resume() { if (started) SoundEffects.resumeLoop(track); }
    void stop() { if (started) SoundEffects.stop(track); started = false; }
}
