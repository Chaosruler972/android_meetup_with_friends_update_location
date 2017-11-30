package com.example.chaosruler.lab

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.chaosruler.lab.R.id.main_textview
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Created by chaosruler on 11/30/17.
 */
class Location_listener(private var context: Context, private var textView: TextView) : LocationListener
{
    override fun onLocationChanged(location: Location?)
    {
        if(!MainActivity.found)
            return
        if(location != null)
        {
            if(MainActivity.systime == 0.toLong()) // first time systime update..
            {
                MainActivity.systime = SystemClock.elapsedRealtime()
            }
            else
            {
                var now = SystemClock.elapsedRealtime()
                var distance:Float = get_distance(MainActivity.my_lat,MainActivity.my_long,MainActivity.target_lat,MainActivity.target_long)
                if (distance == 0.toFloat())
                {
                     // We got to target location
                }
                var str:String
                if(now - MainActivity.systime >= MainActivity.tenminutes_inmillis)
                {
                    str = context.getString(R.string.distance_str) + " " +  distance.toString()
                    MainActivity.send_sms(str)
                }
                else
                {
                    var speed:String = if(now-MainActivity.systime == 0.toLong())
                    {
                        context.getString(R.string.infinity)
                    }
                    else
                    {
                        (distance.toLong()/((now-MainActivity.systime)/1000)).toString()
                    }
                    str = context.getString(R.string.location_update_recorded).replace(context.getString(R.string.location_update_key),speed)
                    Toast.makeText(context,str, Toast.LENGTH_SHORT).show()
                }

                textView.text = str
                MainActivity.systime = now
            }


            MainActivity.my_lat = location.latitude
            MainActivity.my_long = location.longitude
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }

    fun get_distance(lat1:Double,long1:Double, lat2:Double,long2:Double):Float
    {
        val locationA = Location(context.getString(R.string.location_1))

        locationA.latitude = lat1
        locationA.longitude = long1

        val locationB = Location(context.getString(R.string.location_2))

        locationB.latitude = lat2
        locationB.longitude = long2
        return locationA.distanceTo(locationB)
    }

}