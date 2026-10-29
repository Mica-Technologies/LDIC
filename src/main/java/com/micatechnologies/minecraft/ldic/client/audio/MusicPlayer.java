package com.micatechnologies.minecraft.ldic.client.audio;

import com.micatechnologies.minecraft.ldic.Ldic;
import com.micatechnologies.minecraft.ldic.client.AssetResolver;
import com.micatechnologies.minecraft.ldic.theme.ThemeDef;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Plays a theme's music on its own thread through Java Sound.
 *
 * <p>Not through Minecraft's sound system on purpose: the loading screen starts before that
 * exists, and opening a second OpenAL context would break the game's own audio (OpenAL allows
 * one). Java Sound is independent, so music can start the moment the window appears and carry
 * on into the menu.</p>
 *
 * <p>Volume is applied in software every buffer, which makes fades smooth and lets the game's
 * Master and Music sliders (read through {@code gameVolume}) apply live.</p>
 */
public final class MusicPlayer {

    private static volatile MusicPlayer current;

    private final ThemeDef.Music music;
    private final AssetResolver assets;
    private final double playerVolume;
    private final DoubleSupplier gameVolume;
    private final Thread thread;
    private volatile boolean stopping;
    private volatile long stopStartNanos;
    private volatile double stopSeconds;

    private MusicPlayer(ThemeDef.Music music, AssetResolver assets, double playerVolume, DoubleSupplier gameVolume) {
        this.music = music;
        this.assets = assets;
        this.playerVolume = playerVolume;
        this.gameVolume = gameVolume;
        this.thread = new Thread(this::run, "LDIC music");
        this.thread.setDaemon(true);
    }

    /** Starts {@code music}, fading out whatever LDIC music was playing. */
    public static synchronized MusicPlayer play(ThemeDef.Music music, AssetResolver assets, double playerVolume, DoubleSupplier gameVolume) {
        stopCurrent();
        if (music == null || music.tracks.isEmpty() || playerVolume <= 0 || music.volume <= 0) {
            return null;
        }
        MusicPlayer p = new MusicPlayer(music, assets, playerVolume, gameVolume);
        current = p;
        p.thread.start();
        return p;
    }

    /** The music playing now, or null. */
    public static MusicPlayer current() {
        MusicPlayer p = current;
        return p != null && p.thread.isAlive() && !p.stopping ? p : null;
    }

    public static synchronized void stopCurrent() {
        MusicPlayer p = current;
        if (p != null) {
            p.fadeOut();
            current = null;
        }
    }

    public ThemeDef.Music music() {
        return music;
    }

    /** Fades out over the theme's {@code fade_out} seconds, then stops. */
    public void fadeOut() {
        if (!stopping) {
            stopSeconds = music.fadeOut;
            stopStartNanos = System.nanoTime();
            stopping = true;
        }
    }

    private void run() {
        List<String> order = new ArrayList<>(music.tracks);
        long start = System.nanoTime();
        do {
            if (music.shuffle) {
                Collections.shuffle(order);
            }
            for (String track : order) {
                if (stopping && fadeGain() <= 0) {
                    return;
                }
                if (!playTrack(track, start)) {
                    return;
                }
            }
        } while (music.loop && !stopping);
    }

    /** Plays one track; false when playback should stop entirely. */
    private boolean playTrack(String track, long start) {
        try (OggStream ogg = new OggStream(new BufferedInputStream(assets.open(track)))) {
            AudioFormat format = new AudioFormat(ogg.rate(), 16, ogg.channels(), true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            // ~100 ms of buffer: fades and volume changes take effect quickly.
            int bytes = Math.max(4096, (int) (ogg.rate() * ogg.channels() * 2 * 0.1));
            line.open(format, bytes * 2);
            line.start();
            Ldic.LOGGER.info("LDIC: playing music '{}' ({} Hz, {} channel(s))", track, ogg.rate(), ogg.channels());
            byte[] buf = new byte[bytes];
            try {
                while (true) {
                    double t = (System.nanoTime() - start) / 1e9;
                    double fadeIn = music.fadeIn <= 0 ? 1 : Math.min(1, t / music.fadeIn);
                    double fade = fadeGain();
                    if (fade <= 0) {
                        return false;
                    }
                    float gain = (float) (music.volume * playerVolume * gameVolume.getAsDouble() * fadeIn * fade);
                    int n = ogg.read(buf, gain);
                    if (n < 0) {
                        line.drain();
                        return true;
                    }
                    line.write(buf, 0, n);
                }
            } finally {
                line.stop();
                line.close();
            }
        } catch (Exception e) {
            Ldic.LOGGER.error("LDIC could not play music track '{}': {}", track, e.toString());
            return music.tracks.size() > 1;
        }
    }

    private double fadeGain() {
        if (!stopping) {
            return 1;
        }
        if (stopSeconds <= 0) {
            return 0;
        }
        double t = (System.nanoTime() - stopStartNanos) / 1e9;
        return Math.max(0, 1 - t / stopSeconds);
    }
}
