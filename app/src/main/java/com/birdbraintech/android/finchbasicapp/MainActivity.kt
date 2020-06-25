package com.birdbraintech.android.finchbasicapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random

class MainActivity : AppCompatActivity(), Finch.FinchListener {

    /* You must put this in every activity that uses the Finch. Just trust me. */
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


    var toggle = false
    fun testButtonClicked(view: View) {
        if (toggle) {
            finch?.resetEncoders()
            finch?.stopAll()
        } else {
            finch?.setDisplay(
                arrayOf(
                    1,
                    0,
                    0,
                    0,
                    0,
                    1,
                    0,
                    0,
                    0,
                    0,
                    1,
                    0,
                    0,
                    0,
                    0,
                    1,
                    0,
                    0,
                    0,
                    0,
                    1,
                    0,
                    0,
                    0,
                    0
                )
            )
            finch?.setTurn("R", 90.0, 50)
       finch?.setBeak(Random.nextInt(0,100),Random.nextInt(0,100),Random.nextInt(0,100))
        }
        toggle = !toggle
//        finch?.setTail(Random.nextInt(1,4),Random.nextInt(0,100),Random.nextInt(0,100),Random.nextInt(0,100))
//        finch?.playNote(60,1.0)
    }

    override fun onConnected() {
        /* This is where you would handle anything you want to do if the Finch connects (for instance, if you are handling
        reconnection).
         */
    }
    override fun onDisconnected() {
        /* This is where you would handle anything you want to do if the Finch disconnects. */
    }

    override fun onData() {
        this.runOnUiThread(java.lang.Runnable({
            this.distanceNumber.text = finch?.inputState?.distance.toString() + " cm"
            val lightSensors = finch?.correctLightSensorValues()
            this.lightNumbers.text = "(" + lightSensors?.get(0).toString() + ", " +
                    lightSensors?.get(1).toString() + ")"

            this.lineNumbers.text = "(" + finch?.inputState?.leftLine.toString() + ", " +
                    finch?.inputState?.rightLine.toString() + ")"
            this.encoderNumbers.text = "(" + finch?.inputState?.leftEncoder.toString() + ", " +
                    finch?.inputState?.rightEncoder.toString() + ")"
            this.accelerationNumbers.text =
                "(" + finch?.inputState?.acceleration?.get(0).toString() + ", " +
                        finch?.inputState?.acceleration?.get(1).toString() + ", " +
                        finch?.inputState?.acceleration?.get(2).toString() + ")"
            this.magnetometerNumbers.text =
                "(" + finch?.inputState?.magnetometer?.get(0).toString() + ", " +
                        finch?.inputState?.magnetometer?.get(1).toString() + ", " +
                        finch?.inputState?.magnetometer?.get(2).toString() + ")"
            this.compassNumber.text = finch?.inputState?.compass?.toString() + "°"
            this.buttonValues.text = "(" + finch?.inputState?.buttonA.toString() + ", " +
                    finch?.inputState?.buttonB.toString() + ", " +
                    finch?.inputState?.shake.toString() + ")"
            this.batteryNumber.text = finch?.inputState?.batteryVoltage.toString() + " V"
            this.movementValue.text = finch?.inputState?.movementFlag.toString()
        }))

    }
}
