// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import edu.utep.cs.cs4330.battleship.R;
import edu.utep.cs.cs4330.battleship.ai.AIPlayer;
import edu.utep.cs.cs4330.battleship.ai.Strategy;
import edu.utep.cs.cs4330.battleship.ai.StrategyType;
import edu.utep.cs.cs4330.battleship.model.game.BattleshipGame;
import edu.utep.cs.cs4330.battleship.model.board.Board;
import edu.utep.cs.cs4330.battleship.model.game.Player;
import edu.utep.cs.cs4330.battleship.model.board.Ship;
import edu.utep.cs.cs4330.battleship.util.Vector2;
import edu.utep.cs.cs4330.battleship.view.BoardView;

public class AIGameActivity extends AppCompatActivity implements BattleshipGame.GameListener {

    private enum Sound {Hit, Sink, Gameover}

    private static final boolean isAIAllowedMultipleShots = true;
    private boolean isSoundEnabled = true;

    /**
     * Small BoardView
     * This is our deployed board that the AI will try to beat
     */
    private BoardView boardViewDeployed;
    private Board boardDeployed;
    private int sunkShipsByCpu;

    /**
     * Big BoardView
     * This is the randomized board we want to beat
     */
    private BoardView boardViewRandom;
    private Board boardRandom;
    private int sunkShipsByHuman;

    private Switch switchSound;
    private TextView textCurrentPlayer;

    /**
     * Main game manager
     */
    private BattleshipGame gameMain;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("SOUND", isSoundEnabled);
        outState.putInt("SUNKCPU", sunkShipsByCpu);
        outState.putInt("SUNKHUMAN", sunkShipsByHuman);
        outState.putSerializable("DEPLOYED", boardDeployed);
        outState.putSerializable("RAND", boardRandom);
        outState.putSerializable("GAME", gameMain);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        isSoundEnabled = savedInstanceState.getBoolean("SOUND");
        switchSound.setChecked(isSoundEnabled);

        sunkShipsByCpu = savedInstanceState.getInt("SUNKCPU");
        sunkShipsByHuman = savedInstanceState.getInt("SUNKHUMAN");

        boardDeployed = (Board) savedInstanceState.getSerializable("DEPLOYED");
        boardViewDeployed.setBoard(boardDeployed);

        boardRandom = (Board) savedInstanceState.getSerializable("RAND");
        boardViewRandom.setBoard(boardRandom);

