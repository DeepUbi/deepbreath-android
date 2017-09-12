package edu.gatech.ubicomp.deepbreath;

import android.Manifest;

public class Config {
    public static final int AUDIO_SAMPLE_RATE = 22050;
    public static final int AUDIO_BUFFER_SIZE = 2048;
    public static final String RECORD_FOLDER = "DeepBreath";
    public static final int AUDIO_CHUNK_LENGTH = 3000;
    public static final boolean SST_DELETE_TMP = true;
    public static final int INSTRUCTIONS_DELAY = 2000;
    public static final int SCORE_DELAY = 30000;
    public static String[] APP_PERMISSIONS = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WAKE_LOCK, Manifest.permission.INTERNET,};
}
