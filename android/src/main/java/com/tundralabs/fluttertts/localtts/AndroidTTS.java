package com.tundralabs.fluttertts.localtts;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class AndroidTTS {

    private static String TAG = AndroidTTS.class.getSimpleName();
    private TextToSpeech tts;
    private final CountDownLatch ttsInitLatch = new CountDownLatch(1);
    private final String tag = "TTS";
    private final String googleTtsEngine = "com.google.android.tts";
    private String uuid;
    private Bundle bundle;
    private boolean mFastMode = false;
    private int silencems;
    private static final String SILENCE_PREFIX = "SIL_";
    private Context context;
    private IAndroidTTsCallback ttsListener;

    private UtteranceProgressListener utteranceProgressListener =
            new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    ttsListener.onStart();
                }

                @Override
                public void onDone(String utteranceId) {
                    if (utteranceId != null && utteranceId.startsWith(SILENCE_PREFIX)) return;
                    ttsListener.onComplete();
                }

                @Override
                @Deprecated
                public void onError(String utteranceId) {
                    ttsListener.onError(utteranceId);
                }

                @Override
                public void onError(String utteranceId, int errorCode) {
                    ttsListener.onError(utteranceId);
                }
            };

    private TextToSpeech.OnInitListener onInitListener =
            new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        tts.setOnUtteranceProgressListener(utteranceProgressListener);
                        ttsInitLatch.countDown();

                        try {
                            Locale locale = tts.getDefaultVoice().getLocale();
                            if (isLanguageAvailable(locale)) {
                                tts.setLanguage(locale);
                            }
                        } catch (NullPointerException | IllegalArgumentException e) {
                            Log.e(tag, "getDefaultLocale: " + e.getMessage());
                        }
                    } else {
                        Log.e(tag, "Failed to initialize TextToSpeech");
                    }
                }
            };

    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
//            Log.d(TAG, "Audio completed");
            ttsListener.onComplete();
        }
    };


    public AndroidTTS(Context context, IAndroidTTsCallback listener) {
        this.context = context;
        this.ttsListener = listener;
        bundle = new Bundle();
        tts = new TextToSpeech(context.getApplicationContext(), onInitListener, googleTtsEngine);
    }


    public void setSpeechRate(float rate) {
        tts.setSpeechRate(rate * 2.0f);
    }

    public Boolean isLanguageAvailable(Locale locale) {
        return tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int setLanguage(String language) {
        Locale locale = Locale.forLanguageTag(language);
        if (isLanguageAvailable(locale)) {
            tts.setLanguage(locale);
            return 1;
        }
        return 0;
    }

    public void setSilencems(int s) {
        this.silencems = s;
    }

    public int setVoice(String voice) {
        for (Voice ttsVoice : tts.getVoices()) {
            if (ttsVoice.getName().equals(voice)) {
                tts.setVoice(ttsVoice);
                return 1;
            }
        }
        Log.d(tag, "Voice name not found: " + voice);
        return 0;
    }

    public int setVolume(float volume) {
        if (volume >= 0.0F && volume <= 1.0F) {
            bundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume);
            return 1;
        } else {
            Log.d(tag, "Invalid volume " + volume + " value - Range is from 0.0 to 1.0");
            return 0;
        }
    }

    public int setPitch(float pitch) {
        if (pitch >= 0.5F && pitch <= 2.0F) {
            tts.setPitch(pitch);
            return 1;
        } else {
            Log.d(tag, "Invalid pitch " + pitch + " value - Range is from 0.5 to 2.0");
            return 0;
        }
    }

    public List<String> getVoices() {
        ArrayList<String> voices = new ArrayList<>();
        try {
            for (Voice voice : tts.getVoices()) {
                voices.add(voice.getName());
            }
            return voices;
        } catch (NullPointerException e) {
            Log.d(tag, "getVoices: " + e.getMessage());
            return null;
        }
    }

    public List<String> getLanguages() {
        ArrayList<String> locales = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // While this method was introduced in API level 21, it seems that it
            // has not been implemented in the speech service side until API Level 23.
            for (Locale locale : tts.getAvailableLanguages()) {
                locales.add(locale.toLanguageTag());
            }
        } else {
            for (Locale locale : Locale.getAvailableLocales()) {
                if (locale.getVariant().isEmpty() && isLanguageAvailable(locale)) {
                    locales.add(locale.toLanguageTag());
                }
            }
        }
        return locales;
    }

    private SoundPoolPlayer player;

    public void speak(String text, boolean fastMode) {
        this.mFastMode = fastMode;

        // we should stop all audio in here

        if (fastMode) {
            uuid = UUID.randomUUID().toString();
            if (silencems > 0) {
                tts.playSilentUtterance(silencems, TextToSpeech.QUEUE_FLUSH, SILENCE_PREFIX + uuid);
                tts.speak(text, TextToSpeech.QUEUE_ADD, bundle, uuid);
            } else {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, uuid);
            }
        } else {
            generateAudioFile(text, new IGenerateTTsCallback() {
                @Override
                public void onSuccess(File audio) {
                    if (mFastMode) return; // for fast mode only
                    if (player != null) {
                        player.stop();
                        player.release();
                    }
                    player = SoundPoolPlayer.create(context, audio.getAbsolutePath());
                    player.setOnCompletionListener(onCompletionListener);
                    player.play();
                    ttsListener.onStart();
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, message);
                    ttsListener.onError(message);
                }
            });
        }
    }

    public void stop() {
        tts.stop();
        if (player != null) {
            player.stop();
        }
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    public void resume() {
        if (player != null) {
            player.resume();
        }
    }

    public boolean isSpeaking() {
        if (player != null) {
            return player.isPlaying();
        }
        return false;
    }

    private void generateAudioFile(String textToConvert, final IGenerateTTsCallback cb) {
        final String utteranceId = "tts-flutter-cached";
        final File destinationFile = new File(context.getCacheDir(), utteranceId + ".wav");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.synthesizeToFile(textToConvert, null, destinationFile, utteranceId);
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
            tts.synthesizeToFile(textToConvert, params, destinationFile.getPath());
        }

        if (Build.VERSION.SDK_INT >= 15) {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    cb.onSuccess(destinationFile);
                }

                @Override
                public void onError(String utteranceId) {
                    cb.onError(utteranceId);
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });
        } else {
            tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    cb.onSuccess(destinationFile);
                }
            });
        }
    }
}
