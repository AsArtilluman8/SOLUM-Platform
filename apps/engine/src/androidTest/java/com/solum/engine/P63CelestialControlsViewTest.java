package com.solum.engine;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;

/** Self-contained real Android View instrumentation; no legacy android.test or external dependency. */
public final class P63CelestialControlsViewTest extends Instrumentation {
    @Override
    public void onStart() {
        new Thread(() -> {
            Bundle result = new Bundle();
            try {
                runViewRegression();
                result.putString("P63_VIEW_TEST", "PASS slider numeric preset reset; accordion defaults and toggle; Activity recreation restores shared state");
                finish(Activity.RESULT_OK, result);
            } catch (Throwable error) {
                result.putString("P63_VIEW_TEST", "FAIL " + error.getClass().getSimpleName() + " " + error.getMessage());
                finish(Activity.RESULT_CANCELED, result);
            }
        }, "p63-celestial-view-test").start();
    }

    private void runViewRegression() {
        Intent intent = new Intent();
        intent.setClassName(getTargetContext().getPackageName(), FilamentGlbPreviewActivity.class.getName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Activity activity = startActivitySync(intent);
        waitForIdleSync();
        View timeContent = tagged(activity, "p63.accordion.content.time", View.class);
        View cameraContent = tagged(activity, "p63.accordion.content.camera", View.class);
        View skyContent = tagged(activity, "p63.accordion.content.sky", View.class);
        View sunContent = tagged(activity, "p63.accordion.content.sun", View.class);
        View moonContent = tagged(activity, "p63.accordion.content.moon", View.class);
        View postContent = tagged(activity, "p63.accordion.content.post_process", View.class);
        View audioContent = tagged(activity, "p63.accordion.content.audio", View.class);
        View debugContent = tagged(activity, "p63.accordion.content.debug", View.class);
        require(timeContent.getVisibility() == View.VISIBLE && cameraContent.getVisibility() == View.VISIBLE,
            "Time and Camera must be open by default");
        require(skyContent.getVisibility() == View.GONE && sunContent.getVisibility() == View.GONE
            && moonContent.getVisibility() == View.GONE && postContent.getVisibility() == View.GONE
            && audioContent.getVisibility() == View.GONE && debugContent.getVisibility() == View.GONE,
            "only Time and Camera may be open by default");
        Button skyHeader = tagged(activity, "p63.accordion.sky", Button.class);
        runOnMainSync(skyHeader::performClick);
        waitForIdleSync();
        require(skyContent.getVisibility() == View.VISIBLE, "accordion defaults and toggle failed");
        tagged(activity, "p63.quick.focus_sun", Button.class);
        tagged(activity, "p63.quick.focus_moon", Button.class);
        tagged(activity, "p63.camera.overview", Button.class);
        tagged(activity, "p63.camera.materials", Button.class);
        tagged(activity, "p63.camera.horizon", Button.class);
        tagged(activity, "p63.camera.under_roof", Button.class);
        tagged(activity, "p63.camera.zoom_in", Button.class);
        tagged(activity, "p63.camera.zoom_out", Button.class);
        SeekBar slider = tagged(activity, "p63.slider.sun_light_intensity", SeekBar.class);
        EditText exact = tagged(activity, "p63.numeric.sun_light_intensity", EditText.class);
        Button apply = tagged(activity, "p63.numeric.apply.sun_light_intensity", Button.class);
        runOnMainSync(() -> { exact.setText("27.5"); apply.performClick(); });
        waitForIdleSync();
        require(slider.getProgress() == 55, "numeric Apply did not synchronize SeekBar thumb");

        Button bright = tagged(activity, "p63.sun_preset.bright", Button.class);
        runOnMainSync(bright::performClick);
        waitForIdleSync();
        require(slider.getProgress() == 70, "preset did not synchronize thumb");

        Button reset = tagged(activity, "p63.sun_reset", Button.class);
        runOnMainSync(reset::performClick);
        waitForIdleSync();
        require(slider.getProgress() == 36, "reset did not synchronize thumb");

        runOnMainSync(() -> { exact.setText("24.5"); apply.performClick(); activity.finish(); });
        waitForIdleSync();
        Activity recreated = startActivitySync(intent);
        waitForIdleSync();
        SeekBar restored = tagged(recreated, "p63.slider.sun_light_intensity", SeekBar.class);
        require(restored.getProgress() == 49, "Activity recreation did not restore state/thumb");
        runOnMainSync(recreated::finish);
    }

    private static <T extends View> T tagged(Activity activity, String tag, Class<T> type) {
        View value = activity.getWindow().getDecorView().findViewWithTag(tag);
        require(value != null, "missing tagged View " + tag);
        return type.cast(value);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
