package com.solum.engine.skyweather;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import com.google.android.filament.Engine;
import com.google.android.filament.Scene;
import com.google.android.filament.gltfio.AssetLoader;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.android.filament.gltfio.MaterialProvider;
import com.google.android.filament.gltfio.ResourceLoader;
import com.google.android.filament.gltfio.UbershaderProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SkyWeatherVisualLayer {
    private AssetLoader assetLoader;
    private ResourceLoader resourceLoader;
    private MaterialProvider materialProvider;
    private FilamentAsset asset;
    private AudioTrack audioTrack;
    private String sunDiskStatus = "not_created";
    private String moonDiskStatus = "not_created";
    private String starsStatus = "not_created";
    private String cloudLayerStatus = "not_created";
    private String rainStatus = "disabled";
    private String snowStatus = "disabled";
    private String weatherAudioStatus = "placeholder_only";
    private String visualLayerStatus = "not_created";
    private int generatedSkyWeatherAssetBytes = 0;
    private int publicSkyWeatherAssetCount = 0;
    private int runtimeGeneratedAssetCount = 0;
    private float cloudCoverageActual = 0.0f;
    private float cloudDensityActual = 0.0f;
    private float weatherSoundVolume = 0.18f;

    public void apply(Engine engine, Scene scene, SkyActualState sky, WeatherActualState weather) {
        if (engine == null || scene == null || sky == null || weather == null) {
            visualLayerStatus = "missing_engine_scene_or_state";
            return;
        }
        releaseAsset(engine, scene);
        try {
            if (materialProvider == null) materialProvider = new UbershaderProvider(engine);
            if (assetLoader == null) assetLoader = new AssetLoader(engine, materialProvider, com.google.android.filament.EntityManager.get());
            if (resourceLoader == null) resourceLoader = new ResourceLoader(engine);

            SkyWeatherGlbBuilder builder = new SkyWeatherGlbBuilder();
            buildSkyWeatherGeometry(builder, sky, weather);
            byte[] glb = builder.build();
            generatedSkyWeatherAssetBytes = glb.length;
            runtimeGeneratedAssetCount = 1;
            publicSkyWeatherAssetCount = 0;
            ByteBuffer data = ByteBuffer.allocateDirect(glb.length).order(ByteOrder.LITTLE_ENDIAN);
            data.put(glb);
            data.flip();
            asset = assetLoader.createAsset(data);
            if (asset == null) {
                visualLayerStatus = "gltfio_asset_create_failed";
                return;
            }
            resourceLoader.loadResources(asset);
            scene.addEntities(asset.getEntities());
            asset.releaseSourceData();
            visualLayerStatus = "generated_gltf_layer_visible bytes=" + generatedSkyWeatherAssetBytes;
        } catch (Throwable t) {
            visualLayerStatus = "generated_gltf_layer_failed: " + shortMessage(t);
        }
    }

    public void setWeatherSoundVolume(float value) {
        weatherSoundVolume = SkySettings.clamp(value, 0.0f, 1.0f);
        if (audioTrack != null) audioTrack.setVolume(weatherSoundVolume);
    }

    public void applyWeatherAudio(WeatherActualState weather, float volume) {
        setWeatherSoundVolume(volume);
        if (weather == null) {
            stopWeatherAudio();
            weatherAudioStatus = "placeholder_only_weather_state_missing";
            return;
        }
        boolean audible = weather.getRainIntensity() > 0.02f || weather.getSnowIntensity() > 0.02f || weather.getWindIntensity() > 0.20f;
        if (!audible || weatherSoundVolume <= 0.0f) {
            stopWeatherAudio();
            weatherAudioStatus = "generated_basic_disabled";
            return;
        }
        try {
            int sampleRate = 16000;
            int sampleCount = sampleRate * 2;
            short[] pcm = new short[sampleCount];
            Random random = new Random(9355L + weather.getPreset().ordinal() * 17L);
            double thunderPhase = 0.0;
            for (int i = 0; i < sampleCount; i++) {
                float white = (random.nextFloat() * 2.0f - 1.0f);
                float rain = white * weather.getRainIntensity() * 0.28f;
                float snow = white * weather.getSnowIntensity() * 0.07f;
                float wind = (float) Math.sin(i * 2.0 * Math.PI * 92.0 / sampleRate) * weather.getWindIntensity() * 0.08f;
                float thunder = 0.0f;
                if (weather.getPreset() == WeatherPreset.STORM) {
                    thunderPhase += 2.0 * Math.PI * 37.0 / sampleRate;
                    thunder = (float) Math.sin(thunderPhase) * 0.12f * (1.0f - (i / (float) sampleCount));
                }
                float value = SkySettings.clamp((rain + snow + wind + thunder) * weatherSoundVolume, -0.75f, 0.75f);
                pcm[i] = (short) (value * 32767.0f);
            }
            AudioTrack next = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .setAudioFormat(new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.length * 2)
                .build();
            next.write(pcm, 0, pcm.length);
            next.setLoopPoints(0, pcm.length, -1);
            next.setVolume(weatherSoundVolume);
            stopWeatherAudio();
            audioTrack = next;
            audioTrack.play();
            weatherAudioStatus = "generated_basic_enabled preset=" + weather.getPreset().name().toLowerCase(Locale.US);
        } catch (Throwable t) {
            stopWeatherAudio();
            weatherAudioStatus = "placeholder_only_audio_failed: " + shortMessage(t);
        }
    }

    public void release(Engine engine, Scene scene) {
        releaseAsset(engine, scene);
        stopWeatherAudio();
        try {
            if (resourceLoader != null) resourceLoader.destroy();
        } catch (Throwable ignored) { }
        try {
            if (assetLoader != null) assetLoader.destroy();
        } catch (Throwable ignored) { }
        try {
            if (materialProvider != null) materialProvider.destroy();
        } catch (Throwable ignored) { }
        resourceLoader = null;
        assetLoader = null;
        materialProvider = null;
    }

    public String getSunDiskStatus() { return sunDiskStatus; }
    public String getMoonDiskStatus() { return moonDiskStatus; }
    public String getStarsStatus() { return starsStatus; }
    public String getCloudLayerStatus() { return cloudLayerStatus; }
    public String getRainStatus() { return rainStatus; }
    public String getSnowStatus() { return snowStatus; }
    public String getWeatherAudioStatus() { return weatherAudioStatus; }
    public String getVisualLayerStatus() { return visualLayerStatus; }
    public int getGeneratedSkyWeatherAssetBytes() { return generatedSkyWeatherAssetBytes; }
    public int getPublicSkyWeatherAssetCount() { return publicSkyWeatherAssetCount; }
    public int getRuntimeGeneratedAssetCount() { return runtimeGeneratedAssetCount; }
    public float getCloudCoverageActual() { return cloudCoverageActual; }
    public float getCloudDensityActual() { return cloudDensityActual; }
    public float getWeatherSoundVolume() { return weatherSoundVolume; }

    private void releaseAsset(Engine engine, Scene scene) {
        if (asset == null) return;
        try {
            if (scene != null) scene.removeEntities(asset.getEntities());
        } catch (Throwable ignored) { }
        try {
            if (assetLoader != null) assetLoader.destroyAsset(asset);
        } catch (Throwable ignored) { }
        asset = null;
    }

    private void stopWeatherAudio() {
        if (audioTrack == null) return;
        try { audioTrack.stop(); } catch (Throwable ignored) { }
        try { audioTrack.release(); } catch (Throwable ignored) { }
        audioTrack = null;
    }

    private void buildSkyWeatherGeometry(SkyWeatherGlbBuilder builder, SkyActualState sky, WeatherActualState weather) throws Exception {
        cloudCoverageActual = weather.getCloudCoverage();
        cloudDensityActual = weather.getCloudDensity();
        MaterialRef sunMaterial = builder.material("sun_core", 1.0f, 0.86f, 0.42f, 1.0f, 1.0f, 0.74f, 0.28f);
        MaterialRef moonMaterial = builder.material("moon_phase", 0.62f, 0.70f, 0.86f, 0.95f, 0.28f, 0.34f, 0.50f);
        MaterialRef starMaterial = builder.material("stars_generated", 0.82f, 0.90f, 1.0f, 0.92f, 0.36f, 0.42f, 0.55f);
        MaterialRef cloudMaterial = builder.material("clouds_generated", 0.62f, 0.66f, 0.70f, SkySettings.clamp(0.22f + weather.getCloudDensity() * 0.48f, 0.0f, 0.74f), 0.0f, 0.0f, 0.0f);
        MaterialRef rainMaterial = builder.material("rain_generated", 0.42f, 0.62f, 0.95f, 0.72f, 0.02f, 0.04f, 0.10f);
        MaterialRef snowMaterial = builder.material("snow_generated", 0.92f, 0.96f, 1.0f, 0.86f, 0.18f, 0.20f, 0.23f);

        if (sky.getSun().isVisible() && sky.getSun().getElevationDeg() > 0.0f) {
            builder.disc("sun_disk", skyDir(sky.getSun().getAzimuthDeg(), sky.getSun().getElevationDeg()), 21.0f, 0.42f, 28, sunMaterial.index, 1.0f);
            sunDiskStatus = "world_space_visible";
        } else {
            sunDiskStatus = "hidden_below_horizon";
        }
        if (sky.getMoon().isVisible() && sky.getMoon().getElevationDeg() > 0.0f) {
            float phaseScale = SkySettings.clamp(0.24f + sky.getMoon().getPhase() * 0.76f, 0.18f, 1.0f);
            builder.disc("moon_disk", skyDir(sky.getMoon().getAzimuthDeg(), sky.getMoon().getElevationDeg()), 20.5f, 0.34f, 24, moonMaterial.index, phaseScale);
            moonDiskStatus = "world_space_visible";
        } else {
            moonDiskStatus = "hidden_below_horizon";
        }
        if (sky.getStarsIntensity() > 0.03f) {
            builder.stars(72, 27.0f, 0.035f, sky.getStarsIntensity(), starMaterial.index);
            starsStatus = "generated_visible";
        } else {
            starsStatus = "generated_hidden_day";
        }
        if (weather.getCloudCoverage() > 0.02f) {
            builder.clouds(Math.max(1, Math.round(weather.getCloudCoverage() * 10.0f)), 18.0f, weather.getCloudDensity(), cloudMaterial.index);
            cloudLayerStatus = "generated_visible";
        } else {
            cloudLayerStatus = "generated_hidden_clear";
        }
        if (weather.getRainIntensity() > 0.02f) {
            builder.rain(Math.max(8, Math.round(weather.getRainIntensity() * 72.0f)), weather.getRainIntensity(), rainMaterial.index);
            rainStatus = "generated_particles_visible";
        } else {
            rainStatus = "disabled";
        }
        if (weather.getSnowIntensity() > 0.02f) {
            builder.snow(Math.max(8, Math.round(weather.getSnowIntensity() * 54.0f)), weather.getSnowIntensity(), snowMaterial.index);
            snowStatus = "generated_particles_visible";
        } else {
            snowStatus = "disabled";
        }
    }

    private static float[] skyDir(float azimuthDeg, float elevationDeg) {
        double az = Math.toRadians(azimuthDeg);
        double el = Math.toRadians(elevationDeg);
        return new float[] {(float) (Math.cos(el) * Math.sin(az)), (float) Math.sin(el), (float) (Math.cos(el) * Math.cos(az))};
    }

    private static String shortMessage(Throwable t) {
        String msg = t == null ? "" : t.getMessage();
        if (msg == null || msg.trim().isEmpty()) return t == null ? "unknown" : t.getClass().getSimpleName();
        return msg.length() > 120 ? msg.substring(0, 120) : msg;
    }

    private static final class MaterialRef {
        final int index;
        MaterialRef(int index) { this.index = index; }
    }

    private static final class SkyWeatherGlbBuilder {
        private final List<Float> positions = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();
        private final List<MeshRef> meshes = new ArrayList<>();
        private final JSONArray materials = new JSONArray();
        private int minVertex = 0;

        MaterialRef material(String name, float r, float g, float b, float a, float er, float eg, float eb) throws Exception {
            JSONObject pbr = new JSONObject();
            JSONArray base = new JSONArray();
            base.put(r).put(g).put(b).put(a);
            pbr.put("baseColorFactor", base);
            pbr.put("metallicFactor", 0.0);
            pbr.put("roughnessFactor", 1.0);
            JSONObject mat = new JSONObject();
            mat.put("name", name);
            mat.put("doubleSided", true);
            mat.put("alphaMode", a < 0.99f ? "BLEND" : "OPAQUE");
            mat.put("pbrMetallicRoughness", pbr);
            mat.put("emissiveFactor", new JSONArray().put(er).put(eg).put(eb));
            mat.put("extensions", new JSONObject().put("KHR_materials_unlit", new JSONObject()));
            materials.put(mat);
            return new MaterialRef(materials.length() - 1);
        }

        void disc(String name, float[] dir, float distance, float radius, int segments, int material, float phaseScaleX) {
            float[] center = scale(normalize(dir), distance);
            float[] right = normalize(cross(new float[] {0.0f, 1.0f, 0.0f}, center));
            if (length(right) < 0.001f) right = new float[] {1.0f, 0.0f, 0.0f};
            float[] up = normalize(cross(center, right));
            int start = vertexCount();
            addVertex(center);
            for (int i = 0; i <= segments; i++) {
                double a = i * Math.PI * 2.0 / segments;
                float x = (float) Math.cos(a) * radius * phaseScaleX;
                float y = (float) Math.sin(a) * radius;
                addVertex(add(center, add(scale(right, x), scale(up, y))));
            }
            int indexStart = indices.size();
            for (int i = 1; i <= segments; i++) {
                indices.add(start);
                indices.add(start + i);
                indices.add(start + i + 1);
            }
            addMesh(name, material, indexStart, segments * 3);
        }

        void stars(int count, float distance, float size, float intensity, int material) {
            Random random = new Random(5501L);
            for (int i = 0; i < count; i++) {
                float az = random.nextFloat() * 360.0f;
                float el = 18.0f + random.nextFloat() * 62.0f;
                float s = size * (0.5f + random.nextFloat() * 1.2f) * intensity;
                quad("star_" + i, skyDir(az, el), distance, s, s, material, 0.0f);
            }
        }

        void clouds(int count, float distance, float density, int material) {
            for (int i = 0; i < count; i++) {
                float az = 20.0f + i * (320.0f / Math.max(1, count));
                float el = 16.0f + (i % 3) * 4.0f;
                float sx = 0.95f + density * 1.4f + (i % 2) * 0.4f;
                float sy = 0.22f + density * 0.28f;
                quad("cloud_" + i, skyDir(az, el), distance, sx, sy, material, i * 11.0f);
            }
        }

        void rain(int count, float intensity, int material) {
            Random random = new Random(8181L);
            for (int i = 0; i < count; i++) {
                float x = -3.6f + random.nextFloat() * 7.2f;
                float y = -0.7f + random.nextFloat() * 4.8f;
                float z = -2.5f + random.nextFloat() * 3.2f;
                float len = 0.20f + intensity * 0.42f;
                quadWorld("rain_" + i, new float[] {x, y, z}, 0.012f, len, material, -14.0f);
            }
        }

        void snow(int count, float intensity, int material) {
            Random random = new Random(7171L);
            for (int i = 0; i < count; i++) {
                float x = -3.8f + random.nextFloat() * 7.6f;
                float y = -0.8f + random.nextFloat() * 4.6f;
                float z = -2.6f + random.nextFloat() * 3.6f;
                float s = 0.035f + intensity * 0.045f;
                quadWorld("snow_" + i, new float[] {x, y, z}, s, s, material, random.nextFloat() * 180.0f);
            }
        }

        void quad(String name, float[] dir, float distance, float sx, float sy, int material, float rotationDeg) {
            float[] center = scale(normalize(dir), distance);
            float[] right = normalize(cross(new float[] {0.0f, 1.0f, 0.0f}, center));
            if (length(right) < 0.001f) right = new float[] {1.0f, 0.0f, 0.0f};
            float[] up = normalize(cross(center, right));
            addQuad(name, center, right, up, sx, sy, material, rotationDeg);
        }

        void quadWorld(String name, float[] center, float sx, float sy, int material, float rotationDeg) {
            addQuad(name, center, new float[] {1.0f, 0.0f, 0.0f}, new float[] {0.0f, 1.0f, 0.0f}, sx, sy, material, rotationDeg);
        }

        void addQuad(String name, float[] center, float[] right, float[] up, float sx, float sy, int material, float rotationDeg) {
            double r = Math.toRadians(rotationDeg);
            float c = (float) Math.cos(r);
            float s = (float) Math.sin(r);
            float[] rr = add(scale(right, c), scale(up, s));
            float[] uu = add(scale(right, -s), scale(up, c));
            int start = vertexCount();
            addVertex(add(center, add(scale(rr, -sx), scale(uu, -sy))));
            addVertex(add(center, add(scale(rr, sx), scale(uu, -sy))));
            addVertex(add(center, add(scale(rr, sx), scale(uu, sy))));
            addVertex(add(center, add(scale(rr, -sx), scale(uu, sy))));
            int indexStart = indices.size();
            indices.add(start); indices.add(start + 1); indices.add(start + 2);
            indices.add(start); indices.add(start + 2); indices.add(start + 3);
            addMesh(name, material, indexStart, 6);
        }

        byte[] build() throws Exception {
            ByteBuffer bin = ByteBuffer.allocate(vertexCount() * 12 + indices.size() * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (float v : positions) bin.putFloat(v);
            int indexOffset = bin.position();
            for (int index : indices) bin.putShort((short) index);
            int binLength = align4(bin.position());
            while (bin.position() < binLength) bin.put((byte) 0);
            byte[] binary = new byte[binLength];
            bin.rewind();
            bin.get(binary);

            JSONObject root = new JSONObject();
            root.put("asset", new JSONObject().put("version", "2.0").put("generator", "SOLUM P55B runtime sky weather"));
            root.put("extensionsUsed", new JSONArray().put("KHR_materials_unlit"));
            root.put("scene", 0);
            JSONArray scenes = new JSONArray();
            JSONArray nodes = new JSONArray();
            JSONArray meshesJson = new JSONArray();
            JSONArray accessors = new JSONArray();
            JSONArray bufferViews = new JSONArray();
            bufferViews.put(new JSONObject().put("buffer", 0).put("byteOffset", 0).put("byteLength", vertexCount() * 12).put("target", 34962));
            bufferViews.put(new JSONObject().put("buffer", 0).put("byteOffset", indexOffset).put("byteLength", indices.size() * 2).put("target", 34963));
            int positionAccessor = accessors.length();
            accessors.put(new JSONObject()
                .put("bufferView", 0).put("componentType", 5126).put("count", vertexCount()).put("type", "VEC3")
                .put("min", minArray()).put("max", maxArray()));
            for (MeshRef mesh : meshes) {
                int indexAccessor = accessors.length();
                accessors.put(new JSONObject()
                    .put("bufferView", 1)
                    .put("byteOffset", mesh.indexStart * 2)
                    .put("componentType", 5123)
                    .put("count", mesh.indexCount)
                    .put("type", "SCALAR"));
                JSONObject primitive = new JSONObject()
                    .put("attributes", new JSONObject().put("POSITION", positionAccessor))
                    .put("indices", indexAccessor)
                    .put("material", mesh.material)
                    .put("mode", 4);
                meshesJson.put(new JSONObject().put("name", mesh.name).put("primitives", new JSONArray().put(primitive)));
                int nodeIndex = nodes.length();
                nodes.put(new JSONObject().put("name", mesh.name).put("mesh", meshesJson.length() - 1));
                mesh.nodeIndex = nodeIndex;
            }
            JSONArray sceneNodes = new JSONArray();
            for (MeshRef mesh : meshes) sceneNodes.put(mesh.nodeIndex);
            scenes.put(new JSONObject().put("nodes", sceneNodes));
            root.put("scenes", scenes);
            root.put("nodes", nodes);
            root.put("meshes", meshesJson);
            root.put("materials", materials);
            root.put("buffers", new JSONArray().put(new JSONObject().put("byteLength", binary.length)));
            root.put("bufferViews", bufferViews);
            root.put("accessors", accessors);

            byte[] json = root.toString().getBytes(StandardCharsets.UTF_8);
            int jsonLength = align4(json.length);
            int total = 12 + 8 + jsonLength + 8 + binary.length;
            ByteBuffer glb = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
            glb.putInt(0x46546C67);
            glb.putInt(2);
            glb.putInt(total);
            glb.putInt(jsonLength);
            glb.putInt(0x4E4F534A);
            glb.put(json);
            while (glb.position() < 20 + jsonLength) glb.put((byte) 0x20);
            glb.putInt(binary.length);
            glb.putInt(0x004E4942);
            glb.put(binary);
            return glb.array();
        }

        private int vertexCount() { return positions.size() / 3; }
        private void addVertex(float[] p) {
            positions.add(p[0]); positions.add(p[1]); positions.add(p[2]);
        }
        private void addMesh(String name, int material, int indexStart, int indexCount) {
            meshes.add(new MeshRef(name, material, indexStart, indexCount));
        }
        private JSONArray minArray() throws Exception {
            float[] m = bounds(true);
            return new JSONArray().put(m[0]).put(m[1]).put(m[2]);
        }
        private JSONArray maxArray() throws Exception {
            float[] m = bounds(false);
            return new JSONArray().put(m[0]).put(m[1]).put(m[2]);
        }
        private float[] bounds(boolean min) {
            float x = min ? Float.MAX_VALUE : -Float.MAX_VALUE;
            float y = x;
            float z = x;
            for (int i = 0; i < positions.size(); i += 3) {
                x = min ? Math.min(x, positions.get(i)) : Math.max(x, positions.get(i));
                y = min ? Math.min(y, positions.get(i + 1)) : Math.max(y, positions.get(i + 1));
                z = min ? Math.min(z, positions.get(i + 2)) : Math.max(z, positions.get(i + 2));
            }
            return new float[] {x, y, z};
        }
        private static int align4(int value) { return (value + 3) & ~3; }
    }

    private static final class MeshRef {
        final String name;
        final int material;
        final int indexStart;
        final int indexCount;
        int nodeIndex;
        MeshRef(String name, int material, int indexStart, int indexCount) {
            this.name = name;
            this.material = material;
            this.indexStart = indexStart;
            this.indexCount = indexCount;
        }
    }

    private static float[] add(float[] a, float[] b) {
        return new float[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static float[] scale(float[] a, float s) {
        return new float[] {a[0] * s, a[1] * s, a[2] * s};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static float length(float[] a) {
        return (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
    }

    private static float[] normalize(float[] a) {
        float l = Math.max(0.0001f, length(a));
        return new float[] {a[0] / l, a[1] / l, a[2] / l};
    }
}
