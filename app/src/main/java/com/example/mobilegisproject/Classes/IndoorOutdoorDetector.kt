package com.example.mobilegisproject.Classes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.mobilegisproject.MainActivity.Companion.cursor
import com.example.mobilegisproject.MainActivity.Companion.database
import com.example.mobilegisproject.MainActivity.Companion.databasepath
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.osgeo.proj4j.ProjCoordinate
import java.lang.Exception
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.properties.Delegates

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class IndoorOutdoorDetector(private val activity: Activity, private val context: Context, val textView: TextView, val mMap: GoogleMap): SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    companion object{
        var camerazoom by Delegates.notNull<Float>()
        var gpsSatelliteCount = 0
        var lightLevel: Float = -1f
        var wifiSignals: String = ""
        val check_in_out_handler = Handler(Looper.getMainLooper())
        lateinit var check_indoor_outdoor_runnable: Runnable
        lateinit var fusedlocationrunnable : Runnable
    }
    lateinit var wifiScanReceiver: BroadcastReceiver
    private var isReceiverRegistered = "false"
    var position = "outdoor"
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    var lastlocationuser = LatLng(0.0,0.0)
    lateinit var scanResults: List<ScanResult>
    var usermarker : Marker ?= null
    val readjson = ReadJson(context,context.assets,"")


    init {
        FusedLocationClient()
    }




    fun FusedLocationClient(){
        activity.runOnUiThread {
            fusedlocationrunnable = object: Runnable{
                override fun run() {
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        // دریافت لوکیشن کاربر
                        fusedLocationClient.lastLocation.addOnSuccessListener {
                            if (it != null) {
                                lastlocationuser = LatLng(it.latitude, it.longitude)
                                camerazoom = mMap.cameraPosition.zoom
                            }
                        }

                        // دریافت لوکیشن کاربر

                        check_in_out_handler.postDelayed(this, 1000)// هر ۱ ثانیه چک میکنه که لوکیشن رو دریافت کرده یا نه
                    }
                }

            }
            check_in_out_handler.post(fusedlocationrunnable)
        }
    }







    private fun Num_Connected_GPS() { //تعداد ماهواره های متصل
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val gnssStatusCallback = object : GnssStatus.Callback() {
                override fun onSatelliteStatusChanged(status: GnssStatus) {
                    super.onSatelliteStatusChanged(status)
                    Log.i("gnss", "in Num_Connected_GPS")
                    gpsSatelliteCount = 0
                    for (i in 0 until status.satelliteCount) {
                        if (status.usedInFix(i)) {
                            gpsSatelliteCount++
                        }
                    }
                }
            }
            locationManager.registerGnssStatusCallback(gnssStatusCallback, null)
        }
    }

    private fun setupWifiScanReceiver() {
        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(Context: Context?, intent: Intent?) { // با دربافت سیگنال های اکسس پوینت ها این متد فراخوانی میشود
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    if (isReceiverRegistered == "false") {
                        val intentFilter = IntentFilter()
                        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                        context.registerReceiver(wifiScanReceiver, intentFilter)
                        isReceiverRegistered = "true"
                        wifiManager.startScan()
                    }
                }
            }
        }
    }

    override fun onSensorChanged(sensorevent: SensorEvent?) { //با تغییرات مقدار دریافتی سنسور نو این متد فراخوانی میشود
        if (sensorevent?.sensor?.type == Sensor.TYPE_LIGHT) {
            lightLevel = sensorevent.values[0]
//            checkIndoorOutdoor()
        }
    }




    @SuppressLint("SetTextI18n")
    private fun checkIndoorOutdoor() {
        try {
            check_indoor_outdoor_runnable = Runnable {
                if (ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
                ) {
                    scanResults = wifiManager.scanResults
                    wifiSignals = scanResults.joinToString(", ") { "${it.SSID}|${it.BSSID}|${it.level}" }
//                Log.i("message", "lightLevel = $lightLevel")
//                Log.i("message", "gpsSatelliteCount = $gpsSatelliteCount")
//                Log.i("message", "wifiSignals = $wifiSignals")
                    //            Toast.makeText(context, "gpsSatelliteCount = $gpsSatelliteCount", Toast.LENGTH_SHORT).show()
                    textView.text = " gps Connected: $gpsSatelliteCount \n\n lightLevel: $lightLevel \n\n AccessPoints: $wifiSignals"
                }

                val isIndoor = when {
                    lightLevel >= 0 && lightLevel < 1000 && gpsSatelliteCount < 25 -> true
                    lightLevel > 1000 && gpsSatelliteCount > 25 -> false
                    else -> null
                }
                val message = if (isIndoor == true) "Indoor" else if (isIndoor == false) "Outdoor" else position
                if (message != position) {
                    position = message
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (message == "Indoor"){
                        check_in_out_handler.removeCallbacks(fusedlocationrunnable)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastlocationuser, 20.0f))
                        ShowPlan(activity, mMap)
                    }else{
                        check_in_out_handler.post(fusedlocationrunnable)
                    }
                }

                if (message == "Indoor"){
                    if (!wifiManager.isWifiEnabled){
                        buildAlertMessageNoWiFi()
                    }else{
                        UpdateLocationIndoor().start()
                        if (usermarker == null) {
                            usermarker = mMap.addMarker(MarkerOptions().position(lastlocationuser))
                        } else {
                            usermarker?.position = lastlocationuser
                        }
                        check_in_out_handler.postDelayed(check_indoor_outdoor_runnable,1000)
                    }

                } else if (message == "Outdoor"){
                    FusedLocationClient()
                    check_in_out_handler.postDelayed(check_indoor_outdoor_runnable,1000)
                }
                else{
                    check_in_out_handler.postDelayed(check_indoor_outdoor_runnable,1000)
                }
            }
            check_in_out_handler.post(check_indoor_outdoor_runnable)
        }catch (e:Exception){
            Log.e("catching", e.message.toString())
        }

    }



    @SuppressLint("Recycle")
    inner class UpdateLocationIndoor: Thread(){
        override fun run() {
            try {

                val drssDict = mutableMapOf<String, MutableList<Double>>()
                val ReceivedBssid = mutableListOf<String>()
                val ReceivedRss = mutableListOf<String>()
                val ApInfo = wifiSignals.split(", ")
                for (i in ApInfo) {
                    ReceivedBssid.add(i.split("|")[1])
                    ReceivedRss.add(i.split("|")[2])
                }

                for (Index in ReceivedBssid.indices) {
                    database = SQLiteDatabase.openDatabase(
                        "${databasepath}/LocationDB.db", null,
                        SQLiteDatabase.CREATE_IF_NECESSARY
                    )
                    cursor = database.rawQuery(
                        "SELECT * FROM WiFiTB WHERE bssid = ?",
                        arrayOf(ReceivedBssid[Index])
                    )
                    if (cursor.moveToFirst()) {
                        do {
                            val rowData = mapOf(
                                "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                                "projectname" to cursor.getString(cursor.getColumnIndexOrThrow("projectname")),
                                "x" to cursor.getDouble(cursor.getColumnIndexOrThrow("x")),
                                "y" to cursor.getDouble(cursor.getColumnIndexOrThrow("y")),
                                "ssid" to cursor.getString(cursor.getColumnIndexOrThrow("ssid")),
                                "bssid" to cursor.getString(cursor.getColumnIndexOrThrow("bssid")),
                                "rss" to cursor.getInt(cursor.getColumnIndexOrThrow("rss")),
                                "pointname" to cursor.getString(cursor.getColumnIndexOrThrow("pointname"))
                            )
                            drssDict.getOrPut(rowData["pointname"] as String) { mutableListOf() }
                                .add(
                                    ((rowData["rss"] as Int).toDouble() - ReceivedRss[Index].toDouble()).pow(
                                        2.0
                                    )
                                )

                        } while (cursor.moveToNext())
                    }
                    Log.i("message", drssDict.toString())
                    var Sumdrss = Double.POSITIVE_INFINITY
                    var MindrssP = ""
                    for ((key, valuelist) in drssDict) {
                        if (sqrt(valuelist.sum()) < Sumdrss) {
                            MindrssP = key
                            Sumdrss = sqrt(valuelist.sum())
                        }
                    }
                    cursor = database.rawQuery(
                        "SELECT * FROM WiFiTB WHERE pointname = ?",
                        arrayOf(MindrssP)
                    )
                    if (cursor.moveToFirst()) {
                        val x = cursor.getDouble(cursor.getColumnIndexOrThrow("x"))
                        val y = cursor.getDouble(cursor.getColumnIndexOrThrow("y"))
                        lastlocationuser = readjson.UTM_to_WGS84(arrayListOf(ProjCoordinate(x, y)))[0]
                    }
                }

                super.run()
            }catch (e:Exception){
                Log.e("catching",e.message.toString())
            }
        }
    }






    fun buildAlertMessageNoWiFi(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setMessage("Your WIFI seems to be disabled, do you want to enable it? \n (Required for indoor positioning)")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id -> context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                UpdateLocationIndoor()}
            .setNegativeButton("No") { dialog, id -> dialog.cancel()
                checkIndoorOutdoor()}
        val alert: AlertDialog = builder.create()
        alert.show()
    }



    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }



    fun startDetection() {
        lightSensor?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
        setupWifiScanReceiver()
        Num_Connected_GPS()
        checkIndoorOutdoor()
    }

    fun stopDetection() {
        sensorManager.unregisterListener(this)
        if (isReceiverRegistered == "true") {
            context.unregisterReceiver(wifiScanReceiver)
        }
        locationManager.unregisterGnssStatusCallback(object : GnssStatus.Callback() {})
    }

}