package app.everding.notepadminusminus

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

/**
 * Home-Screen-Widget: zeigt eine read-only Ansicht des Notiztexts.
 * Tippen auf das Widget öffnet MainActivity (die Bearbeitungsschicht).
 * Pro-Widget-Einstellungen (Hintergrund an/aus, Farbe, Transparenz) kommen
 * aus WidgetConfigureActivity, gespeichert unter dem jeweiligen appWidgetId.
 */
class NoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE).edit()
        for (id in appWidgetIds) {
            editor.remove(keyEnabled(id)).remove(keyColor(id)).remove(keyAlpha(id))
        }
        editor.apply()
    }

    companion object {
        fun keyEnabled(id: Int) = "bg_enabled_$id"
        fun keyColor(id: Int) = "bg_color_$id"
        fun keyAlpha(id: Int) = "bg_alpha_$id"

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NoteWidgetProvider::class.java))
            for (id in ids) updateWidget(context, manager, id)
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val widgetPrefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val bgEnabled = widgetPrefs.getBoolean(keyEnabled(appWidgetId), true)
            val bgColor = widgetPrefs.getInt(keyColor(appWidgetId), PAPER_YELLOW)
            val alpha = widgetPrefs.getInt(keyAlpha(appWidgetId), 255)

            val noteText = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREFS_KEY, "") ?: ""

            val views = RemoteViews(context.packageName, R.layout.widget_note)
            views.setTextViewText(
                R.id.widget_text,
                noteText.ifBlank { "Tippen, um zu schreiben …" },
            )

            val backgroundColor = if (bgEnabled) {
                (bgColor and 0x00FFFFFF) or (alpha shl 24)
            } else {
                Color.TRANSPARENT
            }
            views.setInt(R.id.widget_root, "setBackgroundColor", backgroundColor)

            val openIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
