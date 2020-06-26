package com.birdbraintech.android.finchbeebot.Finch


import android.content.Context
import com.birdbraintech.android.finchbeebot.R
import java.util.*

/**
 * Created by Steve on 6/9/2016.
 * A class that generates the three word names for Finches and Hummingbirds.
 */
object NamingHandler {

    fun generateName(context: Context, mac: String): String {
        val firstNames = context.resources.getStringArray(R.array.first_names)
        val middleNames = context.resources.getStringArray(R.array.middle_names)
        val lastNames = context.resources.getStringArray(R.array.last_names)
        val badNames = context.resources.getStringArray(R.array.bad_names)
        val badWordsLen = badNames.size
        val badNamesList = Arrays.asList(*badNames)
        // expected input: "aa:bb:cc:dd:ee:ff" => "d:ee:ff"
        val truncatedMac = mac.substring(9)

        // grab bits from the MAC address (6 bits, 6 bits, 8 bits => last, middle, first)
        var i = Integer.parseInt(truncatedMac.substring(6), 16)
        val mid = java.lang.Long.parseLong(truncatedMac.substring(1, 2) + truncatedMac.substring(3, 5), 16)
        var k = (mid / 64).toInt()
        var j = (mid % 64).toInt()

        // use the last 4 bits to "shift" all of the indices.
        val offset = i % 16
        i += offset
        j += offset
        k += offset

        val first = firstNames[i]
        var middle = middleNames[j]
        val last = lastNames[k]

        var prefix = "${first[0]}${middle[0]}${last[0]}"
        while (badNamesList.contains(prefix)) {
            j = (j + 1) % badWordsLen
            middle = middleNames[j]
            prefix = "${first[0]}${middle[0]}${last[0]}"
        }
        return "$first $middle $last"
    }

}
