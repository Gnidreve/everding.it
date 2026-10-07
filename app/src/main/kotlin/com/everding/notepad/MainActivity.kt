package com.everding.notepad

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.FrameLayout

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
 */
class MainActivity : Activity() {

    private lateinit var editText: RuledEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyYellowSystemBars()

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
            background = null
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

    override fun onPause() {
        super.onPause()
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_KEY, editText.text.toString())
            .commit()
    }

    private fun applyYellowSystemBars() {
        window.statusBarColor = PAPER_YELLOW
        window.navigationBarColor = PAPER_YELLOW
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
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
        var y = topOffsetPx
        while (y < bottom) {
            if (y >= top - lineHeightPx) {
                canvas.drawLine(scrollX.toFloat(), y, (scrollX + width).toFloat(), y, linePaint)
            }
            y += lineHeightPx
        }
        canvas.drawLine(
            marginXPx + scrollX, top.toFloat(),
            marginXPx + scrollX, bottom.toFloat(), marginPaint,
        )
        super.onDraw(canvas)
    }
}
