package com.birdbraintech.android.finchbeebot.Finch
import android.util.Log
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

/* June 1, 2020, Bambi Brewer, BirdBrain Technologies */
/* This is the class that represents the Finch. It includes a data structure that the user can
use to access the values of the Finch sensors, as well as public functions that can be used to
control the Finch motors, lights, and buzzer. */

class Finch(var finchConnection: UARTConnection): UARTConnection.DataListener, UARTConnection.ConnectionListener {

    /* To get data and connection/disconnection information from the Finch, you must implement the
    FinchListener protocol and add a listener. */
    private var finchListener: FinchListener? = null

    fun setFinchListener(l: FinchListener) {
        finchListener = l
    }

    /* These are the functions that your activity must implement in order to set yourself as the
    FinchListener.
     */
    interface FinchListener {
        /* This function determines what happens when someone connects to the Finch. Useful if
        you want to implement reconnection.
         */
        fun onConnected() {}

        /* This function is called when the Finch becomes disconnected. */
        fun onDisconnected()

        /* This function is called when the Finch has new data. */
        fun onData()
    }

    /* This companion object defines all the constants that we need for the Finch class. */
    object Constants {
        const val expectedRawStateByteCount = 20   // Number of bytes in a Finch data packet

        const val batteryVoltageConversionFactor: Float = 0.00937F
        const val cmPerDistance = 0.0919   // Converting encoder ticks to distance in cm
        const val ticksPerCM = 49.7        // Converting distance in cm to encoder ticks
        const val ticksPerDegree = 4.335   // For converting encoder ticks to the angle the Finch has turned
        const val ticksPerRotation = 792.0 // For converting encoder ticks to the number of rotations of the Finch wheel

        /* These variables tell you the indices that identify different values in a Bluetooth data
         packet sent by the Finch. */
        const val distanceMSB = 0      // MSB = most significant byte
        const val distanceLSB = 1      // LSB = least significant byte
        const val leftLight = 2
        const val rightLight = 3
        const val leftLine = 4         // Byte 4 contains two things
        const val movementFlag = 4
        const val rightLine = 5
        const val battery = 6
        const val leftEncoder = 7      // 3 bytes
        const val rightEncoder = 10    // 3 bytes
        const val accX = 13            // 3 bytes
        const val buttonShake = 16     // Contains bits for both micro:bit buttons and the shake
        const val magX = 17            // 3 bytes
    }

    /* This structure contains the raw bytes sent by the Finch over Bluetooth, along with a timestamp for the data. */
    data class RawInputState(val data: ByteArray) {
        val timestamp: Date = Date()
        var isStale: Boolean = false // is the data old?
    }

    /* This structure contains the processed values of the Finch sensors. This is the data type
    that you will use in your program to hold the state of the Finch inputs. */
    class SensorState(private val rawState: RawInputState) {

        /* These functions are used to transform the raw Finch data into values the user can
        understand. This may involve scaling, converting the data, or manipulating bytes to deal
        with values that are encoded in more that one byte in the raw data. Do not change these
        functions unless you are very sure that you understand the Bluetooth protocol. */

        private fun parseBatteryVoltage(rawStateData: ByteArray): Float {
            val battery: Byte = rawStateData[Constants.battery]
            return (battery.toFloat() + 320)*Constants.batteryVoltageConversionFactor
        }

        private fun parseAccelerationMagnetometerCompass(rawStateData: ByteArray): Triple<Array<Double>, Array<Double>, Int?> {
            val rawAcc = byteArrayOf(rawStateData[Constants.accX],rawStateData[Constants.accX + 1],rawStateData[Constants.accX + 2])
            val accValues =
                RawToFinchAccl(
                    rawAcc
                )
            //val accValues = [rawToAccelerometer(rawFinchAcc[0]), rawToAccelerometer(rawFinchAcc[1]), rawToAccelerometer(rawFinchAcc[2])]

            val rawMag = byteArrayOf(rawStateData[Constants.magX],rawStateData[Constants.magX + 1],rawStateData[Constants.magX + 2])
            val finchMag =
                RawToFinchMag(
                    rawMag
                )

            var compass:Int? = null
            compass =
                DoubleToCompass(
                    accValues,
                    finchMag
                )
            if (compass != null) {
                //turn it around so that the finch beak points north at 0
                compass = (compass + 180) % 360
            }

            return Triple(accValues, finchMag, compass)
        }

