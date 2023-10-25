package com.example.usbcommunicator;

public interface IUsbCallback {
    void onConnectionEstablished() ;

    void onDeviceDisconnected() ;

    void onDataReceived(byte[] data, int num) ;
}