        gameMain = (BattleshipGame) savedInstanceState.getSerializable("GAME");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_ai_game);

        // Get the necessary views
        boardViewDeployed = (BoardView) findViewById(R.id.board_view_small);
        boardViewRandom = (BoardView) findViewById(R.id.board_view_big);

        textCurrentPlayer = (TextView) findViewById(R.id.text_current_player);

        switchSound = (Switch) findViewById(R.id.switch_sound);
        switchSound.setChecked(isSoundEnabled);

        if (savedInstanceState != null) {
            return;
        }

        Bundle extras = getIntent().getExtras();

        // Small BoardView
        // This is what the CPU will try to beat
        // It contains our deployment units

        // Load the board and strategy from our deployment activity
        Strategy strategyAI = Strategy.fromStrategyType(StrategyType.Sweep);
        if (extras != null) {
            boardDeployed = (Board) extras.get(getString(R.string.deployment_intent_board));

            StrategyType strategyType = (StrategyType) extras.get(getString(R.string.deployment_intent_strategy));
            strategyAI = Strategy.fromStrategyType(strategyType);
        }
        //boardDeployed = new Board(10);
        //boardDeployed.addRandomShips();
        boardDeployed.addBoardListener(new Board.BoardListener() {
            @Override
            public void onShipHit(Ship ship) {
                boardViewRandom.invalidate();
                boardViewDeployed.invalidate();
                playSound(Sound.Hit);
                if (ship.isDestroyed()) {
                    playSound(Sound.Sink);
                    sunkShipsByCpu++;

                    if (sunkShipsByCpu >= boardDeployed.getTotalShips())
                        showDialogGameover(false);
                }
            }

            @Override
            public void onShipMiss() {
                boardViewRandom.invalidate();
                boardViewDeployed.invalidate();

            }
        });
        boardViewDeployed.setBoard(boardDeployed);

        // This AI wants to defeat our deployed map
        final Player playerAI = new AIPlayer(boardDeployed, isAIAllowedMultipleShots, strategyAI);

        // Big BoardView
        // This is what we want to defeat
        // It contains random deployment units
        boardRandom = new Board(10);
        boardRandom.addRandomShips();
        boardRandom.addBoardListener(new Board.BoardListener() {
            @Override
            public void onShipHit(Ship ship) {
                boardViewRandom.invalidate();
                boardViewDeployed.invalidate();

                playSound(Sound.Hit);

                if (ship.isDestroyed()) {
                    playSound(Sound.Sink);
                    sunkShipsByHuman++;

                    if (sunkShipsByHuman >= boardRandom.getTotalShips())
                        showDialogGameover(true);
                }
            }

            @Override
            public void onShipMiss() {
                boardViewRandom.invalidate();
                boardViewDeployed.invalidate();
            }
        });
        boardViewRandom.setBoard(boardRandom);

        // We want to defeat the random board
        Player playerHuman = new Player(boardRandom, true);

        // Start our main game
        gameMain = new BattleshipGame(playerHuman, playerAI);
        gameMain.addGameListener(this);

        // For sound control
        switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSoundEnabled = isChecked;
            }
        });
    }


    @Override
    public void onBackPressed() {
        showDialogExit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_game_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_game_menu_sound) {
            isSoundEnabled = !isSoundEnabled;
            switchSound.setChecked(isSoundEnabled);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTurnChange(Player currentPlayer) {
        if (currentPlayer instanceof AIPlayer) {
            boardViewRandom.disableBoardTouch = true;
            textCurrentPlayer.setText(getString(R.string.game_turn_ai));
            Vector2 simulatedPlay = currentPlayer.onOwnTurn();
            boardDeployed.hit(simulatedPlay);

        } else {
            boardViewRandom.disableBoardTouch = false;
            textCurrentPlayer.setText(getString(R.string.game_turn_player));
        }
        boardViewRandom.invalidate();
        boardViewDeployed.invalidate();
    }

    public void playSound(Sound sound) {
        if (!isSoundEnabled)
            return;

        MediaPlayer mp;

        if (sound == Sound.Hit)
            mp = MediaPlayer.create(getApplicationContext(), R.raw.hit);
        else if (sound == Sound.Sink)
            mp = MediaPlayer.create(getApplicationContext(), R.raw.sink);
        else
            mp = MediaPlayer.create(getApplicationContext(), R.raw.gameover);

        mp.start();
    }

    public void onClickMainMenu(View v) {
        showDialogExit();
    }

    public void showDialogExit() {
        AlertDialog.Builder dialogExit = new AlertDialog.Builder(this);
        dialogExit.setTitle(getString(R.string.game_exit_title));
        dialogExit.setMessage(R.string.game_exit_message);
        dialogExit.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(getApplication(), MainMenuActivity.class);
                startActivity(i);
                finish();
            }
        });
        dialogExit.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialogExit.setIcon(android.R.drawable.ic_dialog_alert);
        dialogExit.show();
    }

    public void showDialogGameover(boolean humanWin) {
        String message;

        if (humanWin)
            message = getString(R.string.game_won);
        else
            message = getString(R.string.game_lost);

        playSound(Sound.Gameover);

        AlertDialog.Builder dialogGameover = new AlertDialog.Builder(this);
        dialogGameover.setTitle(getString(R.string.game_gameover_title));
        dialogGameover.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent i = new Intent(getApplication(), MainMenuActivity.class);
                startActivity(i);
                finish();
            }
        });
        dialogGameover.setIcon(android.R.drawable.ic_dialog_alert);
        dialogGameover.setMessage(message);
        dialogGameover.show();
    }
}
