package com.solum.engine;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Deprecated legacy entry point kept only so old explicit intents do not crash.
 * Normal app launch is FilamentGlbPreviewActivity from AndroidManifest.
 */
@Deprecated
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView status = new TextView(this);
        status.setText("Legacy Vulkan renderer: removed_from_normal_flow / deprecated / build_required_only");
        status.setTextSize(14.0f);
        status.setPadding(24, 24, 24, 24);
        setContentView(status);
        Intent intent = new Intent(this, FilamentGlbPreviewActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
