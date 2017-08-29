package edu.gatech.ubicomp.deepbreath;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.writer.WaveHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private class AudioRecordingSaveTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            TarsosDSPAudioFormat audioFormat = dispatcher.getFormat();
            AudioRecording audioRecord = (AudioRecording) params[0];
            Log.v("AudioRecordingSaveTask", "audioRecord base file: " + audioRecord.getBaseFilePath());
            WaveHeader waveHeader = new WaveHeader(WaveHeader.FORMAT_PCM, (short) audioFormat.getChannels(),
                    (int) audioFormat.getSampleRate(), (short) 16, audioRecord.getRecordWavLength());
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            try {
                if (audioRecord != null) {
                    waveHeader.write(header);
                    audioRecord.getRecordWav().seek(0);
                    audioRecord.getRecordWav().write(header.toByteArray());
                    audioRecord.getRecordWav().close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private Button startButton;
    private TextView counterText;
    private AndroidSpeechToTextService androidSpeechToTextService;

    private AudioService audioService;
    private AudioDispatcher dispatcher;

    private AudioRecording currentRecord = null;

    private String sayWord = "la";

    private void initializeRecordService() {
        audioService = AudioService.getInstance();
        dispatcher = audioService.getAudioDispatcher();
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                if (currentRecord != null) {
                    try {
                        currentRecord.setRecordWavLength(currentRecord.getRecordWavLength() + audioEvent.getByteBuffer().length);
                        currentRecord.getRecordWav().write(audioEvent.getByteBuffer());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            }

            @Override
            public void processingFinished() {

            }
        });
    }

    private int countMatches(String str, String findStr) {
        int lastIndex = 0;
        int count = 0;
        while (lastIndex != -1) {
            lastIndex = str.indexOf(findStr, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += findStr.length();
            }
        }
        return count;
    }

    private void initializeSpeechService() {
        androidSpeechToTextService = AndroidSpeechToTextService.getInstance(this);
        androidSpeechToTextService.setSpeechToTextCallback(new SpeechToTextCallback() {
            @Override
            public void process(String result) {
                int sayCount = countMatches(result, sayWord);
                Log.v("SpeechToTextCallback",
                        result + " count: " + sayCount);
                setCounterCount(sayCount);
            }
        });
        androidSpeechToTextService.setPartialSpeechToTextCallback(new SpeechToTextCallback() {
            @Override
            public void process(String result) {
                int sayCount = countMatches(result, sayWord);
                if (sayCount == 0) {
                    return;
                }
                Log.v("PSpeechToTextCallback",
                        "partial: " + result + " count: " + sayCount);
                setCounterCount(sayCount);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        initializeSpeechService();
        initializeRecordService();
        counterText = (TextView) findViewById(R.id.counterText);
        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("MainActivity", "startButton clicked");
                if (currentRecord != null) {
                    saveAudioRecordingToFile(currentRecord);
                    Log.v("MainActivity", "currentRecord saved");
                }
                currentRecord = new AudioRecording("" + System.currentTimeMillis());
                setCounterCount(0);
            }
        });

    }

    private void saveAudioRecordingToFile(AudioRecording audioRecording) {
        AudioRecordingSaveTask saveTask = new AudioRecordingSaveTask();
        saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, audioRecording);
    }

    private void setCounterCount(int count) {
        counterText.setText("Counter: " + count);
    }

    private void beginRecording() {
        setCounterCount(0);
        androidSpeechToTextService.startListening();
        currentRecord = new AudioRecording("" + System.currentTimeMillis());
    }

    private void endRecording() {
        androidSpeechToTextService.stopListening();
        saveAudioRecordingToFile(currentRecord);
        currentRecord = null;
    }
}
