package com.solum.engine;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Compact HSV picker shared by the Sun and Moon tabs. */
public final class P63HsvColorPickerDialog extends Dialog {
    public interface Listener { void onColorApplied(float red, float green, float blue); }

    private final Activity activity;
    private final String key;
    private final Listener listener;
    private final float[] hsv = new float[3];
    private final float[] resetHsv = new float[3];
    private HueBarView hueView;
    private SaturationValueView saturationValueView;
    private TextView preview;

    public P63HsvColorPickerDialog(Activity activity, String key, float[] initialRgb,
                                   float[] resetRgb, Listener listener) {
        super(activity);
        this.activity = activity;
        this.key = key;
        this.listener = listener;
        Color.colorToHSV(rgbColor(initialRgb), hsv);
        Color.colorToHSV(rgbColor(resetRgb), resetHsv);
    }

    public static P63HsvColorPickerDialog show(Activity activity, String key, float[] initialRgb,
                                                float[] resetRgb, Listener listener) {
        P63HsvColorPickerDialog dialog = new P63HsvColorPickerDialog(activity, key, initialRgb, resetRgb, listener);
        dialog.show();
        return dialog;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(14), dp(16), dp(12));
        root.setBackgroundColor(Color.rgb(8, 24, 30));

        TextView title = text(("sun".equals(key) ? "Sun" : "Moon") + " Color · HSV");
        title.setTextSize(18.0f);
        root.addView(title);

