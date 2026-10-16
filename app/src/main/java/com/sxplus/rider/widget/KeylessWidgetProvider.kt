package com.sxplus.rider.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sxplus.rider.R
import com.sxplus.rider.receiver.KeylessActionReceiver

/** Home-screen widget: Find / Lock / Unlock buttons that fire keyless commands. */
class KeylessWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_keyless).apply {
                setOnClickPendingIntent(R.id.widget_find, pi(context, KeylessActionReceiver.ACTION_FIND, 1))
                setOnClickPendingIntent(R.id.widget_lock, pi(context, KeylessActionReceiver.ACTION_LOCK, 2))
                setOnClickPendingIntent(R.id.widget_unlock, pi(context, KeylessActionReceiver.ACTION_UNLOCK, 3))
            }
            manager.updateAppWidget(id, views)
        }
    }

    private fun pi(context: Context, action: String, req: Int): PendingIntent {
        val intent = Intent(context, KeylessActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, req, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
