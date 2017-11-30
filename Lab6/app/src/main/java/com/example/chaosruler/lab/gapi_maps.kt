package com.example.chaosruler.lab

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices

/**
 * Created by chaosruler on 11/30/17.
 */
class gapi_maps(private var context: Context) : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    companion object
    {
        public lateinit var mGoogleApiClient: GoogleApiClient
    }

    override fun onConnected(p0: Bundle?): Unit
    {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
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

    @SuppressLint("MissingPermission")
    public fun do_google_thingy()
    {
        mGoogleApiClient = GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
    }
}