// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.fragment;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;

import edu.utep.cs.cs4330.battleship.R;
import edu.utep.cs.cs4330.battleship.activity.DeploymentActivity;
import edu.utep.cs.cs4330.battleship.network.NetworkAdapterType;


public class DeployNetworkFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {
    private DeploymentActivity activity;
    private RadioGroup radioGroupNetwork;
    public NetworkAdapterType networkAdapterType;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (DeploymentActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_deploy_multi, container, false);

        radioGroupNetwork = (RadioGroup) view.findViewById(R.id.radioGroupNetwork);
        radioGroupNetwork.setOnCheckedChangeListener(this);

        if (BluetoothAdapter.getDefaultAdapter() == null)
            view.findViewById(R.id.radioBluetooth).setEnabled(false);

        return view;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        if (checkedId == R.id.radioBluetooth)
            networkAdapterType = NetworkAdapterType.Bluetooth;

        activity.onFragmentUpdate(true);
    }
}