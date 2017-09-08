package edu.gatech.ubicomp.deepbreath;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ResearcherActivity extends AppCompatActivity {

    private EditText participantNumberText;
    private Spinner participantSexSpinner;
    private EditText participantAgeText;
    private Button researcherConfirmButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_researcher);
        participantNumberText = (EditText) findViewById(R.id.participantIdText);
        participantSexSpinner = (Spinner) findViewById(R.id.participantSexSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.participant_sex_array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        participantSexSpinner.setAdapter(adapter);
        participantAgeText = (EditText) findViewById(R.id.participantAgeText);
        researcherConfirmButton = (Button) findViewById(R.id.researcherConfirmButton);
        researcherConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createParticipant();
            }
        });
    }

    private void createInfoFile(String participantNumber, Map<String, String> data) {
        String filename = Environment.getExternalStorageDirectory() + "/" + Config.RECORD_FOLDER + "/" +
                participantNumber + "_info.txt";
        try {
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            for (String key : data.keySet()) {
                writer.println(key + "," + data.get(key));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createParticipant() {
        String participantNumber = participantNumberText.getText().toString();
        String participantSex = participantSexSpinner.getSelectedItem().toString();
        String participantAge = participantAgeText.getText().toString();
        if (participantNumber.equals("") || participantAge.equals("")) {
            Log.v("createParticipant", "blank fields exist");
            Toast.makeText(this, "Blank fields exist", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.v("createParticipant", "#: " + participantNumber + " s: " + participantSex + " a: " +
                participantAge);
        Map<String, String> participantData = new HashMap<>();
        participantData.put("number", participantNumber);
        participantData.put("sex", participantSex);
        participantData.put("age", participantAge);
        String participantPrefix = participantNumber + "_t_" + System.currentTimeMillis();
        createInfoFile(participantPrefix, participantData);
        Intent activityIntent = new Intent(ResearcherActivity.this, StartActivity.class);
        Bundle extras = new Bundle();
        extras.putString("prefix", participantPrefix);
        activityIntent.putExtras(extras);
        startActivity(activityIntent);
    }
}
