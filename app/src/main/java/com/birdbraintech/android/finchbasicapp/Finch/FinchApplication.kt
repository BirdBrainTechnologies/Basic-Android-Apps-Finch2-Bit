package com.birdbraintech.android.finchbasicapp.Finch
import android.app.Application
import android.bluetooth.BluetoothDevice

/* To pass the Finch's Bluetooth connection from one activity to another, we need to change the
application delegate. The instance of this class can be accessed from any activity, and we set
the app to use it in the manifest.
 */
class FinchApplication: Application() {

    var finch: Finch? = null    // This is the Finch that all activities can access

    /* This function is used to set up a Finch. It is called when you tap the name of a device in
     the scan list.
     */
    fun startFinchConnection(bluetoothDevice: BluetoothDevice) {

        /* Set up a Bluetooth connection and a Finch for it. */
        var uartConnection =
            UARTConnection(
                this,
                bluetoothDevice
            )
        finch =
            Finch(uartConnection)

        /* Set up the UART Listeners */
        finch?.finchConnection?.addRxDataListener(object :
            UARTConnection.DataListener {
            override fun onData(newData: ByteArray) {
                finch?.onData(newData)
            }
        })
        finch?.finchConnection?.addConnectionListener(object :
            UARTConnection.ConnectionListener {
            override fun onConnected() {
                finch?.onConnected()
            }

            override fun onDisconnected() {
                finch?.onDisconnected()
            }
        })
    }

}