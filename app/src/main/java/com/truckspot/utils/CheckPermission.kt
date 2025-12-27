package com.truckspot.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

 class CheckPermission {
     companion object{
         val RC_LOCATION_PERMISSION = 111

         fun checkIsMarshMallowVersion(): Boolean {
             val sdkVersion = Build.VERSION.SDK_INT
             return sdkVersion >= Build.VERSION_CODES.M
         }

         fun checkLocationPermission(context: Context?): Boolean {
             val result = ContextCompat.checkSelfPermission(
                 context!!, Manifest.permission.ACCESS_COARSE_LOCATION
             )
             val result1 = ContextCompat.checkSelfPermission(
                 context, Manifest.permission.ACCESS_FINE_LOCATION
             )
             val result2 = ContextCompat.checkSelfPermission(
                 context, Manifest.permission.WRITE_EXTERNAL_STORAGE
             )

             return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED
         }

         fun requestLocationPermission(activity: Activity?) {
             if (ActivityCompat.shouldShowRequestPermissionRationale(
                     activity!!, Manifest.permission.ACCESS_COARSE_LOCATION
                 ) || ActivityCompat.shouldShowRequestPermissionRationale(
                     activity, Manifest.permission.ACCESS_FINE_LOCATION

                 ) || ActivityCompat.shouldShowRequestPermissionRationale(
                     activity, Manifest.permission.WRITE_EXTERNAL_STORAGE

                 )
             ) {
                 ActivityCompat.requestPermissions(
                     activity, arrayOf<String>(
                         Manifest.permission.ACCESS_COARSE_LOCATION,
                         Manifest.permission.ACCESS_FINE_LOCATION,
                         Manifest.permission.WRITE_EXTERNAL_STORAGE
                     ), RC_LOCATION_PERMISSION
                 )
             } else {
                 ActivityCompat.requestPermissions(
                     activity, arrayOf<String>(
                         Manifest.permission.ACCESS_COARSE_LOCATION,
                         Manifest.permission.ACCESS_FINE_LOCATION,
                         Manifest.permission.WRITE_EXTERNAL_STORAGE
                     ), RC_LOCATION_PERMISSION
                 )
             }
         }
     }




}