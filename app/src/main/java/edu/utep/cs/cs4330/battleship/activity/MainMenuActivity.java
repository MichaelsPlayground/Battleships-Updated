// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import edu.utep.cs.cs4330.battleship.R;
import edu.utep.cs.cs4330.battleship.model.GameType;

public class MainMenuActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add fade transitions
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_main_menu);
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
