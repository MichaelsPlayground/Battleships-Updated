// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import edu.utep.cs.cs4330.battleship.R;
import edu.utep.cs.cs4330.battleship.fragment.DeployAIFragment;
import edu.utep.cs.cs4330.battleship.fragment.DeployNetworkFragment;
import edu.utep.cs.cs4330.battleship.model.GameType;
import edu.utep.cs.cs4330.battleship.model.board.Ship;
import edu.utep.cs.cs4330.battleship.network.NetworkAdapterType;
import edu.utep.cs.cs4330.battleship.view.DeploymentBoardView;

public class DeploymentActivity extends AppCompatActivity implements DeploymentBoardView.DeploymentListener {

    private TextView textGamemode;
    private TextView textDeployStatus;
    private DeploymentBoardView boardViewDeployment;
    private Switch switchRotation;
    private Button buttonDeploy;

    private GameType gameType;
    private Fragment fragmentDeployment;
    private boolean isFragmentReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_deployment);

        textGamemode = (TextView) findViewById(R.id.textDepGamemode);
        boardViewDeployment = (DeploymentBoardView) findViewById(R.id.boardViewDeployment);
        buttonDeploy = (Button) findViewById(R.id.btnDeploy);
        switchRotation = (Switch) findViewById(R.id.switch_rotation);
        textDeployStatus = (TextView) findViewById(R.id.textDeployStatus);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            gameType = (GameType)extras.get(getString(R.string.main_menu_intent_gamemode));

            Fragment newFragment;

            if(gameType == GameType.Singleplayer){
                textGamemode.setText(getString(R.string.main_menu_singleplayer_description));
                newFragment = new DeployAIFragment();
            }
            else{
                textGamemode.setText(getString(R.string.main_menu_multiplayer_description));
                newFragment = new DeployNetworkFragment();
            }

            // Add Fragment to UI
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if(fragmentDeployment == null){
                transaction.add(R.id.fragment_container, newFragment);
            }
            else{
                transaction.replace(R.id.fragment_container, newFragment);
            }

            fragmentDeployment = newFragment;
            transaction.commit();

        }

        boardViewDeployment.addListener(this);

        switchRotation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boardViewDeployment.rotationMode = isChecked;
            }
        });

        updateDeployStatus();
    }

    @Override
    public void onShipDeployed(Ship ship) {
        updateDeployStatus();
    }

    public void updateDeployStatus() {
        int shipTotal = boardViewDeployment.getRemainingShips();

        if (shipTotal == 0)
            textDeployStatus.setText(getString(R.string.deployment_all_ships));
        else
            textDeployStatus.setText(getString(R.string.deployment_remaining_ships) + shipTotal);

        checkReady();
    }

    public void onFragmentUpdate(boolean isFragmentReady){
        this.isFragmentReady = isFragmentReady;
        checkReady();
    }

    public void checkReady() {
        int shipTotal = boardViewDeployment.getRemainingShips();
        boolean isReady = (shipTotal == 0 && isFragmentReady);
        buttonDeploy.setEnabled(isReady);
    }

    public void onClickReset(View v) {
        boardViewDeployment.onCreate();
        updateDeployStatus();
        checkReady();
    }

    public void onClickDeploy(View v) {
        // Single-player VS AI game
        if (gameType == GameType.Singleplayer) {
            // Get the strategy from the fragment
            DeployAIFragment fragment = (DeployAIFragment) fragmentDeployment;
            Intent i = new Intent(this, AIGameActivity.class);
            i.putExtra(getString(R.string.deployment_intent_board), boardViewDeployment.getBoard());
            i.putExtra(getString(R.string.deployment_intent_strategy), fragment.strategyAI);
            startActivity(i);
        }
        // Multi-player VS Human game
        else {
            DeployNetworkFragment fragment = (DeployNetworkFragment) fragmentDeployment;
            NetworkAdapterType networkType = fragment.networkAdapterType;

            if(networkType == NetworkAdapterType.Bluetooth){
                Intent i = new Intent(this, BluetoothSetupActivity.class);
                i.putExtra("BOARD", boardViewDeployment.getBoard());
                startActivity(i);
                finish();
            }
        }
    }
}
