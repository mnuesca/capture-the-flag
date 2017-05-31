package csm117.ucla.capturetheflag;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.graphics.Color;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WaitingRoomActivity extends Activity {

    private String mGameName;
    private String mPlayerName;
    private boolean mTeamLeader;
    private String mTeam;
    private DatabaseReference mDatabase;

    private LinearLayout mBlueTeamView;
    private LinearLayout mRedTeamView;
    private LinearLayout mNoTeamView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_room);

        mGameName = getIntent().getStringExtra("game");
        mPlayerName = getIntent().getStringExtra("player");
        boolean creator = getIntent().getExtras().getBoolean("creator");
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mTeamLeader = false;
        mTeam = "none";

        mBlueTeamView = (LinearLayout) findViewById(R.id.blueTeam);
        mRedTeamView = (LinearLayout) findViewById(R.id.redTeam);
        mNoTeamView = (LinearLayout) findViewById(R.id.noTeam);
        if (creator) {
            Button button = new Button(this);
            button.setText("Start Game");
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    pressStart(view);
                }
            });

            LinearLayout startGameView = (LinearLayout) findViewById(R.id.startGame);
            startGameView.addView(button);
        }

        mDatabase.child("players").child(mGameName).orderByChild("time").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mBlueTeamView.removeAllViews();
                mRedTeamView.removeAllViews();
                mNoTeamView.removeAllViews();
                int bluePlayers = 0;
                int redPlayers = 0;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Player temp = child.getValue(Player.class);
                    String team = temp.team;
                    String name = child.getKey();
                    String leader = "";
                    if(bluePlayers == 0 && team.equals("blue")){
                        bluePlayers++;
                        leader = " (LEADER)";
                        mTeamLeader = name.equals(mPlayerName);
                    } else if(redPlayers == 0 && team.equals("red")){
                        redPlayers++;
                        leader = " (LEADER)";
                        mTeamLeader = name.equals(mPlayerName);
                    }
                    if(name.equals(mPlayerName)){
                        mTeam = team;
                    }
                    addPlayer(child.getKey()+leader, team);
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });

        mDatabase.child("games").child(mGameName).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if((boolean)dataSnapshot.getValue()){
                    Toast.makeText(getApplicationContext(), "Starting game", Toast.LENGTH_SHORT).show();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            startGame();
                        }
                    }, 2000);
                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });
    }

    public void addPlayer(String key, String team) {
        TextView view = new TextView(this);
        view.setText(key);
        switch (team) {
            case "none":
                mNoTeamView.addView(view);
                break;
            case "red":
                mRedTeamView.addView(view);
                break;
            case "blue":
                mBlueTeamView.addView(view);
                break;
        }
    }

    public void pressBlueTeam(View view) {
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("team").setValue("blue");
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("time").setValue(System.currentTimeMillis());

    }

    public void pressRedTeam(View view) {
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("team").setValue("red");
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("time").setValue(System.currentTimeMillis());
    }

    public void pressNoTeam(View view) {
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("team").setValue("none");
        mDatabase.child("players").child(mGameName).child(mPlayerName).child("time").setValue(System.currentTimeMillis());
    }

    public void pressStart(View view) {
        sortPlayers();
        mDatabase.child("games").child(mGameName).setValue(true);
    }

    public void startGame(){
        Intent intent = new Intent(this,PlaceFlagActivity.class);
        intent.putExtra("game",mGameName);
        intent.putExtra("player",mPlayerName);
        intent.putExtra("leader",mTeamLeader);
        intent.putExtra("team",mTeam);
        startActivity(intent);


    }

    public void sortPlayers(){
        mDatabase.child("players").child(mGameName).orderByChild("time").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mBlueTeamView.removeAllViews();
                mRedTeamView.removeAllViews();
                mNoTeamView.removeAllViews();

                int bluePlayers = 0;
                int redPlayers = 0;
                for (DataSnapshot child : dataSnapshot.getChildren()){
                    Player temp = child.getValue(Player.class);
                    if(temp.team.equals("red")){
                        redPlayers++;
                    } else if(temp.team.equals("blue")){
                        bluePlayers++;
                    }
                }

                Random rand = new Random();
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Player temp = child.getValue(Player.class);
                    String team = temp.team;
                    if(team.equals("none")) {
                        if (bluePlayers < redPlayers) {
                            team = "blue";
                            bluePlayers++;
                        } else if (redPlayers < bluePlayers) {
                            team = "red";
                            redPlayers++;
                        } else {
                            team = (rand.nextInt(2) == 0) ? "red" : "blue";
                        }
                    }
                    String name = child.getKey();
                    String leader = "";
                    if(bluePlayers == 1 && team.equals("blue")){
                        leader = " (LEADER)";
                        mTeamLeader = name.equals(mPlayerName);
                    } else if(redPlayers == 1 && team.equals("red")){
                        leader = " (LEADER)";
                        mTeamLeader = name.equals(mPlayerName);
                    }
                    if(name.equals(mPlayerName)){
                        mTeam = team;
                    }
                    addPlayer(child.getKey()+leader, team);

                }
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
            }
        });
    }
}