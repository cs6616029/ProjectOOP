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
        int starts, stops, loops; boolean running;
        Clip clip() {
            return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(), new Class<?>[]{Clip.class}, (p, method, args) -> {
                switch (method.getName()) {
                    case "isRunning": return running;
                    case "start": starts++; running = true; return null;
                    case "loop": loops++; running = true; return null;
                    case "stop": stops++; running = false; return null;
                    case "setFramePosition": check((int) args[0] == 0, "Effects replay from the start"); return null;
                    default: throw new AssertionError("Unexpected audio operation: " + method.getName());
                }
            });
        }
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        for (String name : List.of("classic_hurt.wav", "sword-slash.wav", "effect-frost.wav", "tornado.wav",
                "sword-of-justice.wav", "strong-boy-punch.wav", "walk.wav")) {
            try (var audio = AudioSystem.getAudioInputStream(SoundFeedbackTest.class.getResource("/project/game/sounds/" + name))) {
                check(audio.getFormat().getSampleSizeInBits() == 16
                        && audio.getFormat().getSampleRate() == (name.equals("tornado.wav") ? 48000 : 44100),
                        "Supplied sound is packaged as compatible PCM WAV");
                byte[] samples = audio.readAllBytes();
                check(audio.getFrameLength() > 1000 && samples.length > 4000, "Converted sound contains playable audio");
                if (name.equals("tornado.wav")) {
                    int bytes = Math.min(samples.length, (int) (audio.getFormat().getSampleRate() * .1) * audio.getFormat().getFrameSize());
                    double energy = 0;
                    for (int i = 0; i < bytes - 1; i += 2) {
                        int sample = (short) ((samples[i] & 255) | (samples[i + 1] << 8));
                        energy += sample * (double) sample;
                    }
                    check(Math.sqrt(energy / (bytes / 2)) > 500, "Tornado is audible in first 100ms of orbit, without quiet introduction");
                }
            }
        }
        Map<SoundEffects.Effect, Clip> clips = (Map<SoundEffects.Effect, Clip>) field(SoundEffects.class, "clips").get(null);
        Playback hurt = new Playback(), slash = new Playback(), frost = new Playback(), tornado = new Playback(), justice = new Playback();
        Playback punch = new Playback();
        clips.put(SoundEffects.Effect.PLAYER_HURT, hurt.clip()); clips.put(SoundEffects.Effect.SWORD, slash.clip());
        clips.put(SoundEffects.Effect.FROST_HAMMER, frost.clip());
        clips.put(SoundEffects.Effect.TORNADO, tornado.clip());
        clips.put(SoundEffects.Effect.SWORD_OF_JUSTICE, justice.clip());
        clips.put(SoundEffects.Effect.STRONG_BOY_PUNCH, punch.clip());
        check(field(SoundEffects.Effect.class, "resourceName").get(SoundEffects.Effect.SWORD).equals("sword-slash.wav"),
                "Sword effect uses the newly supplied sound");
        ISEKAISurvivors game = new ISEKAISurvivors();
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
        Enemy skeleton = new Enemy(0, 0, EnemyType.BOSS, 1, BossSkill.Kind.SKELETON);
        fighter.x = 150;
        skeleton.updateMeleeAttack(fighter, 20000);
        skeleton.updateMeleeAttack(fighter, 20459);
        check(slash.starts == 2, "Skeleton warning and dash are silent before slash");
        check(skeleton.updateMeleeAttack(fighter, 20460) == skeleton.damage && slash.starts == 3,
                "Skeleton plays sword sound exactly when melee slash hits");
        skeleton.updateMeleeAttack(fighter, 20460); skeleton.updateMeleeAttack(fighter, 20500);
        check(slash.starts == 3, "Skeleton slash frames cannot repeat sound");
        skeleton = new Enemy(0, 0, EnemyType.BOSS, 1, BossSkill.Kind.SKELETON);
        skeleton.updateMeleeAttack(fighter, 30000); fighter.y = 300;
        check(skeleton.updateMeleeAttack(fighter, 30460) == 0 && slash.starts == 4,
                "Dodged slash still plays its sword sound");
        fighter.y = 0;
        for (int interruption = 0; interruption < 3; interruption++) {
            skeleton = new Enemy(0, 0, EnemyType.BOSS, 1, BossSkill.Kind.SKELETON);
            skeleton.updateMeleeAttack(fighter, 40000);
            if (interruption == 0) skeleton.hp = 0;
            else if (interruption == 1) { skeleton.applyFrostHit(40300); skeleton.applyFrostHit(40300); }
            else skeleton.applyAirborne(40300);
            skeleton.updateMeleeAttack(fighter, 40460);
            check(slash.starts == 4, "Cancelled Skeleton slash stays silent");
        }
        FrostHammerWeapon hammer = new FrostHammerWeapon(1);
        Player mage = new Player(0, 0);
        Enemy target = new Enemy(120, 0, EnemyType.NORMAL, 1); target.hp = 1000;
        hammer.update(mage, List.of(), new ArrayList<>(), 49000);
        check(frost.starts == 0, "Hammer without a target stays silent");
        hammer.update(mage, List.of(target), new ArrayList<>(), 50000);
        hammer.update(mage, List.of(target), new ArrayList<>(), 50479);
        check(frost.starts == 0, "Hammer windup stays silent until impact");
        hammer.update(mage, List.of(target), new ArrayList<>(), 50480);
        check(frost.starts == 1 && target.hp == 968, "Frost sound plays exactly with impact damage");
        hammer.update(mage, List.of(target), new ArrayList<>(), 50480);
        hammer.update(mage, List.of(target), new ArrayList<>(), 50800);
        check(frost.starts == 1, "Impact sound plays once per slam");
        hammer.update(mage, List.of(target), new ArrayList<>(), 51200);
        target.x = 1000;
        hammer.update(mage, List.of(target), new ArrayList<>(), 51680);
        check(frost.starts == 2 && frost.stops == 1 && target.hp == 968,
                "Next ground slam replays frost sound even if target dodges and previous audio is still running");
        TornadoWeapon storm = new TornadoWeapon(5);
        target.x = 400; target.hp = 1000;
        storm.update(mage, List.of(), new ArrayList<>(), 60000);
        check(tornado.starts == 0, "Tornado without target stays silent");
        storm.update(mage, List.of(target), new ArrayList<>(), 60000);
        check(tornado.starts == 1, "Summoning three tornadoes plays one sound per cast");
        storm.update(mage, List.of(target), new ArrayList<>(), 60000);
        storm.update(mage, List.of(target), new ArrayList<>(), 60600);
        storm.update(mage, List.of(target), new ArrayList<>(), 61200);
        check(tornado.starts == 1, "Orbit and launch do not repeat cast sound");
        storm.update(mage, List.of(target), new ArrayList<>(), 63200);
        storm.update(mage, List.of(target), new ArrayList<>(), 63520);
        storm.update(mage, List.of(target), new ArrayList<>(), 64500);
        check(tornado.starts == 2 && tornado.stops == 1, "Next cast replays tornado sound");
        SwordOfJusticeWeapon holySword = new SwordOfJusticeWeapon(5);
        target.x = 200; target.hp = 1000;
        Enemy secondTarget = new Enemy(-200, 0, EnemyType.NORMAL, 1); secondTarget.hp = 1000;
        holySword.update(mage, List.of(), new ArrayList<>(), 70000);
        check(justice.starts == 0, "Sword of Justice without targets stays silent");
        holySword.update(mage, List.of(target, secondTarget), new ArrayList<>(), 70000);
        check(justice.starts == 1 && target.hp == 1000, "Two falling swords play one sound immediately on cast");
        holySword.update(mage, List.of(target, secondTarget), new ArrayList<>(), 70000);
        holySword.update(mage, List.of(target, secondTarget), new ArrayList<>(), 70320);
        check(justice.starts == 1, "Falling swords and impacts do not repeat cast sound");
        holySword.update(mage, List.of(target, secondTarget), new ArrayList<>(), 72499);
        check(justice.starts == 1, "Justice cooldown suppresses extra sound");
        holySword.update(mage, List.of(target, secondTarget), new ArrayList<>(), 72500);
        check(justice.starts == 2 && justice.stops == 1, "Next Justice cast replays supplied sound");
        StrongBoyPunch fists = new StrongBoyPunch();
        target.x = 70; target.hp = 1000;
        fists.update(mage, List.of(), 79000);
        fists.update(mage, List.of(target), 80000);
        fists.update(mage, List.of(target), 80239);
        check(punch.starts == 0, "Strong Boy windup and lunge stay silent before punch");
        fists.update(mage, List.of(target), 80240);
        check(punch.starts == 1 && target.hp == 982, "Punch sound plays with the 18-damage impact");
        fists.update(mage, List.of(target), 80240); fists.update(mage, List.of(target), 80600);
        check(punch.starts == 1, "Punch animation cannot repeat audio");
        fists.update(mage, List.of(target), 80900); target.x = 400;
        fists.update(mage, List.of(target), 81140);
        check(punch.starts == 2 && punch.stops == 1 && target.hp == 982, "Next punch replays sound even when target dodges");
        StrongBoyPunch cancelledPunch = new StrongBoyPunch(); target.x = 70;
        cancelledPunch.update(mage, List.of(target), 82000); mage.hp = 0;
        cancelledPunch.update(mage, List.of(target), 82240);
        check(punch.starts == 2, "Death cancels pending punch sound");
        Playback walk = new Playback(), lavaWalk = new Playback();
        clips.put(SoundEffects.Effect.WALKING_NORMAL_MAPS, walk.clip());
        clips.put(SoundEffects.Effect.WALKING, lavaWalk.clip());
        for (String map : List.of("forest", "desert", "night", "lava")) {
            ISEKAISurvivors walkingGame = new ISEKAISurvivors(Player.CharacterType.SURVIVOR,
                    "/project/game/Map/" + map + ".jpg");
            Playback expected = map.equals("lava") ? lavaWalk : walk;
            Playback other = map.equals("lava") ? walk : lavaWalk;
            int previousLoops = expected.loops;
            var tick = walkingGame.getClass().getDeclaredMethod("updateGame"); tick.setAccessible(true);
            var pressed = new java.awt.event.KeyEvent(walkingGame, java.awt.event.KeyEvent.KEY_PRESSED, 0, 0,
                    java.awt.event.KeyEvent.VK_W, 'w');
            walkingGame.keyPressed(pressed); tick.invoke(walkingGame); tick.invoke(walkingGame);
            check(expected.running && expected.loops == previousLoops + 1 && !other.running,
                    "Only map-specific walking sound loops once while moving: " + map);
            walkingGame.keyReleased(new java.awt.event.KeyEvent(walkingGame, java.awt.event.KeyEvent.KEY_RELEASED, 0, 0,
                    java.awt.event.KeyEvent.VK_W, 'w')); tick.invoke(walkingGame);
            check(!expected.running, "Stopping movement stops footsteps: " + map);
            walkingGame.keyPressed(pressed); tick.invoke(walkingGame);
            walkingGame.keyPressed(new java.awt.event.KeyEvent(walkingGame, java.awt.event.KeyEvent.KEY_PRESSED, 0, 0,
                    java.awt.event.KeyEvent.VK_ESCAPE, java.awt.event.KeyEvent.CHAR_UNDEFINED));
            check(!expected.running, "Pause immediately stops map footsteps: " + map);
        }
        System.out.println("PASS: supplied PCM sounds, skill timing and replay, walk.wav for maps 1-3 only, original Lava footsteps, movement and pause");
    }
}
