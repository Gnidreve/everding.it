package app.everding.notepadminusminus

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

private val SWATCHES = intArrayOf(
    0xFFFFF3B0.toInt(), // Gelb (Standard)
    0xFFFFFFFF.toInt(), // Weiß
    0xFFFFD1DC.toInt(), // Rosa
    0xFFBBDEFB.toInt(), // Blau
    0xFFC8E6C9.toInt(), // Grün
    0xFF2E2A1F.toInt(), // Graphit
)

/**
 * Wird von Android beim Platzieren des Widgets gestartet (APPWIDGET_CONFIGURE).
 * Drei Einstellungen wie gewünscht: Hintergrund an/aus, Hintergrundfarbe,
 * Transparenz. Muss laut App-Widget-Protokoll immer mit setResult()
 * abschließen (RESULT_CANCELED als Default, RESULT_OK erst nach "Speichern"),
 * sonst verwirft Android die Widget-Platzierung.
 */
class WidgetConfigureActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var selectedColor = SWATCHES[0]
    private lateinit var prefs: SharedPreferences
    private lateinit var swatchContainer: LinearLayout
    private lateinit var alphaSeek: SeekBar
    private lateinit var bgSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        prefs = getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
        selectedColor = prefs.getInt(NoteWidgetProvider.keyColor(appWidgetId), SWATCHES[0])

        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PAPER_YELLOW)
            setPadding(dp(24), dp(40), dp(24), dp(24))
        }

        root.addView(
            TextView(this).apply {
                text = "Widget-Einstellungen"
                textSize = 20f
                setTextColor(Color.BLACK)
            },
        )

        bgSwitch = Switch(this).apply {
            text = "Hintergrund"
            textSize = 16f
            isChecked = prefs.getBoolean(NoteWidgetProvider.keyEnabled(appWidgetId), true)
        }
        root.addView(bgSwitch, topMargined(28))

        root.addView(label("Hintergrundfarbe"))
        swatchContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        SWATCHES.forEach { color -> swatchContainer.addView(swatchView(color)) }
        root.addView(swatchContainer, topMargined(8))

        root.addView(label("Transparenz"))
        alphaSeek = SeekBar(this).apply {
            max = 100
            progress = (prefs.getInt(NoteWidgetProvider.keyAlpha(appWidgetId), 255) * 100) / 255
        }
        root.addView(alphaSeek, topMargined(8))

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        buttonRow.addView(
            Button(this).apply {
                text = "Abbrechen"
                setOnClickListener { finish() }
            },
        )
        buttonRow.addView(
            Button(this).apply {
                text = "Speichern"
                setOnClickListener { save() }
            },
        )
        root.addView(buttonRow, topMargined(40))

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(PAPER_YELLOW)
                addView(root)
            },
        )
    }

    private fun swatchView(color: Int): View {
        val size = dp(40)
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
            background = swatchDrawable(color)
            setOnClickListener {
                selectedColor = color
                refreshSwatchSelection()
            }
        }
    }

    private fun swatchDrawable(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setStroke(dp(if (color == selectedColor) 3 else 1), Color.DKGRAY)
    }

    private fun refreshSwatchSelection() {
        for (i in 0 until swatchContainer.childCount) {
            swatchContainer.getChildAt(i).background = swatchDrawable(SWATCHES[i])
        }
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.DKGRAY)
    }

    private fun topMargined(topDp: Int) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(topDp) }

    private fun save() {
        val alpha = (alphaSeek.progress * 255) / 100
        prefs.edit()
            .putBoolean(NoteWidgetProvider.keyEnabled(appWidgetId), bgSwitch.isChecked)
            .putInt(NoteWidgetProvider.keyColor(appWidgetId), selectedColor)
            .putInt(NoteWidgetProvider.keyAlpha(appWidgetId), alpha)
            .apply()

        NoteWidgetProvider.updateWidget(this, AppWidgetManager.getInstance(this), appWidgetId)

        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        finish()
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics,
    ).toInt()
}
