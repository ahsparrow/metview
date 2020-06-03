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

        val refreshIntent = Intent(context, UpdateIntentService::class.java)
            .let { refreshIntent ->
                PendingIntent.getService(context, 0, refreshIntent, 0)
            }
        refreshIntent.send()

        val appIntent = Intent(context, MainActivity::class.java)
            .let { appIntent ->
                PendingIntent.getActivity(context, 0, appIntent, 0)
            }

        appWidgetIds?.forEach { appWidgetId ->
            val views = RemoteViews(
                context?.packageName,
                R.layout.met_view_widget
            ).apply {
                setOnClickPendingIntent(R.id.appwidget_image_top, refreshIntent)
                setOnClickPendingIntent(R.id.appwidget_image_bottom, appIntent)
            }

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager?.updateAppWidget(appWidgetId, views)
        }
    }

    class UpdateIntentService : IntentService("WidgetIntentService") {
        override fun onHandleIntent(intent: Intent?) {
            Log.v(TAG, "onHandleIntent")

            val imageUrl = "http://metcloud.freeflight.org.uk/images/summary"
            val imageRequest1 = ImageRequest(
                imageUrl,
                Response.Listener { bitmap ->
                    val bitmapTop = Bitmap.createBitmap(bitmap, 0, 0, 512, 256)
                    val bitmapBottom = Bitmap.createBitmap(bitmap, 0, 256, 512, 256)

                    val views = RemoteViews(this.packageName, R.layout.met_view_widget)
                    views.setImageViewBitmap(R.id.appwidget_image_top, bitmapTop)
                    views.setImageViewBitmap(R.id.appwidget_image_bottom, bitmapBottom)

                    val thisWidget = ComponentName(this, MetViewWidget::class.java)
                    val manager = AppWidgetManager.getInstance(this)
                    manager.updateAppWidget(thisWidget, views)
                },
                0, 0,null, Bitmap.Config.RGB_565,
                Response.ErrorListener {}
            )
            imageRequest1.setShouldCache(false)

            val queue = RequestQueueSingleton.getInstance(this).requestQueue
            queue.add(imageRequest1)
        }
    }
}