package com.solum.companion

import android.app.Activity
import android.content.ActivityNotFoundException
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
        content.addView(actionButton("Open Output Folder") {
            openOutputFolder()
        })

        return scrollView
    }

    private fun refreshStatusText() {
        statusTextView.text = """
            packageName: $packageName
            appVersion: ${appVersion()}
        """.trimIndent()
    }

    private fun testWriteEvidenceFiles() {
        val result = SolumDeviceAgentState.writeManualEvidenceFiles(
            packageName = packageName,
            appVersion = appVersion(),
        )
        pathTextView.text = SolumDeviceAgentState.outputPathsText()
        if (result.success) {
            toast("Evidence files written")
        } else {
            toast("Evidence write failed: ${result.error ?: "unknown"}")
        }
    }

    private fun openOutputFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            startActivity(intent)
            toast(SolumDeviceAgentState.CREATIVE_ROOT)
        } catch (_: ActivityNotFoundException) {
            pathTextView.text = SolumDeviceAgentState.outputPathsText()
            toast(SolumDeviceAgentState.CREATIVE_ROOT)
        }
    }

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
