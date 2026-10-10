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
        LEVEL_UP("level-up.wav"),
        LASER("Laser (1).wav"),
        FLAME("flames-effect.wav"),
        EXPLOSION("explosion.wav"),
        WALKING("walking_6QdYC8X.wav");

        private final String resourceName;

        Effect(String resourceName) {
            this.resourceName = resourceName;
        }
    }

    private static final String RESOURCE_DIRECTORY = "/project/game/sounds/";
    private static final Map<Effect, Clip> clips = new EnumMap<>(Effect.class);
    private static final Set<Effect> loadAttempts = EnumSet.noneOf(Effect.class);

    private SoundEffects() {}

    static synchronized void play(Effect effect) {
        Clip clip = getClip(effect);
        if (clip == null || clip.isRunning()) return;

        clip.setFramePosition(0);
        clip.start();
    }

    static synchronized void loop(Effect effect) {
        Clip clip = getClip(effect);
        if (clip == null || clip.isRunning()) return;

        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    static synchronized void stop(Effect effect) {
        Clip clip = clips.get(effect);
        if (clip != null && clip.isRunning()) clip.stop();
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
            return clip;
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException exception) {
            System.err.println("Unable to load sound effect " + resourcePath + ": " + exception.getMessage());
            return null;
        }
    }
}