        private fun parseEncoders(rawStateData: ByteArray): Array<Double> {
            val leftValues = byteArrayOf(rawStateData[Constants.leftEncoder],rawStateData[Constants.leftEncoder + 1],rawStateData[Constants.leftEncoder + 2])
            val rightValues =  byteArrayOf(rawStateData[Constants.rightEncoder],rawStateData[Constants.rightEncoder + 1],rawStateData[Constants.rightEncoder + 2])

            //3 bytes is a 24bit int which is not a type in kotlin. Therefore, we shove the bytes over such that the sign will be carried over correctly when converted and then divide to go back to 24bit.
            val msbL: Int = rawStateData[Constants.leftEncoder].toInt() and 0xFF
            val ssbL: Int = rawStateData[Constants.leftEncoder + 1].toInt() and 0xFF
            val lsbL: Int = rawStateData[Constants.leftEncoder + 2].toInt() and 0xFF

            val unsignedL = (msbL shl 16) + (ssbL shl 8) + lsbL
            val signedL = unsignedL shl 8 shr 8

            val leftRotations = signedL.toDouble()/Constants.ticksPerRotation

            val msbR: Int = rawStateData[Constants.rightEncoder].toInt() and 0xFF
            val ssbR: Int = rawStateData[Constants.rightEncoder + 1].toInt() and 0xFF
            val lsbR: Int = rawStateData[Constants.rightEncoder + 2].toInt() and 0xFF

            val unsignedR = (msbR shl 16) + (ssbR shl 8) + lsbR
            val signedR = unsignedR shl 8 shr 8

            val rightRotations = signedR.toDouble()/Constants.ticksPerRotation

            return arrayOf(leftRotations, rightRotations)

        }

        private fun parseLine(rawStateData: ByteArray): Array<Int> {
            val rightLineDouble = (rawStateData[Constants.rightLine].toDouble()-6.0)*100.0/121.0
            val rightLineInt = (100 - Math.round(rightLineDouble)).toInt()

            var leftLineDouble = rawStateData[Constants.leftLine].toDouble()
            //the value for the left line sensor also contains the move flag
            if (leftLineDouble > 127) { leftLineDouble -= 128 }
            leftLineDouble = (leftLineDouble - 6.0)*100.0/121.0
            val leftLineInt = (100 - Math.round(leftLineDouble)).toInt()

            return arrayOf(leftLineInt, rightLineInt)
        }

        private fun parseMovementFlag(rawStateData: ByteArray): Boolean {

            val dataValue = rawStateData[Constants.movementFlag].toInt() and 0xFF
            // If this value is greater than 127, it means that the Finch is still
            // running a position control movement
            return (dataValue > 127)
        }

        private fun parseDistance(rawStateData: ByteArray): Int {
            val msb = rawStateData[Constants.distanceMSB].toInt() and 0xFF
            val lsb = rawStateData[Constants.distanceLSB].toInt() and 0xFF
            val distance = (msb shl 8) + lsb
            return Math.round(distance.toDouble()*Constants.cmPerDistance).toInt()
        }

        /* These are the variables that hold the sensor values that you will use in your programs. */
        val timestamp: Date = rawState.timestamp
        var batteryVoltage: Float
        val isStale: Boolean = rawState.isStale
        var distance: Int
        var leftLight: Int
        var rightLight: Int
        var leftLine: Int
        var rightLine: Int
        var leftEncoder: Double
        var rightEncoder: Double
        var acceleration: Array<Double>
        var magnetometer: Array<Double>
        var compass: Int?    // Undefined in the case of 0 z direction acceleration
        var buttonA: Boolean
        var buttonB: Boolean
        var shake: Boolean
        var movementFlag: Boolean  // True when the Finch is executing a movement command. You
        // need to watch this flag if you don't want to start another Finch movement until the
        // first one is finished.

        /* This struct is initialized based on the raw sensor data. */
        init {
            batteryVoltage = parseBatteryVoltage(rawState.data)
            distance = parseDistance(rawState.data)
            leftLight = rawState.data[Constants.leftLight].toInt() and 0xFF
            rightLight = rawState.data[Constants.rightLight].toInt() and 0xFF
            val lineSensors = parseLine(rawState.data)
            leftLine = lineSensors[0]
            rightLine = lineSensors[1]
            val tripleAcc = parseAccelerationMagnetometerCompass(rawState.data)
            acceleration = tripleAcc.first
            magnetometer = tripleAcc.second
            compass = tripleAcc.third

            val encoders = parseEncoders(rawState.data)
            leftEncoder = encoders[0]
            rightEncoder = encoders[1]

            val rawButtonShakeValue = rawState.data[Constants.buttonShake].toInt()
            buttonA = (((rawButtonShakeValue shr 4) and 0x1) == 0x0)
            buttonB = (((rawButtonShakeValue shr 5) and 0x1) == 0x0)
            shake = ((rawButtonShakeValue and 0x1) != 0x0)

            movementFlag = parseMovementFlag(rawState.data)

        }
    }

