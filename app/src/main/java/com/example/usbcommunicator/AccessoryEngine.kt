import android.annotation.SuppressLint
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
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean


class AccessoryEngine(private val mContext: Context, private val mCallback: IEngineCallback) {
    private val mUsbManager: UsbManager =
        mContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val componentName = "AccessoryEngine"

    @Volatile
    private var mAccessoryConnected = false
    private var mAccessory: UsbAccessory? = null
    private var mParcelFileDescriptor: ParcelFileDescriptor? = null
    private var mFileDescriptor: FileDescriptor? = null
    private var mInputStream: FileInputStream? = null
    private var mOutputStream: FileOutputStream? = null

    interface IEngineCallback {
        fun onConnectionEstablished()
        fun onDeviceDisconnected()
        fun onDataReceived(data: ByteArray?, num: Int)
    }

    fun onNewIntent(intent: Intent?) {
        connectAccessory()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun connectAccessory() {
        if (mAccessoryConnected && mOutputStream != null && mInputStream != null) {
            Log.d(componentName, "Already connected!")
            return
        }

        Log.d(componentName, "Discovering accessories...")
        mAccessory = mUsbManager.accessoryList?.firstOrNull()
        if (mAccessory == null) {
            Log.d(componentName, "No accessories found.")
            return
        }


        if (!mUsbManager.hasPermission(mAccessory)) {
            Log.d(componentName, "Permission missing, requesting...")
            mContext.registerReceiver(mPermissionReceiver, IntentFilter(ACTION_USB_PERMISSION))
            val pi = PendingIntent.getBroadcast(mContext, 0, Intent(ACTION_USB_PERMISSION), 0)
            mUsbManager.requestPermission(mAccessory, pi)
            return
        }

        Log.d(componentName, "Permission available, connecting...")
        mParcelFileDescriptor = mUsbManager.openAccessory(mAccessory)
        if (mParcelFileDescriptor == null) {
            Log.e(componentName, "Unable to open accessory!")
            return
        }

        mFileDescriptor = mParcelFileDescriptor!!.fileDescriptor
        mInputStream = FileInputStream(mFileDescriptor)
        mOutputStream = FileOutputStream(mFileDescriptor)
        mAccessoryConnected = true
        mCallback.onConnectionEstablished()

        sAccessoryThread = Thread(
            mAccessoryReader,
            "Reader Thread"
        )
        sAccessoryThread!!.start()
        Log.d(componentName, "Connection established.")
    }

    fun onDestroy() {
        mContext.unregisterReceiver(mDetachedReceiver)
    }

    private val mDetachedReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_ACCESSORY_DETACHED == intent.action) {
                mCallback.onDeviceDisconnected()
            }
        }
    }

    private val mPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mContext.unregisterReceiver(this)
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                Log.d(componentName, "USB Permission granted! Let's try to connect.")
                connectAccessory()
            } else {
                Log.d(componentName, "Permission denied!")
                Toast.makeText(mContext, "Permission denied! Please give permission!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun write(data: ByteArray) {
        if (!mAccessoryConnected || mOutputStream == null) {
            Log.d(componentName, "Unable to write: not connected.")
            Toast.makeText(mContext, "Not connected!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mOutputStream!!.write(data)
            Log.d(componentName, "write: Data send: $data")
        } catch (e: IOException) {
            Log.d(componentName, "write: could not send data")
        }
    }

    private val mAccessoryReader = Runnable {
        val buf = ByteArray(BUFFER_SIZE)
        while (true) {
            try {
                val read = mInputStream!!.read(buf)
                mCallback.onDataReceived(buf, read)
            } catch (e: Exception) {
                Log.d(componentName, "run:" + e.message)
                break
            }
        }
        Log.d(componentName, "run: exiting reader thread")
        if (mParcelFileDescriptor != null) {
            try {
                mParcelFileDescriptor!!.close()
            } catch (e: IOException) {
                Log.d(componentName, "run: Unable to close ParcelFD")
            }
        }
        if (mInputStream != null) {
            try {
                mInputStream!!.close()
            } catch (e: IOException) {
                Log.d(componentName, "run: Unable to close InputStream")
            }
        }
        if (mOutputStream != null) {
            try {
                mOutputStream!!.close()
            } catch (e: IOException) {
                Log.d(componentName, "run: Unable to close OutputStream")
            }
        }
        mAccessoryConnected = false
        sAccessoryThread = null
    }

    init {
        mContext.registerReceiver(
            mDetachedReceiver, IntentFilter(
                ACTION_ACCESSORY_DETACHED
            )
        )
    }

    companion object {
        private const val BUFFER_SIZE = 1024
        private const val ACTION_USB_PERMISSION = "com.example.usbcommunicator.USB_PERMISSION"
        private const val ACTION_ACCESSORY_DETACHED =
            "android.hardware.usb.action.USB_ACCESSORY_DETACHED"
        private var sAccessoryThread: Thread? = null
    }
}