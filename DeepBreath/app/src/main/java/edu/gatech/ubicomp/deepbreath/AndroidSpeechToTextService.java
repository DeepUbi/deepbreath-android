package edu.gatech.ubicomp.deepbreath;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

public class AndroidSpeechToTextService implements RecognitionListener {
    private static AndroidSpeechToTextService ourInstance = null;

    private Context context = null;
    private Intent recognizerIntent = null;
    private SpeechRecognizer speech = null;
    private boolean isListening = false;

    private SpeechToTextCallback speechToTextCallback = null;
    private SpeechToTextCallback partialSpeechToTextCallback = null;

    private AndroidSpeechToTextService(Context context) {
        this.context = context;
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speech = SpeechRecognizer.createSpeechRecognizer(context);
        speech.setRecognitionListener(this);
    }

    public static AndroidSpeechToTextService getInstance() {
        return ourInstance;
    }

    public static AndroidSpeechToTextService getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new AndroidSpeechToTextService(context);
        }
        return ourInstance;
    }

    public SpeechToTextCallback getPartialSpeechToTextCallback() {
        return partialSpeechToTextCallback;
    }

    public void setPartialSpeechToTextCallback(SpeechToTextCallback partialSpeechToTextCallback) {
        this.partialSpeechToTextCallback = partialSpeechToTextCallback;
    }

    public SpeechToTextCallback getSpeechToTextCallback() {
        return speechToTextCallback;
    }

    public void setSpeechToTextCallback(SpeechToTextCallback speechToTextCallback) {
        this.speechToTextCallback = speechToTextCallback;
    }

    public void startListening() {
        if (!isListening) {
            speech.startListening(recognizerIntent);
        }
    }

    public void stopListening() {
        if (isListening) {
            speech.stopListening();
        }
    }

    public boolean getIsListening() {
        return isListening;
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {

    }

    @Override
    public void onBeginningOfSpeech() {
        isListening = true;
    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onEndOfSpeech() {
        isListening = false;
    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onResults(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = matches.get(0);
        if (speechToTextCallback != null) {
            speechToTextCallback.process(text);
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = matches.get(0);
        if (partialSpeechToTextCallback != null) {
            partialSpeechToTextCallback.process(text);
        }
    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }
}
