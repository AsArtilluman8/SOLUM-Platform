package com.solum.companion

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val outputFolderRequestCode = 4101
    private val prefsName = "solum_companion_output"
    private val treeUriPrefKey = "saf_output_tree_uri"

    private lateinit var pathTextView: TextView
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "SOLUM Companion"
        setContentView(buildContentView())
        refreshStatusText()
    }

    private fun buildContentView(): View {
        val scrollView = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(28))
        }
        scrollView.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        content.addView(
            TextView(this).apply {
                text = "SOLUM Companion"
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
            },
        )

        statusTextView = blockTextView()
        content.addView(sectionTitle("Status"))
        content.addView(statusTextView)

        pathTextView = blockTextView().apply {
            text = SolumDeviceAgentState.outputPathsText()
        }
        content.addView(sectionTitle("Output paths"))
        content.addView(pathTextView)

        content.addView(sectionTitle("Restricted Settings"))
        content.addView(
            blockTextView().apply {
                text = """
                    Если Android пишет Доступ к настройкам ограничен:
                    Settings -> Apps -> SOLUM Companion -> menu/dots -> Allow restricted settings.
                    На некоторых TECNO/HiOS этот пункт может быть скрыт.
                    Если пункта нет, используй adb/wireless debugging install route.
                """.trimIndent()
            },
        )

        content.addView(actionButton("Open Accessibility Settings") {
            openIntent(
                intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                failureMessage = "Accessibility Settings недоступны",
            )
        })
        content.addView(actionButton("Open App Details Settings") {
            openIntent(
                intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                },
                failureMessage = "App Details Settings недоступны",
            )
        })
        content.addView(actionButton("Test Write Evidence Files") {
            testWriteEvidenceFiles()
        })
        content.addView(actionButton("Choose SOLUMCreative Output Folder") {
            chooseOutputFolder()
        })
        content.addView(actionButton("Clear Output Folder Permission") {
            clearOutputFolderPermission()
        })

        return scrollView
    }

    private fun refreshStatusText() {
        statusTextView.text = """
            packageName: $packageName
            appVersion: ${appVersion()}
            SAF output folder: ${if (savedTreeUri() != null) "configured" else "not configured"}
            treeUri: ${redactedTreeUri()}
        """.trimIndent()
    }

    private fun testWriteEvidenceFiles() {
        val result = SolumDeviceAgentState.writeManualEvidenceFiles(
            context = this,
            treeUri = savedTreeUri(),
            packageName = packageName,
            appVersion = appVersion(),
        )
        pathTextView.text = SolumDeviceAgentState.outputPathsText()
        refreshStatusText()
        if (result.success) {
            when (result.writeRoute) {
                "saf" -> toast("Evidence files written via SAF")
                "direct" -> toast("Evidence files written via direct path")
                else -> toast("Evidence files written")
            }
        } else {
            toast("Evidence write failed: choose output folder")
        }
    }

    private fun chooseOutputFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        }
        try {
            startActivityForResult(intent, outputFolderRequestCode)
        } catch (_: ActivityNotFoundException) {
            pathTextView.text = SolumDeviceAgentState.outputPathsText()
            toast("Folder picker unavailable")
        }
    }

    @Deprecated("Activity result callback is enough for this no-AndroidX launcher Activity.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != outputFolderRequestCode) return
        if (resultCode != RESULT_OK) {
            refreshStatusText()
            toast("Output folder not configured")
            return
        }

        val treeUri = data?.data
        if (treeUri == null) {
            refreshStatusText()
            toast("Output folder not configured")
            return
        }

        val permissionFlags = data.flags and (
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val persistFlags = if (permissionFlags != 0) {
            permissionFlags
        } else {
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        try {
            contentResolver.takePersistableUriPermission(treeUri, persistFlags)
            prefs().edit().putString(treeUriPrefKey, treeUri.toString()).apply()
            refreshStatusText()
            pathTextView.text = SolumDeviceAgentState.outputPathsText()
            toast("SOLUMCreative output folder configured")
        } catch (_: Exception) {
            refreshStatusText()
            toast("Failed to persist output folder permission")
        }
    }

    private fun clearOutputFolderPermission() {
        savedTreeUri()?.let { uri ->
            try {
                contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (_: Exception) {
            }
        }
        prefs().edit().remove(treeUriPrefKey).apply()
        refreshStatusText()
        pathTextView.text = SolumDeviceAgentState.outputPathsText()
        toast("Output folder permission cleared")
    }

    private fun savedTreeUri(): Uri? {
        val value = prefs().getString(treeUriPrefKey, null) ?: return null
        return runCatching { Uri.parse(value) }.getOrNull()
    }

    private fun redactedTreeUri(): String {
        val value = savedTreeUri()?.toString() ?: return "not configured"
        if (value.length <= 28) return "configured"
        return "${value.take(16)}...${value.takeLast(10)}"
    }

    private fun prefs() = getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private fun openIntent(intent: Intent, failureMessage: String) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            toast(failureMessage)
        }
    }

    private fun appVersion(): String {
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }

    private fun sectionTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(18), 0, dp(6))
        }
    }

    private fun blockTextView(): TextView {
        return TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
            setLineSpacing(0f, 1.08f)
        }
    }

    private fun actionButton(textValue: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = textValue
            isAllCaps = false
            gravity = Gravity.CENTER
            setOnClickListener { onClick() }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
