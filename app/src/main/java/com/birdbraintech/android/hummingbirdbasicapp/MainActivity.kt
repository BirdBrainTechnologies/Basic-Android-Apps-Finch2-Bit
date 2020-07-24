package com.birdbraintech.android.hummingbirdbasicapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.birdbraintech.android.hummingbirdbasicapp.Hummingbird.Hummingbird
import com.birdbraintech.android.hummingbirdbasicapp.Hummingbird.HummingbirdApplication
import kotlinx.android.synthetic.main.activity_main.*

/* This controls the main screen of the app that allows you to control the lights and motors of
the Hummingbird. It also displays the Hummingbird sensor values and the connection status. */
class MainActivity : AppCompatActivity(), Hummingbird.HummingbirdListener {

    /* In every activity that uses the Hummingbird, you should define a Hummingbird variable that is equal to
    * the Hummingbird variable we declared in HummingbirdApplication. */
    val hummingbird: Hummingbird?
        get() = (application as HummingbirdApplication).hummingbird

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
            /* Set the values of the TextViews for the sensors. */
            this.distanceNumber.text = hummingbird?.sensorState?.sensor1?.distance.toString() + " cm"
            this.lightNumbers.text = hummingbird?.sensorState?.sensor2?.light.toString()
            this.lineNumbers.text = hummingbird?.sensorState?.sensor3?.dial.toString()
            this.accelerationNumbers.text =
                "(" + hummingbird?.sensorState?.acceleration?.get(0).toString() + ", " +
                        hummingbird?.sensorState?.acceleration?.get(1).toString() + ", " +
                        hummingbird?.sensorState?.acceleration?.get(2).toString() + ")"
            this.magnetometerNumbers.text =
                "(" + hummingbird?.sensorState?.magnetometer?.get(0).toString() + ", " +
                        hummingbird?.sensorState?.magnetometer?.get(1).toString() + ", " +
                        hummingbird?.sensorState?.magnetometer?.get(2).toString() + ")"
            this.compassNumber.text = hummingbird?.sensorState?.compass?.toString() + "°"
            this.buttonValues.text = "(" + hummingbird?.sensorState?.buttonA.toString() + ", " +
                    hummingbird?.sensorState?.buttonB.toString() + ", " +
                    hummingbird?.sensorState?.shake.toString() + ")"
            this.batteryNumber.text = hummingbird?.sensorState?.batteryVoltage.toString() + " V"
        }))

    }
}
