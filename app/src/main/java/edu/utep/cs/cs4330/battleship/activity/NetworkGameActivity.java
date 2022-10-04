// Author: Jose Perez <josegperez@mail.com> and Diego Reynoso
package edu.utep.cs.cs4330.battleship.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import edu.utep.cs.cs4330.battleship.R;
import edu.utep.cs.cs4330.battleship.model.board.Board;
import edu.utep.cs.cs4330.battleship.model.board.Ship;
import edu.utep.cs.cs4330.battleship.model.game.BattleshipGame;
import edu.utep.cs.cs4330.battleship.model.game.Player;
import edu.utep.cs.cs4330.battleship.network.NetworkInterface;
import edu.utep.cs.cs4330.battleship.network.NetworkManager;
import edu.utep.cs.cs4330.battleship.network.NetworkPlayer;
import edu.utep.cs.cs4330.battleship.network.packet.Packet;
import edu.utep.cs.cs4330.battleship.network.packet.PacketGameover;
import edu.utep.cs.cs4330.battleship.network.packet.PacketHit;
import edu.utep.cs.cs4330.battleship.network.packet.PacketRequestResponse;
import edu.utep.cs.cs4330.battleship.network.packet.PacketRestartRequest;
import edu.utep.cs.cs4330.battleship.view.NetworkBoardView;

public class NetworkGameActivity extends AppCompatActivity implements BattleshipGame.GameListener, NetworkInterface {
    private enum Sound {Hit, Sink, Gameover}

    private boolean isSoundEnabled = true;
    /**
     * Small BoardView
     * This is our deployed board that the network player will try to beat
     */
    private NetworkBoardView boardViewOwn;
    private Board boardOwn;
    private int sunkShipsByOpponent;

    /**
     * Big BoardView
     * This is the board we want to beat
     */
    private NetworkBoardView boardViewOpponent;
    private Board boardOpponent;
    private int sunkShipsByUs;

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
        outState.putInt("SUNKCPU", sunkShipsByOpponent);
        outState.putInt("SUNKHUMAN", sunkShipsByUs);
        outState.putSerializable("DEPLOYED", boardOwn);
        outState.putSerializable("RAND", boardOpponent);
        Log.d("Debug", "Saving game");
        //outState.putSerializable("GAME", gameMain);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        isSoundEnabled = savedInstanceState.getBoolean("SOUND");
        switchSound.setChecked(isSoundEnabled);

        sunkShipsByOpponent = savedInstanceState.getInt("SUNKCPU");
        sunkShipsByUs = savedInstanceState.getInt("SUNKHUMAN");

        boardOwn = (Board) savedInstanceState.getSerializable("DEPLOYED");
        boardViewOwn.setBoard(boardOwn);

        boardOpponent = (Board) savedInstanceState.getSerializable("RAND");
        boardViewOpponent.setBoard(boardOpponent);

