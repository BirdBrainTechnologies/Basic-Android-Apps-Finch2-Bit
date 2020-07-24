package com.birdbraintech.android.hummingbirdbasicapp

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.birdbraintech.android.hummingbirdbasicapp.Hummingbird.Hummingbird
import com.birdbraintech.android.hummingbirdbasicapp.Hummingbird.HummingbirdApplication
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt

/* This controls the main screen of the app that allows you to control the lights and motors of
the Hummingbird. It also displays the Hummingbird sensor values and the connection status. */
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

        /* Set up the seek bars. */
        val seekBarColor: SeekBar = findViewById(R.id.seekBarColor)
        seekBarColor?.setOnSeekBarChangeListener(object :
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
                        hummingbird?.setTriLED(1, 100,100,100)
                        hummingbird?.setTriLED(2, 100,100,100)
                    }
                    in 13..24 -> {
                        hummingbird?.setTriLED(1, 100,100,0)
                        hummingbird?.setTriLED(2, 100,100,0)
                    }
                    in 25..36 -> {
                        hummingbird?.setTriLED(1, 0,100,100)
                        hummingbird?.setTriLED(2,0,100,100)
                    }
                    in 37..48 -> {
                        hummingbird?.setTriLED(1, 0,100,0)
                        hummingbird?.setTriLED(2,0,100,0)
                    }
                    in 49..60 -> {
                        hummingbird?.setTriLED(1, 100,0,100)
                        hummingbird?.setTriLED(2, 100,0,100)
                    }
                    in 61..72 -> {
                        hummingbird?.setTriLED(1, 100,0,0)
                        hummingbird?.setTriLED(2, 100,0,0)
                    }
                    in 73..84 -> {
                        hummingbird?.setTriLED(1, 0,0,100)
                        hummingbird?.setTriLED(2, 0,0,100)
                    }
                    else -> {
                        hummingbird?.setTriLED(1, 0,0,0)
                        hummingbird?.setTriLED(2, 0,0,0)
                    }
                }
            }
        })
        seekBarColor.progress = 100

        val seekBarServo1: SeekBar = findViewById(R.id.seekBarServo1)
        seekBarServo1?.setOnSeekBarChangeListener(object :
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
                hummingbird?.setPositionServo(1,(1.8*seek.progress).roundToInt())

            }
        })

        val seekBarServo2: SeekBar = findViewById(R.id.seekBarServo2)
        seekBarServo2?.setOnSeekBarChangeListener(object :
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
                hummingbird?.setPositionServo(2,(1.8*seek.progress).roundToInt())

            }
        })

        val seekBarServo3: SeekBar = findViewById(R.id.seekBarServo3)
        seekBarServo3?.setOnSeekBarChangeListener(object :
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
                hummingbird?.setPositionServo(3,(1.8*seek.progress).roundToInt())

            }
        })

        val seekBarServo4: SeekBar = findViewById(R.id.seekBarServo4)
        seekBarServo4?.setOnSeekBarChangeListener(object :
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
                hummingbird?.setPositionServo(4,(1.8*seek.progress).roundToInt())

            }
        })
    }

    /* This function toggles the single color LEDs when the light button is pressed. */
    fun lightButtonClicked(view: View) {
        if (lightsOn) {
            hummingbird?.setLED(1,0)
            hummingbird?.setLED(2,0)
            hummingbird?.setLED(3,0)
        } else {
            hummingbird?.setLED(1,100)
            hummingbird?.setLED(2,100)
            hummingbird?.setLED(3,100)
        }
        lightsOn = !lightsOn
    }

    /* This function plays a random music note when the music button is pressed. */
    fun musicButtonClicked(view: View) {
        hummingbird?.playNote((32..135).random(),0.5)
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
        }))

    }
}
