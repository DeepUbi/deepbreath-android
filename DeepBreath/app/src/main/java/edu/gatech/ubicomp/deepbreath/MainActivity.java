package edu.gatech.ubicomp.deepbreath;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
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
            Runnable onError = (Runnable) params[5];
            Runnable onFailure = (Runnable) params[6];
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
                                partialCallback, finalCallback, onFailure, onError);
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

    private String[] sayWords = {"妈", "娜" , "他", "8", "爸", "打", "慢"};
    private int previousAccum = 0;
    private long tempStart = 0;
    private int displayCount = 0;

    private String participantPrefix;
    private String filePrefix;

    private SpeechToTextCallback chunkPartialSTTCallback = null;
    private SpeechToTextCallback chunkFinalSTTCallback = null;
    private Runnable chunkOnError = null;
    private Runnable chunkOnFail = null;

    private SpeechToTextCallback completePartialSTTCallback = null;
    private SpeechToTextCallback completeFinalSTTCallback = null;
    private Runnable completeOnError = null;
    private Runnable completeOnFail = null;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private int field = 0x00000020;

    private boolean waitForResult = true;
    private boolean launchedResult = false;
    private boolean hasInternet;

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
                                chunkFinalSTTCallback, chunkOnError, chunkOnFail);
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
        if (!hasInternet) {
            return;
        }
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
        chunkOnError = new Runnable() {
            @Override
            public void run() {
                Log.v("chunkOnError", "A chunk error has occured");
                waitForResult = false;
            }
        };
        chunkOnFail = new Runnable() {
            @Override
            public void run() {
                Log.v("chunkOnFail", "A chunk failure has occured");
                waitForResult = false;
            }
        };
        completePartialSTTCallback = new SpeechToTextCallback() {
            @Override
            public void process(String result) {
                Log.v("completePSTT", result);
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
                // TODO: add some kind of callback and loading animation
                launchFinishScreen("" + count);
            }
        };
        chunkOnError = new Runnable() {
            @Override
            public void run() {
                Log.v("chunkOnError", "A chunk error has occured");
                waitForResult = false;
            }
        };
        chunkOnFail = new Runnable() {
            @Override
            public void run() {
                Log.v("chunkOnFail", "A chunk fail has occured");
                waitForResult = false;
            }
        };
        completeOnError = new Runnable() {
            @Override
            public void run() {
                Log.v("completeOnError", "A complete error has occured");
                waitForResult = false;
            }
        };
        completeOnFail = new Runnable() {
            @Override
            public void run() {
                Log.v("completeOnFail", "A complete fail has occured");
                waitForResult = false;
            }
        };
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    private void presentInstructions() {
        disableStartButton();
        final Runnable failedRunnable = new Runnable() {
            @Override
            public void run() {
                enableStartButton();
            }
        };
        Runnable instructionsRunnable = new Runnable() {
            @Override
            public void run() {
                final AudioPlayerService playerService = AudioPlayerService.getInstance(getApplicationContext());
                playerService.playAssetFile("instructions.mp3", new Runnable() {
                    @Override
                    public void run() {
                        enableStartButton();
                        startButton.performClick();
                        playerService.playAssetFile("beep.wav", null, failedRunnable);
                    }
                }, failedRunnable);
            }
        };
        final Handler handler = new Handler();
        handler.postDelayed(instructionsRunnable, Config.INSTRUCTIONS_DELAY);
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
        acquireWakeLock();
        participantPrefix = getIntent().getExtras().getString("prefix");
        filePrefix = participantPrefix + "_";
        boolean hasInternet = getIntent().getExtras().getInt("internet") == 1;
        this.hasInternet = hasInternet;
        waitForResult = hasInternet;
        initializeSpeechService();
        initializeRecordService();
        counterText = (TextView) findViewById(R.id.counterText);
        if (!hasInternet) {
            counterText.setVisibility(View.INVISIBLE);
        }
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
                    disableStartButton();
                    if (!waitForResult) {
                        launchFinishScreen(counterText.getText().toString());
                    } else {
                        final Handler resultHandler = new Handler();
                        resultHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                launchFinishScreen(counterText.getText().toString());
                            }
                        }, Config.SCORE_DELAY);
                    }
                }
            }
        });
        presentInstructions();
    }

    private void launchFinishScreen(String score) {
        if (launchedResult) {
            return;
        }
        Intent activityIntent = new Intent(MainActivity.this, FinishActivity.class);
        Bundle extras = new Bundle();
        extras.putString("prefix", participantPrefix);
        if (hasInternet) {
            extras.putString("score", score);
        } else {
            extras.putString("score", "NA");
        }
        activityIntent.putExtras(extras);
        startActivity(activityIntent);
        finish();
        launchedResult = true;
    }

    private void disableStartButton() {
        startButton.setVisibility(View.INVISIBLE);
        startButton.setClickable(false);
    }

    private void enableStartButton() {
        startButton.setVisibility(View.VISIBLE);
        startButton.setClickable(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        acquireWakeLock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseWakeLock();
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
                chunkFinalSTTCallback, chunkOnError, chunkOnFail);
        processAndSaveAudioFile(processRecord, false, false, completePartialSTTCallback,
                completeFinalSTTCallback, completeOnError, completeOnFail);
}

    private void processAndSaveAudioFile(AudioRecording audioRecording, boolean deleteFile, boolean shortMode,
                                         SpeechToTextCallback partialCallback, SpeechToTextCallback finalCallback,
                                         Runnable onError, Runnable onFailure) {
        AudioRecordingProcessAndSaveTask saveTask = new AudioRecordingProcessAndSaveTask();
        saveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, audioRecording, new Boolean(deleteFile),
                new Boolean(shortMode), partialCallback, finalCallback, onError, onFailure);
    }

    private void setCounterCount(int count) {
        counterText.setText("Counter: " + count);
        displayCount = count;
    }
}
