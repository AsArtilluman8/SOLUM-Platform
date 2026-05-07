package com.solum.companion

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SolumAccessibilityService : AccessibilityService() {
    private var activePackageName: String? = null
    private var lastEventAt: String? = null
    private val actionLogEntries = ArrayDeque<String>()

    override fun onServiceConnected() {
        currentInstance = this
        appendActionLogEntry(
            command = SolumCompanionCommand.STATUS,
            status = "connected",
            reason = null,
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        activePackageName = packageName
        lastEventAt = SolumDeviceAgentState.timestampUtc()
        appendActionLogEntry(
            command = SolumCompanionCommand.STATUS,
            status = if (isPackageAllowed(packageName)) "observed" else "blocked",
            reason = if (isPackageAllowed(packageName)) null else "package_not_allowlisted",
        )
    }

    override fun onInterrupt() {
        appendActionLogEntry(
            command = SolumCompanionCommand.STATUS,
            status = "interrupted",
            reason = "accessibility_service_interrupted",
        )
    }

    override fun onDestroy() {
        if (currentInstance === this) {
            currentInstance = null
        }
        super.onDestroy()
    }

    fun isPackageAllowed(packageName: String): Boolean {
        return packageName in SOLUM_ALLOWED_PACKAGES
    }

    fun buildStatusJson(): String {
        val activePackage = activePackageInfo()
        val status = if (activePackage.isAllowlisted) "ready" else "blocked"
        val reason = if (activePackage.isAllowlisted) null else "package_not_allowlisted"
        return SolumDeviceAgentState.statusJson(
            status = status,
            command = SolumCompanionCommand.STATUS,
            activePackage = activePackage,
            reason = reason,
        )
    }

    fun runVisualDiagnostics(context: android.content.Context, treeUri: Uri?, requestingPackage: String): SolumDeviceAgentState.VisualDiagnosticsWriteResult {
        val activePackage = activePackageInfo(fallbackPackage = requestingPackage)
        if (activePackageName == null && activePackage.isAllowlisted) {
            activePackageName = requestingPackage
            lastEventAt = SolumDeviceAgentState.timestampUtc()
        }
        appendActionLogEntry(
            command = SolumCompanionCommand.RUN_VISUAL_DIAGNOSTICS,
            status = "started",
            reason = null,
        )

        if (!activePackage.isAllowlisted) {
            val result = writeVisualDiagnosticsFailure(
                context = context,
                treeUri = treeUri,
                status = "blocked",
                reason = "package_not_allowlisted",
                activePackage = activePackage,
            )
            writeActionLog(context, treeUri)
            return result
        }

        if (treeUri == null) {
            val result = writeVisualDiagnosticsFailure(
                context = context,
                treeUri = null,
                status = "failed",
                reason = "saf_not_configured",
                activePackage = activePackage,
            )
            writeActionLog(context, null)
            return result
        }

        dumpUiTree(context = context, treeUri = treeUri, fallbackPackage = requestingPackage)
        writeActionLog(context, treeUri)
        captureScreenshot(context = context, treeUri = treeUri, fallbackPackage = requestingPackage)
        return SolumDeviceAgentState.VisualDiagnosticsWriteResult(
            status = "partial",
            reason = "screenshot_capture_requested",
        )
    }

    fun captureScreenshot(context: android.content.Context? = null, treeUri: Uri? = null, fallbackPackage: String? = null) {
        val activePackage = activePackageInfo(fallbackPackage = fallbackPackage)
        if (!activePackage.isAllowlisted) {
            writeScreenshotFailure(context, treeUri, "blocked", "package_not_allowlisted", activePackage)
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            writeScreenshotFailure(context, treeUri, "failed", "screenshot_api_unavailable", activePackage)
            return
        }
        if (context == null || treeUri == null) {
            writeScreenshotFailure(context, treeUri, "failed", "saf_not_configured", activePackage)
            return
        }

        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    try {
                        val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace,
                        )
                        if (hardwareBitmap == null) {
                            writeScreenshotFailure(context, treeUri, "failed", "screenshot_failed", activePackage)
                            return
                        }
                        val bitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        val pngWrite = SolumDeviceAgentState.writePngViaSaf(
                            context = context,
                            treeUri = treeUri,
                            relativePath = SolumDeviceAgentState.SCREENSHOT_RELATIVE_PATH,
                            bitmap = bitmap,
                        )
                        if (!pngWrite.success) {
                            writeScreenshotFailure(context, treeUri, "failed", "screenshot_failed", activePackage)
                            return
                        }
                        writeVisualManifest(
                            context = context,
                            treeUri = treeUri,
                            status = "ok",
                            reason = null,
                            activePackage = activePackage,
                            finalPath = SolumDeviceAgentState.SCREENSHOT_RELATIVE_PATH,
                        )
                        appendActionLogEntry(
                            command = SolumCompanionCommand.CAPTURE_SCREENSHOT,
                            status = "ok",
                            reason = null,
                            outputPath = SolumDeviceAgentState.SCREENSHOT_PATH,
                        )
                    } catch (error: Exception) {
                        writeScreenshotFailure(context, treeUri, "failed", "screenshot_failed", activePackage)
                    } finally {
                        screenshot.hardwareBuffer.close()
                    }
                }

                override fun onFailure(errorCode: Int) {
                    writeScreenshotFailure(context, treeUri, "failed", "screenshot_failed", activePackage)
                }
            },
        )
    }

    fun dumpUiTree(context: android.content.Context? = null, treeUri: Uri? = null, fallbackPackage: String? = null) {
        val activePackage = activePackageInfo(fallbackPackage = fallbackPackage)
        if (!activePackage.isAllowlisted) {
            writeBlockedOutput(
                command = SolumCompanionCommand.DUMP_UI_TREE,
                path = SolumDeviceAgentState.UI_TREE_PATH,
                activePackage = activePackage,
            )
            return
        }

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            val failed = SolumDeviceAgentState.statusJson(
                status = "failed",
                command = SolumCompanionCommand.DUMP_UI_TREE,
                activePackage = activePackage,
                reason = "root_window_unavailable",
                outputPath = SolumDeviceAgentState.UI_TREE_PATH,
            )
            if (context != null) {
                SolumDeviceAgentState.writeUiTreeViaSaf(context, treeUri, failed)
            } else {
                SolumDeviceAgentState.writeTextSafely(SolumDeviceAgentState.UI_TREE_PATH, failed)
            }
            appendActionLogEntry(
                command = SolumCompanionCommand.DUMP_UI_TREE,
                status = "failed",
                reason = "root_window_unavailable",
                outputPath = SolumDeviceAgentState.UI_TREE_PATH,
            )
            return
        }

        val treeJson = """
            {
              "schema": "solum.device_agent.ui_tree",
              "schemaVersion": 1,
              "timestampUtc": ${SolumDeviceAgentState.jsonEscape(SolumDeviceAgentState.timestampUtc())},
              "status": "ok",
              "activePackage": ${SolumDeviceAgentState.activePackageJson(activePackage)},
              "root": ${nodeToJson(rootNode, 0)}
            }
        """.trimIndent()
        if (context != null) {
            SolumDeviceAgentState.writeUiTreeViaSaf(context, treeUri, treeJson)
        } else {
            SolumDeviceAgentState.writeTextSafely(SolumDeviceAgentState.UI_TREE_PATH, treeJson)
        }
        appendActionLogEntry(
            command = SolumCompanionCommand.DUMP_UI_TREE,
            status = "ok",
            reason = null,
            outputPath = SolumDeviceAgentState.UI_TREE_PATH,
        )
    }

    fun writeActionLog(context: android.content.Context? = null, treeUri: Uri? = null) {
        val activePackage = activePackageInfo()
        if (!activePackage.isAllowlisted) {
            appendActionLogEntry(
                command = SolumCompanionCommand.WRITE_ACTION_LOG,
                status = "blocked",
                reason = "package_not_allowlisted",
                outputPath = SolumDeviceAgentState.ACTION_LOG_PATH,
            )
            if (context != null) {
                SolumDeviceAgentState.writeActionLogViaSaf(context, treeUri, actionLogEntries.toList())
            }
            return
        }
        appendActionLogEntry(
            command = SolumCompanionCommand.WRITE_ACTION_LOG,
            status = "ok",
            reason = null,
            outputPath = SolumDeviceAgentState.ACTION_LOG_PATH,
        )
        if (context != null) {
            SolumDeviceAgentState.writeActionLogViaSaf(context, treeUri, actionLogEntries.toList())
        }
    }

    fun buildVisualPack() {
        val activePackage = activePackageInfo()
        if (!activePackage.isAllowlisted) {
            writeBlockedOutput(
                command = SolumCompanionCommand.BUILD_VISUAL_PACK,
                path = SolumDeviceAgentState.VISUAL_MANIFEST_PATH,
                activePackage = activePackage,
            )
            return
        }
        writeVisualManifest(context = null, treeUri = null, status = "ok", reason = null, activePackage = activePackage)
        appendActionLogEntry(
            command = SolumCompanionCommand.BUILD_VISUAL_PACK,
            status = "ok",
            reason = null,
            outputPath = SolumDeviceAgentState.VISUAL_MANIFEST_PATH,
        )
    }

    fun launchSolumStub() {
        appendActionLogEntry(
            command = SolumCompanionCommand.LAUNCH_SOLUM_STUB,
            status = "stub",
            reason = "future_only",
        )
    }

    fun forceStopSolumStub() {
        appendActionLogEntry(
            command = SolumCompanionCommand.FORCE_STOP_SOLUM_STUB,
            status = "stub",
            reason = "future_only",
        )
    }

    private fun activePackageInfo(fallbackPackage: String? = null): SolumActivePackageInfo {
        val packageName = activePackageName ?: fallbackPackage
        return SolumActivePackageInfo(
            packageName = packageName,
            isAllowlisted = packageName != null && isPackageAllowed(packageName),
            lastEventAt = lastEventAt,
        )
    }

    private fun writeScreenshotFailure(
        context: android.content.Context?,
        treeUri: Uri?,
        status: String,
        reason: String,
        activePackage: SolumActivePackageInfo,
    ) {
        writeVisualManifest(context = context, treeUri = treeUri, status = status, reason = reason, activePackage = activePackage, finalPath = null)
        appendActionLogEntry(
            command = SolumCompanionCommand.CAPTURE_SCREENSHOT,
            status = status,
            reason = reason,
            outputPath = SolumDeviceAgentState.VISUAL_MANIFEST_PATH,
        )
    }

    private fun writeVisualDiagnosticsFailure(
        context: android.content.Context,
        treeUri: Uri?,
        status: String,
        reason: String,
        activePackage: SolumActivePackageInfo,
    ): SolumDeviceAgentState.VisualDiagnosticsWriteResult {
        writeVisualManifest(
            context = context,
            treeUri = treeUri,
            status = status,
            reason = reason,
            activePackage = activePackage,
            finalPath = null,
        )
        appendActionLogEntry(
            command = SolumCompanionCommand.RUN_VISUAL_DIAGNOSTICS,
            status = status,
            reason = reason,
            outputPath = SolumDeviceAgentState.VISUAL_MANIFEST_PATH,
        )
        return SolumDeviceAgentState.VisualDiagnosticsWriteResult(status = status, reason = reason, finalPath = null)
    }

    private fun writeBlockedOutput(
        command: SolumCompanionCommand,
        path: String,
        activePackage: SolumActivePackageInfo,
    ) {
        val blocked = SolumDeviceAgentState.statusJson(
            status = "blocked",
            command = command,
            activePackage = activePackage,
            reason = "package_not_allowlisted",
            outputPath = path,
        )
        SolumDeviceAgentState.writeTextSafely(path, blocked)
        appendActionLogEntry(
            command = command,
            status = "blocked",
            reason = "package_not_allowlisted",
            outputPath = path,
        )
    }

    private fun writeVisualManifest(
        context: android.content.Context?,
        treeUri: Uri?,
        status: String,
        reason: String?,
        activePackage: SolumActivePackageInfo,
        finalPath: String? = SolumDeviceAgentState.SCREENSHOT_PATH,
    ) {
        if (context != null) {
            SolumDeviceAgentState.writeVisualDiagnosticsManifestViaSaf(
                context = context,
                treeUri = treeUri,
                status = status,
                reason = reason,
                activePackage = activePackage,
                finalPath = finalPath,
            )
        } else {
            val manifest = SolumDeviceAgentState.visualDiagnosticsManifestJson(
                status = status,
                reason = reason,
                activePackage = activePackage,
                finalPath = finalPath,
            )
            SolumDeviceAgentState.writeTextSafely(SolumDeviceAgentState.VISUAL_MANIFEST_PATH, manifest)
        }
    }

    private fun appendActionLogEntry(
        command: SolumCompanionCommand,
        status: String,
        reason: String?,
        outputPath: String? = null,
    ) {
        if (actionLogEntries.size >= MAX_ACTION_LOG_ENTRIES) {
            actionLogEntries.removeFirst()
        }
        val reasonLine = reason?.let { ",\n      \"reason\": ${SolumDeviceAgentState.jsonEscape(it)}" } ?: ""
        val outputLine = outputPath?.let { ",\n      \"outputPath\": ${SolumDeviceAgentState.jsonEscape(it)}" } ?: ""
        val activePackage = activePackageInfo()
        actionLogEntries.addLast(
            """
                {
                  "timestampUtc": ${SolumDeviceAgentState.jsonEscape(SolumDeviceAgentState.timestampUtc())},
                  "command": ${SolumDeviceAgentState.jsonEscape(command.name)},
                  "status": ${SolumDeviceAgentState.jsonEscape(status)}$reasonLine$outputLine,
                  "activePackage": ${SolumDeviceAgentState.activePackageJson(activePackage)}
                }
            """.trimIndent(),
        )
        val log = """
            {
              "schema": "solum.device_agent.action_log",
              "schemaVersion": 1,
              "timestampUtc": ${SolumDeviceAgentState.jsonEscape(SolumDeviceAgentState.timestampUtc())},
              "entries": [
                ${actionLogEntries.joinToString(",\n    ")}
              ]
            }
        """.trimIndent()
        SolumDeviceAgentState.writeTextSafely(SolumDeviceAgentState.ACTION_LOG_PATH, log)
    }

    private fun nodeToJson(node: AccessibilityNodeInfo, depth: Int): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val childCount = if (depth >= MAX_UI_TREE_DEPTH) 0 else node.childCount
        val children = mutableListOf<String>()
        for (index in 0 until childCount.coerceAtMost(MAX_UI_TREE_CHILDREN_PER_NODE)) {
            val child = node.getChild(index) ?: continue
            children.add(nodeToJson(child, depth + 1))
        }

        return """
            {
              "className": ${SolumDeviceAgentState.jsonEscape(node.className?.toString())},
              "viewIdResourceName": ${SolumDeviceAgentState.jsonEscape(node.viewIdResourceName)},
              "text": ${SolumDeviceAgentState.jsonEscape(node.text?.toString())},
              "contentDescription": ${SolumDeviceAgentState.jsonEscape(node.contentDescription?.toString())},
              "clickable": ${node.isClickable},
              "enabled": ${node.isEnabled},
              "focused": ${node.isFocused},
              "boundsInScreen": {
                "left": ${bounds.left},
                "top": ${bounds.top},
                "right": ${bounds.right},
                "bottom": ${bounds.bottom}
              },
              "children": [
                ${children.joinToString(",\n    ")}
              ]
            }
        """.trimIndent()
    }

    companion object {
        private const val MAX_ACTION_LOG_ENTRIES = 100
        private const val MAX_UI_TREE_DEPTH = 12
        private const val MAX_UI_TREE_CHILDREN_PER_NODE = 64

        val SOLUM_ALLOWED_PACKAGES = setOf(
            "com.solum.companion",
            "com.solum.engine",
            "com.solum.launcher",
            "com.solum.assethub",
            "com.solum.materialstudio",
            "com.asart.solum",
        )

        @Volatile
        var currentInstance: SolumAccessibilityService? = null
            private set
    }
}
