package edu.gatech.ubicomp.deepbreath;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.writer.WaveHeader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private class AudioRecordingProcessAndSaveTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            TarsosDSPAudioFormat audioFormat = dispatcher.getFormat();
            AudioRecording audioRecord = (AudioRecording) params[0];
            Log.v("AudioRecordingPSTask", "audioRecord base file: " + audioRecord.getBaseFilePath());
            WaveHeader waveHeader = new WaveHeader(WaveHeader.FORMAT_PCM, (short) audioFormat.getChannels(),
                    (int) audioFormat.getSampleRate(), (short) 16, audioRecord.getRecordWavLength());
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            try {
                if (audioRecord != null) {
                    waveHeader.write(header);
                    audioRecord.getRecordWav().seek(0);
                    audioRecord.getRecordWav().write(header.toByteArray());
                    audioRecord.getRecordWav().close();
                    if (microsoftSpeechToTextService != null) {
                        microsoftSpeechToTextService.dataRecognition(new File(audioRecord.getBaseFilePath() + ".wav"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

     private class AudioRecordingSaveTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... params) {
            TarsosDSPAudioFormat audioFormat = dispatcher.getFormat();
            AudioRecording audioRecord = (AudioRecording) params[0];
            Log.v("AudioRecordingPSTask", "audioRecord base file: " + audioRecord.getBaseFilePath());
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
    private MicrosoftSpeechToTextService microsoftSpeechToTextService;

    private AudioService audioService;
    private AudioDispatcher dispatcher;

    private AudioRecording currentRecord = null;
    private AudioRecording tempRecord = null;

    private String[] sayWords = {"妈", "娜" , "他"};
    private int previousAccum = 0;
    private long tempStart = 0;
    private int displayCount = 0;

    private void appendAudioEvent(AudioRecording recording, AudioEvent audioEvent) {
        if (recording != null) {
            try {
                recording.setRecordWavLength(recording.getRecordWavLength() + audioEvent.getByteBuffer().length);
                recording.getRecordWav().write(audioEvent.getByteBuffer());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeRecordService() {
        audioService = AudioService.getInstance();
        dispatcher = audioService.getAudioDispatcher();
        dispatcher.addAudioProcessor(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                appendAudioEvent(currentRecord, audioEvent);
                appendAudioEvent(tempRecord, audioEvent);
                if (currentRecord != null) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - tempStart;
                    if (elapsedTime > Config.AUDIO_CHUNK_LENGTH) {
                        processAndSaveAudioFile(tempRecord);
                        tempRecord = new AudioRecording("tmp_" + currentTime);
                        tempStart = currentTime;
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

    private int countMatchesAll(String str, String[] findStrs) {
        int count = 0;
        for (String s : findStrs) {
            count += countMatches(str, s);
        }
        return count;
    }

    private void initializeSpeechService() {
        microsoftSpeechToTextService = MicrosoftSpeechToTextService.getInstance(this);
        microsoftSpeechToTextService.setSpeechToTextCallback(new SpeechToTextCallback() {
            @Override
            public void process(String result) {
                Log.v("SSTCallback", result);
                int count = countMatchesAll(result, sayWords);
                if (count > 0) {
                    Log.v("final previousAccum", "" + previousAccum + " count: " + count);
                    previousAccum += count;
                    setCounterCount(previousAccum);
                }
            }
        });
        microsoftSpeechToTextService.setPartialSpeechToTextCallback(new SpeechToTextCallback() {
            @Override
            public void process(String result) {
                Log.v("Partial SSTCallback", result);
                int count = countMatchesAll(result, sayWords);
                if (count > 0) {
                    Log.v("previousAccum", "" + previousAccum + " count: " + count);
                    int newCount = previousAccum + count;
                    if (newCount > displayCount) {
                        setCounterCount(previousAccum + count);
                    }
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeSpeechService();
        initializeRecordService();
        counterText = (TextView) findViewById(R.id.counterText);
        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v("MainActivity", "startButton clicked");
                if (currentRecord == null) {
                    startButton.setText("Stop");
                    beginRecord();
                } else {
                    startButton.setText("Start");
                    endRecord();
                }
            }
        });
    }

    private void beginRecord() {
        long currentTimeVal = System.currentTimeMillis();
        String currentTime = "" + currentTimeVal;
        tempStart = currentTimeVal;
        currentRecord = new AudioRecording(currentTime);
        tempRecord = new AudioRecording("tmp_" + currentTime);
        setCounterCount(0);
        previousAccum = 0;
    }

    private void endRecord() {
        AudioRecording processRecord = currentRecord;
        AudioRecording countRecord = tempRecord;
        currentRecord = null;
        tempRecord = null;
        saveAudioFile(processRecord);
        processAndSaveAudioFile(countRecord);
}

    private void processAndSaveAudioFile(AudioRecording audioRecording) {
        AudioRecordingProcessAndSaveTask saveTask = new AudioRecordingProcessAndSaveTask();
        saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, audioRecording);
    }

    private void saveAudioFile(AudioRecording audioRecording) {
        AudioRecordingSaveTask saveTask = new AudioRecordingSaveTask();
        saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, audioRecording);
    }

    private void setCounterCount(int count) {
        counterText.setText("Counter: " + count);
        displayCount = count;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (microsoftSpeechToTextService != null) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (microsoftSpeechToTextService != null) {
        }
    }
}
