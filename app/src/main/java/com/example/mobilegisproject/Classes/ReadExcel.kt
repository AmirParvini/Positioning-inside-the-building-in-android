package com.example.mobilegisproject.Classes

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.mobilegisproject.MainActivity.Companion.sharedPreferences
import jxl.Workbook
import java.io.InputStream

class ReadExcel(private val context: Context, private val database: SQLiteDatabase) {

    val Values = ArrayList<ArrayList<String>>()
    @SuppressLint("Recycle")
    fun readExcelFile(fileName: String) {
//        try {
            val inputStream: InputStream = context.assets.open(fileName)
            val workbook = Workbook.getWorkbook(inputStream)
            val sheet = workbook.getSheet(0) // دسترسی به اولین شیت
            for (row in 1 until sheet.rows) {
                val rowvalue = ArrayList<String>()
                for (col in 0 until sheet.columns) {
                    val cell = sheet.getCell(col, row)
                    if (cell.contents.toString() != "") {
                        rowvalue.add(cell.contents.toString())
                        Log.d("ExcelReader", "Cell[$row][$col]: ${cell.contents}")
                    }
                }
                Values.add(rowvalue)
            }
            inputStream.close()
            FillingWiFiTable(Values)
            val editor = sharedPreferences.edit()
            editor.putString("plan_excel", "Excel has been read")
            editor.apply()
//        } catch (e: Exception) {
//            Log.d("catching", e.message.toString())
//        }
    }


    fun FillingWiFiTable(values:ArrayList<ArrayList<String>>){
        for (row in values){
            Log.i("message", "Row = $row")
        database.execSQL("INSERT INTO WiFiTB (projectname, x, y, ssid, bssid, rss, pointname) " +
                "VALUES ('${row[0]}', '${row[1].toDouble()}', '${row[2].toDouble()}', '${row[3]}', '${row[4]}', '${row[5].toInt()}', '${row[6]}')")
        }
    }
}