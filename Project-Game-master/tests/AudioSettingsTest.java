package project.game;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

public final class AudioSettingsTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Object get(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(target);
    }
    private static final class Gain extends FloatControl {
        Gain() { super(FloatControl.Type.MASTER_GAIN, -80, 6, .1f, 1, 0, "dB"); }
    }
    private static final class Mute extends BooleanControl {
        Mute() { super(BooleanControl.Type.MUTE, false); }
    }
    private static final class Playback {
        final Gain gain = new Gain(); final Mute mute = new Mute();
        boolean running; int starts, loops, resets;
        Clip clip() {
            return (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(), new Class<?>[]{Clip.class}, (p, method, args) -> {
                switch (method.getName()) {
                    case "isControlSupported": return args[0] == FloatControl.Type.MASTER_GAIN || args[0] == BooleanControl.Type.MUTE;
                    case "getControl": return args[0] == FloatControl.Type.MASTER_GAIN ? gain : mute;
                    case "isRunning": return running;
                    case "start": starts++; running = true; return null;
                    case "loop": loops++; running = true; return null;
                    case "stop": running = false; return null;
                    case "setFramePosition": resets++; return null;
                    default: throw new AssertionError(method.getName());
                }
            });
        }
    }
    private static void click(VampireSurvivorsGame game, int x, int y) {
        var event = new MouseEvent(game, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1);
        for (var listener : game.getMouseListeners()) listener.mousePressed(event);
    }
    private static void drag(VampireSurvivorsGame game, int x, int y) {
        var event = new MouseEvent(game, MouseEvent.MOUSE_DRAGGED, 0, MouseEvent.BUTTON1_DOWN_MASK, x, y, 0, false);
        for (var listener : game.getMouseMotionListeners()) listener.mouseDragged(event);
    }
    private static void key(VampireSurvivorsGame game, int code) {
        game.keyPressed(new KeyEvent(game, KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED));
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Map<SoundEffects.Effect, Clip> clips = (Map<SoundEffects.Effect, Clip>) getStatic(SoundEffects.class, "clips");
        Map<SoundEffects.Effect, Playback> states = new EnumMap<>(SoundEffects.Effect.class);
        for (var effect : SoundEffects.Effect.values()) {
            Playback state = new Playback(); states.put(effect, state); clips.put(effect, state.clip());
        }
        SoundEffects.setSoundVolume(.5); SoundEffects.setMusicVolume(.25);
        for (var effect : SoundEffects.Effect.values()) {
            double expected = effect.name().startsWith("MUSIC_") ? 20 * Math.log10(.25) : 20 * Math.log10(.5);
            check(Math.abs(states.get(effect).gain.getValue() - expected) < .001, "All music and effects use their separate volume gain");
        }
        SoundEffects.play(SoundEffects.Effect.SWORD); SoundEffects.loop(SoundEffects.Effect.WALKING);
        SoundEffects.loop(SoundEffects.Effect.MUSIC_LAVA);
        SoundEffects.setSoundVolume(0);
        check(!states.get(SoundEffects.Effect.SWORD).running && !states.get(SoundEffects.Effect.WALKING).running,
                "Muting effects stops existing sword and walking sounds");
        SoundEffects.play(SoundEffects.Effect.SWORD); SoundEffects.loop(SoundEffects.Effect.WALKING);
        check(states.get(SoundEffects.Effect.SWORD).starts == 1 && states.get(SoundEffects.Effect.WALKING).loops == 1,
                "Muted effects cannot restart");
        check(states.get(SoundEffects.Effect.MUSIC_LAVA).running, "Effects mute does not mute map music");
        SoundEffects.setMusicVolume(0);
        SoundEffects.resumeLoop(SoundEffects.Effect.MUSIC_LAVA);
        check(!states.get(SoundEffects.Effect.MUSIC_LAVA).running && states.get(SoundEffects.Effect.MUSIC_LAVA).mute.getValue(),
                "Map music mute stops and suppresses looping");
        SoundEffects.setMusicVolume(1); SoundEffects.resumeLoop(SoundEffects.Effect.MUSIC_LAVA);
        check(states.get(SoundEffects.Effect.MUSIC_LAVA).running && states.get(SoundEffects.Effect.MUSIC_LAVA).resets == 1,
                "Unmuting music resumes without resetting position");
        SoundEffects.setSoundVolume(1); SoundEffects.setMusicVolume(.25);
        VampireSurvivorsGame game = new VampireSurvivorsGame();
        key(game, KeyEvent.VK_ESCAPE); click(game, 450, 513);
        AudioSettingsOverlay settings = (AudioSettingsOverlay) get(game, "audioSettings");
        check(settings.isOpen() && (boolean) get(game, "paused"), "Pause audio button opens settings without resuming");
        long time = (long) get(game, "activeTimeNanos");
        var update = game.getClass().getDeclaredMethod("updateGame"); update.setAccessible(true); update.invoke(game);
        check((long) get(game, "activeTimeNanos") == time, "Gameplay clock remains frozen in settings");
        var sound = AudioSettingsOverlay.sliderBounds(900, 675, 0);
        var music = AudioSettingsOverlay.sliderBounds(900, 675, 1);
        click(game, sound.x + sound.width / 2, sound.y + 20);
        check(SoundEffects.getSoundVolume() == .5 && SoundEffects.getMusicVolume() == .25, "Sound slider changes only effects");
        drag(game, sound.x - 100, sound.y); check(SoundEffects.getSoundVolume() == 0, "Dragging below left end mutes sound");
        drag(game, sound.x + sound.width + 100, sound.y); check(SoundEffects.getSoundVolume() == 1, "Dragging beyond right end clamps to maximum");
        var released = new MouseEvent(game, MouseEvent.MOUSE_RELEASED, 0, 0, sound.x, sound.y, 1, false, MouseEvent.BUTTON1);
        for (var listener : game.getMouseListeners()) listener.mouseReleased(released);
        drag(game, sound.x, sound.y); check(SoundEffects.getSoundVolume() == 1, "Released handle no longer drags");
        check(settings.isInteractive(sound.x + sound.width + 20, sound.y + 20, 900, 675),
                "Right side of maximum-volume knob is clickable");
        click(game, sound.x + sound.width + 12, sound.y + 20);
        check(SoundEffects.getSoundVolume() == 1, "Grabbing outer knob edge does not jump volume");
        drag(game, sound.x + sound.width + 12 - 55, sound.y);
        check(SoundEffects.getSoundVolume() == .75, "Dragging grabbed knob edge moves volume smoothly");
        SoundEffects.setSoundVolume(0);
        click(game, sound.x - 12, sound.y + 20);
        check(SoundEffects.getSoundVolume() == 0, "Left side of muted knob is clickable without jumping");
        drag(game, sound.x - 12 + 55, sound.y);
        check(SoundEffects.getSoundVolume() == .25, "Muted knob can be dragged from outside track");
        click(game, sound.x + 150, sound.y - 10);
        check(Math.abs(SoundEffects.getSoundVolume() - 150.0 / 220) < .0001,
                "Clicking near track above its visual bounds adjusts volume");
        click(game, music.x, music.y + 20); check(SoundEffects.getMusicVolume() == 0, "Music slider can turn off map music");
        key(game, KeyEvent.VK_RIGHT); check(Math.abs(SoundEffects.getMusicVolume() - .05) < .0001, "Arrow keys adjust selected slider");
        key(game, KeyEvent.VK_END); check(SoundEffects.getMusicVolume() == 1, "End selects maximum volume");
        SoundEffects.setSoundVolume(.7); SoundEffects.setMusicVolume(.4);
        if (args.length > 0) {
            BufferedImage preview = new BufferedImage(900, 675, BufferedImage.TYPE_INT_ARGB);
            var g = preview.createGraphics(); g.setColor(new java.awt.Color(14, 24, 23)); g.fillRect(0, 0, 900, 675);
            settings.draw(g, 900, 675); g.dispose(); ImageIO.write(preview, "png", new java.io.File(args[0]));
        }
        key(game, KeyEvent.VK_ESCAPE);
        check(!settings.isOpen() && (boolean) get(game, "paused"), "Escape goes back to pause menu");
        click(game, 450, 513); var back = AudioSettingsOverlay.backBounds(900, 675); click(game, back.x + 50, back.y + 20);
        check(!settings.isOpen() && (boolean) get(game, "paused"), "Back button returns to pause menu");
        click(game, 450, 369);
        check(!(boolean) get(game, "paused") && SoundEffects.getSoundVolume() == .7 && SoundEffects.getMusicVolume() == .4,
                "Restart preserves both volume settings");
        new VampireSurvivorsGame(Player.CharacterType.SURVIVOR, "/project/game/Map/lava.jpg");
        check(SoundEffects.getSoundVolume() == .7 && SoundEffects.getMusicVolume() == .4, "Changing map preserves settings");
        SoundEffects.setSoundVolume(1); SoundEffects.setMusicVolume(.25);
        System.out.println("PASS: separate gain for all effects and four songs, mute, resume, pause settings, dragging, keyboard, back, restart and map persistence");
    }
    private static Object getStatic(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(null);
    }
}
