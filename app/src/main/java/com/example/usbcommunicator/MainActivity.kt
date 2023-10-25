package com.example.usbcommunicator

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var usbEngine: UsbEngine;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onNewIntent(intent);

        findViewById<Button>(R.id.button0).setOnClickListener {
            usbEngine.write("0".toByteArray())
        }
        findViewById<Button>(R.id.button1).setOnClickListener {
            usbEngine.write("1".toByteArray())

        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (!::usbEngine.isInitialized) {
            usbEngine = UsbEngine(applicationContext, mCallback)
        }
        usbEngine.onNewIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        onNewIntent(intent)
    }

    private val mCallback: IUsbCallback = object: IUsbCallback  {
        override fun onConnectionEstablished() {
            val tv = findViewById<TextView>(R.id.textView)
            tv.text = usbEngine.connectionStatus()
            tv.setTextColor(Color.GREEN)
        }

        override fun onDeviceDisconnected() {
            val tv = findViewById<TextView>(R.id.textView)
            tv.text = usbEngine.connectionStatus()
            tv.setTextColor(Color.RED);
        }

        override fun onDataReceived(data: ByteArray?, num: Int) {
            val tv = findViewById<TextView>(R.id.textView2)
            if (data != null) {
                Log.d("App", data.decodeToString(endIndex = num))
                var text = data.decodeToString(endIndex = num)
                if (text.length > 10){
                    text = text.substring(0, 10) + "..."
                }
                tv.text = text
            }
        }
    }
}