package app.everding.notepadminusminus

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import java.io.PrintWriter
import java.io.StringWriter

private const val INK_COLOR = 0xFF2E2A1F.toInt()
private const val HINT_COLOR = 0x802E2A1F.toInt()
private const val LINE_COLOR = 0x33000000
private const val MARGIN_COLOR = 0xFFE2857A.toInt()

// Statische Platzhalter für die Sidebar — noch ohne Funktion (Feeling-Test).
private val SIDEBAR_ITEMS = listOf("Einkaufsliste", "Ideen", "Arbeit", "Privat", "Ohne Titel")

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
    private lateinit var drawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            setupNote()
        } catch (t: Throwable) {
            showCrashScreen(t)
        }
    }

    override fun onBackPressed() {
        if (::drawer.isInitialized && drawer.isDrawerOpen(Gravity.START)) {
            drawer.closeDrawer(Gravity.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun setupNote() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Statusleiste gelb statt transparent: sonst schimmert das System-Schwarz durch.
        window.statusBarColor = PAPER_YELLOW
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(PAPER_YELLOW))
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

        val content = FrameLayout(this).apply {
            setBackgroundColor(PAPER_YELLOW)
            addView(
                editText,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        // Edge-Swipe von links öffnet die Sidebar, kein Button.
        val root = DrawerLayout(this).apply {
            setBackgroundColor(PAPER_YELLOW)
            fitsSystemWindows = false
            addView(
                content,
                DrawerLayout.LayoutParams(
                    DrawerLayout.LayoutParams.MATCH_PARENT,
                    DrawerLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            addView(
                buildSidebar(),
                DrawerLayout.LayoutParams(dp(280), DrawerLayout.LayoutParams.MATCH_PARENT, Gravity.START),
            )
        }
        drawer = root
        setContentView(root)

        // Hintergrund + rote Linie bleiben edge-to-edge (RuledEditText selbst
        // bekommt keine Insets-Clips). Nur Text-Padding + erste Zeile weichen
        // der Status-/Navigationsleiste aus.
        ViewCompat.setOnApplyWindowInsetsListener(editText) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Unten endet der sichtbare Textbereich über der Tastatur (Fenster wird nicht
            // verkleinert, siehe adjustNothing im Manifest). Der Cursor bleibt so immer
            // auf einer Linie, die oberhalb der Tastatur sichtbar ist.
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomPad = if (imeVisible) {
                insets.getInsets(WindowInsetsCompat.Type.ime()).bottom + dp(24)
            } else {
                dp(64) + bars.bottom
            }
            view.setPadding(dp(36) + bars.left, dp(16) + bars.top, dp(16) + bars.right, bottomPad)
            if (imeVisible) {
                // Nach Padding-Änderung den Cursor erneut in den sichtbaren Bereich holen.
                editText.post { editText.bringPointIntoView(editText.selectionEnd) }
            } else {
                // Tastatur weg -> Ansichtsmodus ohne Cursor.
                editText.isCursorVisible = false
            }
            insets
        }
        editText.setOnClickListener { enterEditMode() }
        ViewCompat.requestApplyInsets(editText)

        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        editText.setText(loadText())
        editText.setSelection(editText.text.length)
        editText.isCursorVisible = false
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

    // Tipp irgendwo ins Blatt: Cursor an, Tastatur auf.
    private fun enterEditMode() {
        editText.isCursorVisible = true
        editText.requestFocus()
        getSystemService(InputMethodManager::class.java)?.showSoftInput(editText, 0)
    }

    private fun buildSidebar(): View {
        val title = TextView(this).apply {
            text = "Notepad--"
            setTextColor(INK_COLOR)
            textSize = 26f
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(dp(24), dp(24), dp(24), dp(20))
        }
        val divider = View(this).apply { setBackgroundColor(LINE_COLOR) }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            SIDEBAR_ITEMS.forEach { addView(sidebarRow(it)) }
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PAPER_YELLOW)
            addView(title)
            addView(divider, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)))
            addView(list)
            // Status-/Navigationsleiste: Sidebar bleibt edge-to-edge, Inhalt weicht aus.
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
    }

    private fun sidebarRow(label: String): View {
        val labelView = TextView(this).apply {
            text = label
            setTextColor(INK_COLOR)
            textSize = 16f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        // Stift: rein optisch, kein Klick-Handler.
        val pencil = ImageView(this).apply {
            setImageResource(R.drawable.ic_pencil)
            imageTintList = ColorStateList.valueOf(HINT_COLOR)
            contentDescription = "Umbenennen"
        }
        val ripple = TypedValue().also {
            theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(52)
            setPadding(dp(24), 0, dp(16), 0)
            setBackgroundResource(ripple.resourceId)
            isClickable = true
            addView(labelView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(pencil, LinearLayout.LayoutParams(dp(20), dp(20)))
        }
    }

    private fun showCrashScreen(t: Throwable) {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        val textView = TextView(this).apply {
            text = "Notepad-- ist beim Start gecrasht:\n\n${sw}"
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
        NoteWidgetProvider.updateAllWidgets(this)
    }

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics,
    ).toInt()
}

/**
 * EditText, das linierte Notizblock-Linien scrollsynchron hinter dem Text
 * zeichnet. Linienposition kommt direkt aus den echten Font-Metriken
 * (getBaseline()/getLineHeight()) statt aus geschätzten dp-Werten — damit
 * sitzt der Text exakt auf der Linie, und beides skaliert zusammen mit der
 * System-Schriftgröße.
 */
class RuledEditText(context: Context, attrs: AttributeSet? = null) : EditText(context, attrs) {

    private val linePaint = Paint().apply {
        color = LINE_COLOR
        strokeWidth = 1f
    }
    private val marginPaint = Paint().apply {
        color = MARGIN_COLOR
        strokeWidth = 2f
    }
    private val marginXPx = dip(28f)
    private val baselineGapPx = dip(2f)

    private fun dip(value: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.resources.displayMetrics)

    override fun onDraw(canvas: Canvas) {
        val top = scrollY
        val bottom = scrollY + height

        val firstBaseline = baseline
        val lineH = lineHeight.toFloat()
        if (firstBaseline >= 0 && lineH > 0f) {
            var y = firstBaseline + baselineGapPx
            while (y < bottom + lineH) {
                if (y >= top - lineH) {
                    canvas.drawLine(scrollX.toFloat(), y, (scrollX + width).toFloat(), y, linePaint)
                }
                y += lineH
            }
        }

        // Rote Randlinie läuft bewusst edge-to-edge, auch durch die Safe Areas.
        canvas.drawLine(
            marginXPx + scrollX, top.toFloat(),
            marginXPx + scrollX, bottom.toFloat(), marginPaint,
        )
        super.onDraw(canvas)
    }
}