        saturationValueView = new SaturationValueView(activity);
        saturationValueView.setTag("p63.color.sv." + key);
        saturationValueView.setHue(hsv[0]);
        saturationValueView.setSaturationValue(hsv[1], hsv[2]);
        saturationValueView.setOnChanged((saturation, value) -> {
            hsv[1] = saturation; hsv[2] = value; updatePreview();
        });
        root.addView(saturationValueView, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(220)));

        hueView = new HueBarView(activity);
        hueView.setTag("p63.color.hue." + key);
        hueView.setHue(hsv[0]);
        hueView.setOnChanged(hue -> {
            hsv[0] = hue; saturationValueView.setHue(hue); updatePreview();
        });
        LinearLayout.LayoutParams hueParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
        hueParams.topMargin = dp(10);
        root.addView(hueView, hueParams);

        preview = text("");
        preview.setTag("p63.color.preview." + key);
        preview.setGravity(android.view.Gravity.CENTER);
        preview.setPadding(dp(8), dp(10), dp(8), dp(10));
        root.addView(preview);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button apply = action("Apply", "p63.color.apply." + key, view -> applyDraft());
        Button reset = action("Reset", "p63.color.reset." + key, view -> resetDraft());
        Button cancel = action("Cancel", "p63.color.cancel." + key, view -> cancel());
        buttons.addView(apply, weighted()); buttons.addView(reset, weighted()); buttons.addView(cancel, weighted());
        root.addView(buttons);
        setContentView(root);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(params);
        }
        updatePreview();
    }

    public void setHueForTest(float hue) {
        hsv[0] = clamp(hue, 0.0f, 360.0f);
        if (hueView != null) hueView.setHue(hsv[0]);
        if (saturationValueView != null) saturationValueView.setHue(hsv[0]);
        updatePreview();
    }

    public void setSaturationValueForTest(float saturation, float value) {
        hsv[1] = clamp(saturation, 0.0f, 1.0f); hsv[2] = clamp(value, 0.0f, 1.0f);
        if (saturationValueView != null) saturationValueView.setSaturationValue(hsv[1], hsv[2]);
        updatePreview();
    }

    public void applyDraftForTest() { applyDraft(); }
    public void resetDraftForTest() { resetDraft(); }
    public int getDraftColorForTest() { return Color.HSVToColor(hsv); }

    private void applyDraft() {
        int color = Color.HSVToColor(hsv);
        if (listener != null) listener.onColorApplied(Color.red(color) / 255.0f,
            Color.green(color) / 255.0f, Color.blue(color) / 255.0f);
        dismiss();
    }

    private void resetDraft() {
        System.arraycopy(resetHsv, 0, hsv, 0, 3);
        if (hueView != null) hueView.setHue(hsv[0]);
        if (saturationValueView != null) {
            saturationValueView.setHue(hsv[0]);
            saturationValueView.setSaturationValue(hsv[1], hsv[2]);
        }
        updatePreview();
    }

    private void updatePreview() {
        if (preview == null) return;
        int color = Color.HSVToColor(hsv);
        preview.setBackgroundColor(color);
        double luminance = Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114;
        preview.setTextColor(luminance > 150.0 ? Color.BLACK : Color.WHITE);
        preview.setText(String.format(java.util.Locale.US, "#%06X  H %.0f° · S %.2f · V %.2f",
            color & 0xFFFFFF, hsv[0], hsv[1], hsv[2]));
    }

    private Button action(String label, String tag, View.OnClickListener click) {
        Button button = new Button(activity);
        button.setText(label); button.setTag(tag); button.setOnClickListener(click);
        button.setTextColor(Color.WHITE); button.setBackgroundColor(Color.rgb(18, 72, 80));
        return button;
    }

    private TextView text(String value) {
        TextView view = new TextView(activity); view.setText(value); view.setTextColor(Color.WHITE); return view;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }

    private static int rgbColor(float[] rgb) {
        return Color.rgb(Math.round(clamp(rgb[0], 0.0f, 1.0f) * 255.0f),
            Math.round(clamp(rgb[1], 0.0f, 1.0f) * 255.0f), Math.round(clamp(rgb[2], 0.0f, 1.0f) * 255.0f));
    }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private interface HueChanged { void onChanged(float hue); }
    private interface SaturationValueChanged { void onChanged(float saturation, float value); }

    private static final class HueBarView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float hue;
        private HueChanged listener;

        HueBarView(Activity context) { super(context); }
        void setHue(float value) { hue = value; invalidate(); }
        void setOnChanged(HueChanged value) { listener = value; }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int[] colors = {Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED};
            paint.setShader(new LinearGradient(0, 0, getWidth(), 0, colors, null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4.0f); paint.setColor(Color.WHITE);
            float x = hue / 360.0f * getWidth(); canvas.drawLine(x, 0, x, getHeight(), paint); paint.setStyle(Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN && event.getActionMasked() != MotionEvent.ACTION_MOVE) return true;
            hue = clamp(event.getX() / Math.max(1.0f, getWidth()) * 360.0f, 0.0f, 360.0f);
            if (listener != null) listener.onChanged(hue); invalidate(); return true;
        }
    }

    private static final class SaturationValueView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float hue;
        private float saturation = 1.0f;
        private float value = 1.0f;
        private SaturationValueChanged listener;

        SaturationValueView(Activity context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        void setHue(float value) { hue = value; invalidate(); }
        void setSaturationValue(float saturation, float value) { this.saturation = saturation; this.value = value; invalidate(); }
        void setOnChanged(SaturationValueChanged value) { listener = value; }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int hueColor = Color.HSVToColor(new float[] {hue, 1.0f, 1.0f});
            Shader saturationShader = new LinearGradient(0, 0, getWidth(), 0, Color.WHITE, hueColor, Shader.TileMode.CLAMP);
            Shader valueShader = new LinearGradient(0, 0, 0, getHeight(), Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP);
            paint.setShader(new ComposeShader(saturationShader, valueShader, PorterDuff.Mode.MULTIPLY));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4.0f); paint.setColor(Color.WHITE);
            canvas.drawCircle(saturation * getWidth(), (1.0f - value) * getHeight(), 10.0f, paint); paint.setStyle(Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN && event.getActionMasked() != MotionEvent.ACTION_MOVE) return true;
            saturation = clamp(event.getX() / Math.max(1.0f, getWidth()), 0.0f, 1.0f);
            value = 1.0f - clamp(event.getY() / Math.max(1.0f, getHeight()), 0.0f, 1.0f);
            if (listener != null) listener.onChanged(saturation, value); invalidate(); return true;
        }
    }
}
