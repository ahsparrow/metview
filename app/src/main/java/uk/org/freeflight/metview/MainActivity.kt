package uk.org.freeflight.metview

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_refresh -> {
            refreshMet()
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun refreshMet() {
        val tempTextView = findViewById<TextView>(R.id.temperatureTextView)
        val windTextView = findViewById<TextView>(R.id.windTextView)
        val gustTextView = findViewById<TextView>(R.id.gustTextView)
        val minTempTextView = findViewById<TextView>(R.id.minTempTextView)
        val maxTempTextView = findViewById<TextView>(R.id.maxTempTextView)
        val maxGustTextView = findViewById<TextView>(R.id.maxGustTextView)

        val queue = RequestQueueSingleton(this)
        val url = "http://metcloud.freeflight.org.uk/"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
                Response.Listener { response ->
                    tempTextView.text = String.format("%.1f", response.optDouble("temp", 0.0))
                    windTextView.text = String.format("%.0f", mph(response.optDouble("wind", 0.0)))
                    gustTextView.text = String.format("%.0f", mph(response.optDouble("gust", 0.0)))
                    minTempTextView.text = String.format("%.1f", response.optDouble("min_temp", 0.0))
                    maxTempTextView.text = String.format("%.1f", response.optDouble("max_temp", 0.0))
                    maxGustTextView.text = String.format("%.0f", mph(response.optDouble("max_gust", 0.0)))
                },
                Response.ErrorListener {
                }
        )

        jsonObjectRequest.setShouldCache(false)
        queue.addToRequestQueue(jsonObjectRequest)
    }

    private fun mph(val_ms: Double): Double {
        return val_ms * 2.237
    }
}