package com.example.btday4;

import android.app.Application;
import com.example.btday4.tracking.BehaviorTracker;

/**
 * Application entry point initializing the isolated user behavior tracker.
 */
public class BtDay4App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BehaviorTracker.init(this);
    }
}
