package com.example.usbcommunicator

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var mAccessoryEngine: AccessoryEngine;
    private lateinit var mDeviceEngine: DeviceEngine;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onNewIntent(intent);

        findViewById<Button>(R.id.button0).setOnClickListener {
            mAccessoryEngine.write(byteArrayOf(0x0))
            mDeviceEngine.write("0".toByteArray())
        }
        findViewById<Button>(R.id.button1).setOnClickListener {
            mAccessoryEngine.write(byteArrayOf(0x1))
            mDeviceEngine.write("1".toByteArray())
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (!::mAccessoryEngine.isInitialized) {
            mAccessoryEngine = AccessoryEngine(applicationContext, mCallback)
        }
        if (!::mDeviceEngine.isInitialized) {
            mDeviceEngine = DeviceEngine(applicationContext, mCallback)
        }
        mDeviceEngine.onIntent(intent)
        mAccessoryEngine.onIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        onNewIntent(intent)
    }

    private val mCallback: IUsbEngineCallback = object :
        IUsbEngineCallback {
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