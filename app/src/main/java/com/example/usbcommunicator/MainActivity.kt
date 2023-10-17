package com.example.usbcommunicator

import AccessoryEngine
import android.content.Intent
import android.graphics.Color
import android.hardware.usb.UsbAccessory
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var mEngine: AccessoryEngine;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onNewIntent(intent);

        findViewById<Button>(R.id.button0).setOnClickListener {
            mEngine.write(byteArrayOf(0x0))
        }
        findViewById<Button>(R.id.button1).setOnClickListener {
            mEngine.write(byteArrayOf(0x1))
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (!::mEngine.isInitialized) {
            mEngine = AccessoryEngine(applicationContext, mCallback)
        }
        mEngine.onNewIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        onNewIntent(intent)
    }

    private val mCallback: AccessoryEngine.IEngineCallback = object :
        AccessoryEngine.IEngineCallback {
        override fun onConnectionEstablished() {
            val tv = findViewById<TextView>(R.id.textView)
            tv.text = "Connected"
            tv.setTextColor(Color.GREEN)
        }

        override fun onDeviceDisconnected() {
            val tv = findViewById<TextView>(R.id.textView)
            tv.text = "Device disconnected"
            tv.setTextColor(Color.RED);
        }

        override fun onDataReceived(data: ByteArray?, num: Int) {
            val tv = findViewById<TextView>(R.id.textView2)
            if (data != null) {
                tv.text = data.decodeToString()
            }
        }
    }
}