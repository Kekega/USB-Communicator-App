package com.example.usbcommunicator

import android.content.Intent
import android.graphics.Color
import android.hardware.usb.UsbAccessory
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.usbcommunicator.AccessoryEngine.IEngineCallback


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

    private val mCallback: IEngineCallback = object : IEngineCallback {
        override fun onDeviceDisconnected() {
            val tv = findViewById<TextView>(R.id.textView)
            tv.text = "Device disconnected"
            tv.setTextColor(Color.RED);
        }

        override fun onConnectionEstablished(mAccessory: UsbAccessory) {
            val tv = findViewById<TextView>(R.id.textView)
            tv.text = "Connected to ${mAccessory.model}"
            tv.setTextColor(Color.GREEN)
        }
    }
}