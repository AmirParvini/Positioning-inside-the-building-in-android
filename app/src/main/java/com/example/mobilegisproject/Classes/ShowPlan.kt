package com.example.mobilegisproject.Classes

import android.app.Activity
import android.graphics.Color
import com.example.mobilegisproject.Classes.ReadJson.Companion.pathpoints_WGS
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolylineOptions

class ShowPlan(activity:Activity, val mMap: GoogleMap) {

    init {
            for (path in pathpoints_WGS){
                val polylineOptions = PolylineOptions()
                polylineOptions.addAll(path).color(Color.RED)
                activity.runOnUiThread {
                    mMap.addPolyline(polylineOptions)
                }
            }
    }
}