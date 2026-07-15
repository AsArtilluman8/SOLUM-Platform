package com.solum.engine.environment.p63;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class SolumEnvironmentAudioSystem {
    private static final int SAMPLE_RATE = 22050;
    private static final int LOOP_SECONDS = 3;
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, LoopVoice> voices = new LinkedHashMap<>();
    private SoundPool hits;
    private int[] rainHits = new int[0];
    private int dustHit;
    private long lastLightningEvent;
    private float hitClock;
    private int hitIndex;
    private boolean initialized;
    private String status = "not_initialized";
    private String optionalAssetStatus = "not_checked";

    public SolumEnvironmentAudioSystem(Context context) {
        this.context = context.getApplicationContext();
    }

    public void initialize() {
        if (initialized) return;
        try {
            voices.put("rain", new LoopVoice(makeLoop("rain", false), makeLoop("rain", true)));
            voices.put("wind", new LoopVoice(makeLoop("wind", false), makeLoop("wind", true)));
            voices.put("snow", new LoopVoice(makeLoop("snow", false), makeLoop("snow", true)));
            voices.put("sand", new LoopVoice(makeLoop("sand", false), makeLoop("sand", true)));
            for (LoopVoice voice : voices.values()) voice.start();
            loadOptionalVerifiedHits();
            initialized = true;
            status = "functional_procedural_loops_crossfade_ready";
        } catch (Throwable error) {
            status = "audio_initialization_failed_" + safe(error);
            release();
        }
    }

    public void update(SolumEnvironmentState state, float deltaSeconds) {
        if (state == null) return;
        if (!initialized) initialize();
        if (!initialized) return;
        SolumEnvironmentAudioState audio = state.audio;
        float master = audio.muted ? 0.0f : clamp(audio.masterVolume);
        set("rain", audio.rainGain * master, audio.lowPassMix);
        set("wind", audio.windGain * master * 0.72f, audio.lowPassMix);
        set("snow", audio.snowGain * master * 0.45f, audio.lowPassMix);
        set("sand", audio.sandGain * master * 0.68f, audio.lowPassMix);
        if (state.lightning.eventIndex > lastLightningEvent) {
            lastLightningEvent = state.lightning.eventIndex;
            long delay = Math.max(0L, (long) (state.lightning.thunderDelaySeconds * 1000.0f));
            float pan = clampSigned(state.lightning.strikeX / 12.0f);
            float gain = master * (0.62f + state.lightning.flash * 0.38f);
            handler.postDelayed(() -> playThunder(gain, pan, lastLightningEvent), delay);
        }
        hitClock += Math.max(0.0f, deltaSeconds) * state.weather.rain;
        if (hitClock > 0.42f) {
            hitClock = 0.0f;
            playSurfaceHit(master * state.weather.rain * state.audio.interiorAttenuation);
        }
    }

    public void pause() {
        for (LoopVoice voice : voices.values()) voice.pause();
    }

    public void resume() {
        if (!initialized) initialize();
        for (LoopVoice voice : voices.values()) voice.start();
    }

    public void release() {
        for (LoopVoice voice : voices.values()) voice.release();
        voices.clear();
        if (hits != null) { hits.release(); hits = null; }
        handler.removeCallbacksAndMessages(null);
        initialized = false;
        status = "released";
    }

    public String getStatus() { return status + " optional=" + optionalAssetStatus; }

    private void set(String name, float gain, float lowPassMix) {
        LoopVoice voice = voices.get(name);
        if (voice != null) voice.setGain(clamp(gain), clamp(lowPassMix));
    }

    private void loadOptionalVerifiedHits() {
        String[] names = {"p59-49ee892adda7d795e1a3-e3.wav", "p59-8c5897f5a3b5552ffe66-e3.wav", "p59-bd21c0b09abe66f8bef4-e3.wav", "p59-dfdacb121efb65977184-e3.wav"};
        try {
            hits = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes()).build();
            int[] loaded = new int[3];
            for (int i = 0; i < names.length; i++) {
                try (AssetFileDescriptor descriptor = context.getAssets().openFd(names[i])) {
                    int sound = hits.load(descriptor, 1);
                    if (i < 3) loaded[i] = sound; else dustHit = sound;
                }
            }
            rainHits = loaded;
            optionalAssetStatus = "p62b_verified_wav_hits_loaded_local_only";
        } catch (Throwable ignored) {
            rainHits = new int[0]; dustHit = 0;
            optionalAssetStatus = "optional_hits_missing_procedural_fallback";
        }
    }

    private void playSurfaceHit(float gain) {
        if (hits == null || rainHits.length == 0 || gain <= 0.005f) return;
        int id = rainHits[hitIndex++ % rainHits.length];
        if (id != 0) hits.play(id, gain * 0.42f, gain * 0.42f, 0, 0, 0.94f + (hitIndex % 5) * 0.025f);
    }

    private void playThunder(float gain, float pan, long event) {
        if (!initialized || gain <= 0.005f || event != lastLightningEvent) return;
        try {
            byte[] pcm = thunderPcm(event);
            AudioTrack track = createTrack(pcm, false);
            float left = gain * (pan > 0.0f ? 1.0f - pan * 0.55f : 1.0f);
            float right = gain * (pan < 0.0f ? 1.0f + pan * 0.55f : 1.0f);
            track.setStereoVolume(clamp(left), clamp(right));
            track.play();
            handler.postDelayed(track::release, 4300L);
        } catch (Throwable error) {
            status = "thunder_play_failed_" + safe(error);
        }
    }

    private static byte[] makeLoop(String kind, boolean lowPass) {
        int frames = SAMPLE_RATE * LOOP_SECONDS;
        byte[] pcm = new byte[frames * 4];
        Random rng = new Random(SEED(kind) + (lowPass ? 17 : 0));
        float filtered = 0.0f;
        for (int frame = 0; frame < frames; frame++) {
            float t = frame / (float) SAMPLE_RATE;
            float noise = rng.nextFloat() * 2.0f - 1.0f;
            float value;
            if ("rain".equals(kind)) {
                filtered = filtered * 0.72f + noise * 0.28f;
                float drops = rng.nextFloat() > 0.992f ? (rng.nextFloat() * 2.0f - 1.0f) * 0.8f : 0.0f;
                value = filtered * 0.22f + drops;
            } else if ("wind".equals(kind)) {
                filtered = filtered * 0.985f + noise * 0.015f;
                value = filtered * (0.62f + 0.24f * (float) Math.sin(t * 0.71f));
            } else if ("snow".equals(kind)) {
                filtered = filtered * 0.91f + noise * 0.09f;
                value = filtered * 0.055f + (float) Math.sin(t * 1830.0f) * (rng.nextFloat() > 0.9985f ? 0.08f : 0.0f);
            } else {
                filtered = filtered * 0.80f + noise * 0.20f;
                value = filtered * 0.17f + (float) Math.sin(t * 72.0f) * 0.025f;
            }
            if (lowPass) value = filtered * 0.34f;
            short sample = (short) (Math.max(-1.0f, Math.min(1.0f, value)) * 11800.0f);
            int offset = frame * 4;
            pcm[offset] = (byte) sample; pcm[offset + 1] = (byte) (sample >> 8);
            pcm[offset + 2] = (byte) sample; pcm[offset + 3] = (byte) (sample >> 8);
        }
        return pcm;
    }

    private static byte[] thunderPcm(long event) {
        int frames = SAMPLE_RATE * 4;
        byte[] pcm = new byte[frames * 4];
        Random rng = new Random(1597463007L ^ event * 7919L);
        float rumble = 0.0f;
        for (int frame = 0; frame < frames; frame++) {
            float t = frame / (float) SAMPLE_RATE;
            float noise = rng.nextFloat() * 2.0f - 1.0f;
            rumble = rumble * 0.965f + noise * 0.035f;
            float crack = t < 0.14f ? noise * (1.0f - t / 0.14f) : 0.0f;
            float envelope = (float) Math.exp(-t * 0.82f) * (0.72f + 0.28f * (float) Math.sin(t * 9.0f));
            float value = crack * 0.66f + rumble * envelope * 1.18f + (float) Math.sin(t * 128.0f) * envelope * 0.11f;
            short sample = (short) (Math.max(-1.0f, Math.min(1.0f, value)) * 17000.0f);
            int offset = frame * 4;
            pcm[offset] = (byte) sample; pcm[offset + 1] = (byte) (sample >> 8);
            pcm[offset + 2] = (byte) sample; pcm[offset + 3] = (byte) (sample >> 8);
        }
        return pcm;
    }

    private static int SEED(String value) { return 1597463007 ^ value.hashCode(); }
    private static float clamp(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }
    private static float clampSigned(float value) { return Math.max(-1.0f, Math.min(1.0f, value)); }
    private static String safe(Throwable error) { return error == null ? "unknown" : error.getClass().getSimpleName(); }

    private static AudioAttributes attributes() {
        return new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    }

    private static AudioTrack createTrack(byte[] pcm, boolean looping) {
        AudioFormat format = new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build();
        AudioTrack track = new AudioTrack(attributes(), format, pcm.length, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
        track.write(pcm, 0, pcm.length);
        if (looping) track.setLoopPoints(0, pcm.length / 4, -1);
        return track;
    }

    private static final class LoopVoice {
        final AudioTrack exterior;
        final AudioTrack interior;
        boolean playing;
        LoopVoice(byte[] exteriorPcm, byte[] interiorPcm) { exterior = createTrack(exteriorPcm, true); interior = createTrack(interiorPcm, true); }
        void start() { if (playing) return; exterior.setVolume(0.0f); interior.setVolume(0.0f); exterior.play(); interior.play(); playing = true; }
        void pause() { if (!playing) return; exterior.pause(); interior.pause(); playing = false; }
        void setGain(float gain, float lowPassMix) { exterior.setVolume(gain * (1.0f - lowPassMix)); interior.setVolume(gain * lowPassMix * 0.72f); }
        void release() { try { exterior.release(); } catch (Throwable ignored) { } try { interior.release(); } catch (Throwable ignored) { } }
    }
}
