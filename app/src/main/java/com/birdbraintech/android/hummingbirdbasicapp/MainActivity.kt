package com.birdbraintech.android.hummingbirdbasicapp

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.birdbraintech.android.hummingbirdbasicapp.Hummingbird.Hummingbird
import com.birdbraintech.android.hummingbirdbasicapp.Hummingbird.HummingbirdApplication
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt

/* This controls the main screen of the app. It is blank so that you can create your own app. */
class MainActivity : AppCompatActivity(), Hummingbird.HummingbirdListener {

    /* In every activity that uses the Hummingbird, you should define a Hummingbird variable that is equal to
    * the Hummingbird variable we declared in HummingbirdApplication. */
    val hummingbird: Hummingbird?
        get() = (application as HummingbirdApplication).hummingbird

    var lightsOn = false    // whether or not single color LEDs 1-3 are on

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Make this the activity that is receiving Hummingbird data */
        hummingbird?.setHummingbirdListener(this)

    }


    /* This Bluetooth does not disconnect automatically, so I want to disconnect it if this activity stops (since there are no
    other activities in my app.
     */
    override fun onStop() {
        super.onStop()
        hummingbird?.disconnect()
    }


    fun testButtonClicked(view: View) {
        hummingbird?.calibrateCompass()
    }

    override fun onConnected() {
        /* This is where you would handle anything you want to do if the Hummingbird connects (for instance, if you are handling
        reconnection).
         */
    }
    override fun onDisconnected() {
        /* This is where you would handle anything you want to do if the Hummingbird disconnects. */
    }

    /* This is the function that is called when the Hummingbird has new sensor data. That data is in
    the sensorState variable. */
    override fun onData() {
        this.runOnUiThread(java.lang.Runnable({
            // Do whatever you want with sensor data here
        }))

    }
}
