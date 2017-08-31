package edu.gatech.ubicomp.deepbreath;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class AudioRecording {
    private static final int HEADER_LENGTH = 44;

    private String recordName;
    private String recordPath;
    private RandomAccessFile recordWav = null;
    private int recordWavLength = 0;

    public AudioRecording(String recordName) {
        this.recordName = recordName;
    }

    public String getRecordName() {
        return recordName;
    }

    public String getBaseFilePath() {
        if (recordPath == null) {
            String basePath = Environment.getExternalStorageDirectory() + "/" + Config.RECORD_FOLDER + "/";
            File folder = new File(basePath);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            recordPath = basePath + recordName;
        }
        return recordPath;
    }

    public RandomAccessFile getRecordWav() {
        if (recordWav == null) {
            try {
                recordWav = new RandomAccessFile(new File(getBaseFilePath() + ".wav"), "rw");
                recordWav.write(new byte[HEADER_LENGTH]);
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return recordWav;
    }

    public void setRecordWavLength(int recordWavLength) {
        this.recordWavLength = recordWavLength;
    }

    public int getRecordWavLength() {
        return recordWavLength;
    }
}
