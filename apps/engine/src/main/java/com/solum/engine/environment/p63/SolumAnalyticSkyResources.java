package com.solum.engine.environment.p63;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.android.filament.Engine;
import com.google.android.filament.Texture;
import com.google.android.filament.TextureSampler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Loads the immutable P63.3 Moon and star textures once for one sky material instance. */
public final class SolumAnalyticSkyResources {
    private static final String[] MOON_ALBEDO_PATHS = {
        "private_premium/p63_3/sky/moon_color.png",
        "private_premium/p63_2a/celestial/Moon_Color.png"
    };
    private static final String[] MOON_NORMAL_PATHS = {
        "private_premium/p63_3/sky/moon_normal.png",
        "private_premium/p63_2a/celestial/Moon_PhaseNormal.png"
    };
    private static final String[] REAL_STAR_PATHS = {
        "private_premium/p63_3/sky/real_stars.png",
        "private_premium/uds_sky/Real_Stars.png"
    };
    private static final String[] TILING_STAR_PATHS = {
        "private_premium/p63_3/sky/tiling_stars.png",
        "private_premium/uds_sky/Tiling_Stars.png"
    };

    private final Engine engine;
    public final TextureSampler moonSampler = new TextureSampler(
        TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.CLAMP_TO_EDGE);
    public final TextureSampler starSampler = new TextureSampler(
        TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
        TextureSampler.MagFilter.LINEAR,
        TextureSampler.WrapMode.REPEAT);

    public Texture moonAlbedo;
    public Texture moonNormal;
    public Texture realStars;
    public Texture tilingStars;
    public String moonSource = "SOLUM_NATIVE_FLAT_FALLBACK";
    public String starSource = "SOLUM_NATIVE_PROCEDURAL";
    public float starTextureAvailable;

    public SolumAnalyticSkyResources(AssetManager assets, Engine engine) {
        if (assets == null || engine == null) throw new IllegalArgumentException("analytic_sky_resource_dependency_missing");
        this.engine = engine;
        LoadedTexture moonColorLoaded = loadFirst(assets, MOON_ALBEDO_PATHS, true, 186, 186, 186, 255);
        LoadedTexture moonNormalLoaded = loadFirst(assets, MOON_NORMAL_PATHS, false, 128, 128, 255, 255);
        LoadedTexture realStarsLoaded = loadFirst(assets, REAL_STAR_PATHS, true, 0, 0, 0, 255);
        LoadedTexture tilingStarsLoaded = loadFirst(assets, TILING_STAR_PATHS, true, 0, 0, 0, 255);
        moonAlbedo = moonColorLoaded.texture;
        moonNormal = moonNormalLoaded.texture;
        realStars = realStarsLoaded.texture;
        tilingStars = tilingStarsLoaded.texture;
        if (moonColorLoaded.path != null && moonNormalLoaded.path != null) {
            moonSource = "UDS_VERIFIED moon_color+moon_normal";
        }
        if (realStarsLoaded.path != null && tilingStarsLoaded.path != null) {
            starSource = "UDS_VERIFIED Real_Stars+Tiling_Stars";
            starTextureAvailable = 1.0f;
        }
    }

    public void release() {
        destroy(moonAlbedo); moonAlbedo = null;
        destroy(moonNormal); moonNormal = null;
        destroy(realStars); realStars = null;
        destroy(tilingStars); tilingStars = null;
    }

    private LoadedTexture loadFirst(AssetManager assets, String[] paths, boolean srgb,
                                    int fallbackR, int fallbackG, int fallbackB, int fallbackA) {
        for (String path : paths) {
            try (InputStream stream = assets.open(path, AssetManager.ACCESS_STREAMING)) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap == null) continue;
                try {
                    return new LoadedTexture(upload(bitmap, srgb), path);
                } finally {
                    bitmap.recycle();
                }
            } catch (IOException ignored) {
                // The private payload is optional. The deterministic fallback below is intentional.
            }
        }
        ByteBuffer pixel = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        pixel.put((byte)fallbackR).put((byte)fallbackG).put((byte)fallbackB).put((byte)fallbackA).flip();
        Texture texture = new Texture.Builder().width(1).height(1).levels(1)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(srgb ? Texture.InternalFormat.SRGB8_A8 : Texture.InternalFormat.RGBA8)
            .build(engine);
        texture.setImage(engine, 0, new Texture.PixelBufferDescriptor(pixel, Texture.Format.RGBA, Texture.Type.UBYTE, 1));
        return new LoadedTexture(texture, null);
    }

    private Texture upload(Bitmap bitmap, boolean srgb) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= 0 || height <= 0 || width > 4096 || height > 4096) {
            throw new IllegalArgumentException("analytic_sky_texture_dimensions_invalid_" + width + "x" + height);
        }
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        ByteBuffer rgba = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        for (int argb : pixels) {
            rgba.put((byte)((argb >> 16) & 0xff));
            rgba.put((byte)((argb >> 8) & 0xff));
            rgba.put((byte)(argb & 0xff));
            rgba.put((byte)((argb >>> 24) & 0xff));
        }
        rgba.flip();
        int levels = 1 + (int)Math.floor(Math.log(Math.max(width, height)) / Math.log(2.0));
        Texture texture = new Texture.Builder().width(width).height(height).levels(levels)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(srgb ? Texture.InternalFormat.SRGB8_A8 : Texture.InternalFormat.RGBA8)
            .build(engine);
        texture.setImage(engine, 0, new Texture.PixelBufferDescriptor(rgba, Texture.Format.RGBA, Texture.Type.UBYTE, 1));
        if (levels > 1) texture.generateMipmaps(engine);
        return texture;
    }

    private void destroy(Texture texture) {
        if (texture == null) return;
        try { engine.destroyTexture(texture); } catch (Throwable ignored) { }
    }

    private static final class LoadedTexture {
        final Texture texture;
        final String path;
        LoadedTexture(Texture texture, String path) { this.texture = texture; this.path = path; }
    }
}
