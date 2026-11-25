package com.example.btday4;

import android.app.Application;
import com.example.btday4.data.repository.VoucherRepository;
import com.example.btday4.tracking.BehaviorTracker;

/**
 * Application entry point initializing the isolated user behavior tracker and default voucher catalog.
 */
public class BtDay4App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BehaviorTracker.init(this);
        VoucherRepository.seedVouchersIfEmpty();
    }
}
