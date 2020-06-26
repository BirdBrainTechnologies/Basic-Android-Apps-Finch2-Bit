package com.birdbraintech.android.finchbeebot.Finch

import kotlin.math.*

/* This file contains a lot of small function that are used to process the Finch data or encode
values into the form that the Bluetooth protocol requires. You should not need to change anything
in this file, though you can add your own utilities if you wish. */

/* This function is used to make sure Finch output values are within the required bound.*/
fun clampToBounds(num: Int, minBound: Int, maxBound: Int): Int {
    return Math.min(Math.max(num,minBound),maxBound)
}

/* This function is used to make sure Finch output values are within the required bound.*/
fun clampToBounds(num: Double, minBound: Double, maxBound: Double): Double {
    return Math.min(Math.max(num,minBound),maxBound)
}

/**
 * Converts raw readings from sensors [0,255] into accelerometer values.
 * @param rawAccl the byte array of raw accelerometer values in 3 directions
 * @param axisString the axis of acceleration
 * @return the acceleration in a specific axis based on the raw value.
 */
fun RawToAccl(rawAccl: ByteArray): Array<Double> {
    val x = Complement(
        RawToInt(rawAccl[0])
    ) * 196.0 / 1280.0
    val y = Complement(
        RawToInt(rawAccl[1])
    ) * 196.0 / 1280.0
    val z = Complement(
        RawToInt(rawAccl[2])
    ) * 196.0 / 1280.0

    return arrayOf(x, y, z)
}

/**
 * Converts raw readings from sensors [0,255] into raw accelerometer values in the finch
 * reference frame.
 * @param rawAccl the byte array of raw accelerometer values in 3 directions
 * @param axisString the axis of acceleration
 * @return the acceleration in a specific axis based on the raw value.
 */
fun RawToFinchAccl(rawAccl: ByteArray): Array<Double> {
    val x = Complement(
        RawToInt(rawAccl[0])
    ).toDouble()
    val y = Complement(
        RawToInt(rawAccl[1])
    ).toDouble()
    val z = Complement(
        RawToInt(rawAccl[2])
    ).toDouble()

    var yConverted = y * cos(Math.toRadians(40.0)) - z * sin(Math.toRadians(40.0))
    var zConverted = y * sin(Math.toRadians(40.0)) + z * cos(Math.toRadians(40.0))

    val xConverted = x * 196.0 / 1280.0
    yConverted = yConverted * 196.0 / 1280.0
    zConverted = zConverted * 196.0 / 1280.0

    return arrayOf(xConverted, yConverted, zConverted)
}

/**
 * Converts raw readings from sensors [0,255] into magnetometer values.
 * @param rawMag the byte array of raw magnetometer values in 3 directions
 * @param axisString the axis of magnetometer.
 * @return the magnetometer value in a specific axis based on the raw value.
 */
fun RawToMag(rawMag: ByteArray): Array<Double>  {
    val mx = (rawMag[1].toInt() and 0xFF or (rawMag[0].toInt() shl 8)).toDouble()
    val my = (rawMag[3].toInt() and 0xFF or (rawMag[2].toInt() shl 8)).toDouble()
    val mz = (rawMag[5].toInt() and 0xFF or (rawMag[4].toInt() shl 8)).toDouble()

    return arrayOf(mx, my, mz)
}

/**
 * Converts raw readings from sensors [0,255] into magnetometer values in the finch reference
 * frame.
 * @param rawMag the byte array of raw magnetometer values in 3 directions
 * @param axisString the axis of magnetometer.
 * @return the magnetometer value in a specific axis based on the raw value.
 */
fun RawToFinchMag(rawMag: ByteArray): Array<Double> {
    val x = Complement(
        RawToInt(rawMag[0])
    ).toDouble()
    val y: Double = Complement(
        RawToInt(rawMag[1])
    ).toDouble()
    val z: Double = Complement(
        RawToInt(rawMag[2])
    ).toDouble()
    val yConverted = y * cos(Math.toRadians(40.0)) + z * sin(Math.toRadians(40.0))
    val zConverted = z * cos(Math.toRadians(40.0)) - y * sin(Math.toRadians(40.0))

    return arrayOf(x, yConverted, zConverted)
}

/**
 * Converts raw readings from sensors [0,255] into angle in degrees.
 * @param rawMag the byte array of raw magnetometer values in 3 directions
 * @param rawAccl the byte array of raw accelerometer values in 3 directions
 * @param finchRef true if value should be returned in the finch reference frame
 * @return the angle in degrees based on the raw magnetometer values and raw accelerometer values.
 */
fun DoubleToCompass(acc: Array<Double>, mag: Array<Double>): Int? {
    if (acc[2] == 0.toDouble()) {
        return null
    }

    val ax = acc[0]
    val ay = acc[1]
    val az = acc[2]

    val mx = mag[0]
    val my = mag[1]
    val mz = mag[2]

    val phi = atan(-ay/az)
    val theta = atan( ax / (ay* sin(phi) + az* cos(phi)) )

    val xP = mx
    val yP = my * cos(phi) - mz * sin(phi)
    val zP = my * sin(phi) + mz * sin(phi)

    val xPP = xP * sin(theta) + zP * sin(theta)
    val yPP = yP

    val angle = 180 + Math.toDegrees(atan2(xPP, yPP))
    val roundedAngle = round(angle).toInt()

    return roundedAngle
}


/**
Converts a note number to a period in microseconds (us)
See: https://newt.phys.unsw.edu.au/jw/notes.html
fm  =  (2^((m−69)/12))(440 Hz)
 */
fun noteToPeriod(note: Byte): Int? {

    val frequency = 440 * Math.pow(2.toDouble(), (note.toDouble() - 69.0)/12.0)
    val period = (1/frequency) * 1000000
    if ((period > 0) && (period <= Short.MAX_VALUE.toDouble())) {
        return period.toInt()
    } else {
        return null
    }
}

/* Converts a velocity into the form it has to have for a Bluetooth command. */
fun convertVelocity(velocity: Int): Byte {
    var v = Math.abs(velocity)
    if (velocity > 0) { v += 128}
    return v.toByte()
}

/* Used to construct bytes to set the micro:bit display */
fun ConstructByteFromInts(data: IntArray, start: Int, end: Int): Byte {
    var resultByte = 0
    for (i in start until end) {
        resultByte += (data[i] shl i - start)
    }
    return resultByte.toByte()
}


/**
 * Take 2s complement of a given int
 * @param prev the number that 2s complement will be taken on
 * @return 2s complement of input
 */
fun Complement(prev: Int): Int {
    var prev = prev
    if (prev > 127) {
        prev = prev - 256
    }
    return prev
}

/**
 * Converts raw readings from bytes (always signed), into an unsigned int value
 *
 * @param raw Reading from sensor
 * @return Sensor value represented as an int [0,255]
 */
fun RawToInt(raw: Byte): Int {
    return raw.toInt() and 0xff
}