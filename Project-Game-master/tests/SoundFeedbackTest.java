package project.game;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class SoundFeedbackTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static final class Playback {
        int starts, stops; boolean running;
        Clip clip() {
            return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(), new Class<?>[]{Clip.class}, (p, method, args) -> {
                switch (method.getName()) {
                    case "isRunning": return running;
                    case "start": starts++; running = true; return null;
                    case "stop": stops++; running = false; return null;
                    case "setFramePosition": check((int) args[0] == 0, "Effects replay from the start"); return null;
                    default: throw new AssertionError("Unexpected audio operation: " + method.getName());
                }
            });
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (String name : List.of("classic_hurt.wav", "sword-slash.wav")) {
            try (var audio = AudioSystem.getAudioInputStream(SoundFeedbackTest.class.getResource("/project/game/sounds/" + name))) {
                check(audio.getFormat().getSampleSizeInBits() == 16 && audio.getFormat().getSampleRate() == 44100,
                        "MP3 is packaged as compatible PCM WAV");
                check(audio.getFrameLength() > 1000 && audio.readAllBytes().length > 4000, "Converted sound contains playable audio");
            }
        }
        Map<SoundEffects.Effect, Clip> clips = (Map<SoundEffects.Effect, Clip>) field(SoundEffects.class, "clips").get(null);
        Playback hurt = new Playback(), slash = new Playback();
        clips.put(SoundEffects.Effect.PLAYER_HURT, hurt.clip()); clips.put(SoundEffects.Effect.SWORD, slash.clip());
        check(field(SoundEffects.Effect.class, "resourceName").get(SoundEffects.Effect.SWORD).equals("sword-slash.wav"),
                "Sword effect uses the newly supplied sound");
        VampireSurvivorsGame game = new VampireSurvivorsGame();
        Player player = (Player) field(game.getClass(), "player").get(game);
        Field clock = field(game.getClass(), "gameTimeMillis"); long now = (long) clock.get(game);
        player.lastDamageTime = now - player.invincibilityDuration;
        var damage = game.getClass().getDeclaredMethod("damagePlayer", int.class); damage.setAccessible(true);
        damage.invoke(game, 3); check(hurt.starts == 1, "Actual damage plays hurt sound");
        damage.invoke(game, 3); damage.invoke(game, 0); check(hurt.starts == 1, "Invincibility and zero damage remain silent");
        clock.set(game, now + player.invincibilityDuration); damage.invoke(game, 3);
        check(hurt.starts == 2 && hurt.stops == 1, "A later hit restarts the hurt sound");
        player.hp = 1; clock.set(game, now + player.invincibilityDuration * 2); damage.invoke(game, 3);
        check(hurt.starts == 3, "Lethal damage also plays hurt sound");
        damage.invoke(game, 3); check(hurt.starts == 3, "Game over cannot repeat hurt sound");
        SwordWeapon sword = new SwordWeapon(1); Player fighter = new Player(0, 0);
        sword.update(fighter, List.of(), new ArrayList<>(), 10000);
        sword.update(fighter, List.of(), new ArrayList<>(), 10001);
        check(slash.starts == 1, "Sword sound plays once per swing");
        sword.update(fighter, List.of(), new ArrayList<>(), 11500);
        check(slash.starts == 2 && slash.stops == 1, "Next swing replays the replacement sound");
        System.out.println("PASS: supplied sounds decode as PCM, hurt on real and lethal damage, silent invincibility, sword replacement and replay");
    }
}
