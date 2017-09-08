package edu.gatech.ubicomp.deepbreath;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class FinishActivity extends AppCompatActivity {

    private String participantPrefix;
    private String participantScore;

    private Button redoButton;
    private Button exitButton;

    private TextView scoreText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);
        participantPrefix = getIntent().getStringExtra("prefix");
        participantScore = getIntent().getStringExtra("score");
        redoButton = (Button) findViewById(R.id.retryButton);
        exitButton = (Button) findViewById(R.id.doneButton);
        scoreText = (TextView) findViewById(R.id.scoreText);
        if (participantScore != null) {
            scoreText.setText(participantScore);
        }
        redoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redoExperiment();
            }
        });
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitExperiment();
            }
        });
    }

    private void redoExperiment() {
        Intent activityIntent = new Intent(FinishActivity.this, StartActivity.class);
        Bundle extras = new Bundle();
        extras.putString("prefix", participantPrefix);
        activityIntent.putExtras(extras);
        startActivity(activityIntent);
        finish();
    }

    private void exitExperiment() {
        finish();
    }
}
