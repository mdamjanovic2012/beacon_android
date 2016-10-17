package com.google.android.gms.nearby.messages.samples.nearbybackgroundbeacons.Services;

import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by milos on 16-Oct-16.
 */

public class CountdownService extends Service {

    private final static String TAG = "BroadcastService";

    public static final String COUNTDOWN_BR = "your_package_name.countdown_br";
    Intent bi = new Intent(COUNTDOWN_BR);

    CountDownTimer cdt = null;
    int secondsLeft = 0;
    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Starting timer...");

        cdt = new CountDownTimer(1200000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

                if (Math.round((float)millisUntilFinished / 1000.0f) != secondsLeft)
                {
                    secondsLeft = Math.round((float)millisUntilFinished / 1000.0f);
                }

                Log.i(TAG, "Countdown seconds remaining: " + millisUntilFinished / 1000);
                bi.putExtra("countdown", millisUntilFinished / 1000);
                int seconds = (int) (millisUntilFinished / 1000) % 60 ;
                int minutes = (int) ((millisUntilFinished / (1000*60)) % 60);
                bi.putExtra("seconds", seconds);
                bi.putExtra("minutes", minutes);
                sendBroadcast(bi);
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "Timer finished");
            }
        };

        cdt.start();
    }

    @Override
    public void onDestroy() {

        cdt.cancel();
        Log.i(TAG, "Timer cancelled");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}