package com.tundralabs.fluttertts;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.tundralabs.fluttertts.localtts.AndroidTTS;
import com.tundralabs.fluttertts.localtts.IAndroidTTsCallback;

import java.util.Locale;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterTtsPlugin
 */
public class FlutterTtsPlugin implements MethodCallHandler {

    private static String TAG = FlutterTtsPlugin.class.getSimpleName();
    private final Handler handler;
    private final MethodChannel channel;
    Bundle bundle;

    private AndroidTTS androidTTS;

    /**
     * Plugin registration.
     */
    private FlutterTtsPlugin(Context context, MethodChannel channel) {
        this.channel = channel;
        this.channel.setMethodCallHandler(this);

        handler = new Handler(Looper.getMainLooper());
        bundle = new Bundle();
        androidTTS = new AndroidTTS(context, ttsCallback);
    }

    private IAndroidTTsCallback ttsCallback = new IAndroidTTsCallback() {
        @Override
        public void onStart() {
            invokeMethod("speak.onStart", true);
        }

        @Override
        public void onComplete() {
            invokeMethod("speak.onComplete", true);
        }

        @Override
        public void onError(String message) {
            invokeMethod("speak.onError", "Error from TextToSpeech");
        }
    };

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_tts");
        channel.setMethodCallHandler(new FlutterTtsPlugin(registrar.activeContext(), channel));
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("speak")) {
            String text = call.argument("text");
            boolean fastMode = call.argument("fastMode");
            androidTTS.speak(text, fastMode);
            result.success(1);
        } else if (call.method.equals("stop")) {
            androidTTS.stop();
            result.success(1);
        } else if (call.method.equals("pause")) {
            androidTTS.pause();
            result.success(1);
        } else if (call.method.equals("resume")) {
            androidTTS.resume();
            result.success(1);
        } else if (call.method.equals("isSpeaking")) {
            result.success(androidTTS.isSpeaking() ? 1 : 0);
        } else if (call.method.equals("setSpeechRate")) {
            String rate = call.arguments.toString();
            androidTTS.setSpeechRate(Float.parseFloat(rate));
            result.success(1);
        } else if (call.method.equals("setVolume")) {
            String volume = call.arguments.toString();
            result.success(androidTTS.setVolume(Float.parseFloat(volume)));
        } else if (call.method.equals("setPitch")) {
            String pitch = call.arguments.toString();
            result.success(androidTTS.setPitch(Float.parseFloat(pitch)));
        } else if (call.method.equals("setLanguage")) {
            String language = call.arguments.toString();
            result.success(androidTTS.setLanguage(language));
        } else if (call.method.equals("getLanguages")) {
            result.success(androidTTS.getLanguages());
        } else if (call.method.equals("getVoices")) {
            result.success(androidTTS.getVoices());
        } else if (call.method.equals("setVoice")) {
            String voice = call.arguments.toString();
            result.success(androidTTS.setVoice(voice));
        } else if (call.method.equals("isLanguageAvailable")) {
            String language = call.arguments().toString();
            Locale locale = Locale.forLanguageTag(language);
            result.success(androidTTS.isLanguageAvailable(locale));
        } else if (call.method.equals("setSilence")) {
            String silencems = call.arguments.toString();
            androidTTS.setSilencems(Integer.parseInt(silencems));
            result.success(1);
        } else {
            result.notImplemented();
        }
    }

    private void invokeMethod(final String method, final Object arguments) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                channel.invokeMethod(method, arguments);
            }
        });
    }
}
