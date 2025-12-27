/*
 * Copyright (c) 2016 - 2017 by Pacific Track, LLC
 * All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property
 * of Pacific Track Incorporated and its suppliers, if any.
 * Dissemination of this information or reproduction of this material is
 * strictly forbidden unless prior written permission is obtained from
 * Pacific Track, LLC.
 */
package com.truckspot.pt.devicemanager;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;

import com.pt.sdk.Sdk;
import com.truckspot.BuildConfig;
import com.truckspot.R;
import com.truckspot.utils.ExceptionHelper;
import com.whizpool.supportsystem.SLog;

import dagger.hilt.android.HiltAndroidApp;

//import com.squareup.leakcanary.LeakCanary;
@HiltAndroidApp

public class App extends Application {

    public static final String CONNECTED_DEVICE_CHANNEL = "connected_device_channel";
    public static final String UPDATE_CHANNEL = "update_channel";

    AppModel mModel;

    @Override
    public void onCreate() {
        super.onCreate();
        // Force light mode for the entire application
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        ExceptionHelper.init(getApplicationContext());
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            // This process is dedicated to LeakCanary for heap analysis.
//            // You should not init your app in this process.
//            return;
//        }
//        LeakCanary.install(this);
        // PT sdk init
        Sdk.getInstance().setLogger(new DefaultLogger());
        Sdk.getInstance().initialize(this);
        // Note: For PT managed service user, provide your API key here or update the app/build.gradle
        Sdk.getInstance().setApiKey(BuildConfig.API_KEY);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //DfuServiceInitiator.createDfuNotificationChannel(this);

            final NotificationChannel channel = new NotificationChannel(CONNECTED_DEVICE_CHANNEL, getString(R.string.channel_connected_devices_title), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_connected_devices_description));
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);


            final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            final NotificationChannel upd_channel = new NotificationChannel(UPDATE_CHANNEL, getString(R.string.channel_update_title), NotificationManager.IMPORTANCE_LOW);
            upd_channel.setDescription(getString(R.string.channel_update_description));
            upd_channel.setShowBadge(false);
            upd_channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(upd_channel);
        }


        Log.d(AppModel.TAG, "App created ...");

        mModel = AppModel.getInstance();


        SLog.INSTANCE.setFileProviderSuffix("fileprovider");

        SLog.INSTANCE.setDefaultTag("tag")
                .setLogFileName("log file")
                .setDaysForLog(4)// number of last working days for collecting logs
                .setPassword("1122330")        // log file password
                .hideReportDialogue(false)
//                .setEmail("shahzzz68@gmail.com")
                .build(this);



        instance = this;
    }

    public static App instance;


        @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(AppModel.TAG, "App terminated. ----------");
    }
}
