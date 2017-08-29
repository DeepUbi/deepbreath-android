package edu.gatech.ubicomp.deepbreath;

import android.app.Activity;
import com.microsoft.cognitiveservices.speechrecognition.*;

import java.io.*;

public class MicrosoftSpeechToTextService implements ISpeechRecognitionServerEvents {
    public static final String MS_STT_LANGUAGE = "en-us";
    public static final SpeechRecognitionMode MS_STT_MODE = SpeechRecognitionMode.LongDictation;

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
                null, Secret.MS_STT_KEY);
    }

    @Override
    public void onPartialResponseReceived(String s) {
        if (partialSpeechToTextCallback != null) {
            partialSpeechToTextCallback.process(s);
        }
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
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
            Contract.fail();
            e.printStackTrace();
        } finally {
            dataRecognitionClient.endAudio();
        }
    }

    @Override
    public void onIntentReceived(String s) {

    }

    @Override
    public void onError(int i, String s) {

    }

    @Override
    public void onAudioEvent(boolean b) {

    }
}
