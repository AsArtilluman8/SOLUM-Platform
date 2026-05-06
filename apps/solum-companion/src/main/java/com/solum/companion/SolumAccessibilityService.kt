package com.solum.companion

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class SolumAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (!isPackageAllowed(packageName)) {
            return
        }

        // P01G is skeleton only. Real SOLUM device actions will be implemented in P01H.
    }

    override fun onInterrupt() {
        // P01G has no long-running action to interrupt.
    }

    fun isPackageAllowed(packageName: String): Boolean {
        return packageName in SOLUM_ALLOWED_PACKAGES
    }

    fun captureScreenshotStub() {
        // Real screenshot capture will be implemented in P01H for allowlisted SOLUM packages only.
    }

    fun dumpUiTreeStub() {
        // Real AccessibilityNode tree dump will be implemented in P01H for allowlisted SOLUM packages only.
    }

    fun writeActionLogStub() {
        // Real action log writing will be implemented in P01H.
    }

    fun launchSolumStub() {
        // Real controlled SOLUM launch will be implemented in P01H.
    }

    fun forceStopSolumStub() {
        // Real controlled SOLUM force-stop will be implemented in P01H.
    }

    companion object {
        val SOLUM_ALLOWED_PACKAGES = setOf(
            "com.solum.engine",
            "com.solum.launcher",
            "com.solum.assethub",
            "com.solum.materialstudio",
            "com.asart.solum",
        )
    }
}