    /* This is the sensor data as it comes from the Finch in a 20-byte packet. It has to be
    decoded to provide meaningful information to the user. */
    private var rawInputState: RawInputState? = null
    var inputState: SensorState? = null
        get(): SensorState? {
            return if (rawInputState == null) null else SensorState(rawInputState!!)
        }

    /* This structure keeps track of the current values of the Finch beak and tail lights. It is
     used by setLightsAndBuzzer so that the lights don't get turned off when we send a command
     to the buzzer. */
    private class LightState {
        var beakColor = byteArrayOf(0,0,0)
        var tailColor1 = byteArrayOf(0,0,0)
        var tailColor2 = byteArrayOf(0,0,0)
        var tailColor3 = byteArrayOf(0,0,0)
        var tailColor4 = byteArrayOf(0,0,0)
    }

    private var lightState = LightState()   /* Remembers what the Finch beak and tail lights are set to. */

    /* This function sends a Bluetooth command to set the lights and buzzer of the Finch. The
    lights are set from the Finch output state so that they remain unchanged until the user sets
    them to something different. The buzzer, on the other hand, is set from input parameters,
    because we only want to play each note once. */
    private fun setLightsAndBuzzer(buzzerPeriod: Int, buzzerDuration: Int) {

        val letter = 0xD0.toByte()

        var buzzerArray= byteArrayOf()
        // Time_us_MSB, Time_us_LSB, Time_ms_MSB, Time_ms_LSB
        buzzerArray += (buzzerPeriod shr 8).toByte()
        buzzerArray += (buzzerPeriod and 0x00ff).toByte()
        buzzerArray += (buzzerDuration shr 8).toByte()
        buzzerArray += (buzzerDuration and 0x00ff).toByte()

        val array= byteArrayOf(letter,
            lightState.beakColor[0],
            lightState.beakColor[1],
            lightState.beakColor[2],
            lightState.tailColor1[0],
            lightState.tailColor1[1],
            lightState.tailColor1[2],
            lightState.tailColor2[0],
            lightState.tailColor2[1],
            lightState.tailColor2[2],
            lightState.tailColor3[0],
            lightState.tailColor3[1],
            lightState.tailColor3[2],
            lightState.tailColor4[0],
            lightState.tailColor4[1],
            lightState.tailColor4[2],
            buzzerArray[0],
            buzzerArray[1],
            buzzerArray[2],
            buzzerArray[3])

        finchConnection.writeBytes(array)

    }


    /* This function sends the Bluetooth command to set the Finch to move at particular speeds
    for the left and right wheels. The Finch will move until each wheel reaches a specified
    number of ticks. Then it will stop. */
    private fun sendPositionControlCommand(leftSpeed: Int, rightSpeed: Int, leftTicks: Int, rightTicks: Int) {

        val lTicksMSB = (leftTicks shr 16).toByte()
        val lTicksSSB = ((leftTicks and 0x00ff00) shr 8).toByte()
        val lTicksLSB = (leftTicks and 0x0000ff).toByte()

        val rTicksMSB = (rightTicks shr 16).toByte()
        val rTicksSSB = ((rightTicks and 0x00ff00) shr 8).toByte()
        val rTicksLSB = (rightTicks and 0x0000ff).toByte()

        val leftConvertedSpeed1 = ((36.0 * leftSpeed).roundToInt() /100.0).toInt()
        val leftConvertedSpeed2 =
            convertVelocity(
                leftConvertedSpeed1
            )

        val rightConvertedSpeed1 = ((36.0 * rightSpeed).roundToInt() /100.0).toInt()
        val rightConvertedSpeed2 =
            convertVelocity(
                rightConvertedSpeed1
            )

        val array = byteArrayOf(0xD2.toByte(),0x40.toByte(),leftConvertedSpeed2,lTicksMSB,lTicksSSB,lTicksLSB,rightConvertedSpeed2,rTicksMSB,rTicksSSB,rTicksLSB)
        finchConnection.writeBytes(array)
    }

