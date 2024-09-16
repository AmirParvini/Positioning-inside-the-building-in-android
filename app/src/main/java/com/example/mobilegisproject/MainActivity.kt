package com.example.mobilegisproject

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.mobilegisproject.Classes.ReadExcel
import com.example.mobilegisproject.Classes.ReadJson
import com.example.mobilegisproject.Classes.ReadJson.Companion.pathpoints_WGS
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    companion object{
        lateinit var sharedPreferences: SharedPreferences
        lateinit var database: SQLiteDatabase
        lateinit var databasepath: String
        lateinit var cursor: Cursor
        var cycle: Int = 3
    }
    var mapbtn: ImageView? = null

    @SuppressLint("Recycle")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("Stores", MODE_PRIVATE)

        //Database  -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -
        databasepath = this.filesDir.toString()
        database = SQLiteDatabase.openDatabase("$databasepath/LocationDB.db",null,SQLiteDatabase.CREATE_IF_NECESSARY)
        val create_location_table_query = "create table if not Exists UserLocationTB(id INTEGER PRIMARY KEY AUTOINCREMENT, latitude DOUBLE, longitude DOUBLE)"
        val create_setting_table_query = "create table if not Exists SettingTB(id INTEGER PRIMARY KEY AUTOINCREMENT, cycle INT)"
        val create_wifi_table_query = "create table if not Exists WiFiTB(id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "projectname STRING, x DOUBLE, y DOUBLE, ssid STRING, bssid STRING, rss INT, pointname STRING)"
        database.execSQL(create_location_table_query)
        database.execSQL(create_setting_table_query)
        database.execSQL(create_wifi_table_query)
        cursor = database.rawQuery("SELECT COUNT(*) FROM settingTB", null)
        cursor.moveToFirst()
        if (cursor.getInt(0) == 0){
            database.execSQL("INSERT INTO SettingTB (cycle) VALUES (3)")
            cycle = 3
            cursor.close()
        }
        else{
            cursor = database.rawQuery("SELECT cycle FROM SettingTB WHERE id=1", null)
            cursor.moveToFirst()
            cycle = cursor.getInt(cursor.getColumnIndexOrThrow("cycle"))
            cursor.close()
        }
        //Database  -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -




        //Read excel and json files -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -
        val cursor = database.rawQuery("SELECT COUNT(*) FROM WiFiTB", null)
        cursor.moveToFirst()
        if (cursor.getInt(0) == 0){
            val readExcel = ReadExcel(this,database)
            readExcel.readExcelFile("plan excel.xls")
        }
        if (sharedPreferences.getString("poly_lines","")?.isEmpty() == true){
            Log.i("message", "sharedPreferences is null")
            ReadJson(this, this.assets,"plan to json.json").start()
        }else{
            Log.i("message", "sharedPreferences is not Empty")
            val polylines_json = sharedPreferences.getString("poly_lines","")
            val arraylisttype = object : TypeToken<ArrayList<List<LatLng>>>() {}.type
            pathpoints_WGS = Gson().fromJson(polylines_json,arraylisttype)
        }
        //Read excel and json files -   -   -   -   -   -   -   -   -   -   -   -   -   -   -   -





        if (Build.VERSION.SDK_INT >= 23) {
            isStoragePermissionGranted()
        }

        mapbtn = findViewById(R.id.map_menu_id)

        mapbtn!!.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun isStoragePermissionGranted(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED){

                Log.v(ContentValues.TAG, "Permission is granted")
                return true

            }

            else {
                Log.v(ContentValues.TAG, "Permission is revoked")
                ActivityCompat.requestPermissions(this,
                    arrayOf(
                        android.Manifest.permission.ACCESS_WIFI_STATE,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), 1)
                return false
            }

        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(ContentValues.TAG, "Permission is granted")
            return true
        }

    }
}