package com.solum.companion

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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
    const val ACTION_LOG_RELATIVE_PATH = "device_agent/latest/action_log.json"
    const val VISUAL_MANIFEST_RELATIVE_PATH = "diagnostics/latest/visual_diagnostics_manifest.json"
    const val DIRECT_PUBLIC_STORAGE_FAILED_CHOOSE_OUTPUT_FOLDER =
        "direct_public_storage_failed_choose_output_folder"

    data class ManualEvidenceWriteResult(
        val success: Boolean,
        val actionLogPath: String,
        val visualManifestPath: String,
        val error: String?,
        val writeRoute: String?,
        val reason: String?,
    )

    data class SafWriteResult(
        val success: Boolean,
        val uri: Uri?,
        val reason: String?,
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

    fun writeTextViaSaf(context: Context, treeUri: Uri, relativePath: String, text: String): SafWriteResult {
        return try {
            val normalizedPath = relativePath.trim('/').split('/').filter { it.isNotBlank() }
            if (normalizedPath.isEmpty()) {
                return SafWriteResult(false, null, "empty_relative_path")
            }

            val resolver = context.contentResolver
            val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            var parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)

            normalizedPath.dropLast(1).forEach { directoryName ->
                parentUri = findChildDocument(
                    context = context,
                    treeUri = treeUri,
                    parentUri = parentUri,
                    displayName = directoryName,
                    mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
                ) ?: DocumentsContract.createDocument(
                    resolver,
                    parentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    directoryName,
                ) ?: return SafWriteResult(false, null, "failed_to_create_saf_directory:$directoryName")
            }

            val fileName = normalizedPath.last()
            val fileUri = findChildDocument(
                context = context,
                treeUri = treeUri,
                parentUri = parentUri,
                displayName = fileName,
                mimeType = "application/json",
            ) ?: DocumentsContract.createDocument(
                resolver,
                parentUri,
                "application/json",
                fileName,
            ) ?: return SafWriteResult(false, null, "failed_to_create_saf_file:$fileName")

            resolver.openOutputStream(fileUri, "wt")?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            } ?: return SafWriteResult(false, fileUri, "failed_to_open_saf_output_stream")

            SafWriteResult(true, fileUri, null)
        } catch (exception: Exception) {
            SafWriteResult(false, null, exception.javaClass.simpleName.ifBlank { "saf_write_failed" })
        }
    }

    fun writeManualEvidenceFiles(
        context: Context? = null,
        treeUri: Uri? = null,
        packageName: String,
        appVersion: String,
    ): ManualEvidenceWriteResult {
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

        if (context != null && treeUri != null) {
            val actionSaf = writeTextViaSaf(context, treeUri, ACTION_LOG_RELATIVE_PATH, actionLog)
            val manifestSaf = writeTextViaSaf(context, treeUri, VISUAL_MANIFEST_RELATIVE_PATH, manifest)
            if (actionSaf.success && manifestSaf.success) {
                return ManualEvidenceWriteResult(
                    success = true,
                    actionLogPath = ACTION_LOG_RELATIVE_PATH,
                    visualManifestPath = VISUAL_MANIFEST_RELATIVE_PATH,
                    error = null,
                    writeRoute = "saf",
                    reason = null,
                )
            }
        }

        val actionOk = writeTextSafely(ACTION_LOG_PATH, actionLog)
        val manifestOk = writeTextSafely(VISUAL_MANIFEST_PATH, manifest)
        val error = when {
            actionOk && manifestOk -> null
            else -> DIRECT_PUBLIC_STORAGE_FAILED_CHOOSE_OUTPUT_FOLDER
        }
        return ManualEvidenceWriteResult(
            success = actionOk && manifestOk,
            actionLogPath = ACTION_LOG_PATH,
            visualManifestPath = VISUAL_MANIFEST_PATH,
            error = error,
            writeRoute = if (actionOk && manifestOk) "direct" else null,
            reason = error,
        )
    }

    private fun findChildDocument(
        context: Context,
        treeUri: Uri,
        parentUri: Uri,
        displayName: String,
        mimeType: String,
    ): Uri? {
        val parentDocumentId = DocumentsContract.getDocumentId(parentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val childName = cursor.getString(nameIndex)
                val childMime = cursor.getString(mimeIndex)
                if (childName == displayName && (mimeType == childMime || mimeType != DocumentsContract.Document.MIME_TYPE_DIR)) {
                    val childDocumentId = cursor.getString(idIndex)
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocumentId)
                }
            }
        }
        return null
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
