package com.birdbraintech.android.finchblankapp.Scan

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.birdbraintech.android.finchblankapp.*
import com.birdbraintech.android.finchblankapp.Finch.FinchApplication

/* This is the first activity in the app. It displays a list of Finches. When you click one, it
connects to the Finch and moves to MainActivity.
 */
class ScanActivity : AppCompatActivity(),
    ScanItemFragment.OnListFragmentInteractionListener {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var scanListAdapter: ScanViewAdapter? = null

    private lateinit var deviceListLayout: ConstraintLayout

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {

            val device = result.device
            Log.d("Bluetooth", "onLeScan result: " + device.name)

            scanListAdapter?.addToList(device, result.rssi)
            super.onScanResult(callbackType, result)
        }
    }


    private fun runLeScan() {
        // TODO ScanFilter https://developer.android.com/reference/android/bluetooth/le/ScanFilter
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
    }


    /* This function makes sure you have permission for Bluetooth and starts the scan. */
    private fun onScan() {
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent,
                REQUEST_ENABLE_BT
            )
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i("Bluetooth", "Don't have permission to do bluetooth scan.")

            AlertDialog.Builder(this)
                    .setMessage("This app needs location permissions to scan for nearby Bluetooth devices.")
                    .setPositiveButton("OK") { _, _ ->
                        // The previous permission check should only be able to fail on M or higher
                        requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                            PERMISSION_REQUEST_COARSE_LOCATION
                        )
                    }
                    .show()
        } else {
            runLeScan()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runLeScan()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("Bluetooth", "onActivityResult: $requestCode, $resultCode")
        if (resultCode == Activity.RESULT_OK) {
            onScan()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        deviceListLayout = findViewById(R.id.constraintLayoutFlutterList)


        val list = findViewById<RecyclerView>(R.id.fragmentScanItem)
        this.scanListAdapter = list.adapter as ScanViewAdapter?

        // Initializes Bluetooth adapter.
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        scanListAdapter!!.clearList()
        onScan()
    }


    /* This is the function that is called when you tap on an item in the list. It connects to
    the Finch and starts MainActivity
     */
    override fun onListFragmentInteraction(item: BluetoothDevice) {
        Thread {
            (application as FinchApplication).startFinchConnection(item)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }.start()
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)

    }

    companion object {

        private const val PERMISSION_REQUEST_COARSE_LOCATION = 456 //Arbitrary, but needs to be unique
        private const val REQUEST_ENABLE_BT = 1
    }

}
