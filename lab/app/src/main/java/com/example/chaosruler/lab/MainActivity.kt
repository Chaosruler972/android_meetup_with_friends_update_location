package com.example.chaosruler.lab

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.SystemClock
import android.provider.Contacts
import android.provider.ContactsContract
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlacePicker
import kotlinx.android.synthetic.main.activity_main.*
import android.widget.Toast
import com.google.android.gms.location.LocationServices


class MainActivity : Activity(),GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener
{

    /*
        Premission and request codes
     */
    private  var MY_PERMISSIONS_REQUEST_LOCATION_ABSOLUT:Int =0
    private  var MY_PERMISSIONS_REQUEST_LOCATION:Int = 0
    private  var MY_PERMISSIONS_REQUEST_SMS:Int =0
    private  var PHONE_STATE_PREMISSION_CODE:Int = 0
    private  var PLACE_PICKER_REQUEST =0
    private  var MIN_TIME_FOR_UPDATE: Long =0.toLong()
    private  var MIN_DIS_FOR_UPFATE: Float =0.toFloat()
    private  var CTCT_PICKER_RESULT_CODE:Int = 0
    private  var CONTACT_PERMISSION_CODE:Int = 0
    /*
        API client variable
     */
    private lateinit var mGoogleApiClient: GoogleApiClient

    private var found:Boolean=false // flag to mark location was loaded and active updates are ready to go
    private var target_lat:Double =0.toDouble() // initator, will be rewritten
    private var target_long:Double =0.toDouble() // initator, will be rewritten

    private var my_lat:Double=0.toDouble()
    private var my_long:Double =0.toDouble()

    private var systime:Long = 0 // systime (SystemClock.elapsedtime...) is fully monotonic, easier to count 10 minutes and be sure of its accuracy, uses device online time

    private var tenminutes_inmillis:Long = 60*1000


