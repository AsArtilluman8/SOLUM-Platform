package com.solum.engine.environment.p63;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Manifest-backed P63.2A diagnostic playback. No procedural or automatic ambience is created. */
public final class SolumEnvironmentAudioSystem {
    private static final String MANIFEST = "env/p63/P63_2A_VERIFIED_AUDIO_MANIFEST.json";
    private final Context context;
    private final List<Entry> entries = new ArrayList<>();
    private MediaPlayer player;
    private Entry active;
    private float masterVolume = 0.45f;
    private boolean muted;
    private boolean resumeAfterPause;
    private boolean initialized;
    private String status = "not_initialized";
    private String playbackState = "STOPPED";

    public SolumEnvironmentAudioSystem(Context context) {
        this.context = context.getApplicationContext();
    }

    public void initialize() {
        if (initialized) return;
        entries.clear();
        try (InputStream input = context.getAssets().open(MANIFEST)) {
            byte[] bytes = readAll(input);
            JSONObject root = new JSONObject(new String(bytes, "UTF-8"));
            JSONArray array = root.getJSONArray("entries");
            for (int i = 0; i < array.length(); i++) entries.add(Entry.from(array.getJSONObject(i)));
            initialized = true;
            status = "verified_manifest_loaded_entries=" + entries.size() + " longLoop=NO_VERIFIED_LONG_LOOP procedural=false";
        } catch (Throwable error) {
            initialized = false;
            status = "verified_manifest_load_failed_" + safe(error);
            playbackState = "ERROR_MANIFEST";
        }
    }

    public void update(SolumEnvironmentState state, float deltaSeconds) {
        if (state == null) return;
        if (!initialized) initialize();
        setMasterVolume(state.audio.masterVolume);
        setMuted(state.audio.muted);
        state.audio.activeProfile = playbackState;
        state.audio.rainGain = 0.0f;
        state.audio.windGain = 0.0f;
        state.audio.snowGain = 0.0f;
        state.audio.sandGain = 0.0f;
    }

    public void setMasterVolume(float value) {
        masterVolume = SolumCelestialControlState.finiteClamp(value, 0.0f, 1.0f, 0.45f);
        applyVolume();
    }

    public void setMuted(boolean value) {
        muted = value;
        applyVolume();
    }

    public void playRainHit() { playFirst("rain_surface_hit"); }
    public void playThunder() { playFirst("thunder_one_shot"); }
    public void playWind() { playFirst("wind_whistle_layer"); }

    public void playFirst(String semanticRole) {
        if (!initialized) initialize();
        Entry entry = null;
        for (Entry candidate : entries) {
            if (semanticRole.equals(candidate.semanticRole) && candidate.available()) { entry = candidate; break; }
        }
        if (entry == null) {
            playbackState = "ERROR_NO_VERIFIED_" + semanticRole.toUpperCase(Locale.US);
            status = playbackState;
            return;
        }
        play(entry);
    }

    public void play(Entry entry) {
        stopAll();
        if (entry == null || !entry.available()) {
            playbackState = "ERROR_ENTRY_UNAVAILABLE";
            return;
        }
        try {
            MediaPlayer next = new MediaPlayer();
            next.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            try (AssetFileDescriptor descriptor = context.getAssets().openFd(entry.assetPath)) {
                next.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            } catch (Throwable compressedAsset) {
                next.setDataSource(copyToCache(entry).getAbsolutePath());
            }
            next.setLooping(false);
            next.setOnCompletionListener(completed -> {
                playbackState = "COMPLETED " + entry.verifiedFileName;
                releasePlayer();
            });
            next.setOnErrorListener((failed, what, extra) -> {
                playbackState = "ERROR_PLAYBACK_" + what + "_" + extra + " " + entry.verifiedFileName;
                releasePlayer();
                return true;
            });
            next.prepare();
            player = next;
            active = entry;
            applyVolume();
            next.start();
            playbackState = "PLAYING " + entry.verifiedFileName;
            status = "verified_wav_playing_no_loop";
        } catch (Throwable error) {
            playbackState = "ERROR " + entry.verifiedFileName + " " + safe(error);
            status = "verified_wav_play_failed_" + safe(error);
            releasePlayer();
        }
    }

