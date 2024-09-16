package com.example.mobilegisproject.Classes

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.mobilegisproject.MainActivity.Companion.sharedPreferences
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import org.json.JSONObject
import org.osgeo.proj4j.CRSFactory
import org.osgeo.proj4j.CoordinateReferenceSystem
import org.osgeo.proj4j.ProjCoordinate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.DecimalFormat

class ReadJson(val context: Context, private val assetManager: AssetManager, private val fileName: String): Thread(){
    companion object{
        var pathpoints_WGS = ArrayList<List<LatLng>>()
    }

    @SuppressLint("CommitPrefEdits")
    override fun run() {
        try {

        val inputStream = assetManager.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String? = reader.readLine()

            while (line != null) {
                stringBuilder.append(line)
                line = reader.readLine()

            }
            reader.close()
            inputStream.close()
            val jsonString = stringBuilder.toString()

            val jsonObject = JSONObject(jsonString)
            val featuresArray = jsonObject.getJSONArray("features")

            val df = DecimalFormat("#.##############")
            for (i in 0 until featuresArray.length()) {

                val pathpoints_UTM = ArrayList<ProjCoordinate>()
                val featureObject = featuresArray.getJSONObject(i)
                val attributes = featureObject.getJSONObject("attributes")
                val geometry = featureObject.getJSONObject("geometry")
                val paths = geometry.getJSONArray("paths")
                val path = paths.getJSONArray(0)

                for (j in 0 until path.length()) {
                    val x = path.getJSONArray(j).getDouble(0)
                    val y = path.getJSONArray(j).getDouble(1)
                    pathpoints_UTM.add(ProjCoordinate(x, y))
                }
                pathpoints_WGS.add(UTM_to_WGS84(pathpoints_UTM))
//            MarkersList.geometry_pathpoints.add(pathpoints)
//            MarkersList.FID.add(attributes.getInt("FID"))
//            MarkersList.ID.add(attributes.getInt("ID"))
//            MarkersList.length.add(df.format(attributes.getDouble("length")).toDouble())
//            MarkersList.x_end.add(df.format(attributes.getDouble("x_end")).toDouble())
//            MarkersList.x_start.add(df.format(attributes.getDouble("x_start")).toDouble())
//            MarkersList.y_end.add(df.format(attributes.getDouble("y_end")).toDouble())
//            MarkersList.y_start.add(df.format(attributes.getDouble("y_start")).toDouble())
            }
            Log.i("message","read json: " + pathpoints_WGS.toString())
            sharedPreferences = context.getSharedPreferences("Stores", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val pathpoints_WGS_json = Gson().toJson(pathpoints_WGS)
            editor.putString("poly_lines",pathpoints_WGS_json)
            editor.apply()
//            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.764953 ,51.409953), 20.0f))
            }catch (e:Exception){
                    Log.e("catching",e.message.toString())
            }


        super.run()
    }


    fun UTM_to_WGS84(utmPoints: ArrayList<ProjCoordinate>): List<LatLng> {
        val crsFactory = CRSFactory()
        val utmCrs: CoordinateReferenceSystem =
            crsFactory.createFromName("EPSG:32639") // Example for UTM zone 33N
        val wgs84Crs: CoordinateReferenceSystem = crsFactory.createFromName("EPSG:4326")
        val transform = org.osgeo.proj4j.BasicCoordinateTransform(utmCrs, wgs84Crs)

        val latLngPoints = utmPoints.map { utmCoord ->
            val wgs84Coord = ProjCoordinate()
            transform.transform(utmCoord, wgs84Coord)
            LatLng(wgs84Coord.y, wgs84Coord.x)
        }
        return latLngPoints
    }


}