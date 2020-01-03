package com.tundralabs.fluttertts.localtts;

public interface IAndroidTTsCallback {
    void onStart();
    void onComplete();
    void onError(String message);
}
