package com.example.mobilegisproject

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.mobilegisproject.MainActivity.Companion.cycle
import com.example.mobilegisproject.MainActivity.Companion.database
import com.google.android.material.textfield.TextInputEditText


class SettingActivity : AppCompatActivity() {

    lateinit var timecycle: TextInputEditText
    lateinit var savebtn: Button

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        timecycle = findViewById(R.id.ltc_id)
        savebtn = findViewById(R.id.save_and_exit_btn_id)


        savebtn.setOnClickListener {
            if (timecycle.text.toString() != "" && timecycle.text.toString().toInt() > 0){
                val values = ContentValues()
                values.put("cycle", timecycle.text.toString().toInt())
                database.update("SettingTB",values,"id", null)
                cycle = timecycle.text.toString().toInt()
                Toast.makeText(this,"Location Time Cycle is set ${timecycle.text}s", Toast.LENGTH_SHORT).show()
                this.finish()
            }
            if (timecycle.text.toString() != "" && timecycle.text.toString().toInt() < 0){
                Toast.makeText(this,"Please set a Positive Number!", Toast.LENGTH_LONG).show()
            }
            if (timecycle.text.toString() == "" ){
                Toast.makeText(this,"Please set Location Time Cycle!", Toast.LENGTH_LONG).show()
            }
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        this.finish()
        super.onBackPressed()
    }
}