    private var premission_flag:Boolean = true
    /*
        more location variables
     */
    private var locationManager:LocationManager? = null


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init_vars()
        /*
            premission subroutines call
         */
        request_premission_subroutine(Manifest.permission.ACCESS_COARSE_LOCATION,MY_PERMISSIONS_REQUEST_LOCATION)

    }

    private fun after_premission_check()
    {
        if(premission_flag)
        {
            init_buttons()
            init_location_manager()
            do_google_thingy()
        }
        else
            mark_undoable()
    }

    private fun mark_undoable()
    {
         main_textview.text = getString(R.string.cant_work_without)
         main_phone.visibility = View.INVISIBLE
         main_getcord.visibility = View.INVISIBLE
    }
    private fun init_vars()
    {
        /*
            request codes initation, any updates on configuration is pointless
         */
        MY_PERMISSIONS_REQUEST_LOCATION_ABSOLUT = getString(R.string.wifi_cords_coe).toInt()
        MY_PERMISSIONS_REQUEST_LOCATION = getString(R.string.GPS_cords_code).toInt()
        MY_PERMISSIONS_REQUEST_SMS=getString(R.string.premission_sms_code).toInt()
        PLACE_PICKER_REQUEST = getString(R.string.place_picker_request).toInt()
        PHONE_STATE_PREMISSION_CODE = getString(R.string.phone_state_permission_code).toInt()
        CTCT_PICKER_RESULT_CODE = getString(R.string.results_from_contact_pick_code).toInt()
        CONTACT_PERMISSION_CODE = getString(R.string.contact_permission_code).toInt()
        /*
            app behavior configuration : GPS live
         */
        MIN_TIME_FOR_UPDATE= getString(R.string.tt_sync).toLong()
        MIN_DIS_FOR_UPFATE = getString(R.string.distance_for_sync).toFloat()
        tenminutes_inmillis*= getString(R.string.seconds_sync).toLong()
    }

    private fun init_location_manager()
    {
        locationManager = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        try
        {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_FOR_UPDATE, MIN_DIS_FOR_UPFATE, this)
        }
        catch(ex: SecurityException)
        {

        }
    }

    private fun init_buttons()
    {
        main_getcord.setOnClickListener({
            if(main_phone.text.isEmpty())
            {
                Toast.makeText(baseContext,getString(R.string.enter_number_fool),Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            place_picker_handle()

        })

        btn_load_ctct.setOnClickListener({

            startActivityForResult(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI), CTCT_PICKER_RESULT_CODE)

        })
    }
    @SuppressLint("MissingPermission")
    private fun do_google_thingy()
    {
        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }



    private fun request_premission_subroutine(premission:String,request_code:Int)
    {

        val premission_request = ContextCompat.checkSelfPermission(this, premission)
        if(premission_request == PackageManager.PERMISSION_GRANTED)
        {
            request_next_permission(request_code)
        }
        else
        {
            ActivityCompat.requestPermissions(this
                    , arrayOf(premission),
                    request_code)

        }

    }

    private fun request_next_permission(code:Int)
    {
        when(code)
        {
            MY_PERMISSIONS_REQUEST_LOCATION->
                    request_premission_subroutine(Manifest.permission.ACCESS_FINE_LOCATION,MY_PERMISSIONS_REQUEST_LOCATION_ABSOLUT)
            MY_PERMISSIONS_REQUEST_LOCATION_ABSOLUT->
                    request_premission_subroutine(Manifest.permission.SEND_SMS,MY_PERMISSIONS_REQUEST_SMS)
            MY_PERMISSIONS_REQUEST_SMS->
                request_premission_subroutine(Manifest.permission.READ_CONTACTS,CONTACT_PERMISSION_CODE)
            CONTACT_PERMISSION_CODE ->
                    request_premission_subroutine(Manifest.permission.READ_PHONE_STATE,PHONE_STATE_PREMISSION_CODE)
            PHONE_STATE_PREMISSION_CODE->
                    after_premission_check()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode)
        {
            MY_PERMISSIONS_REQUEST_LOCATION ->
                if (grantResults!!.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    request_next_permission(MY_PERMISSIONS_REQUEST_LOCATION)
                }
                else
                {
                    premission_flag=false
                    after_premission_check()
                }

            MY_PERMISSIONS_REQUEST_LOCATION_ABSOLUT ->
                if (grantResults!!.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    request_next_permission(MY_PERMISSIONS_REQUEST_LOCATION_ABSOLUT)
                }
                else
                {
                    premission_flag=false
                    after_premission_check()
                }

            MY_PERMISSIONS_REQUEST_SMS ->
            if (grantResults!!.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                request_next_permission(MY_PERMISSIONS_REQUEST_SMS)
            }
            else
            {
                premission_flag=false
                after_premission_check()
            }
            CONTACT_PERMISSION_CODE->
            if (grantResults!!.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                request_next_permission(CONTACT_PERMISSION_CODE)
            }
            else
            {
                premission_flag = false
                after_premission_check()
            }
            PHONE_STATE_PREMISSION_CODE ->
            if (grantResults!!.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                request_next_permission(PHONE_STATE_PREMISSION_CODE)
            }
            else
            {
                premission_flag=false
                after_premission_check()
            }
        }
    }

    private fun place_picker_handle()
    {
        var builder:PlacePicker.IntentBuilder = PlacePicker.IntentBuilder()
        try
        {
            startActivityForResult(builder.build(this),PLACE_PICKER_REQUEST)
        }
        catch (e:GooglePlayServicesRepairableException)
        {
            e.printStackTrace()
        }
        catch (e:GooglePlayServicesNotAvailableException)
        {
            e.printStackTrace()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK)
        {
            when(requestCode)
            {
                PLACE_PICKER_REQUEST->
                {
                    var place:Place = PlacePicker.getPlace(this,data)
                    /*
                        grab location update from picker
                     */
                    var lat:Double = place.latLng.latitude
                    var longti:Double = place.latLng.longitude
                    /*
                        update to inner class
                     */
                    target_lat = lat
                    target_long = longti

                    found=true // mark flag, we grabbed the input ;)

                    /*
                        disables input so app will be running without "more updates"
                     */
                    disable_inputs()
                }
                CTCT_PICKER_RESULT_CODE ->
                {
                    if(data != null)
                    {
                        var contactData = data!!.data
                        var c = contentResolver.query(contactData,null,null,null,null)
                        if(c.moveToFirst())
                        {
                            val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))

                            if(c.getInt(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))>0)
                            {
                                val phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id, null, null)
                                phones!!.moveToFirst()
                                val cNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                main_phone.setText(cNumber)

                            }
                        }
                    }

                }

            }
        }


    }

    /*
        disable input, app is working now
     */

    fun disable_inputs()
    {
        main_phone.isEnabled = false
        main_getcord.isEnabled = false
        btn_load_ctct.isEnabled = false
    }
    /*
        Activity listeners
     */

    override fun onConnected(p0: Bundle?): Unit
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {

        }
        else
        {

        }
    }
    override fun onConnectionSuspended(p0: Int)
    {

        mGoogleApiClient.connect()
    }

    override fun onConnectionFailed(p0: ConnectionResult)
    {

    }

    override fun onStart() {
        super.onStart()
        try {
            if(mGoogleApiClient!=null)
                mGoogleApiClient.connect()
        }
        catch (e:UninitializedPropertyAccessException)
        {
            // kotlin didn't handle variable.isIniatlized on all variables yet..
        }
    }

    override fun onStop()
    {
        super.onStop()
        try
        {
            if (mGoogleApiClient.isConnected)
            {
                mGoogleApiClient.disconnect()
            }
        }
        catch (e:UninitializedPropertyAccessException)
        {
            // kotlin didn't handle variable.isIniatlized on all variables yet..
        }

    }

    override fun onLocationChanged(location: Location?)
    {
        if(!found)
            return
        if(location != null)
        {
            if(systime == 0.toLong()) // first time systime update..
            {
                systime = SystemClock.elapsedRealtime()
            }
            else
            {
                var now = SystemClock.elapsedRealtime()
                var distance:Float = get_distance(my_lat,my_long,target_lat,target_long)
                if (distance == 0.toFloat())
                {
                    finish() // We got to target location
                }
                var str:String
                if(now - systime >= tenminutes_inmillis)
                {
                    str = getString(R.string.distance_str) + " " +  distance.toString()
                    send_sms(str)
                }
                else
                {
                    var speed:String = if(now-systime == 0.toLong())
                    {
                        getString(R.string.infinity)
                    }
                    else
                    {
                        (distance.toLong()/((now-systime)/1000)).toString()
                    }
                    str = getString(R.string.location_update_recorded).replace(getString(R.string.location_update_key),speed)
                    Toast.makeText(this,str,Toast.LENGTH_SHORT).show()
                }
                main_textview.text = str
                systime = now
            }


            my_lat = location.latitude
            my_long = location.longitude
            Log.d("last","Last known location: " + my_lat + " ," + my_long)
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
        val locationA = Location(getString(R.string.location_1))

        locationA.latitude = lat1
        locationA.longitude = long1

        val locationB = Location(getString(R.string.location_2))

        locationB.latitude = lat2
        locationB.longitude = long2
        return locationA.distanceTo(locationB)
    }

    fun send_sms(str:String)
    {
       // try
     //   {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(main_phone.text.toString(), null, str, null, null)
  //      }
    //    catch (e:Exception){}

    }

    override fun onDestroy() {
        super.onDestroy()
        if(locationManager!=null)
            locationManager!!.removeUpdates(this)
    }
}
