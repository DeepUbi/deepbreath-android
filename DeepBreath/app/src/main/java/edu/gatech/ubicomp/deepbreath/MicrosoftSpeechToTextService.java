package edu.gatech.ubicomp.deepbreath;

import android.app.Activity;
import android.util.Log;
import com.microsoft.cognitiveservices.speechrecognition.*;

import java.io.*;

public class MicrosoftSpeechToTextService implements ISpeechRecognitionServerEvents {
    public static final String MS_STT_LANGUAGE = "en-us";
    public static final SpeechRecognitionMode MS_STT_MODE = SpeechRecognitionMode.ShortPhrase;

    private static MicrosoftSpeechToTextService ourInstance = null;

    private Activity activity;
    private DataRecognitionClient dataRecognitionClient;

    private SpeechToTextCallback speechToTextCallback = null;
    private SpeechToTextCallback partialSpeechToTextCallback = null;

    public static MicrosoftSpeechToTextService getInstance(Activity activity) {
        if (ourInstance == null) {
            ourInstance = new MicrosoftSpeechToTextService(activity);
        }
        return ourInstance;
    }

    public static MicrosoftSpeechToTextService getInstance() {
        return ourInstance;
    }

    private MicrosoftSpeechToTextService(Activity activity) {
        this.activity = activity;
        dataRecognitionClient = SpeechRecognitionServiceFactory.createDataClient(activity, MS_STT_MODE, MS_STT_LANGUAGE,
                this, Secret.MS_STT_KEY);
    }

    @Override
    public void onPartialResponseReceived(String s) {
        Log.v("MSSTTService", "onPartialResponseReceived");
        if (partialSpeechToTextCallback != null) {
            partialSpeechToTextCallback.process(s);
        }
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        Log.v("MSSTTService", "onFinalResponseReceived");
        RecognizedPhrase[] results = recognitionResult.Results;
        if (results.length > 0) {
            RecognizedPhrase bestResult = results[0];
            if (results.length > 1) {
                for (int i = 1; i < results.length; i++) {
                    RecognizedPhrase candidateResult = results[i];
                    if (candidateResult.Confidence.compareTo(bestResult.Confidence) > 0) {
                        bestResult = candidateResult;
                    }
                }
            }
            String bestText = bestResult.DisplayText;
            if (speechToTextCallback != null) {
                speechToTextCallback.process(bestText);
            }
        }
    }

    public void dataRecognition(File file) {
        try {
            InputStream fileStream = new FileInputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[1024];

            do {
                bytesRead = fileStream.read(buffer);
                if (bytesRead > -1) {
                    dataRecognitionClient.sendAudio(buffer, bytesRead);
                }
            } while (bytesRead > 0);
        } catch (IOException e) {
            e.printStackTrace();
            Contract.fail();
        } finally {
            dataRecognitionClient.endAudio();
        }
    }

    public void setSpeechToTextCallback(SpeechToTextCallback speechToTextCallback) {
        this.speechToTextCallback = speechToTextCallback;
    }

    public void setPartialSpeechToTextCallback(SpeechToTextCallback partialSpeechToTextCallback) {
        this.partialSpeechToTextCallback = partialSpeechToTextCallback;
    }

    @Override
    public void onIntentReceived(String s) {
        Log.v("MSSTTService", "onIntentReceived");
    }

    @Override
    public void onError(int i, String s) {
        Log.v("MSSTTService", "onError");
    }

    @Override
    public void onAudioEvent(boolean b) {
        Log.v("MSSTTService", "onAudioEvent");
    }
}