    public void stopAll() {
        if (player != null) {
            try { player.stop(); } catch (Throwable ignored) { }
        }
        releasePlayer();
        playbackState = "STOPPED";
    }

    public void pause() {
        resumeAfterPause = player != null && player.isPlaying();
        if (resumeAfterPause) {
            try { player.pause(); playbackState = "PAUSED " + activeName(); } catch (Throwable ignored) { }
        }
    }

    public void resume() {
        if (resumeAfterPause && player != null) {
            try { player.start(); playbackState = "PLAYING " + activeName(); } catch (Throwable error) { playbackState = "ERROR_RESUME_" + safe(error); }
        }
        resumeAfterPause = false;
    }

    public void release() {
        stopAll();
        initialized = false;
        entries.clear();
        status = "released";
    }

    public String getStatus() { return status; }
    public String getPlaybackState() { return playbackState; }
    public List<Entry> getEntries() { return Collections.unmodifiableList(entries); }

    public String getDiagnosticText() {
        StringBuilder text = new StringBuilder();
        text.append("proceduralAudio=false\nlongLoop=NO_VERIFIED_LONG_LOOP\nplayback=").append(playbackState);
        for (Entry entry : entries) {
            text.append("\n").append(entry.verifiedFileName)
                .append(" | ").append(entry.durationSeconds).append("s")
                .append(" | ").append(entry.sampleRate).append("Hz ").append(entry.channels).append("ch")
                .append(" | ").append(entry.provenance).append("/").append(entry.semanticConfidence)
                .append(" | ").append(entry.status);
        }
        return text.toString();
    }

    private void applyVolume() {
        if (player == null) return;
        float applied = muted ? 0.0f : masterVolume;
        try { player.setVolume(applied, applied); } catch (Throwable ignored) { }
    }

    private File copyToCache(Entry entry) throws Exception {
        File dir = new File(context.getCacheDir(), "p63_2a_audio");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("audio_cache_dir_unavailable");
        File target = new File(dir, entry.verifiedFileName + ".wav");
        try (InputStream input = context.getAssets().open(entry.assetPath); FileOutputStream output = new FileOutputStream(target, false)) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) >= 0) if (count > 0) output.write(buffer, 0, count);
        }
        return target;
    }

    private void releasePlayer() {
        MediaPlayer old = player;
        player = null;
        active = null;
        if (old != null) try { old.release(); } catch (Throwable ignored) { }
    }

    private String activeName() { return active == null ? "none" : active.verifiedFileName; }
    private static String safe(Throwable error) { return error == null ? "unknown" : error.getClass().getSimpleName(); }

    private static byte[] readAll(InputStream input) throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) >= 0) if (count > 0) output.write(buffer, 0, count);
        return output.toByteArray();
    }

    public static final class Entry {
        public final String verifiedFileName;
        public final String assetPath;
        public final String sha256;
        public final String provenance;
        public final String semanticConfidence;
        public final String semanticRole;
        public final String status;
        public final float durationSeconds;
        public final int sampleRate;
        public final int channels;

        private Entry(String verifiedFileName, String assetPath, String sha256, String provenance,
                      String semanticConfidence, String semanticRole, String status,
                      float durationSeconds, int sampleRate, int channels) {
            this.verifiedFileName = verifiedFileName; this.assetPath = assetPath; this.sha256 = sha256;
            this.provenance = provenance; this.semanticConfidence = semanticConfidence; this.semanticRole = semanticRole;
            this.status = status; this.durationSeconds = durationSeconds; this.sampleRate = sampleRate; this.channels = channels;
        }

        static Entry from(JSONObject item) {
            return new Entry(item.optString("verifiedFileName", "unknown"), item.optString("assetPath", ""),
                item.optString("sha256", ""), item.optString("provenance", "UNKNOWN"),
                item.optString("semanticConfidence", "LOW"), item.optString("semanticRole", "UNKNOWN"),
                item.optString("status", "UNAVAILABLE"), (float) item.optDouble("durationSeconds", 0.0),
                item.optInt("sampleRate", 0), item.optInt("channels", 0));
        }

        public boolean available() { return "PACKAGED_LOCAL_PRIVATE".equals(status) && !assetPath.isEmpty(); }
    }
}
