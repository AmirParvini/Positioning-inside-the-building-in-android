package com.example.mobilegisproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.example.mobilegisproject.Classes.ExtractTrackingMarkers
import com.example.mobilegisproject.Classes.IndoorOutdoorDetector
import com.example.mobilegisproject.Classes.IndoorOutdoorDetector.Companion.check_in_out_handler
import com.example.mobilegisproject.Classes.IndoorOutdoorDetector.Companion.check_indoor_outdoor_runnable
import com.example.mobilegisproject.Classes.IndoorOutdoorDetector.Companion.gpsSatelliteCount
import com.example.mobilegisproject.Classes.IndoorOutdoorDetector.Companion.lightLevel
import com.example.mobilegisproject.Classes.IndoorOutdoorDetector.Companion.wifiSignals
import com.example.mobilegisproject.MainActivity.Companion.cursor
import com.example.mobilegisproject.MainActivity.Companion.cycle
import com.example.mobilegisproject.MainActivity.Companion.database
import com.example.mobilegisproject.MainActivity.Companion.databasepath
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MapActivity : AppCompatActivity(), OnMapReadyCallback {


    var trackingrunnable: Runnable ?= null
    private lateinit var settingbtn: ImageView
    private lateinit var startbtn: Button
    private lateinit var stopbtn: Button
    private lateinit var showmarkersbtn: Button
    private lateinit var hidemarkersbtn: Button
    lateinit var sensorsInfobtn: Button
    lateinit var sensorsInfoText: TextView
    private lateinit var mMap: GoogleMap
    val handler = Handler(Looper.getMainLooper())
    private lateinit var locationmanager: LocationManager
    lateinit var marker: Marker
    var markersList = ArrayList<Marker>()
    private lateinit var indoorOutdoorDetector: IndoorOutdoorDetector


    @SuppressLint("MissingInflatedId", "Recycle", "Range", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)


        //findViewById
        showmarkersbtn = findViewById(R.id.showmarkers_btn_id)
        hidemarkersbtn = findViewById(R.id.hidemarkers_btn_id)
        settingbtn = findViewById(R.id.setting_btn_id)
        startbtn = findViewById(R.id.starttracking_btn_id)
        stopbtn = findViewById(R.id.stoptracking_btn_id)
        sensorsInfobtn = findViewById(R.id.sensorsInfo_btn_id)
        sensorsInfoText = findViewById(R.id.sensorsInfoText_id)
        //findViewById







        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        locationmanager = getSystemService(LOCATION_SERVICE) as LocationManager






        //Click Buttons
        settingbtn.setOnClickListener {
            val intent = Intent(this, SettingActivity()::class.java)
            startActivity(intent)
        }
        startbtn.setOnClickListener {
            ThreadTracking().start()
            startbtn.isClickable = false
            settingbtn.isClickable = false
            stopbtn.isClickable = true
        }
        stopbtn.setOnClickListener {
            startbtn.isClickable = true
            stopbtn.isClickable = false
            settingbtn.isClickable = true
            try {
                handler.removeCallbacks(trackingrunnable!!)
            }catch (e:Exception){
                Log.e("error",e.message.toString())
            }
        }
        hidemarkersbtn.isClickable = false
        showmarkersbtn.setOnClickListener {
            try {
                if (markersList.size > 0) {
                    for (marker in markersList) {
                        marker.isVisible = true
                    }
                    showmarkersbtn.isClickable = false
                    hidemarkersbtn.isClickable = true
                }else{
                    Toast.makeText(this, "not exist Marker for Showing!",Toast.LENGTH_SHORT).show()
                }
            }catch (e:Exception){
                Log.e("error",e.message.toString())
            }
        }
        hidemarkersbtn.setOnClickListener {
            try {
                for (marker in markersList) {
                    marker.isVisible = false
                }
                hidemarkersbtn.isClickable = false
                showmarkersbtn.isClickable = true
            }catch (e:Exception){
                Log.e("error",e.message.toString())
            }
        }
        sensorsInfobtn.setOnClickListener {
            if (sensorsInfoText.isVisible){
                sensorsInfoText.visibility = View.GONE
            }else
            {
                sensorsInfoText.visibility = View.VISIBLE
            }
            sensorsInfoText.text = " gps Connected: $gpsSatelliteCount \n\n lightLevel: $lightLevel \n\n AccessPoints: $wifiSignals"
        }
        //Click Buttons
    }
