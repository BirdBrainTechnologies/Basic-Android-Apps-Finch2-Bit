package com.birdbraintech.android.finchblankapp

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.birdbraintech.android.finchblankapp.Finch.Finch
import com.birdbraintech.android.finchblankapp.Finch.FinchApplication

/* This controls the main screen of the app that allows you to control the lights and motors of
the Finch. It also displays the Finch sensor values and the connection status. */
class MainActivity : AppCompatActivity(), Finch.FinchListener {

    /* In every activity that uses the Finch, you should define a Finch variable that is equal to
    * the Finch variable we declared in FinchApplication. */
    val finch: Finch?
        get() = (application as FinchApplication).finch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /* Make this the activity that is receiving Finch data */
        finch?.setFinchListener(this)

    }

    /* This Bluetooth does not disconnect automatically, so I want to disconnect it if this activity stops (since there are no
    other activities in my app.
     */
    override fun onStop() {
        super.onStop()
        finch?.disconnect()
    }


    override fun onConnected() {
        /* This is where you would handle anything you want to do if the Finch connects (for instance, if you are handling
        reconnection).
         */
    }
    override fun onDisconnected() {
        /* This is where you would handle anything you want to do if the Finch disconnects. */
    }

    /* This is the function that is called when the Finch has new sensor data. That data is in
    the inputState variable. */
    override fun onData() {
        /* This is where you handle what you want to do with new Finch data. */
    }
}
