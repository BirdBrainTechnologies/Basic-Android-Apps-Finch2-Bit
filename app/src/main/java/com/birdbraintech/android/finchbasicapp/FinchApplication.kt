package com.birdbraintech.android.finchbasicapp
import android.app.Application
import android.bluetooth.BluetoothDevice

class FinchApplication: Application() {

    var finch: Finch? = null

    fun startFinchConnection(bluetoothDevice: BluetoothDevice) {

        var uartConnection = UARTConnection(this, bluetoothDevice)
        finch = Finch(uartConnection)

        finch?.finchConnection?.addRxDataListener(object : UARTConnection.DataListener {
            override fun onData(newData: ByteArray) {
                finch?.onData(newData)
            }
        })
        finch?.finchConnection?.addConnectionListener(object : UARTConnection.ConnectionListener {
            override fun onConnected() {
                finch?.onConnected()
            }

            override fun onDisconnected() {
                finch?.onDisconnected()
            }
        })
    }

}