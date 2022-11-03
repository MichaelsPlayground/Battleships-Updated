// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import edu.utep.cs.cs4330.battleship.R;
import edu.utep.cs.cs4330.battleship.model.GameType;

public class MainMenuActivity extends AppCompatActivity {

    /**
     * This block is for requesting permissions on Android 12+
     * @param savedInstanceState
     */

    private static final int PERMISSIONS_REQUEST_CODE = 191;
    private static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add fade transitions
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_main_menu);

        requestBlePermissions(this, PERMISSIONS_REQUEST_CODE);

    }

    public void onClickSinglePlayer(View v) {
        Intent i = new Intent(this, DeploymentActivity.class);
        i.putExtra(getString(R.string.main_menu_intent_gamemode), GameType.Singleplayer);
        startActivity(i);
    }

    public void onClickMultiPlayer(View view){
        Intent i = new Intent(this, DeploymentActivity.class);
        i.putExtra(getString(R.string.main_menu_intent_gamemode), GameType.Multiplayer);
        startActivity(i);
    }



}