    /* This function sends the Bluetooth command to set the left and right motors to the
    specified speeds. The motors will stay on at these values until they receive another motor
    command. */
    private fun sendVelocityControlCommand(leftSpeed: Int, rightSpeed: Int) {
        val leftConvertedSpeed1 = ((36.0 * leftSpeed).roundToInt() /100.0).toInt()
        val leftConvertedSpeed2 =
            convertVelocity(
                leftConvertedSpeed1
            )

        val rightConvertedSpeed1 = ((36.0 * rightSpeed).roundToInt() /100.0).toInt()
        val rightConvertedSpeed2 =
            convertVelocity(
                rightConvertedSpeed1
            )

        val array = byteArrayOf(0xD2.toByte(),0x40.toByte(),leftConvertedSpeed2,0,0,0,rightConvertedSpeed2,0,0,0)
        finchConnection.writeBytes(array)

    }

    /* This function turns off all the Finch motors, lights, and buzzer. */
    private fun sendStopAllCommand() {
        val command = byteArrayOf(0xDF.toByte())

        finchConnection.writeBytes(command)
    }

    /* This function sends a Bluetooth command to print a string on the micro:bit LED display. */
    private fun sendPrintCommand(stringToPrint: String) {

        val letter = 0xCC.toByte()
        val ledStatusChars = stringToPrint.toCharArray()
        var length = ledStatusChars.size
        if (length > 18) { // can't send strings longer than 18 characters
            length = 18
            print("Error: Cannot print strings longer than 18 characters.")
        }
        val flash = (64 + length).toByte()
        var commandArray = byteArrayOf(letter, flash)
        for (i in 0 until length) {
            commandArray += ledStatusChars[i].toByte()
        }

        finchConnection.writeBytes(commandArray)
    }

    /* This function send the Bluetooth command to display a particular pattern on the micro:bit array. */
    private fun sendLEDArrayCommand(pattern: String) {
        if (pattern.length != 25) {
            print("Error: LED Array pattern must contain 25 characters")
        } else {
            var intArray = IntArray(25)
            for (i in pattern.indices) {
                intArray[i] = (pattern[i].toString() + "").toInt()
            }
            val byte25 =
                ConstructByteFromInts(
                    intArray,
                    24,
                    25
                )
            val byte17to24 =
                ConstructByteFromInts(
                    intArray,
                    16,
                    24
                )
            val byte9to16 =
                ConstructByteFromInts(
                    intArray,
                    8,
                    16
                )
            val byte1to8 =
                ConstructByteFromInts(
                    intArray,
                    0,
                    8
                )

            val ledArrayCommand = byteArrayOf(
                0xD2.toByte(),
                0x20.toByte(),
                byte25,
                byte17to24,
                byte9to16,
                byte1to8
            )
            finchConnection.writeBytes(ledArrayCommand)
        }

    }

    /* This function sets the color of the Finch beak. The red, green, and blue parameters must
    be between 0 and 100. */
    fun setBeak(red: Int, green: Int, blue: Int) {

        lightState.beakColor = byteArrayOf((clampToBounds(
            red,
            0,
            100
        )).toByte(), (clampToBounds(
            green,
            0,
            100
        )).toByte(), (clampToBounds(
            blue,
            0,
            100
        )).toByte())

        // Want to change lights without playing the buzzer
        setLightsAndBuzzer(0, 0)
    }

    /* This function sets the color of the Finch tail if you have specified a single tail light
    (the function is also overloaded to control them all at once). The port is 1, 2, 3, or 4 and
    red, green, and blue must be between 0 and 100. */
    fun setTail(port: Int, red: Int, green: Int, blue: Int) {

        when (port) {
            1 -> lightState.tailColor1 = byteArrayOf((clampToBounds(
                red,
                0,
                100
            )).toByte(), (clampToBounds(
                green,
                0,
                100
            )).toByte(), (clampToBounds(
                blue,
                0,
                100
            )).toByte())
            2 -> lightState.tailColor2 = byteArrayOf((clampToBounds(
                red,
                0,
                100
            )).toByte(), (clampToBounds(
                green,
                0,
                100
            )).toByte(), (clampToBounds(
                blue,
                0,
                100
            )).toByte())
            3 -> lightState.tailColor3 = byteArrayOf((clampToBounds(
                red,
                0,
                100
            )).toByte(), (clampToBounds(
                green,
                0,
                100
            )).toByte(), (clampToBounds(
                blue,
                0,
                100
            )).toByte())
            4 -> lightState.tailColor4 = byteArrayOf((clampToBounds(
                red,
                0,
                100
            )).toByte(), (clampToBounds(
                green,
                0,
                100
            )).toByte(), (clampToBounds(
                blue,
                0,
                100
            )).toByte())
            else -> {
                Log.e("Finch", "Error: Invalid port for setTail()")
                return
            }
        }

        // Want to change lights without playing the buzzer
        setLightsAndBuzzer(0, 0)
    }

