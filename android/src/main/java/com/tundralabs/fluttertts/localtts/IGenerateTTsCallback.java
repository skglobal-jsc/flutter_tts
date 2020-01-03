package com.tundralabs.fluttertts.localtts;

import java.io.File;

public interface IGenerateTTsCallback {
    void onSuccess(File audio);
    void onError(String message);
}
