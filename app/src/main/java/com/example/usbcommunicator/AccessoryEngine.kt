package com.example.usbcommunicator

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import java.io.FileOutputStream
import java.io.IOException

class AccessoryEngine(private val mContext: Context, private val mCallback: IEngineCallback) {
    private val componentName = "AccessoryEngine"
    private val mUsbManager: UsbManager =
        mContext.getSystemService(Context.USB_SERVICE) as UsbManager

    @Volatile
    private var mAccessoryConnected = false
    private var mAccessory: UsbAccessory? = null
    private var mParcelFileDescriptor: ParcelFileDescriptor? = null
    private var mOutputStream: FileOutputStream? = null

    interface IEngineCallback {
        fun onConnectionEstablished(mAccessory: UsbAccessory)
        fun onDeviceDisconnected()
    }

    fun onNewIntent(intent: Intent?) {
        connectAccessory()
    }

    private fun connectAccessory() {
        if (isConnectionEstablished()){
            Log.d(componentName, "Already connected.")
            return;
        }

        // Find accessory
        Log.d(componentName, "Searching for accessories...");
        mAccessory = mUsbManager.accessoryList?.firstOrNull()
        if (mAccessory == null) {
            Log.d(componentName, "No accessories available.")
            return
        }
        Log.d(componentName, "Accessory found")

        // Obtain permission if needed
        if (!mUsbManager.hasPermission(mAccessory)) {
            Log.d(componentName, "Permission missing, requesting...")
            mContext.registerReceiver(mPermissionReceiver, IntentFilter(ACTION_USB_PERMISSION))
            val pi = PendingIntent.getBroadcast(mContext, 0, Intent(ACTION_USB_PERMISSION), 0)
            mUsbManager.requestPermission(mAccessory, pi)
            return
        }
        Log.d(componentName, "Permission available, connecting...")

        // Open accessory
        mParcelFileDescriptor = mUsbManager.openAccessory(mAccessory)
        if (mParcelFileDescriptor == null) {
            Log.w(componentName, "Unable to open accessory")
            return
        }

        // Open output stream
        val mFileDescriptor = mParcelFileDescriptor!!.fileDescriptor
        mOutputStream = FileOutputStream(mFileDescriptor)
        mCallback.onConnectionEstablished(mAccessory!!)
        mAccessoryConnected = true

        if (isConnectionEstablished()){
            Log.d(componentName, "Accessory connected!")
        } else {
            Log.d(componentName, "Failed to establish connection!")
        }
    }

    private fun disconnect(){
        mParcelFileDescriptor?.close()
        mOutputStream?.close()
        mAccessory = null
        mAccessoryConnected = false
        mCallback.onDeviceDisconnected()
    }

    private val mDetachedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED == intent.action) {
                Log.d(componentName, "Accessory detached, closing connection...")
                disconnect()
            }
        }
    }
    private val mAttachedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED == intent.action) {
                Log.d(componentName, "Detected new accessory! Let's try to connect.")
                connectAccessory()
            }
        }
    }
    private val mPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mContext.unregisterReceiver(this)
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                Log.d(componentName, "USB Permission granted! Let's try to connect.")
                connectAccessory()
            }
        }
    }

    private fun isConnectionEstablished(): Boolean {
        if (!mAccessoryConnected || mAccessory == null || mOutputStream == null) {
            return false
        }

        try {
            mOutputStream!!.write(byteArrayOf())
            return true
        } catch (e: IOException) {
            return false
        }
    }

    init {
        mContext.registerReceiver(
            mAttachedReceiver,
            IntentFilter(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        )
        mContext.registerReceiver(
            mDetachedReceiver,
            IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        )
        connectAccessory()
    }

    fun write(data: ByteArray?) {
        if (mAccessoryConnected && mOutputStream != null) {
            try {
                mOutputStream!!.write(data)
            } catch (e: IOException) {
                Toast.makeText(mContext, "Failed to send signal. Check logs", Toast.LENGTH_SHORT)
                    .show()
                Log.e(componentName, "Send failed!", e)
            }
        } else {
            Toast.makeText(mContext, "No device available", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.example.usbcommunicator.USB_PERMISSION"
    }
}