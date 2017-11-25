package com.techyos.andronoid;

import android.app.Application;

import com.parrot.arsdk.ARSDK;


public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ARSDK.loadSDKLibs();
    }
}
