package com.techyos.andronoid;

import android.app.Application;

import com.parrot.arsdk.ARSDK;


public class App extends Application {

    private DroneRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        ARSDK.loadSDKLibs();
        repository = new DroneRepository();
    }

    public DroneRepository getRepository() {
        return repository;
    }
}
