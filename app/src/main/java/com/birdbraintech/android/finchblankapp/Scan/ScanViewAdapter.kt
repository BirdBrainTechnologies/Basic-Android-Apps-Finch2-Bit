package com.birdbraintech.android.finchblankapp.Scan

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.birdbraintech.android.finchblankapp.Finch.NamingHandler
import com.birdbraintech.android.finchblankapp.R
import java.util.*

/**
 * This is the view adapter for the table of Bluetooth devices that is displayed when the app opens.
 * [RecyclerView.Adapter] that can display a [BluetoothDevice] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class ScanViewAdapter(private val appContext: Context, private val listener: ScanItemFragment.OnListFragmentInteractionListener?) : RecyclerView.Adapter<ScanViewAdapter.ViewHolder>() {

    private val values: MutableList<Triple<BluetoothDevice, Int, Date>> = ArrayList()

    //This handler checks periodically for devices that need to be removed from the list
    private val handler = Handler()
    val removeIfNotUpdated = object : Runnable {
        override fun run() {
            val currentDate = Date()

            //Remove any device that has not been seen in the last 2 seconds
            val itemsWereRemoved = values.removeAll {
                currentDate.getTime() - it.third.getTime() > 3000
            }
            if (itemsWereRemoved) { notifyDataSetChanged() }

            //Repeat this check every second
            handler.postDelayed(this, 1000)
        }
    }
    init {
        handler.post(removeIfNotUpdated)
    }

    private var selectedPosition = RecyclerView.NO_POSITION

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val idView: TextView = view.findViewById(R.id.textFlutterName)
        var item: BluetoothDevice? = null

        override fun toString(): String = "${super.toString()} '${idView.text}'"
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fragment_scan_item, parent, false))


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.item = values[position].first
        holder.idView.text =
            NamingHandler.generateName(
                appContext,
                holder.item!!.address
            )
        holder.itemView.isSelected = selectedPosition == position
        holder.view.setOnClickListener {
            if (null != listener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                listener.onListFragmentInteraction(holder.item!!)

            }
        }
    }


    override fun getItemCount(): Int = values.size


    fun clearList() {
        values.clear()
        notifyDataSetChanged()
    }


    fun addToList(device: BluetoothDevice, strength: Int) {
        val prefix = "FN" //BuildConfig.HARDWARE.devicePrefix
        if ((device.name?.startsWith("FN") == true)){

            var oldStrength = strength;
            var deviceFound = false
            var strengthChanged = false;
            values.forEach {
                if (it.first == device) {
                    deviceFound = true
                    oldStrength = it.second
                }
            }

            values.removeAll {
                device == it.first
            }
            val value: Triple<BluetoothDevice, Int, Date>
            if (deviceFound && (oldStrength + 20 < strength || oldStrength - 20 > strength)) {
                value = Triple(device, strength, Date())
                strengthChanged = true;
            } else {
                value = Triple(device, oldStrength, Date())
            }

            var index = values.binarySearch(value, compareByDescending(Triple<BluetoothDevice, Int, Date>::second))
            if (index < 0)
                index = index.inv()
            values.add(index, value)

            if (!deviceFound || strengthChanged) {
                notifyDataSetChanged()
            }

        } else {
            Log.d("Bluetooth", "ignoring device " + device.name + " since it does not contain '" + prefix + "'.")
        }
    }

}
