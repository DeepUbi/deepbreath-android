package edu.gatech.ubicomp.deepbreath;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

public class StartActivity extends AppCompatActivity {

    private String participantPrefix;
    private Button participantStartButton;
    private CheckBox internetCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        participantPrefix = getIntent().getStringExtra("prefix");
        participantStartButton = (Button) findViewById(R.id.participantStartButton);
        participantStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchExperiment();
            }
        });
        internetCheckbox = (CheckBox) findViewById(R.id.internetCheckbox);
    }

    private void launchExperiment() {
        if (participantPrefix == null || participantPrefix.equals("")) {
            Log.v("launchExperiment", "participantPrefix is not valid");
            Toast.makeText(this, "participantPrefix is not valid", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent activityIntent = new Intent(StartActivity.this, MainActivity.class);
        Bundle extras = new Bundle();
        extras.putString("prefix", participantPrefix);
        extras.putInt("internet", internetCheckbox.isChecked() ? 1 : 0);
        activityIntent.putExtras(extras);
        startActivity(activityIntent);
        finish();
    }
}
