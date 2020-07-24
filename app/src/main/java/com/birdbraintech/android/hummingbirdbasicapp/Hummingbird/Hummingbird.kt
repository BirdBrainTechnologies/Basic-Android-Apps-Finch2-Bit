package com.birdbraintech.android.hummingbirdbasicapp.Hummingbird

/* July 21, 2020, Bambi Brewer, BirdBrain Technologies */
/* This is the class that represents the Hummingbird. It includes a data structure that the user can
use to access the values of the Hummingbird sensors, as well as public functions that can be used to
control the Hummingbird motors, lights, and buzzer. */

import android.util.Log
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class Hummingbird(var hummingbirdConnection: UARTConnection): UARTConnection.DataListener,
    UARTConnection.ConnectionListener {

    /* To get data and connection/disconnection information from the Hummingbird, you must implement the HummingbirdListener protocol
    and add a listener.
     */
    private var hummingbirdListener: HummingbirdListener? = null

    fun setHummingbirdListener(l: HummingbirdListener) {
        hummingbirdListener = l
    }

    /* This companion object defines all the constants that we need for the Hummingbird class. */
    object Constants {
        const val expectedRawStateByteCount = 14   // Number of bytes in a Hummingbird data packet

        const val batteryVoltageConversionFactor: Float =  0.0406F

        /* These variable tell you the indices that identify different values in a Bluetooth data packet sent by the Hummingbird. */
        const val sensor1 = 0
        const val sensor2 = 1
        const val sensor3 = 2
        const val battery = 3
        const val accX = 4           // 3 bytes
        const val buttonShake = 7     // Contains bits for both micro:bit buttons and the shake
        const val magX = 8            // 6 bytes
    }

    /* This structure contains the raw bytes sent by the Hummingbird over Bluetooth, along with a timestamp for the data. */
    data class RawInputState(val data: ByteArray) {
        public val timestamp: Date = Date()
        public var isStale: Boolean = false // is the data old?
    }

    /* Each of the Hummingbird sensor ports supports different types of sensors, and we don't know what the user is plugging into each one. This data type computes all of the possibilities for each port. */
    data class HummingbirdSensor(val sensor: Byte) {
        val distance: Int = (((sensor.toInt() and 0xFF).toDouble() * (117.0 / 100.0)).roundToInt())
        val light: Int = (((sensor.toInt() and 0xFF).toDouble() * (100.0 / 255.0)).roundToInt())
        val dial: Int
        val sound: Int =  ((sensor.toInt() and 0xFF).toDouble() * (200.0 / 255.0)).roundToInt()
        val voltage: Double = (sensor.toInt() and 0xFF).toDouble() * (3.3 / 255)

        init {
            var dialTemp: Int = (((sensor.toInt() and 0xFF).toDouble() * (100.0 / 230.0)).roundToInt())
            if (dialTemp > 100) { dialTemp = 100 }
            dial = dialTemp
        }
    }

    /* This structure contains the processed values of the Hummingbird sensors. This is the data type that you will use in your program to hold the state of the Hummingbird sensors. */
    class SensorState(val rawState: RawInputState) {

        /* These functions are used to transform the raw Hummingbird data into values the user can understand. This may involve scaling, converting the data, or manipulating bytes to deal with values that are encoded in more that one byte in the raw data. Do not change these functions unless you are very sure that you understand the Bluetooth protocol. */

        private fun parseBatteryVoltage(rawStateData: ByteArray): Float {
            val battery: Byte = rawStateData[Constants.battery]
            return ((battery.toInt() and 0xFF).toFloat())* Constants.batteryVoltageConversionFactor
        }

        private fun parseAccelerationMagnetometerCompass(rawStateData: ByteArray): Triple<Array<Double>, Array<Double>, Int?> {
            val rawAcc = byteArrayOf(rawStateData[Constants.accX],rawStateData[Constants.accX + 1],rawStateData[Constants.accX + 2])
            val accValues =
                RawToAccl(
                    rawAcc
                )

            val rawMag = byteArrayOf(rawStateData[Constants.magX],rawStateData[Constants.magX + 1],rawStateData[Constants.magX + 2],rawStateData[Constants.magX + 3],rawStateData[Constants.magX + 4],rawStateData[Constants.magX + 5])
            val bitMag =
                RawToMag(
                    rawMag
                )

            var compass:Int? = null
            compass =
                DoubleToCompass(
                    accValues,
                    bitMag
                )

            return Triple(accValues, bitMag, compass)
        }

        /* These are the variables that hold the sensor values that you will use in your programs. */
        val timestamp: Date = rawState.timestamp
        var batteryVoltage: Float
        val isStale: Boolean = rawState.isStale
        var sensor1: HummingbirdSensor
        var sensor2: HummingbirdSensor
        var sensor3: HummingbirdSensor
        var acceleration: Array<Double>
        var magnetometer: Array<Double>
        var compass: Int?    // Undefined in the case of 0 z-direction acceleration
        var buttonA: Boolean
        var buttonB: Boolean
        var shake: Boolean

        /* This struct is initialized based on the raw sensor data. */
        init {
            batteryVoltage = parseBatteryVoltage(rawState.data)

            sensor1 =
                HummingbirdSensor(
                    rawState.data[Constants.sensor1]
                )
            sensor2 =
                HummingbirdSensor(
                    rawState.data[Constants.sensor2]
                )
            sensor3 =
                HummingbirdSensor(
                    rawState.data[Constants.sensor3]
                )

            val tripleAcc = parseAccelerationMagnetometerCompass(rawState.data)
            acceleration = tripleAcc.first
            magnetometer = tripleAcc.second
            compass = tripleAcc.third

            val rawButtonShakeValue = rawState.data[Constants.buttonShake].toInt()
            buttonA = (((rawButtonShakeValue shr 4) and 0x1) == 0x0)
            buttonB = (((rawButtonShakeValue shr 5) and 0x1) == 0x0)
            shake = ((rawButtonShakeValue and 0x1) != 0x0)

        }
    }

    /* This is the sensor data as it comes from the Hummingbird in a packet. It has to be decoded to provide meaningful information to the user. */
    private var rawInputState: RawInputState? = null
    var sensorState: SensorState? = null
        get(): SensorState? {
            return if (rawInputState == null) null else SensorState(
                rawInputState!!
            )
        }

    /* This structure keeps track of the current values of the Hummingbird outputs. These values are all sent at one time, so we have to keep track to make sure they are not overwritten when we send the next command. */
    private class OutputState {
        var triLED1 = byteArrayOf(0,0,0)
        var triLED2 = byteArrayOf(0,0,0)
        var singleLED1: Byte = 0
        var singleLED2: Byte= 0
        var singleLED3: Byte = 0
        var servo1: Byte = 255.toByte()
        var servo2: Byte= 255.toByte()
        var servo3: Byte = 255.toByte()
        var servo4: Byte = 255.toByte()
    }


    private var outputState =
        OutputState()   /* Remembers what the outputs are set to. */


    /* This function sends a Bluetooth command to set the lights, motors, and buzzer of the Hummingbird. The lights and motors are set from the Hummingbird output state so that they remain unchanged until the user sets them to something different. The buzzer, on the other hand, is set from input parameters, because we only want to play each note once. */
    private fun setAllOutputs(buzzerPeriod: Long, buzzerDuration: Long) {

        val letter: Byte = 0xCA.toByte()

        var buzzerArray= byteArrayOf()
        // Time_us_MSB, Time_us_LSB, Time_ms_MSB, Time_ms_LSB
        buzzerArray += (buzzerPeriod shr 8).toByte()
        buzzerArray += (buzzerPeriod and 0x00ff).toByte()
        buzzerArray += (buzzerDuration shr 8).toByte()
        buzzerArray += (buzzerDuration and 0x00ff).toByte()

        val array = byteArrayOf(letter,
            outputState.singleLED1,
            0xFF.toByte(), // reserved for future use
            outputState.triLED1[0],
            outputState.triLED1[1],
            outputState.triLED1[2],
            outputState.triLED2[0],
            outputState.triLED2[1],
            outputState.triLED2[2],
            outputState.servo1,
            outputState.servo2,
            outputState.servo3,
            outputState.servo4,
            outputState.singleLED2,
            outputState.singleLED3,
            buzzerArray[0],
            buzzerArray[1],
            buzzerArray[2],
            buzzerArray[3])

        hummingbirdConnection.writeBytes(array)

    }

    /* This function turns off all the Hummingbird motors, lights, and buzzer. */
    private fun sendStopAllCommand() {
        val command = byteArrayOf(0xCB.toByte())

        hummingbirdConnection.writeBytes(command)
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

        hummingbirdConnection.writeBytes(commandArray)
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
                0xCC.toByte(),
                0x80.toByte(),
                byte25,
                byte17to24,
                byte9to16,
                byte1to8
            )
            hummingbirdConnection.writeBytes(ledArrayCommand)
        }

    }

    /* This function sets the color of a tricolor LED on either port 1 or port 2. The red, green, and blue parameters must be between 0 and 100. */
    fun setTriLED(port: Int, red: Int, green: Int, blue: Int) {

        val portBound =
            clampToBounds(
                port,
                1,
                2
            )
        if (portBound == 1) {
            outputState.triLED1 = byteArrayOf((clampToBounds(
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
        } else {
            outputState.triLED2 = byteArrayOf((clampToBounds(
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
        }

        // Want to change outputs without playing the buzzer
        setAllOutputs(0,0)
    }

    /* This function sets the brightness of a single color LED on port 1, 2, or 3. The intensity must be between 0 and 100. */
    fun setLED(port: Int, intensity: Int) {

        val portBound =
            clampToBounds(
                port,
                1,
                3
            )
        when (portBound) {
            1 -> outputState.singleLED1 = (clampToBounds(
                intensity,
                0,
                100
            )).toByte()
            2 -> outputState.singleLED2 = (clampToBounds(
                intensity,
                0,
                100
            )).toByte()
            3 -> outputState.singleLED3 = (clampToBounds(
                intensity,
                0,
                100
            )).toByte()
            else -> Log.e("Finch","Error setting single color LED")
        }

        // Want to change outputs without playing the buzzer
        setAllOutputs(0, 0)
    }

    /* This function sets the value of a position servo on port 1-4 to a value between 0° and 180°. */
    fun setPositionServo(port: Int, angle: Int) {
        val boundedPort =
            clampToBounds(
                port,
                1,
                4
            )
        val boundedAngle =
            clampToBounds(
                angle,
                0,
                180
            )
        val realAngle = (floor((boundedAngle.toDouble())*254/180)).toByte()
        when (boundedPort) {
            1 -> outputState.servo1 = realAngle
            2 -> outputState.servo2 = realAngle
            3 -> outputState.servo3 = realAngle
            4 -> outputState.servo4 = realAngle
            else -> Log.e("Finch","Error setting position servo")
        }

        // Want to change outputs without playing the buzzer
        setAllOutputs(0, 0)
    }

    /* This function sets the value of a rotation servo on port 1-4 to a speed between -100 and 100. */
    fun setRotationServo(port: Int, speed: Int) {
        val boundedPort =
            clampToBounds(
                port,
                1,
                4
            )
        val boundedSpeed =
            clampToBounds(
                speed,
                -100,
                100
            )

        var realSpeed = 255.toByte()
        if ((boundedSpeed < -10) || (boundedSpeed > 10)) {
            realSpeed = (Math.round(abs(((boundedSpeed.toDouble()) * 23.0 / 100.0) + 122))).toByte()
        }

        when(boundedPort) {
            1 -> outputState.servo1 = realSpeed
            2 -> outputState.servo2 = realSpeed
            3 -> outputState.servo3 = realSpeed
            4 -> outputState.servo4 = realSpeed
            else -> Log.e("Finch","Error setting rotation servo")
        }

        // Want to change outputs without playing the buzzer
        setAllOutputs(0,0)
    }

    /* This function plays a note on the Hummingbird buzzer. We do not save this to the output state of the Hummingbird, because we want it to play just once. Notes are MIDI notes (32-135) and beats must be between 0-16. Each beat is 1 second. */
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
            noteToPeriod(
                (noteInBounds).toByte()
            ) //the period of the note in us
        if (period != null) {
            // Want to set the lights and the buzzer. Lights will be set based on the Hummingbird output state
            setAllOutputs(period.toLong(), duration.toLong())
        }

    }


    /* This function send a Bluetooth command to calibrate the compass. When the Hummingbird receives this command,
    it will place dots on the micro:bit screen as it waits for you to tilt the Hummingbird in different directions.
    If the calibration is successful, you will then see a check on the micro:bit screen. Otherwise, you will
    see an X. */
    fun calibrateCompass() {
        val command: ByteArray = byteArrayOf(0xCE.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

        hummingbirdConnection.writeBytes(command)
    }

    /* This function can be used to print a string on the Hummingbird micro:bit. This function can only print strings up to 18 characters long. */
    fun printString(stringToPrint: String) {
        sendPrintCommand(stringToPrint)
    }

    /* This function sets the LED array of the micro:bit to display a pattern defined by a list of length 25. Each value in the list must be 0 (off) or 1 (on). The first five values in the array correspond to the five LEDs in the first row, the next five values to the second row, etc. */
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

    /* This function turns off all the Hummingbird motors, lights, and buzzer. */
    fun stopAll() {
        outputState =
            OutputState()
        sendStopAllCommand()
    }

    /* This function determines what happens when the Bluetooth device has new data. Part of the HummingbirdListener protocol. */
    override fun onData(newData: ByteArray) {
        var rawState: RawInputState? = null
        rawState =
            RawInputState(
                newData
            )

        if (rawState != null) {
            rawInputState = rawState
        } else {
            /* If we have an error, the data is stale. */
            rawInputState?.isStale = true
        }
        hummingbirdListener?.onData()
    }

    /* This function disconnects the Hummingbird. */
    fun disconnect() {
        hummingbirdConnection.disconnect()
    }

    /* This function determines what happens when the Bluetooth device connects. Part of the HummingbirdListener protocol. */
    override fun onConnected() {
        hummingbirdListener?.onConnected()
    }

    /* This function determines what happens when the Bluetooth device disconnects. Part of the HummingbirdListener protocol. */
    override fun onDisconnected() {
        hummingbirdListener?.onDisconnected()
    }

    /* Defines the protocol for listening to the Hummingbird. */
    interface HummingbirdListener {
        fun onConnected() {}

        fun onDisconnected()

        fun onData()
    }
}