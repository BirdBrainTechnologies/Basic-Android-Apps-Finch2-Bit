package com.birdbraintech.android.hummingbirdbasicapp.Hummingbird
import android.app.Application
import android.bluetooth.BluetoothDevice

/* To pass the Hummingbird's Bluetooth connection from one activity to another, we need to change the
application delegate. The instance of this class can be accessed from any activity, and we set
the app to use it in the manifest.
 */

class HummingbirdApplication: Application() {

    var hummingbird: Hummingbird? = null    // This is the Hummingbird that all activities can access

    /* This function is used to set up a Hummingbird. It is called when you tap the name of a device in
     the scan list.
     */
    fun startHummingbirdConnection(bluetoothDevice: BluetoothDevice) {

        /* Set up a Bluetooth connection and a Hummingbird for it. */
        var uartConnection =
            UARTConnection(
                this,
                bluetoothDevice
            )
        hummingbird =
            Hummingbird(
                uartConnection
            )

        /* Set up the UART Listeners */
        hummingbird?.hummingbirdConnection?.addRxDataListener(object :
            UARTConnection.DataListener {
            override fun onData(newData: ByteArray) {
                hummingbird?.onData(newData)
            }
        })
        hummingbird?.hummingbirdConnection?.addConnectionListener(object :
            UARTConnection.ConnectionListener {
            override fun onConnected() {
                hummingbird?.onConnected()
            }

            override fun onDisconnected() {
                hummingbird?.onDisconnected()
            }
        })
    }

}