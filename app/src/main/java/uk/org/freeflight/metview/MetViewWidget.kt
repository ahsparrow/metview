package uk.org.freeflight.metview

import android.app.IntentService
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.core.content.ContextCompat.startActivity
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest

private const val TAG = "METVIEW"

/**
 * Implementation of App Widget functionality.
 */
class MetViewWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        Log.v(TAG, "onUpdate")

        val intent = Intent(context, UpdateIntentService::class.java)
            .let { intent ->
                PendingIntent.getService(context, 0, intent, 0)
            }
        intent.send()

        appWidgetIds?.forEach { appWidgetId ->
            val views = RemoteViews(
                context?.packageName,
                R.layout.met_view_widget
            ).apply {
                setOnClickPendingIntent(R.id.appwidget_image, intent)
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }

    class UpdateIntentService : IntentService("WidgetIntentService") {
        override fun onHandleIntent(intent: Intent?) {
            Log.v(TAG, "onHandleIntent")

            val imageUrl = "http://metcloud.freeflight.org.uk/images/summary"
            val imageRequest = ImageRequest(
                imageUrl,
                Response.Listener { response ->
                    val views = RemoteViews(this.packageName, R.layout.met_view_widget)
                    views.setImageViewBitmap(R.id.appwidget_image, response)

                    val thisWidget = ComponentName(this, MetViewWidget::class.java)
                    val manager = AppWidgetManager.getInstance(this)
                    manager.updateAppWidget(thisWidget, views)
                },
                0, 0,  ImageView.ScaleType.FIT_XY, Bitmap.Config.RGB_565,
                Response.ErrorListener {}
            )
            imageRequest.setShouldCache(false)

            val queue = RequestQueueSingleton.getInstance(this).requestQueue
            queue.add(imageRequest)
        }
    }
}