        //gameMain = (BattleshipGame) savedInstanceState.getSerializable("GAME");
    }

    public void restart(){
        Bundle extras = getIntent().getExtras();
        // Small BoardView
        // This is what the CPU will try to beat
        // It contains our deployment units

        // Load the board and strategy from our deployment activity
        boolean weGoFirst = false;
        if (extras != null) {
            boardOwn = (Board) extras.get("OWN");
            boardOpponent = (Board) extras.get("OPPONENT");
            weGoFirst = extras.getBoolean("FIRST");
        }

        boardOwn.addBoardListener(new Board.BoardListener() {
            @Override
            public void onShipHit(Ship ship) {
                boardViewOpponent.invalidate();
                boardViewOwn.invalidate();
                playSound(Sound.Hit);
                if (ship.isDestroyed()) {
                    playSound(Sound.Sink);
                    sunkShipsByOpponent++;

                    if (sunkShipsByOpponent >= boardOwn.getTotalShips())
                        showDialogGameover(true, true);
                }
            }

            @Override
            public void onShipMiss() {
                boardViewOpponent.invalidate();
                boardViewOwn.invalidate();

            }
        });
        boardViewOwn.setBoard(boardOwn);
        boardViewOwn.onCreate(this);
        boardViewOwn.showShips = true;
        boardViewOwn.disableBoardTouch = true;
        boardViewOwn.receivesPackets = true;

        // This AI wants to defeat our deployed map
        //final Player playerAI = new AIPlayer(boardOwn, isAIAllowedMultipleShots, strategyAI);
        final Player playerNetwork = new NetworkPlayer(boardOwn, true);

        boardOpponent.addBoardListener(new Board.BoardListener() {
            @Override
            public void onShipHit(Ship ship) {
                boardViewOpponent.invalidate();
                boardViewOwn.invalidate();

                playSound(Sound.Hit);

                if (ship.isDestroyed()) {
                    playSound(Sound.Sink);
                    sunkShipsByUs++;

                    if (sunkShipsByUs >= boardOpponent.getTotalShips())
                        showDialogGameover(false, true);
                }
            }

            @Override
            public void onShipMiss() {
                boardViewOpponent.invalidate();
                boardViewOwn.invalidate();
            }
        });
        // Big BoardView
        // This is what we want to defeat
        boardViewOpponent.onCreate(this);
        boardViewOpponent.setBoard(boardOpponent);
        boardViewOpponent.disableBoardTouch = !weGoFirst;
        boardViewOpponent.receivesPackets = false;

        // We want to defeat the random board
        Player playerHuman = new Player(boardOpponent, true);

        // Start our main game
        gameMain = new BattleshipGame(playerHuman, playerNetwork);
        gameMain.addGameListener(this);

        // Skip turns if we don't start first
        if(!weGoFirst) {
            gameMain.nextTurn();
            textCurrentPlayer.setText("Opponent");
        }
        else{
            textCurrentPlayer.setText("Yours");
        }
        // For sound control
        switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSoundEnabled = isChecked;
            }
        });

        NetworkManager.registerNetworkInterface(this, this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_network_game);

        // Get the necessary views
        boardViewOwn = (NetworkBoardView) findViewById(R.id.board_view_network_small);
        boardViewOpponent = (NetworkBoardView) findViewById(R.id.board_view_network_big);

        textCurrentPlayer = (TextView) findViewById(R.id.text_network_current_player);

        switchSound = (Switch) findViewById(R.id.switch_network_sound);
        switchSound.setChecked(isSoundEnabled);

        if (savedInstanceState != null)
            return;

        restart();
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
        Log.d("Debug", "Turn change! " + currentPlayer);
        if (currentPlayer instanceof NetworkPlayer) {
            boardViewOpponent.disableBoardTouch = true;
            textCurrentPlayer.setText("Opponent");
        } else {
            boardViewOpponent.disableBoardTouch = false;
            textCurrentPlayer.setText(getString(R.string.game_turn_player));
        }
        boardViewOpponent.invalidate();
        boardViewOwn.invalidate();
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

    public void onClickRequestRestart(View v) {
        showDialogRequestRestart();
    }

    public void showDialogRequestRestart(){
        AlertDialog.Builder dialogExit = new AlertDialog.Builder(this);
        dialogExit.setTitle(getString(R.string.game_exit_title));
        dialogExit.setMessage("Want to request a restart?");
        dialogExit.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                PacketRestartRequest packet = new PacketRestartRequest();
                NetworkManager.sendPacket(packet);
            }
        });
        dialogExit.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialogExit.setIcon(android.R.drawable.ic_dialog_alert);
        dialogExit.show();
    }
    public void showDialogExit() {
        AlertDialog.Builder dialogExit = new AlertDialog.Builder(this);
        dialogExit.setTitle(getString(R.string.game_exit_title));
        dialogExit.setMessage(R.string.game_exit_message);
        dialogExit.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                NetworkManager.sendPacket(new PacketGameover(false));
                NetworkManager.clearNetworkInterface();
                NetworkManager.isRunning = false;
                Intent i = new Intent(NetworkGameActivity.this, MainMenuActivity.class);
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

    public void showDialogRequestResponse(){
        AlertDialog.Builder dialogExit = new AlertDialog.Builder(this);
        dialogExit.setTitle("Confirmation");
        dialogExit.setMessage("Your opponent has requested a restart. Agree?");
        dialogExit.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            PacketRequestResponse response = new PacketRequestResponse(true);
            NetworkManager.sendPacket(response);
            restart();
            }
        });
        dialogExit.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            PacketRequestResponse response = new PacketRequestResponse(false);
            NetworkManager.sendPacket(response);
            }
        });
        dialogExit.setIcon(android.R.drawable.ic_dialog_alert);
        dialogExit.show();
    }

    public void showDialogGameover(boolean weWon, boolean sendPacket) {
        String message;
        if (weWon)
            message = getString(R.string.game_won);

        else
            message = getString(R.string.game_lost);


        boolean theyWon = !weWon;
        PacketGameover packet = new PacketGameover(theyWon);

        if(sendPacket)
            NetworkManager.sendPacket(packet);

        playSound(Sound.Gameover);

        AlertDialog.Builder dialogGameover = new AlertDialog.Builder(this);
        dialogGameover.setTitle(getString(R.string.game_gameover_title));
        dialogGameover.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                NetworkManager.clearNetworkInterface();
                NetworkManager.isRunning = false;
                Intent i = new Intent(NetworkGameActivity.this, MainMenuActivity.class);
                startActivity(i);
                finish();
            }
        });
        dialogGameover.setIcon(android.R.drawable.ic_dialog_alert);
        dialogGameover.setMessage(message);
        dialogGameover.show();
    }


    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect() {

    }

    @Override
    public void onReceive(Packet p) {
        Log.d("Debug", "NetworkGameActivity received: " + p);
        if(p instanceof PacketRestartRequest){
            showDialogRequestResponse();
        }
        else if(p instanceof PacketRequestResponse){
            PacketRequestResponse response = (PacketRequestResponse)p;
            if(response.isAgreed) {
                restart();
            }
        }
        else if(p instanceof PacketGameover){
            PacketGameover packetGameover = (PacketGameover)p;
            boolean weWon = packetGameover.weWon;
            showDialogGameover(!weWon, false);
        }
    }
}
