package com.solum.engine;

import android.app.Activity;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    static {
        System.loadLibrary("solum_engine");
    }

    private long nativeHandle = 0L;
    private TextView statusView;

    private static native long nativeCreate();
    private static native void nativeDestroy(long handle);
    private static native void nativeSurfaceCreated(long handle, Surface surface, String outputRoot);
    private static native void nativeSurfaceChanged(long handle, Surface surface, int width, int height);
    private static native void nativeSurfaceDestroyed(long handle);
    private static native String nativeGetStatus(long handle);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nativeHandle = nativeCreate();

        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(this);

        statusView = new TextView(this);
        statusView.setTextColor(Color.rgb(210, 245, 255));
        statusView.setTextSize(12f);
        statusView.setPadding(18, 18, 18, 18);
        statusView.setBackgroundColor(Color.argb(160, 3, 10, 12));
        statusView.setText("SOLUM Engine\nWaiting for Vulkan surface...");

        FrameLayout root = new FrameLayout(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(statusView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        setContentView(root);
    }

    @Override
    protected void onDestroy() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0L;
        }
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (nativeHandle != 0L) {
            nativeSurfaceCreated(nativeHandle, holder.getSurface(), getSolumRoot());
            updateStatus();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (nativeHandle != 0L) {
            nativeSurfaceChanged(nativeHandle, holder.getSurface(), width, height);
            updateStatus();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (nativeHandle != 0L) {
            nativeSurfaceDestroyed(nativeHandle);
            updateStatus();
        }
    }

    private void updateStatus() {
        runOnUiThread(() -> {
            if (nativeHandle != 0L) {
                statusView.setText(nativeGetStatus(nativeHandle));
            }
        });
    }

    private String getSolumRoot() {
        return "/storage/emulated/0/SOLUMCreative";
    }
}
