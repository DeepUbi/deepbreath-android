package edu.gatech.ubicomp.deepbreath;

import android.app.Activity;
import android.util.Log;
import com.microsoft.cognitiveservices.speechrecognition.*;

import java.io.*;

public class MicrosoftSpeechToTextService {
    public static final String MS_STT_LANGUAGE = "zh-CN";
    private static MicrosoftSpeechToTextService ourInstance = null;

    private Activity activity;

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
    }

    public void dataRecognition(File file, boolean deleteFile, boolean shortMode,
                                SpeechToTextCallback partialSpeechToTextCallback,
                                SpeechToTextCallback speechToTextCallback, final Runnable failureRunnable,
                                final Runnable errorRunnable) {

        final SpeechToTextCallback psttCallback = partialSpeechToTextCallback;
        final SpeechToTextCallback fsttCallback = speechToTextCallback;
        SpeechRecognitionMode speechRecognitionMode = shortMode ? SpeechRecognitionMode.ShortPhrase :
                SpeechRecognitionMode.LongDictation;

        DataRecognitionClient dataRecognitionClient = null;
        try {
             dataRecognitionClient = SpeechRecognitionServiceFactory.createDataClient(activity,
                    speechRecognitionMode, MS_STT_LANGUAGE, new ISpeechRecognitionServerEvents() {
                        @Override
                        public void onPartialResponseReceived(String s) {
                            Log.v("MSSTTService", "onPartialResponseReceived");
                            if (psttCallback != null) {
                                psttCallback.process(s);
                            }
                        }
                        @Override
                        public void onFinalResponseReceived(RecognitionResult recognitionResult) {
                            Log.v("MSSTTService", "onFinalResponseReceived");
                            RecognizedPhrase[] results = recognitionResult.Results;
                            String bestText = "";
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
                                bestText = bestResult.DisplayText;

                            }
                            if (fsttCallback != null) {
                                fsttCallback.process(bestText);
                            }
                        }

                        @Override
                        public void onIntentReceived(String s) {

                        }

                        @Override
                        public void onError(int i, String s) {
                            if (errorRunnable != null) {
                                errorRunnable.run();
                            }
                        }

                        @Override
                        public void onAudioEvent(boolean b) {

                        }
                    }, Secret.MS_STT_KEY);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (dataRecognitionClient == null) {
            failureRunnable.run();
            return;
        }
        try {
            InputStream fileStream = new FileInputStream(file);
            int bytesRead;
            byte[] buffer = new byte[2048];
            do {
                bytesRead = fileStream.read(buffer);
                if (bytesRead > -1) {
                    dataRecognitionClient.sendAudio(buffer, bytesRead);
                }
            } while (bytesRead > 0);
            fileStream.close();
            if (deleteFile) {
                file.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Contract.fail();
            if (failureRunnable != null) {
                failureRunnable.run();
            }
        } finally {
            dataRecognitionClient.endAudio();
        }
    }
}
