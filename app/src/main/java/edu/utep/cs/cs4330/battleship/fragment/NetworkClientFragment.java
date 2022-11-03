// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Network;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import edu.utep.cs.cs4330.battleship.R;
import edu.utep.cs.cs4330.battleship.activity.BluetoothSetupActivity;
import edu.utep.cs.cs4330.battleship.activity.NetworkGameActivity;
import edu.utep.cs.cs4330.battleship.model.board.Board;
import edu.utep.cs.cs4330.battleship.network.NetworkInterface;
import edu.utep.cs.cs4330.battleship.network.NetworkManager;
import edu.utep.cs.cs4330.battleship.network.packet.Packet;
import edu.utep.cs.cs4330.battleship.network.packet.PacketClientHandshake;
import edu.utep.cs.cs4330.battleship.network.packet.PacketHostHandshake;
import edu.utep.cs.cs4330.battleship.network.thread.ClientThread;

import static android.app.Activity.RESULT_OK;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;


public class NetworkClientFragment extends Fragment implements NetworkInterface {
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_COARSE_LOCATION = 5;

    private final BluetoothAdapter bluetoothAdapter;

    private TextView textClientStatus;
    private ProgressBar progressBarClient;
    private Button btnClient;

    public NetworkClientFragment() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_network_client, container, false);

        textClientStatus = (TextView) view.findViewById(R.id.textClientStatus);
        progressBarClient = (ProgressBar) view.findViewById(R.id.progressBarClient);
        btnClient = (Button) view.findViewById(R.id.btnClient);
        btnClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickBtnClient();
            }
        });

        // Make sure everything is started correctly
        reset();

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetworkManager.registerNetworkInterface(getActivity(), this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Request to be notified when another device is found
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        Intent sticky = getActivity().registerReceiver(mReceiver, filter);
        Log.d("Debug", "Sticky: " + (sticky == null));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            // We got permission
            if (resultCode == RESULT_OK)
                onBluetoothEnabled(false);
            else
                reset();
        }
    }

    public void onClickBtnClient() {
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            // It isn't. Ask the user to enable it
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            textClientStatus.setText("Enabling bluetooth");
            progressBarClient.setIndeterminate(true);
        } else
            onBluetoothEnabled(false);
    }


    public void onBluetoothEnabled(boolean overridePermission) {
        // Check permissions (required for > 6.0)
        boolean hasPerm = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        // Start looking for device if we have permission
        if (overridePermission || hasPerm) {
            // Start looking for devices
            boolean discovery = bluetoothAdapter.startDiscovery();

            progressBarClient.setIndeterminate(true);
            textClientStatus.setText("Looking for host: " + discovery);
            btnClient.setEnabled(false);
        } else {
            // No permission so request it
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);

            textClientStatus.setText("Requesting permissions");
        }
    }

    public void reset() {
        textClientStatus.setText("Ready to connect");
        btnClient.setEnabled(true);
        progressBarClient.setIndeterminate(false);
    }

    @SuppressLint("MissingPermission")
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean isBluetoothAction = BluetoothDevice.ACTION_FOUND.equals(action) || BluetoothDevice.ACTION_NAME_CHANGED.equals(action);
            if (isBluetoothAction) {
                // Discovery has found a device. Get the BluetoothDevice object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String deviceName = device.getName();
                if(deviceName != null) {
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    Log.d("Debug", "Name: " + deviceName + ", " + deviceHardwareAddress + " connected");
                    new ClientThread(device).start();
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                // Check to see if they granted us permission
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    textClientStatus.setText("Received permission");
                    onBluetoothEnabled(true);
                } else {
                    // We weren't given permission
                    reset();
                }
                break;
            }
        }
    }

    public boolean isSendingPacket = false;
    @Override
    public void onConnect() {
        textClientStatus.setText("Connected to host");
        progressBarClient.setIndeterminate(false);

        isSendingPacket = true;
        if(isAdded())
            sendHandshake();
    }

    public void sendHandshake(){
        Board board = ((BluetoothSetupActivity) getActivity()).boardDeployment;
        NetworkManager.sendPacket(new PacketClientHandshake("Client", board));

    }

    @Override
    public void onStart() {
        super.onStart();

        if (isSendingPacket)
            sendHandshake();
    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onReceive(final Packet p) {
        if(p instanceof PacketHostHandshake)
        {
            PacketHostHandshake packetHostHandshake = (PacketHostHandshake)p;
            Board board = ((BluetoothSetupActivity)getActivity()).boardDeployment;
            Intent i = new Intent(getActivity(), NetworkGameActivity.class);
            i.putExtra("OWN", board);
            i.putExtra("OPPONENT", packetHostHandshake.hostBoard);
            i.putExtra("FIRST", packetHostHandshake.isClientFirst);
            startActivity(i);
            getActivity().finish();
            NetworkManager.unregisterNetworkInterface(getActivity(), this);
        }
    }
}
