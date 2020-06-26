package com.birdbraintech.android.finchbasicapp

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.birdbraintech.android.finchbasicapp.Finch.Finch
import com.birdbraintech.android.finchbasicapp.Finch.FinchApplication
import kotlinx.android.synthetic.main.activity_main.*

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

        /* Set up the seek bar. */
        val seekBar: SeekBar = findViewById(R.id.seekBar)
        seekBar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                // write custom code for progress is changed on seekbar

            }

            override fun onStartTrackingTouch(seek: SeekBar) {
                // write custom code for progress is started on seekbar
            }

            override fun onStopTrackingTouch(seek: SeekBar) {
                // When progress is stopped on the seekbar, we want to set the lights of the
                // Finch beak and tail based on its value
                when (seek.progress) {
                    in 0..12 -> {
                        finch?.setBeak(100,100,100)
                        finch?.setTail("all",100,100,100)
                    }
                    in 13..24 -> {
                        finch?.setBeak(100,100,0)
                        finch?.setTail("all",100,100,0)
                    }
                    in 25..36 -> {
                        finch?.setBeak(0,100,100)
                        finch?.setTail("all",0,100,100)
                    }
                    in 37..48 -> {
                        finch?.setBeak(0,100,0)
                        finch?.setTail("all",0,100,0)
                    }
                    in 49..60 -> {
                        finch?.setBeak(100,0,100)
                        finch?.setTail("all",100,0,100)
                    }
                    in 61..72 -> {
                        finch?.setBeak(100,0,0)
                        finch?.setTail("all",100,0,0)
                    }
                    in 73..84 -> {
                        finch?.setBeak(0,0,100)
                        finch?.setTail("all",0,0,100)
                    }
                    else -> {
                        finch?.setBeak(0,0,0)
                        finch?.setTail("all",0,0,0)
                    }
                }
            }
        })

        seekBar.progress = 100

    }

    /* This Bluetooth does not disconnect automatically, so I want to disconnect it if this activity stops (since there are no
    other activities in my app.
     */
    override fun onStop() {
        super.onStop()
        finch?.disconnect()
    }

    /* The next four functions are called when the user taps the buttons to make the Finch move. */
    fun upButtonClicked(view: View) {
        finch?.setMove("F",20.0,50)
    }

    fun downButtonClicked(view: View) {
        finch?.setMove("B",20.0,50)
    }

    fun leftButtonClicked(view: View) {
        finch?.setTurn("L",90.0,50)
    }

    fun rightButtonClicked(view: View) {
        finch?.setTurn("R",90.0,50)
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
        /* Set the values of the TextViews for the sensors. */
        this.runOnUiThread(java.lang.Runnable({
            this.distanceNumber.text = finch?.sensorState?.distance.toString() + " cm"
            val lightSensors = finch?.correctLightSensorValues()
            this.lightNumbers.text = "(" + lightSensors?.get(0).toString() + ", " +
                    lightSensors?.get(1).toString() + ")"

            this.lineNumbers.text = "(" + finch?.sensorState?.leftLine.toString() + ", " +
                    finch?.sensorState?.rightLine.toString() + ")"
        }))

    }
}
