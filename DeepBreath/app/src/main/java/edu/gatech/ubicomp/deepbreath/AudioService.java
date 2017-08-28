package edu.gatech.ubicomp.deepbreath;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;

public class AudioService {

    private static AudioService ourInstance = null;

    private AudioDispatcher dispatcher;
    private Thread dispatcherThread = null;

    private int sampleRate = Config.AUDIO_SAMPLE_RATE;
    private int bufferSize = Config.AUDIO_BUFFER_SIZE;

    private AudioService() {
        stopThread();
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, 0);
        dispatcherThread = new Thread(dispatcher);
        startThread();
    }

    public static AudioService getInstance() {
        if (ourInstance == null) {
            ourInstance = new AudioService();
        }
        return ourInstance;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    private void startThread() {
        if (dispatcherThread != null && !dispatcherThread.isAlive()) {
            dispatcherThread.start();
        }
    }

    private void stopThread() {
        if (dispatcherThread != null && dispatcherThread.isAlive()) {
            dispatcherThread.stop();
        }
    }

    public AudioDispatcher getAudioDispatcher() {
        return dispatcher;
    }

}
