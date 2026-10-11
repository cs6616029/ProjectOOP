package project.game;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class EnemyDeathSoundTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static final class Playback {
        int starts; boolean running;
        Clip clip() {
            return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(), new Class<?>[]{Clip.class}, (p, method, args) -> {
                switch (method.getName()) {
                    case "isRunning": return running;
                    case "start": starts++; running = true; return null;
                    case "stop": running = false; return null;
                    case "setFramePosition": return null;
                    default: throw new AssertionError("Unexpected sound operation: " + method.getName());
                }
            });
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (String name : List.of("crab-dead.wav", "dragon-dead.wav", "skeleton-dead.wav", "dog-dead.wav",
                "slime-dead.wav", "goblin-dead.wav", "normal-dead1.wav")) {
            try (var audio = AudioSystem.getAudioInputStream(EnemyDeathSoundTest.class.getResource("/project/game/sounds/" + name))) {
                check(audio.getFormat().getSampleSizeInBits() == 16 && audio.getFormat().getSampleRate() == 44100
                        && audio.getFrameLength() > 1000 && audio.readNBytes(4096).length == 4096,
                        "Supplied death sound decodes as playable PCM: " + name);
            }
        }
        Map<SoundEffects.Effect, Clip> clips = (Map<SoundEffects.Effect, Clip>) field(SoundEffects.class, "clips").get(null);
        Map<SoundEffects.Effect, Playback> playback = new EnumMap<>(SoundEffects.Effect.class);
        for (SoundEffects.Effect sound : List.of(SoundEffects.Effect.CRAB_DEATH, SoundEffects.Effect.DRAGON_DEATH,
                SoundEffects.Effect.SKELETON_DEATH, SoundEffects.Effect.DOG_DEATH, SoundEffects.Effect.SLIME_DEATH,
                SoundEffects.Effect.GOBLIN_DEATH, SoundEffects.Effect.GREEN_MONSTER_DEATH,
                SoundEffects.Effect.ENEMY_DEATH, SoundEffects.Effect.BOSS_DEATH)) {
            Playback state = new Playback(); playback.put(sound, state); clips.put(sound, state.clip());
        }
        List<Enemy> fixtures = new ArrayList<>(); List<SoundEffects.Effect> expected = new ArrayList<>();
        Object[] normalSprites = (Object[]) field(Enemy.class, "NORMAL_SPRITES").get(null);
        SoundEffects.Effect[] normalSounds = {SoundEffects.Effect.SLIME_DEATH, SoundEffects.Effect.GREEN_MONSTER_DEATH,
                SoundEffects.Effect.ENEMY_DEATH, SoundEffects.Effect.ENEMY_DEATH, SoundEffects.Effect.GOBLIN_DEATH};
        for (int index = 0; index < normalSprites.length; index++) {
            Enemy enemy = new Enemy(0, 0, EnemyType.NORMAL, 1);
            field(Enemy.class, "sprite").set(enemy, normalSprites[index]); fixtures.add(enemy); expected.add(normalSounds[index]);
        }
        Object[] fastSprites = (Object[]) field(Enemy.class, "FAST_SPRITES").get(null);
        for (int index = 0; index < 2; index++) {
            Enemy enemy = new Enemy(0, 0, EnemyType.FAST, 1);
            field(Enemy.class, "sprite").set(enemy, fastSprites[index]); fixtures.add(enemy);
            expected.add(index == 0 ? SoundEffects.Effect.DOG_DEATH : SoundEffects.Effect.ENEMY_DEATH);
        }
        fixtures.add(new Enemy(0, 0, EnemyType.TANK, 1)); expected.add(SoundEffects.Effect.CRAB_DEATH);
        for (BossSkill.Kind kind : BossSkill.Kind.values()) {
            fixtures.add(new Enemy(0, 0, EnemyType.BOSS, 1, kind));
            expected.add(switch (kind) {
                case DRAGON -> SoundEffects.Effect.DRAGON_DEATH;
                case SKELETON -> SoundEffects.Effect.SKELETON_DEATH;
                default -> SoundEffects.Effect.BOSS_DEATH;
            });
        }
        ISEKAISurvivors game = new ISEKAISurvivors();
        var update = game.getClass().getDeclaredMethod("updateGame"); update.setAccessible(true);
        var reset = game.getClass().getDeclaredMethod("initGame"); reset.setAccessible(true);
        for (int index = 0; index < fixtures.size(); index++) {
            reset.invoke(game);
            ((List<Weapon>) field(game.getClass(), "weapons").get(game)).clear();
            List<Enemy> enemies = (List<Enemy>) field(game.getClass(), "enemies").get(game); enemies.clear();
            Player player = (Player) field(game.getClass(), "player").get(game);
            Enemy enemy = fixtures.get(index); enemy.x = player.x + 400; enemy.y = player.y; enemy.hp = 0;
            SoundEffects.Effect sound = expected.get(index);
            check(enemy.deathSound() == sound, "Enemy appearance and boss kind select the correct sound");
            for (Playback state : playback.values()) state.running = false;
            int previous = playback.get(sound).starts;
            int total = playback.values().stream().mapToInt(state -> state.starts).sum();
            enemies.add(enemy); update.invoke(game);
            check(playback.get(sound).starts == previous + 1
                    && playback.values().stream().mapToInt(state -> state.starts).sum() == total + 1,
                    "Death plays only the specific sound without also playing the old generic sound");
            check(!enemies.contains(enemy) && (int) field(game.getClass(), "score").get(game) == enemy.scoreValue
                    && ((List<?>) field(game.getClass(), "gems").get(game)).size() == 1,
                    "Death preserves removal, score and EXP drop");
            update.invoke(game);
            check(playback.get(sound).starts == previous + 1, "Removed enemies cannot replay death sounds");
        }
        System.out.println("PASS: seven supplied death sounds including green monster, all appearances and bosses, single playback, generic fallback, scores and EXP drops");
    }
}
