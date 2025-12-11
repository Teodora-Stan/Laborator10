
package ro.pub.cs.systems.eim.lab10.siplinphone

import android.Manifest
import android.content.pm.PackageManager
import android.widget.*
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.linphone.core.*

class MainActivity: AppCompatActivity() {
    private lateinit var core: Core

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val factory = Factory.instance()
        val core = factory.createCore(null, null, this)

        val status = findViewById<TextView>(R.id.registration_status)
        status.text = "linphone lib version: " + core.version

        findViewById<Button>(R.id.register).setOnClickListener {
            findViewById<LinearLayout>(R.id.register_layout).visibility = View.GONE
            findViewById<RelativeLayout>(R.id.call_layout).visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.unregister).setOnClickListener {
            findViewById<LinearLayout>(R.id.register_layout).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.call_layout).visibility = View.GONE
        }
    }
}