//  OnCreate   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -








//Functions -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -
    //برای نمایش نقشه
    override fun onMapReady(googleMap: GoogleMap) {
    mMap = googleMap
    mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(32.42, 53.69), 4.0f))
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            statusCheck() //بررسی خاموش یا روشن بودن لوکیشن
            mMap.isMyLocationEnabled = true //دکمه زوم روی موقعیت کاربر
            database = SQLiteDatabase.openDatabase("$databasepath/LocationDB.db",null,SQLiteDatabase.CREATE_IF_NECESSARY)
            cursor = database.rawQuery("SELECT COUNT(*) FROM UserLocationTB", null)
            cursor.moveToFirst()
            if (cursor.getInt(0) > 0){
                Log.i("message", "markersList is exist = ${cursor.getInt(0)}")
                val extractTrackingMarkers = ExtractTrackingMarkers(mMap)
                markersList = extractTrackingMarkers.extractMarkers(database)
            }
            indoorOutdoorDetector = IndoorOutdoorDetector(this,this, sensorsInfoText, mMap)
            indoorOutdoorDetector.startDetection()
            return
        }

    }
    //برای نمایش نقشه






    // نمایش پیغام روشن کردن لوکیشن کاربر
    fun statusCheck() {
        if (!locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
    }
    private fun buildAlertMessageNoGps() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            .setNegativeButton("No") { dialog, id -> dialog.cancel() }
            val alert: AlertDialog = builder.create()
            alert.show()

    }
    // نمایش پیغام روشن کردن لوکیشن کاربر







    //Tracking
    inner class ThreadTracking: Thread() {
        override fun run() {

            runOnUiThread {
                trackingrunnable = object: Runnable{
                    override fun run() {
                        if (ActivityCompat.checkSelfPermission(
                                this@MapActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                this@MapActivity,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            // دریافت لوکیشن کاربر
                            val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@MapActivity)
                            fusedLocationClient.lastLocation.addOnSuccessListener {
                                if (it != null) {
                                    val userLocation = LatLng(it.latitude, it.longitude)
                                    marker = mMap.addMarker(MarkerOptions().position(userLocation))!!
                                    markersList.add(marker)
                                    database.execSQL("INSERT INTO UserLocationTB (latitude, longitude) VALUES (${it.latitude}, ${it.longitude})")
                                }
                            }
                            // دریافت لوکیشن کاربر
                            handler.postDelayed(this, (cycle*1000).toLong())// هر ۳ ثانیه چک میکنه که لوکیشن رو دریافت کرده یا نه
                        }
                    }

                }
                Handler(Looper.getMainLooper()).post(trackingrunnable!!)
            }

            super.run()
        }

    }
    //Tracking








    override fun onResume() {
        try {
            indoorOutdoorDetector.startDetection() //چک کردن داخل یا خارج خانه بودن
        }catch (_:Exception){

        }
        super.onResume()
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        try {
            if (trackingrunnable != null){
                handler.removeCallbacks(trackingrunnable!!)
            }
            database.close()
            indoorOutdoorDetector.stopDetection()
            check_in_out_handler.removeCallbacks(check_indoor_outdoor_runnable)
        }catch (_:Exception){

        }
        super.onBackPressed()
    }


    override fun onPause() {
        super.onPause()
        indoorOutdoorDetector.stopDetection()
    }

}
