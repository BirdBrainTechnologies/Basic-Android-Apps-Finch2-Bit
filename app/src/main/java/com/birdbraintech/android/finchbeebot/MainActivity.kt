package com.birdbraintech.android.finchbeebot

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.birdbraintech.android.finchbeebot.Finch.Finch
import com.birdbraintech.android.finchbeebot.Finch.FinchApplication
import java.util.*

/* This app lets users push arrow buttons on the screen of the phone or tablet to write a
"program" for the Finch. Then the user pressed the green play button to make the Finch run the
program. This is similar to how the BeeBot works. */
/*  This file contains the main logic for the app. As the user presses buttons, movements are
stored in an array. When the user presses the play button, the app moves through the array and
the Finch performs each movement. This app provides a demonstration of how to send a position
control movement to the Finch and wait for the Finch to finish that movement before you go on to
 the next one. */
class MainActivity : AppCompatActivity(), Finch.FinchListener {

    /* In every activity that uses the Finch, you should define a Finch variable that is equal to
    * the Finch variable we declared in FinchApplication. */
    val finch: Finch?
        get() = (application as FinchApplication).finch


    /* Custom type for defining the movements in the array of movements. */
    enum class FinchMovements {
        FORWARD, BACKWARD, LEFT, RIGHT
    }

    var movements: List<FinchMovements> = emptyList()   // Movements the user has selected

    /* These variables are used to monitor the status of the Finch setMove() and setTurn()
    commands as the user's program plays. This is necessary to make sure one command is complete
    before the next one is sent. Otherwise, the second command will overwrite the first. */
    var programRunning = false      // Whether the play button is running a program
    var movementSent = false        // Whether a Bluetooth command has been sent
    var movementStarted = false     // Whether a movement has started as a result of the Bluetooth command
    var movementFinished = false    // Whether the movement that was started has finished

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

    /* The next four functions are called when the user taps the buttons to create their program.
     We only add a movement to the array when there is not a program running. */
    fun upButtonClicked(view: View) {
        if (!programRunning) {
            movements += FinchMovements.FORWARD
        }
        Log.d("Finch",movements.count().toString())
    }

    fun downButtonClicked(view: View) {
        if (!programRunning) {
            movements += FinchMovements.BACKWARD
        }
    }

    fun leftButtonClicked(view: View) {
        if (!programRunning) {
            movements += FinchMovements.LEFT
        }
    }

    fun rightButtonClicked(view: View) {
        if (!programRunning) {
            movements += FinchMovements.RIGHT
        }
    }

    /* This function stops the Finch and emptys the movement array to end the user's program. */
    fun stopButtonClicked(view: View) {
        movements = emptyList()
        finch?.stop()
    }

    /* This function runs all the stored movements when the user taps the play button. */
    fun playButtonClicked(view: View) {
        if (!programRunning) {    // ignore button press if program already running
            programRunning = true

            /* We set up a timer that will call itself repeatedly until all the movements are complete. */
            val timer = Timer()
            val task = object: TimerTask() {
                override fun run() {
                    if (!movementSent) {   // If we haven't sent a movement that we are waiting to complete
                        if (movements.size > 0) { // If there is another movement to send
                            when (movements.first()) {     // Send the Bluetooth command
                                FinchMovements.FORWARD -> finch?.setMove( "F", 20.0,
                                    50)
                                FinchMovements.BACKWARD -> finch?.setMove( "B", 20.0,
                                    50)
                                FinchMovements.LEFT -> finch?.setTurn( "L", 90.0,
                                    50)
                                FinchMovements.RIGHT -> finch?.setTurn( "R", 90.0,
                                    50)
                                else -> Log.e("Finch","Error: Not a valid direction")
                            }
                            movementSent = true
                        } else {    // We have finished running all the movements!
                            programRunning = false
                            timer.cancel()     // This stops the timer from calling itself any more
                            timer.purge()
                        }
                    } else if (movementSent && !movementStarted) {
                        /* Once we have sent a movement, there is a delay before the Finch receives that command and starts to move. We know the Finch has started moving when the movementFlag is ture. Keep resetting movementStarted until the movementFlag is true. */
                        movementStarted = (finch?.sensorState?.movementFlag == true)
                    } else if (movementStarted && !movementFinished) {
                        /* Once a movement has started, then we have to wait for the movementFlag to turn back to false to indicate that it has finished. Keep resetting movementFinished until it is true. */
                        movementFinished = (finch?.sensorState?.movementFlag == false)
                    } else if (movementFinished) {     // Current movement has finished
                        // Remove the completed movement from the array and set all our flags back to false.
                        Log.d("Finch",movements.size.toString())
                        if (movements.size > 0) {movements = movements.drop(1)}  // remove the
                        // first
                        // element in the array
                        movementSent = false
                        movementStarted = false
                        movementFinished = false
                    } else {}
                }
            }
            timer.schedule(task, 0, 1)

        }
    }

    override fun onConnected() {
        /* This is where you would handle anything you want to do if the Finch connects (for instance, if you are handling
        reconnection).
         */
    }
    override fun onDisconnected() {
        /* This is where you would handle anything you want to do if the Finch disconnects. */
        Log.d("Finch", "disconnected")
    }

    /* This is the function that is called when the Finch has new sensor data. That data is in
    the inputState variable. */
    override fun onData() {
        // Not doing anything here in this app
    }
}
