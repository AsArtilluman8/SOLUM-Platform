package com.solum.companion

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SolumActivePackageInfo(
    val packageName: String?,
    val isAllowlisted: Boolean,
    val lastEventAt: String?,
)

object SolumDeviceAgentState {
    const val CREATIVE_ROOT = "/storage/emulated/0/SOLUMCreative"
    const val DEVICE_AGENT_LATEST_DIR = "$CREATIVE_ROOT/device_agent/latest"
    const val DIAGNOSTICS_LATEST_DIR = "$CREATIVE_ROOT/diagnostics/latest"
    const val ACTION_LOG_PATH = "$DEVICE_AGENT_LATEST_DIR/action_log.json"
    const val UI_TREE_PATH = "$DEVICE_AGENT_LATEST_DIR/ui_tree.json"
    const val SCREENSHOT_PATH = "$DIAGNOSTICS_LATEST_DIR/final.png"
    const val VISUAL_MANIFEST_PATH = "$DIAGNOSTICS_LATEST_DIR/visual_diagnostics_manifest.json"

    data class ManualEvidenceWriteResult(
        val success: Boolean,
        val actionLogPath: String,
        val visualManifestPath: String,
        val error: String?,
    )

    fun outputPathsText(): String {
        return """
            root: $CREATIVE_ROOT
            action log: $ACTION_LOG_PATH
            ui tree: $UI_TREE_PATH
            screenshot: $SCREENSHOT_PATH
            visual manifest: $VISUAL_MANIFEST_PATH
        """.trimIndent()
    }

    fun timestampUtc(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    fun jsonEscape(value: String?): String {
        if (value == null) return "null"
        val escaped = buildString {
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> {
                        if (char.code < 0x20) {
                            append("\\u")
                            append(char.code.toString(16).padStart(4, '0'))
                        } else {
                            append(char)
                        }
                    }
                }
            }
        }
        return "\"$escaped\""
    }

    fun writeTextSafely(path: String, content: String): Boolean {
        return try {
            val target = File(path)
            target.parentFile?.mkdirs()
            val temp = File(target.parentFile, "${target.name}.tmp")
            temp.writeText(content)
            if (target.exists() && !target.delete()) {
                temp.delete()
                return false
            }
            temp.renameTo(target)
        } catch (_: Exception) {
            false
        }
    }

    fun writeManualEvidenceFiles(packageName: String, appVersion: String): ManualEvidenceWriteResult {
        val timestamp = timestampUtc()
        val actionLog = """
            {
              "schema": "solum.device_agent.action_log",
              "schemaVersion": 1,
              "timestampUtc": ${jsonEscape(timestamp)},
              "entries": [
                {
                  "timestampUtc": ${jsonEscape(timestamp)},
                  "command": "MANUAL_TEST_WRITE_EVIDENCE_FILES",
                  "status": "ok",
                  "reason": "launcher_activity_manual_test",
                  "activePackage": {
                    "packageName": ${jsonEscape(packageName)},
                    "allowlisted": true,
                    "lastEventAt": ${jsonEscape(timestamp)}
                  },
                  "outputPath": ${jsonEscape(ACTION_LOG_PATH)}
                }
              ]
            }
        """.trimIndent()
        val manifest = """
            {
              "schema": "solum.visual_diagnostics.manifest",
              "schemaVersion": 1,
              "timestampUtc": ${jsonEscape(timestamp)},
              "status": "ok",
              "reason": "launcher_activity_manual_test_no_screenshot",
              "source": {
                "packageName": ${jsonEscape(packageName)},
                "appVersion": ${jsonEscape(appVersion)}
              },
              "files": {
                "final": null,
                "uiTree": ${jsonEscape(UI_TREE_PATH)},
                "actionLog": ${jsonEscape(ACTION_LOG_PATH)}
              }
            }
        """.trimIndent()

        val actionOk = writeTextSafely(ACTION_LOG_PATH, actionLog)
        val manifestOk = writeTextSafely(VISUAL_MANIFEST_PATH, manifest)
        val error = when {
            actionOk && manifestOk -> null
            !actionOk && !manifestOk -> "failed_to_write_action_log_and_visual_manifest"
            !actionOk -> "failed_to_write_action_log"
            else -> "failed_to_write_visual_manifest"
        }
        return ManualEvidenceWriteResult(
            success = actionOk && manifestOk,
            actionLogPath = ACTION_LOG_PATH,
            visualManifestPath = VISUAL_MANIFEST_PATH,
            error = error,
        )
    }

    fun activePackageJson(activePackage: SolumActivePackageInfo): String {
        return """
            {
              "packageName": ${jsonEscape(activePackage.packageName)},
              "allowlisted": ${activePackage.isAllowlisted},
              "lastEventAt": ${jsonEscape(activePackage.lastEventAt)}
            }
        """.trimIndent()
    }

    fun statusJson(
        status: String,
        command: SolumCompanionCommand,
        activePackage: SolumActivePackageInfo,
        reason: String? = null,
        outputPath: String? = null,
    ): String {
        val reasonLine = reason?.let { ",\n  \"reason\": ${jsonEscape(it)}" } ?: ""
        val outputLine = outputPath?.let { ",\n  \"outputPath\": ${jsonEscape(it)}" } ?: ""
        return """
            {
              "schema": "solum.device_agent.status",
              "schemaVersion": 1,
              "timestampUtc": ${jsonEscape(timestampUtc())},
              "command": ${jsonEscape(command.name)},
              "status": ${jsonEscape(status)}$reasonLine$outputLine,
              "activePackage": ${activePackageJson(activePackage)}
            }
        """.trimIndent()
    }
}
