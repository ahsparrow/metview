package uk.org.freeflight.metview

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.AppWidgetTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition

private const val TAG = "METVIEW"

@GlideModule
class AppGlideModule : AppGlideModule()

/**
 * Implementation of App Widget functionality.
 */
class MetViewWidget : AppWidgetProvider() {
    private var alarmMgr: AlarmManager? = null
    private lateinit var updateIntent: PendingIntent

    override fun onEnabled(context: Context)
    {
        Log.v(TAG, "onEnabled")
        super.onEnabled(context)

        alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        updateIntent = Intent(context, UpdateIntentService::class.java)
            .let { intent ->
                PendingIntent.getService(context, 0, intent, 0)
            }

        alarmMgr?.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime(),
            AlarmManager.INTERVAL_FIFTEEN_MINUTES,
            updateIntent
        )

        context.startService(Intent(context, UpdateIntentService::class.java))
    }

    override fun onDisabled(context: Context) {
        Log.v(TAG, "onDisabled")
        super.onDisabled(context)

        alarmMgr?.cancel(updateIntent)
    }

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

    class UpdateIntentService : IntentService("TestIntentService") {
        override fun onHandleIntent(intent: Intent?) {
            Log.v(TAG, "onHandleIntent")

            val updateViews = buildUpdate(this)

            val thisWidget = ComponentName(this, MetViewWidget::class.java)
            val manager = AppWidgetManager.getInstance(this)
            manager.updateAppWidget(thisWidget, updateViews)
        }

        private fun buildUpdate(context: Context) : RemoteViews
        {
            val views = RemoteViews(context.packageName, R.layout.met_view_widget)
            val thisWidget = ComponentName(this, MetViewWidget::class.java)

            val awt: AppWidgetTarget = object: AppWidgetTarget(context, R.id.appwidget_image, views, thisWidget) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    super.onResourceReady(resource, transition)
                }
            }

            Log.v(TAG, "Glide")
            GlideApp
                .with(context.applicationContext)
                .asBitmap()
                .load("http://metcloud.freeflight.org.uk/images/summary")
                .listener(object: RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean) : Boolean {
                        Log.e("TAG", "Error loading image")
                        return false
                    }

                    override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean) : Boolean {
                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .fitCenter()
                .into(awt)

            return views
        }
    }
}