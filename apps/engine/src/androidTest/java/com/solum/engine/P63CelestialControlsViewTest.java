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
                result.putString("P63_VIEW_TEST", "PASS P63.10 exact UDS Sun controls; ten tabs; integrated Weather; circular color picker; Activity recreation restores shared state");
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
        View quickContent = tagged(activity, "p63.tab.content.quick", View.class);
        View atmosphereContent = tagged(activity, "p63.tab.content.atmosphere", View.class);
        View sunContent = tagged(activity, "p63.tab.content.sun", View.class);
        View moonContent = tagged(activity, "p63.tab.content.moon", View.class);
        View starsContent = tagged(activity, "p63.tab.content.stars", View.class);
        View cloudsContent = tagged(activity, "p63.tab.content.clouds", View.class);
        View weatherContent = tagged(activity, "p63.tab.content.weather", View.class);
        View cameraContent = tagged(activity, "p63.tab.content.camera", View.class);
        View postFxContent = tagged(activity, "p63.tab.content.postfx", View.class);
        View debugContent = tagged(activity, "p63.tab.content.debug", View.class);
        require(quickContent.getVisibility() == View.VISIBLE, "Quick must be the default inner tab");
        require(atmosphereContent.getVisibility() == View.GONE && sunContent.getVisibility() == View.GONE
            && moonContent.getVisibility() == View.GONE && starsContent.getVisibility() == View.GONE
            && cloudsContent.getVisibility() == View.GONE && weatherContent.getVisibility() == View.GONE
            && cameraContent.getVisibility() == View.GONE
            && postFxContent.getVisibility() == View.GONE
            && debugContent.getVisibility() == View.GONE, "only Quick may be visible by default");
        Button sunTab = tagged(activity, "p63.tab.sun", Button.class);
        runOnMainSync(sunTab::performClick);
        waitForIdleSync();
        require(sunContent.getVisibility() == View.VISIBLE && quickContent.getVisibility() == View.GONE,
            "tabs switch correctly");
        tagged(activity, "p63.quick.focus_sun", Button.class);
        tagged(activity, "p63.quick.focus_moon", Button.class);
        tagged(activity, "p63.camera.overview", Button.class);
        tagged(activity, "p63.camera.materials", Button.class);
        tagged(activity, "p63.camera.horizon", Button.class);
        tagged(activity, "p63.camera.under_roof", Button.class);
        tagged(activity, "p63.camera.zoom_in", Button.class);
        tagged(activity, "p63.camera.zoom_out", Button.class);
        Button cameraTab = tagged(activity, "p63.tab.camera", Button.class);
        Button zoomIn = tagged(activity, "p63.camera.zoom_in", Button.class);
        Button zoomOut = tagged(activity, "p63.camera.zoom_out", Button.class);
        runOnMainSync(cameraTab::performClick);
        waitForIdleSync();
        float initialDistance = ((FilamentGlbPreviewActivity)activity).getP63CameraDistanceForTest();
        runOnMainSync(zoomOut::performClick);
        waitForIdleSync();
        float zoomedOutDistance = ((FilamentGlbPreviewActivity)activity).getP63CameraDistanceForTest();
        require(zoomedOutDistance > initialDistance, "Zoom - button did not increase camera distance");
        runOnMainSync(zoomIn::performClick);
        waitForIdleSync();
        require(((FilamentGlbPreviewActivity)activity).getP63CameraDistanceForTest() < zoomedOutDistance,
            "Zoom + button did not decrease camera distance");
        runOnMainSync(sunTab::performClick);
        waitForIdleSync();
        SeekBar slider = tagged(activity, "p63.slider.uds_sun_light_intensity_·_lux", SeekBar.class);
        EditText exact = tagged(activity, "p63.numeric.uds_sun_light_intensity_·_lux", EditText.class);
        Button apply = tagged(activity, "p63.numeric.apply.uds_sun_light_intensity_·_lux", Button.class);
        runOnMainSync(() -> { exact.setText("7.5"); apply.performClick(); });
        waitForIdleSync();
        int exactProgress = slider.getProgress();
        require(exactProgress > 0 && exactProgress < slider.getMax(),
            "numeric Apply did not synchronize exact UDS Sun Light slider thumb");

        Button udsNoon = tagged(activity, "p63.sun_preset.uds_noon", Button.class);
        runOnMainSync(udsNoon::performClick);
        waitForIdleSync();
        require(slider.getProgress() < exactProgress,
            "UDS Noon preset did not restore the extracted 5 lux default");

        Button reset = tagged(activity, "p63.sun_reset", Button.class);
        runOnMainSync(reset::performClick);
        waitForIdleSync();
        require(slider.getProgress() < exactProgress,
            "Sun reset did not restore the extracted UDS light default");

        SeekBar discSlider = tagged(activity, "p63.slider.uds_sun_disk_intensity", SeekBar.class);
        EditText discExact = tagged(activity, "p63.numeric.uds_sun_disk_intensity", EditText.class);
        Button discApply = tagged(activity, "p63.numeric.apply.uds_sun_disk_intensity", Button.class);
        runOnMainSync(() -> { discExact.setText("7.5"); discApply.performClick(); });
        waitForIdleSync();
        int discProgress = discSlider.getProgress();
        require(discProgress > 0 && discProgress < discSlider.getMax()
                && discExact.getText().toString().startsWith("7.5"),
            "exact UDS Sun Disk Intensity did not reach state and slider");
        runOnMainSync(() -> { discExact.setText("Infinity"); discApply.performClick(); });
        waitForIdleSync();
        require(discExact.getText().toString().startsWith("7.5")
                && discSlider.getProgress() == discProgress,
            "non-finite UDS Sun Disk Intensity did not keep the prior finite value");

        Button color = tagged(activity, "p63.color.open.sun", Button.class);
        runOnMainSync(color::performClick);
        waitForIdleSync();
        P63HsvColorPickerDialog dialog = ((FilamentGlbPreviewActivity)activity).getP63ColorPickerDialogForTest();
        require(dialog != null && dialog.isShowing(), "Sun color picker did not open");
        runOnMainSync(() -> { dialog.setHueForTest(180.0f); dialog.setSaturationValueForTest(1.0f, 1.0f); dialog.applyDraftForTest(); });
        waitForIdleSync();
        require(color.getText().toString().contains("#00FFFF"), "color picker Apply did not update Sun state/preview button");
        runOnMainSync(color::performClick);
        waitForIdleSync();
        P63HsvColorPickerDialog resetDialog = ((FilamentGlbPreviewActivity)activity).getP63ColorPickerDialogForTest();
        runOnMainSync(() -> { resetDialog.setHueForTest(300.0f); resetDialog.setSaturationValueForTest(1.0f, 1.0f); resetDialog.resetDraftForTest(); resetDialog.applyDraftForTest(); });
        waitForIdleSync();
        require(color.getText().toString().contains("#FFFFFF"),
            "color picker Reset did not restore the Sun default");

        Button cloudsTab = tagged(activity, "p63.tab.clouds", Button.class);
        runOnMainSync(cloudsTab::performClick);
        waitForIdleSync();
        Button cloudyPreset = tagged(activity, "p63.cloud_preset.cloudy", Button.class);
        SeekBar cloudCoverage = tagged(activity, "p63.slider.cloud_coverage", SeekBar.class);
        runOnMainSync(cloudyPreset::performClick);
        waitForIdleSync();
        require(cloudCoverage.getProgress() == 82, "Cloudy preset did not synchronize coverage");
        Button mediumQuality = tagged(activity, "p63.cloud_quality.medium", Button.class);
        runOnMainSync(mediumQuality::performClick);
        waitForIdleSync();

        Button weatherTab = tagged(activity, "p63.tab.weather", Button.class);
        runOnMainSync(weatherTab::performClick);
        waitForIdleSync();
        require(weatherContent.getVisibility() == View.VISIBLE,
            "Weather tab did not expose integrated controls");
        tagged(activity, "p63.weather.preset.clear_skies", Button.class);
        tagged(activity, "p63.weather.preset.rain_thunderstorm", Button.class);
        tagged(activity, "p63.weather.trigger_lightning", Button.class);
        tagged(activity, "p63.slider.weather_transition_seconds", SeekBar.class);
        tagged(activity, "p63.slider.rain", SeekBar.class);

        Button atmosphereTab = tagged(activity, "p63.tab.atmosphere", Button.class);
        runOnMainSync(atmosphereTab::performClick);
        waitForIdleSync();
        require(atmosphereContent.getVisibility() == View.VISIBLE,
            "Atmosphere tab did not expose analytic scattering controls");
        tagged(activity, "p63.slider.turbidity", SeekBar.class);

        Button starsTab = tagged(activity, "p63.tab.stars", Button.class);
        runOnMainSync(starsTab::performClick);
        waitForIdleSync();
        Button starColor = tagged(activity, "p63.color.open.stars", Button.class);
        runOnMainSync(starColor::performClick);
        waitForIdleSync();
        P63HsvColorPickerDialog starDialog = ((FilamentGlbPreviewActivity)activity).getP63ColorPickerDialogForTest();
        require(starDialog != null && starDialog.isShowing(), "Star color wheel did not open");
        runOnMainSync(() -> { starDialog.setHueForTest(120.0f); starDialog.setSaturationValueForTest(1.0f, 1.0f); starDialog.applyDraftForTest(); });
        waitForIdleSync();
        require(starColor.getText().toString().contains("#00FF00"), "Star color wheel did not update state");

        runOnMainSync(() -> { exact.setText("6.5"); apply.performClick(); });
        waitForIdleSync();
        int persistedProgress = slider.getProgress();
        runOnMainSync(activity::finish);
        waitForIdleSync();
        Activity recreated = startActivitySync(intent);
        waitForIdleSync();
        SeekBar restored = tagged(recreated, "p63.slider.uds_sun_light_intensity_·_lux", SeekBar.class);
        require(restored.getProgress() == persistedProgress,
            "Activity recreation did not restore exact UDS Sun state/thumb");
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
