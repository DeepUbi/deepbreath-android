package edu.gatech.ubicomp.deepbreath;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

public class AudioPlayerService {
    private static AudioPlayerService ourInstance = null;
    public static AudioPlayerService getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new AudioPlayerService(context);
        }
        return ourInstance;
    }
    public static AudioPlayerService getInstance() {
        return ourInstance;
    }
    private Context context;
    private MediaPlayer mediaPlayer = null;
    private AudioPlayerService(Context context) {
        mediaPlayer = new MediaPlayer();
        this.context = context;
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
    }

    public void playAssetFile(String fileName, final Runnable onCompleted, Runnable onFailed) {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
            }
            AssetFileDescriptor afd = context.getAssets().openFd(fileName);
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    if (onCompleted != null) {
                        onCompleted.run();
                    }
                }
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            if (onFailed != null) {
                onFailed.run();
            }
        }
    }
    public void playURL(String url, final Runnable onCompleted, Runnable onFailed) {
         try {
             if (mediaPlayer != null) {
                 if (mediaPlayer.isPlaying()) {
                     mediaPlayer.stop();
                 }
                 mediaPlayer.reset();
             }
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.stop();
                    mediaPlayer.reset();
                    if (onCompleted != null) {
                        onCompleted.run();
                    }
                }
            });
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
            if (onFailed != null) {
                onFailed.run();
            }
        }
    }
}
