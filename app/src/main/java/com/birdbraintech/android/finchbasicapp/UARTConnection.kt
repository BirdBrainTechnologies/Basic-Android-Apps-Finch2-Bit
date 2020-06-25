package com.birdbraintech.android.finchbasicapp

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


/**
 * Represents a UART connection established via Bluetooth Low Energy. Communicates using the RX and
 * TX lines.
 *
 * @author Terence Sun (tsun1215)
 */
class UARTConnection @JvmOverloads constructor(context: Context, device: BluetoothDevice) : BluetoothGattCallback() {

    /* Latches to handle serialization of async reads/writes */
    private var startLatch = CountDownLatch(1)
    private var doneLatch = CountDownLatch(1)
    private var resultLatch = CountDownLatch(1)

    /* UUIDs for the communication lines */
    private val uartUUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val txUUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    private val rxUUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    private val rxConfigUUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val rxListeners = ArrayList<DataListener>()
    private var connectionListener: ConnectionListener? = null

    private var connectionState: Int = BluetoothGatt.STATE_DISCONNECTED
    private var connection: BTGattConnection? = null
    // NOTE: issues with writing if descriptor is not written to first.
    private var onDescriptorWriteForNotify = false

    /**
     * Returns whether or not this connection is connected
     *
     * @return True if connected, false otherwise
     */
    val isConnected: Boolean
        get() = this.connectionState == BluetoothGatt.STATE_CONNECTED

    init {
        if (!establishUARTConnection(context, device)) {
            disconnect()
        }
    }

    /**
     * Sends a byte array to the device across TX
     *
     * @param bytes byte array to send
     * @return True on success, false otherwise
     */
    @Synchronized
    fun writeBytes(bytes: ByteArray): Boolean {
        if (!onDescriptorWriteForNotify) { // Can't call writeBytes until onDescriptorWrite has been called
            Log.e("Error", "Refusing to call writeBytes since onDescriptorWriteForNotify is false")
            return false
        }
        val connection = connection ?: return false
        return try {
            startLatch = CountDownLatch(1)
            doneLatch = CountDownLatch(1)

            connection.tx.value = bytes
            var res: Boolean
            var retryCount = 0
            do {
                res = connection.btGatt.writeCharacteristic(connection.tx)
                if (res) {
                    break
                }
                retryCount++
            } while (retryCount <= MAX_RETRIES)

            // Wait for operation to complete
            startLatch.countDown()
            try {
                doneLatch.await(100, TimeUnit.MILLISECONDS)
                res
            } catch (e: InterruptedException) {
                Log.e("Error", "Error: $e")
                false
            }
        } catch (e: Exception) {
            Log.e("Error", "Error: $e")
            false
        }
    }

    /**
     * Establishes a UARTConnection by connecting to the device and registering a characteristic
     * notification on the RX line
     *
     * @param context Context that this connection is being made in
     * @param device  Bluetooth device being connected to
     * @return True if a connection was successfully established, false otherwise
     */
    private fun establishUARTConnection(context: Context, device: BluetoothDevice): Boolean {

        device.connectGatt(context, true, this)

        // Initialize serialization
        startLatch.countDown()
        Log.d("Bluetooth", "Successfully established connection to $device")
        return true
    }


    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {

        connectionState = newState
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices()
                connectionListener?.onConnected()
            }
        }
        if (newState == BluetoothGatt.STATE_DISCONNECTED) {
            connectionListener?.onDisconnected()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            val tx = gatt.getService(uartUUID).getCharacteristic(txUUID)
            val rx = gatt.getService(uartUUID).getCharacteristic(rxUUID)
            val connection = BTGattConnection(gatt, tx, rx)
            this.connection = connection
            // Notify that the setup process is completed
            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)

            // Enable RX notification
            var error = false
            if (!connection.btGatt.setCharacteristicNotification(connection.rx, true)) {
                Log.e("Error", "Unable to set characteristic notification")
                error = true
            }
            val descriptor = connection.rx.getDescriptor(rxConfigUUID)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (!connection.btGatt.writeDescriptor(descriptor)) {
                Log.e("Error", "Unable to set descriptor")
                error = true
            }
            if (error) {
                disconnect()
            }
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        // For serializing write operations
        try {
            startLatch.await()
        } catch (e: InterruptedException) {
            Log.e("Error", "Error: $e")
        }

        // For serializing write operations
        doneLatch.countDown()
    }


    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {

        // For serializing read operations
        try {
            startLatch.await()
        } catch (e: InterruptedException) {
            Log.e("Error", "Error: $e")
        }

        val newValue = characteristic.value

        for (l in rxListeners) {
            l.onData(newValue)
        }

        // For serializing read operations
        resultLatch.countDown()
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        this.onDescriptorWriteForNotify = true

        // start broadcasting
        if (writeBytes(byteArrayOf(0x62, 0x67))) {
            Log.d("Bluetooth", "UARTConnection.onDescriptorWrite: uartConnection sent to start broadcasting.")
        } else {
            Log.d("Bluetooth", "UARTConnection.onDescriptorWrite: could not write bytes to start broadcasting.")
        }

        super.onDescriptorWrite(gatt, descriptor, status)
    }

    /**
     * Disconnects and closes the connection with the device
     */
    fun disconnect() {
        connection?.also {
            it.btGatt.disconnect()
            it.btGatt.close()
        }
        this.connection = null
    }


    fun addRxDataListener(l: DataListener) {
        rxListeners.add(l)
    }

    fun removeRxDataListener(l: DataListener) {
        rxListeners.remove(l)
    }

    fun addConnectionListener(l: ConnectionListener) {
        connectionListener = l
    }

    /**
     * Listener for new data coming in on RX
     */
    interface DataListener {
        /**
         * Called when new data arrives on the RX line
         *
         * @param newData Data that arrived
         */
        fun onData(newData: ByteArray)
    }

    interface ConnectionListener {

        fun onConnected()

        fun onDisconnected()
    }

    companion object {
        private const val MAX_RETRIES = 100
        private const val CONNECTION_TIMEOUT_IN_SECS = 15
    }

}
/**
 * Initializes a UARTConnection. This needs to know the context the Bluetooth connection is
 * being made from (Activity, Service, etc)
 *
 * @param context  Context that the connection is begin made from
 * @param device   Device to connect to
 * @param settings Settings for connecting via UART
 */

data class BTGattConnection(val btGatt: BluetoothGatt, val tx: BluetoothGattCharacteristic, val rx: BluetoothGattCharacteristic)



