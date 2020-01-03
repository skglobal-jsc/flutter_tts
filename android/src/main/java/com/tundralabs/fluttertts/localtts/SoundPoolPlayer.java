package com.tundralabs.fluttertts.localtts;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/* SoundPoolPlayer: 

   custom extention from SoundPool with setOnCompletionListener
   without the low-efficiency drawback of MediaPlayer

   author: kenliu
*/
public class SoundPoolPlayer extends SoundPool {
    Context context;
    int soundId;
    int streamId;
    String resPath;
    long duration;
    boolean isPlaying = false;
    boolean loaded = false;
    MediaPlayer.OnCompletionListener listener;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                isPlaying = false;
                if (listener != null) {
                    listener.onCompletion(null);
                }
            }
        }
    };

    //timing related
    Handler handler;
    long startTime;
    long endTime;
    long timeSinceStart = 0;

    public void pause() {
        if (streamId > 0) {
            endTime = System.currentTimeMillis();
            timeSinceStart += endTime - startTime;
            super.pause(streamId);
            if (handler != null) {
                handler.removeCallbacks(runnable);
            }
            isPlaying = false;
        }
    }

    public void stop() {
        if (streamId > 0) {
            timeSinceStart = 0;
            super.stop(streamId);
            if (handler != null) {
                handler.removeCallbacks(runnable);
            }
            isPlaying = false;
        }
    }

    public void resume() {
        playIt();
    }

    public void play() {
        if (!loaded) {
            loadAndPlay();
        } else {
            playIt();
        }
    }

    public static SoundPoolPlayer create(Context context, String resPath) {
        SoundPoolPlayer player = new SoundPoolPlayer(1, AudioManager.STREAM_MUSIC, 0);
        player.context = context;
        player.resPath = resPath;
        return player;
    }

    public SoundPoolPlayer(int maxStreams, int streamType, int srcQuality) {
        super(1, streamType, srcQuality);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        this.listener = listener;
    }

    private void loadAndPlay() {
        duration = getSoundDuration(resPath);
        soundId = super.load(resPath, 1);
        setOnLoadCompleteListener(new OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
                playIt();
            }
        });
    }

    private void playIt() {
        if (loaded && !isPlaying) {
            Log.d("debug", "start playing..");
            if (timeSinceStart == 0) {
                streamId = super.play(soundId, 1f, 1f, 1, 0, 1f);
            } else {
                super.resume(streamId);
            }
            startTime = System.currentTimeMillis();
            handler = new Handler();
            handler.postDelayed(runnable, duration - timeSinceStart);
            isPlaying = true;
        }
    }

    private long getSoundDuration(String resPath) {
        MediaPlayer player = MediaPlayer.create(context, Uri.parse(resPath));
        int duration = player.getDuration();
        return duration;
    }
}