package com.example.mobilegisproject.Classes

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class ExtractTrackingMarkers(private val mMap:GoogleMap) {

    lateinit var marker: Marker
    var markersList = ArrayList<Marker>()
    var latitudeArray = ArrayList<Double>()
    var longitudeArray = ArrayList<Double>()


    @SuppressLint("Range")
    fun extractMarkers(database: SQLiteDatabase): ArrayList<Marker> {
        val cursor = database.rawQuery("SELECT latitude, longitude FROM UserLocationTB", null)
        if (cursor.moveToFirst()) {
            do {
                val latitude = cursor.getDouble(cursor.getColumnIndex("latitude"))
                val longitude = cursor.getDouble(cursor.getColumnIndex("longitude"))
                latitudeArray.add(latitude)
                longitudeArray.add(longitude)
            } while (cursor.moveToNext())
        }
        for (i in 0 until latitudeArray.size) {
            val userLocation = LatLng(latitudeArray[i], longitudeArray[i])
            marker = mMap.addMarker(MarkerOptions().position(userLocation))!!
            markersList.add(marker)
        }
        cursor.close()
        return markersList
    }
}