    /* This function sets the color of the Finch tail if you have specified "all" the tail lights
     (the function is also overloaded to control individual lights). The red, green, and blue
     parameters must be between 0 and 100. */
    fun setTail(port: String, red: Int, green: Int, blue: Int) {

        if (port == "all") {
            lightState.tailColor1 = byteArrayOf((clampToBounds(
                red,
                0,
                100
            )).toByte(), (clampToBounds(
                green,
                0,
                100
            )).toByte(), (clampToBounds(
                blue,
                0,
                100
            )).toByte())
            lightState.tailColor2 = byteArrayOf((clampToBounds(
                red,
                0,
                100
            )).toByte(), (clampToBounds(
                green,
                0,
                100
            )).toByte(), (clampToBounds(
                blue,
                0,
                100
            )).toByte())
            lightState.tailColor3 = byteArrayOf((clampToBounds(
                red,
                0,
                100
            )).toByte(), (clampToBounds(
                green,
                0,
                100
            )).toByte(), (clampToBounds(
                blue,
                0,
                100
            )).toByte())
            lightState.tailColor4 = byteArrayOf((clampToBounds(
                red,
                0,
                100
            )).toByte(), (clampToBounds(
                green,
                0,
                100
            )).toByte(), (clampToBounds(
                blue,
                0,
                100
            )).toByte())

            // Want to change lights without playing the buzzer
            setLightsAndBuzzer(0,0)
        } else {
            print("Error: Invalid input for setTail()")
        }
    }

    /* This function plays a note on the Finch buzzer. We do not save this to the output state of
     the Finch, because we want it to play just once. Notes are MIDI notes (32-135) and beats
     must be between 0-16. Each beat is 1 second. */
    fun playNote(note: Int, beats: Double) {
        val noteInBounds =
            clampToBounds(
                note,
                32,
                135
            )
        val beatsInBounds =
            clampToBounds(
                beats,
                0.0,
                16.0
            )

        //duration of buzz in ms - 60bpm, so each beat is 1 s
        val duration = (1000*beatsInBounds).toInt()

        val period =
            noteToPeriod((noteInBounds).toByte()) //the period of the note in us
        if (period != null) {
            // Want to set the lights and the buzzer. Lights will be set based on the Finch output state
            setLightsAndBuzzer(period, duration)
        }

    }

    /* This function moves the Finch forward or back a given distance (in centimeters) at a given speed (-100 to 100%). */
    fun setMove(direction: String, distance: Double, speed: Int) {
        var speedCorrect =
            clampToBounds(
                speed,
                -100,
                100
            )
        if ((direction == "F") || (direction == "B")) {
            if (direction == "B") {
                speedCorrect = -1*speedCorrect
            }
        } else {
            print("Error: Invalid direction for call to setMove()")
        }
        val distanceInTicks = (abs(distance * Constants.ticksPerCM).roundToInt()).toInt()

        sendPositionControlCommand(speedCorrect, speedCorrect, distanceInTicks, distanceInTicks)
    }

    /* This function turns the Finch left or right a given angle (in degrees) at a given speed (-100 to 100%). */
    fun setTurn(direction: String, angle: Double, speed: Int) {
        var speedCorrectLeft =
            clampToBounds(
                speed,
                -100,
                100
            )
        var speedCorrectRight = -1*speedCorrectLeft
        if ((direction == "R") || (direction == "L")) {
            if (direction == "L") {
                speedCorrectLeft = -1*speedCorrectLeft
                speedCorrectRight = -1*speedCorrectRight
            }
        } else {
            print("Error: Invalid direction for call to setMove()")
        }

        val angleInTicks = (abs(angle * Constants.ticksPerDegree).roundToInt()).toInt()

        sendPositionControlCommand(speedCorrectLeft, speedCorrectRight, angleInTicks, angleInTicks)
    }

    /* This function sets the speed of the left and right motors to values between -100 and 100.
    The motors will stay on at these values until you stop them with stop() or stopAll() or call
    setMove(), setTurn(), or set Motors(). */
    fun setMotors(leftSpeed: Int, rightSpeed: Int) {
        sendVelocityControlCommand(
            clampToBounds(
                leftSpeed,
                -100,
                100
            ),
            clampToBounds(
                rightSpeed,
                -100,
                100
            )
        )
    }

