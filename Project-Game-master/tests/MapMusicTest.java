package project.game;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JPanel;

public class MapMusicTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static final class Playback {
        int loops, resets, stops; boolean running;
        Clip clip() {
            return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(), new Class<?>[]{Clip.class}, (p, method, args) -> {
                switch (method.getName()) {
                    case "isRunning": return running;
                    case "loop": check((int) args[0] == Clip.LOOP_CONTINUOUSLY, "Music loops continuously"); loops++; running = true; return null;
                    case "stop": stops++; running = false; return null;
                    case "setFramePosition": check((int) args[0] == 0, "New sessions begin at track start"); resets++; return null;
                    default: throw new AssertionError("Unexpected music operation: " + method.getName());
                }
            });
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (String map : List.of("forest", "desert", "night", "lava")) {
            try (var audio = AudioSystem.getAudioInputStream(MapMusicTest.class.getResource("/project/game/sounds/" + map + "-musicbg.wav"))) {
                check(audio.getFormat().getSampleSizeInBits() == 16 && audio.getFormat().getChannels() == 2,
                        "Converted map music is compatible stereo PCM");
                check(audio.getFrameLength() / audio.getFormat().getSampleRate() > 160 && audio.readNBytes(4096).length == 4096,
                        "Full-length supplied music is readable");
            }
        }
        Map<SoundEffects.Effect, Clip> clips = (Map<SoundEffects.Effect, Clip>) field(SoundEffects.class, "clips").get(null);
        Playback forest = new Playback(), desert = new Playback(), night = new Playback(), lava = new Playback();
        clips.put(SoundEffects.Effect.MUSIC_FOREST, forest.clip()); clips.put(SoundEffects.Effect.MUSIC_DESERT, desert.clip());
        clips.put(SoundEffects.Effect.MUSIC_NIGHT, night.clip());
        clips.put(SoundEffects.Effect.MUSIC_LAVA, lava.clip());
        MapMusic music = new MapMusic("/project/game/Map/forest.jpg");
        music.resume(); check(forest.loops == 0, "Construction does not start background music");
        music.start(); check(forest.loops == 1 && forest.resets == 1 && !night.running && !desert.running, "Forest starts its own looping track");
        music.resume(); music.resume(); check(forest.loops == 1, "Game ticks do not restart a playing track");
        music.pause(); music.resume(); check(forest.loops == 2 && forest.resets == 1, "Pause resumes from the current music position");
        MapMusic desertMusic = new MapMusic("/project/game/Map/desert.jpg"); desertMusic.start();
        check(!forest.running && desert.running, "Changing maps stops old music and selects Desert");
        MapMusic nightMusic = new MapMusic("/project/game/Map/night.jpg"); nightMusic.start();
        check(!desert.running && night.running, "Night selects its supplied song");
        nightMusic.start(); check(night.resets == 2, "Restart begins music from the start");
        nightMusic.stop(); nightMusic.resume(); check(!night.running, "Stopped game music cannot resume accidentally");
        MapMusic lavaMusic = new MapMusic("/project/game/Map/lava.jpg"); lavaMusic.start();
        check(lava.running && lava.loops == 1 && lava.resets == 1 && !forest.running && !desert.running && !night.running,
                "Lava plays its supplied looping music and stops other maps");
        lavaMusic.pause(); lavaMusic.resume();
        check(lava.running && lava.loops == 2 && lava.resets == 1, "Lava resumes from current position after pause");
        new MapMusic("/project/game/Map/forest.jpg").start();
        check(!lava.running && forest.running, "Changing away from Lava stops its song");
        new MapMusic("/project/game/Map/unknown.jpg").start();
        check(!forest.running && !desert.running && !night.running && !lava.running, "Unknown maps do not play unrelated music");
        VampireSurvivorsGame game = new VampireSurvivorsGame();
        MapMusic gameMusic = (MapMusic) field(game.getClass(), "mapMusic").get(game); gameMusic.start();
        LevelManager manager = (LevelManager) field(game.getClass(), "levelManager").get(game);
        Player player = (Player) field(game.getClass(), "player").get(game);
        List<Weapon> weapons = (List<Weapon>) field(game.getClass(), "weapons").get(game);
        List<PassiveItem> passives = (List<PassiveItem>) field(game.getClass(), "passiveItems").get(game);
        manager.gainXP(10, player, weapons, passives);
        var update = game.getClass().getDeclaredMethod("updateGame"); update.setAccessible(true); update.invoke(game);
        check(!forest.running, "Level up pauses background music");
        manager.handleKeyPress(new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_1, '1'), player);
        int resets = forest.resets; update.invoke(game);
        check(forest.running && forest.resets == resets, "Leaving level up resumes without starting the song over");
        long now = (long) field(game.getClass(), "gameTimeMillis").get(game); player.hp = 1; player.lastDamageTime = now - 450;
        var damage = game.getClass().getDeclaredMethod("damagePlayer", int.class); damage.setAccessible(true); damage.invoke(game, 3);
        check(!forest.running, "Death immediately stops map music");
        field(game.getClass(), "running").set(game, true);
        var restart = game.getClass().getDeclaredMethod("initGame"); restart.setAccessible(true); restart.invoke(game);
        check(forest.running && forest.resets == resets + 1, "Restarted run restarts the correct map song");
        field(game.getClass(), "running").set(game, false); gameMusic.stop();
        System.out.println("PASS: four map songs including Lava, PCM decode, looping, map changes, pause and resume, level-up, death and restart");
    }
}
