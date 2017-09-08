package edu.gatech.ubicomp.deepbreath;

import android.os.AsyncTask;
import android.os.PowerManager;
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
            boolean deleteFile = ((Boolean) params[1]).booleanValue();
            boolean shortMode = ((Boolean) params[2]).booleanValue();
            SpeechToTextCallback partialCallback = (SpeechToTextCallback) params[3];
            SpeechToTextCallback finalCallback = (SpeechToTextCallback) params[4];
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
                        microsoftSpeechToTextService.dataRecognition(
                                new File(audioRecord.getBaseFilePath() + ".wav"), deleteFile, shortMode,
                                partialCallback, finalCallback);
                    }
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

    private String[] sayWords = {"妈", "娜" , "他", "8", "爸", "打"};
    private int previousAccum = 0;
    private long tempStart = 0;
    private int displayCount = 0;

    private String participantPrefix;
    private String filePrefix;

    private SpeechToTextCallback chunkPartialSTTCallback = null;
    private SpeechToTextCallback chunkFinalSTTCallback = null;

    private SpeechToTextCallback completePartialSTTCallback = null;
    private SpeechToTextCallback completeFinalSTTCallback = null;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private int field = 0x00000020;

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
                        processAndSaveAudioFile(tempRecord, Config.SST_DELETE_TMP, true, chunkPartialSTTCallback,
                                chunkFinalSTTCallback);
                        tempRecord = new AudioRecording(filePrefix + "tmp_" + currentTime);
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
        chunkFinalSTTCallback = new SpeechToTextCallback() {
            @Override
            public void process(String result) {
                Log.v("STTCallback", result);
                int count = countMatchesAll(result, sayWords);
                if (count > 0) {
                    Log.v("final previousAccum", "" + previousAccum + " count: " + count);
                    previousAccum += count;
                    setCounterCount(previousAccum);
                }
            }
        };
        chunkPartialSTTCallback = new SpeechToTextCallback() {
            @Override
            public void process(String result) {
                Log.v("Partial STTCallback", result);
                int count = countMatchesAll(result, sayWords);
                if (count > 0) {
                    Log.v("previousAccum", "" + previousAccum + " count: " + count);
                    int newCount = previousAccum + count;
                    if (newCount > displayCount) {
                        setCounterCount(previousAccum + count);
                    }
                }
            }
        };
        completeFinalSTTCallback = new SpeechToTextCallback() {
            @Override
            public void process(String result) {
                Log.v("Complete STTCallback", result);
                int count = countMatchesAll(result, sayWords);
                if (count > 0) {
                    Log.v("complete final", "count: " + count);
                    setCounterCount(count);
                }
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (Throwable ignored) {

        }
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());
        setContentView(R.layout.activity_main);
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        participantPrefix = getIntent().getExtras().getString("prefix");
        filePrefix = participantPrefix + "_";
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void beginRecord() {
        long currentTimeVal = System.currentTimeMillis();
        String currentTime = "" + currentTimeVal;
        tempStart = currentTimeVal;
        currentRecord = new AudioRecording(filePrefix + currentTime);
        tempRecord = new AudioRecording(filePrefix + "tmp_" + currentTime);
        setCounterCount(0);
        previousAccum = 0;
    }

    private void endRecord() {
        AudioRecording processRecord = currentRecord;
        AudioRecording countRecord = tempRecord;
        currentRecord = null;
        tempRecord = null;
        processAndSaveAudioFile(countRecord, Config.SST_DELETE_TMP, true, chunkPartialSTTCallback,
                chunkFinalSTTCallback);
        processAndSaveAudioFile(processRecord, false, false, completePartialSTTCallback,
                completeFinalSTTCallback);
}

    private void processAndSaveAudioFile(AudioRecording audioRecording, boolean deleteFile, boolean shortMode,
                                         SpeechToTextCallback partialCallback, SpeechToTextCallback finalCallback) {
        AudioRecordingProcessAndSaveTask saveTask = new AudioRecordingProcessAndSaveTask();
        saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, audioRecording, new Boolean(deleteFile),
                new Boolean(shortMode), partialCallback, finalCallback);
    }

    private void setCounterCount(int count) {
        counterText.setText("Counter: " + count);
        displayCount = count;
    }
}