    /* This function stops the Finch motors. */
    fun stop() {
        sendVelocityControlCommand(0, 0)
    }

    /* This function send a Bluetooth command to calibrate the compass. When the Finch receives
    this command, it will place dots on the micro:bit screen as it waits for you to tilt the
    Finch in different directions. If the calibration is successful, you will then see a check on
     the micro:bit screen. Otherwise, you will see an X. */
    fun calibrateCompass() {
        val command: ByteArray = byteArrayOf(0xCE.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

        finchConnection.writeBytes(command)
    }

    /* The Finch light sensors are slightly affected by the value of the beak. It is a fairly
    small effect, but if you want, you can use this function to correct them */
    fun correctLightSensorValues(): Array<Int?> {
        val beak = lightState.beakColor
        val R = (beak[0]).toDouble()
        val G = (beak[1]).toDouble()
        val B = (beak[2]).toDouble()
        var lightLeftCorrected: Int? = null
        var lightRightCorrected: Int? = null
        val currentInputState = inputState
        if (inputState != null) {
            var lightLeft = (inputState!!.leftLight).toDouble()
            var lightRight = (inputState!!.rightLight).toDouble()

            lightLeft -= 1.06871493e-02*R +  1.94526614e-02*G +  6.12409825e-02*B +  4.01343475e-04*R*G + 4.25761981e-04*R*B +  6.46091068e-04*G*B - 4.41056971e-06*R*G*B
            lightRight -= 6.40473070e-03*R +  1.41015162e-02*G +  5.05547817e-02*B +  3.98301391e-04*R*G +  4.41091223e-04*R*B +  6.40756862e-04*G*B + -4.76971242e-06*R*G*B

            if (lightLeft < 0) {lightLeft = 0.0}
            if (lightRight < 0) {lightRight = 0.0}

            if (lightLeft > 100) {lightLeft = 100.0}
            if (lightRight > 100) {lightRight = 100.0}

            lightLeftCorrected = (Math.round(lightLeft)).toInt()
            lightRightCorrected = (Math.round(lightRight)).toInt()

        }

        return arrayOf(lightLeftCorrected, lightRightCorrected)
    }

    /* This function can be used to print a string on the Finch micro:bit. This function can only
    print strings up to 18 characters long. */
    fun printString(stringToPrint: String) {
        sendPrintCommand(stringToPrint)
    }

    /* This function sets the LED array of the micro:bit to display a pattern defined by a list
    of length 25. Each value in the list must be 0 (off) or 1 (on). The first five values in the
    array correspond to the five LEDs in the first row, the next five values to the second row, etc. */
    fun setDisplay(pattern: Array<Int>) {
        if (pattern.size != 25) {
            print("Error: The array must contain 25 values.")
        } else {
            var stringPattern = ""
            for (value in pattern) {
                if (value == 0) {
                    stringPattern += "0"
                } else {
                    stringPattern += "1"
                }
            }
            sendLEDArrayCommand(stringPattern)
        }
    }

    /* This function sets the right and left encoder values to 0. */
    fun resetEncoders() {
        val command = byteArrayOf(0xD5.toByte())

        finchConnection.writeBytes(command)
    }

    /* This function turns off all the Finch motors, lights, and buzzer. */
    fun stopAll() {
        lightState = LightState()
        sendStopAllCommand()
    }

    /* This function can be used to terminate the Finch's Bluetooth connection. */
    fun disconnect() {
        finchConnection.disconnect()
    }

    /* This function determines what happens when the Bluetooth device has new data. It is the
    function required to fulfill the UARTConnection.DataListener protocol. */
    override fun onData(newData: ByteArray) {
        var rawState: RawInputState? = null
        rawState = RawInputState(newData)

        /* If there is new data, we set the rawState, which will automatically computer the
        sensorState. */
        if (rawState != null) {
            rawInputState = rawState
        } else {
            /* If we have an error, the data is stale. */
            rawInputState?.isStale = true
        }
        finchListener?.onData()
    }

    /* These are the functions required to implement the UARTConnection.ConnectionListener
    protocol. We use them to notify a FinchListener when a Finch is connected or disconnected.
     */
    override fun onConnected() {
        finchListener?.onConnected()
    }

    override fun onDisconnected() {
        finchListener?.onDisconnected()
    }

}