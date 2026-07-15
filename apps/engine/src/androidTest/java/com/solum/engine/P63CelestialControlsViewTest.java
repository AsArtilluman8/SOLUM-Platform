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
                result.putString("P63_VIEW_TEST", "PASS slider numeric preset reset; Activity recreation restores shared state");
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
