package project.game;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

final class SoundEffects {
    enum Effect {
        MENU_CLICK("click sound.wav"),
        SELECT("select sound.wav"),
        PICKUP("select sound.wav"),
        UPGRADE("buy_1.wav"),
        ENEMY_DEATH("enemy-death.wav"),
        BOSS_DEATH("boss-death.wav"),
        CRAB_DEATH("crab-dead.wav"),
        DRAGON_DEATH("dragon-dead.wav"),
        SKELETON_DEATH("skeleton-dead.wav"),
        DOG_DEATH("dog-dead.wav"),
        GREEN_MONSTER_DEATH("normal-dead1.wav"),
        SLIME_DEATH("slime-dead.wav"),
        GOBLIN_DEATH("goblin-dead.wav"),
        LEVEL_UP("level-up.wav"),
        LASER("Laser (1).wav"),
        SWORD("sword-slash.wav"),
        FROST_HAMMER("effect-frost.wav"),
        TORNADO("tornado.wav"),
        SWORD_OF_JUSTICE("sword-of-justice.wav"),
        STRONG_BOY_PUNCH("strong-boy-punch.wav"),
        PLAYER_HURT("classic_hurt.wav"),
        FLAME("flames-effect.wav"),
        EXPLOSION("explosion.wav"),
        MUSIC_FOREST("forest-musicbg.wav"),
        MUSIC_DESERT("desert-musicbg.wav"),
        MUSIC_NIGHT("night-musicbg.wav"),
        MUSIC_LAVA("lava-musicbg.wav"),
        WALKING("walking_6QdYC8X.wav"),
        WALKING_NORMAL_MAPS("walk.wav");

        private final String resourceName;

        Effect(String resourceName) {
            this.resourceName = resourceName;
        }
    }

    private static final String RESOURCE_DIRECTORY = "/project/game/sounds/";
    private static final Map<Effect, Clip> clips = new EnumMap<>(Effect.class);
    private static final Set<Effect> loadAttempts = EnumSet.noneOf(Effect.class);
    private static double soundVolume = 1.0, musicVolume = 0.25;

    private SoundEffects() {}

    static synchronized double getSoundVolume() { return soundVolume; }
    static synchronized double getMusicVolume() { return musicVolume; }
    static synchronized void setSoundVolume(double volume) { setVolume(false, volume); }
    static synchronized void setMusicVolume(double volume) { setVolume(true, volume); }

    private static boolean isMusic(Effect effect) { return effect.name().startsWith("MUSIC_"); }
    private static double volume(Effect effect) { return isMusic(effect) ? musicVolume : soundVolume; }

    private static void setVolume(boolean music, double value) {
        double level = Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
        if (music) musicVolume = level; else soundVolume = level;
        for (var entry : clips.entrySet()) {
            if (isMusic(entry.getKey()) == music) {
                applyVolume(entry.getValue(), level);
                if (level == 0 && entry.getValue().isRunning()) entry.getValue().stop();
            }
        }
    }

    private static void applyVolume(Clip clip, double level) {
        if (clip.isControlSupported(javax.sound.sampled.BooleanControl.Type.MUTE)) {
            ((javax.sound.sampled.BooleanControl) clip.getControl(javax.sound.sampled.BooleanControl.Type.MUTE)).setValue(level == 0);
        }
        if (clip.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
            var gain = (javax.sound.sampled.FloatControl) clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            float decibels = level == 0 ? gain.getMinimum() : (float) (20 * Math.log10(level));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
        }
    }

    static synchronized void play(Effect effect) {
        if (volume(effect) == 0) return;
        Clip clip = getClip(effect);
        if (clip == null) return;
        if (clip.isRunning()) {
            if (effect != Effect.SWORD && effect != Effect.PLAYER_HURT
                    && effect != Effect.FROST_HAMMER && effect != Effect.TORNADO
                    && effect != Effect.SWORD_OF_JUSTICE && effect != Effect.STRONG_BOY_PUNCH) return;
            clip.stop();
        }

        clip.setFramePosition(0);
        clip.start();
    }

    static synchronized void loop(Effect effect) {
        if (volume(effect) == 0) return;
        Clip clip = getClip(effect);
        if (clip == null || clip.isRunning()) return;

        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    static synchronized void stop(Effect effect) {
        Clip clip = clips.get(effect);
        if (clip != null && clip.isRunning()) clip.stop();
    }

    static synchronized void resumeLoop(Effect effect) {
        if (volume(effect) == 0) return;
        Clip clip = getClip(effect);
        if (clip != null && !clip.isRunning()) clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    private static Clip getClip(Effect effect) {
        Clip clip = clips.get(effect);
        if (clip == null && loadAttempts.add(effect)) {
            clip = loadClip(effect);
            if (clip != null) clips.put(effect, clip);
        }
        return clip;
    }

    private static Clip loadClip(Effect effect) {
        String resourcePath = RESOURCE_DIRECTORY + effect.resourceName;
        var resource = SoundEffects.class.getResource(resourcePath);
        if (resource == null) {
            System.err.println("Unable to find sound effect: " + resourcePath);
            return null;
        }

        try (AudioInputStream audio = AudioSystem.getAudioInputStream(resource)) {
            Clip clip = AudioSystem.getClip();
            clip.open(audio);
            applyVolume(clip, volume(effect));
            return clip;
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException exception) {
            System.err.println("Unable to load sound effect " + resourcePath + ": " + exception.getMessage());
            return null;
        }
    }
}
