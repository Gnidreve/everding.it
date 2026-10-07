package com.everding.notepad

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import java.io.PrintWriter
import java.io.StringWriter

private const val PREFS_NAME = "notizblock"
private const val PREFS_KEY = "note_text"
private const val PAPER_YELLOW = 0xFFFFF3B0.toInt()
private const val INK_COLOR = 0xFF2E2A1F.toInt()
private const val HINT_COLOR = 0x802E2A1F.toInt()
private const val LINE_COLOR = 0x33000000
private const val MARGIN_COLOR = 0xFFE2857A.toInt()

/**
 * Einzige Seite der App: ein endlos scrollbares, gelbes Notizblatt.
 * Jede Änderung wird sofort in SharedPreferences geschrieben (apply(), async
 * aber durable); beim Pausieren zusätzlich synchron (commit()) als Netz.
 *
 * Edge-to-edge: Hintergrund (gelb) und die rote Randlinie laufen bewusst
 * unter Status-/Navigationsleiste durch (RuledEditText ist MATCH_PARENT,
 * keine Insets-Clips). Nur der tatsächliche Text + die erste horizontale
 * Linie bekommen über die Insets ein Padding, damit nichts unter der Leiste
 * verschwindet.
 *
 * onCreate ist bewusst komplett in try/catch gewrappt: Diagnose-Build, damit
 * ein Crash als lesbarer Stacktrace auf dem Screen landet statt als
 * "App wurde beendet"-Dialog ohne Logcat-Zugriff.
 */
class MainActivity : Activity() {

    private lateinit var editText: RuledEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setupNote()
        } catch (t: Throwable) {
            showCrashScreen(t)
        }
    }

    private fun setupNote() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        editText = RuledEditText(this).apply {
            setBackgroundColor(PAPER_YELLOW)
            setTextColor(INK_COLOR)
            setHintTextColor(HINT_COLOR)
            textSize = 16f
            setPadding(dp(36), dp(16), dp(16), dp(64))
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setSingleLine(false)
            setHorizontallyScrolling(false)
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(PAPER_YELLOW)
            addView(
                editText,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        setContentView(root)

        // Hintergrund + rote Linie bleiben edge-to-edge (RuledEditText selbst
        // bekommt keine Insets-Clips). Nur Text-Padding + erste Zeile weichen
        // der Status-/Navigationsleiste aus.
        ViewCompat.setOnApplyWindowInsetsListener(editText) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(dp(36) + bars.left, dp(16) + bars.top, dp(16) + bars.right, dp(64) + bars.bottom)
            (view as RuledEditText).topInsetPx = bars.top.toFloat()
            insets
        }
        ViewCompat.requestApplyInsets(editText)

        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        editText.setText(loadText())
        editText.setSelection(editText.text.length)
        editText.requestFocus()

        editText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    saveText(s?.toString() ?: "")
                }
            },
        )
    }

    private fun showCrashScreen(t: Throwable) {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        val textView = TextView(this).apply {
            text = "Notizblock ist beim Start gecrasht:\n\n${sw}"
            setTextIsSelectable(true)
            setTextColor(Color.RED)
            textSize = 12f
            setPadding(dp(16), dp(48), dp(16), dp(48))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            addView(textView)
        }
        try {
            setContentView(scroll)
        } catch (inner: Throwable) {
            // Wenn selbst das fehlschlägt, geben wir auf und lassen den Absturz
            // regulär passieren (inklusive System-Dialog).
            throw inner
        }
    }

    override fun onPause() {
        super.onPause()
        if (!::editText.isInitialized) return
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_KEY, editText.text.toString())
            .commit()
    }

    private fun loadText(): String =
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREFS_KEY, "") ?: ""

    private fun saveText(text: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_KEY, text)
            .apply()
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics,
    ).toInt()
}

/** EditText, das linierte Notizblock-Linien scrollsynchron hinter dem Text zeichnet. */
class RuledEditText(context: Context, attrs: AttributeSet? = null) : EditText(context, attrs) {

    /** Oberer System-Bar-Inset (Status Bar) — Linien starten erst danach, Hintergrund nicht. */
    var topInsetPx: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val linePaint = Paint().apply {
        color = LINE_COLOR
        strokeWidth = 1f
    }
    private val marginPaint = Paint().apply {
        color = MARGIN_COLOR
        strokeWidth = 2f
    }
    private val lineHeightPx = dip(30f)
    private val topOffsetPx = dip(26f)
    private val marginXPx = dip(28f)

    private fun dip(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)

    override fun onDraw(canvas: Canvas) {
        val top = scrollY
        val bottom = scrollY + height
        var y = topInsetPx + topOffsetPx
        while (y < bottom) {
            if (y >= top - lineHeightPx) {
                canvas.drawLine(scrollX.toFloat(), y, (scrollX + width).toFloat(), y, linePaint)
            }
            y += lineHeightPx
        }
        // Rote Randlinie läuft bewusst edge-to-edge, auch durch die Safe Areas.
        canvas.drawLine(
            marginXPx + scrollX, top.toFloat(),
            marginXPx + scrollX, bottom.toFloat(), marginPaint,
        )
        super.onDraw(canvas)
    }
}
