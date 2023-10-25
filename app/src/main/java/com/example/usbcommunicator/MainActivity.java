package com.example.usbcommunicator;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;

public final class MainActivity extends AppCompatActivity {
    private final IUsbCallback mCallback = new IUsbCallback() {
        public void onConnectionEstablished() {
            TextView tv = findViewById(R.id.textView);
            tv.setText(usbEngine.connectionStatus());
            tv.setTextColor(Color.GREEN);
        }

        public void onDeviceDisconnected() {
            TextView tv = findViewById(R.id.textView);
            tv.setText(usbEngine.connectionStatus());
            tv.setTextColor(Color.RED);
        }

        public void onDataReceived(@Nullable byte[] data, int num) {
            TextView tv = findViewById(R.id.textView2);
            if (data == null) {
                Log.d("App", "Received empty data!");
                return;
            }

            String text = new String(data, 0, num, StandardCharsets.UTF_8);
            Log.d("App", "Received: " + text);
            if (text.length() > 10) {
                text = text.substring(0, 10) + "...";
            }

            tv.setText(text);
        }

    };
    private UsbEngine usbEngine;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbEngine = new UsbEngine(getApplicationContext(), mCallback);
        this.onNewIntent(this.getIntent());

        this.findViewById(R.id.button0).setOnClickListener(it -> {
            usbEngine.write("0".getBytes());
        });

        this.findViewById(R.id.button1).setOnClickListener(it -> {
            usbEngine.write("1".getBytes());
        });
    }

    protected void onNewIntent(@Nullable Intent intent) {
        super.onNewIntent(intent);
        usbEngine.onNewIntent(intent);
    }

    protected void onResume() {
        super.onResume();
        Intent intent = this.getIntent();
        this.onNewIntent(intent);
    }
}