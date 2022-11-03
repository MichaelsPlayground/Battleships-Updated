// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.network.thread;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

import edu.utep.cs.cs4330.battleship.R;

public class HostThread extends Thread {
    private BluetoothServerSocket mmServerSocket;
    private final BluetoothAdapter bluetoothAdapter;
    //private Context mContext;

    @SuppressLint("MissingPermission")
    //public HostThread(Resources resources, Context context) {
    public HostThread(Resources resources) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        BluetoothServerSocket tmp = null;

        try {
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(resources.getString(R.string.app_name), UUID.fromString(resources.getString(R.string.app_uuid)));
        } catch (IOException e) {
            Log.e("Debug", "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        Log.d("Debug", "HostThread in run()");
        BluetoothSocket socket;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                Log.d("Debug", "Waiting for client...");
                socket = mmServerSocket.accept();
                Log.d("Debug", "Found a client");
            } catch (IOException e) {
                Log.e("Debug", "Socket's accept() method failed", e);
                break;
            }
            Log.d("Debug", "Client is null: " + (socket == null));

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                manageMyConnectedSocket(socket);
                try {
                    // Stop listening for clients to connect
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.e("Debug", "Server Socket's close() method failed", e);
                }
                break;
            }
        }
    }

    public void manageMyConnectedSocket(BluetoothSocket socket) {
        Log.d("Debug", "Host thread is starting threads");
        new SendingThread(socket).start();
        new ReceivingThread(socket).start();
    }
}