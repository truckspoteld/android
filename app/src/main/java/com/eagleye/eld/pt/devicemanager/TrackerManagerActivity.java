//package com.eagleye.eld.pt.devicemanager;
//
//import android.app.Activity;
//import android.app.Dialog;
//import android.bluetooth.BluetoothDevice;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.hardware.usb.UsbDevice;
//import android.os.Bundle;
//import android.text.method.LinkMovementMethod;
//import android.util.Log;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.Window;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.widget.SwitchCompat;
//import androidx.appcompat.widget.Toolbar;
//import androidx.drawerlayout.widget.DrawerLayout;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentTransaction;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//import androidx.preference.PreferenceManager;
//
//import com.pt.sdk.Uart;
//import com.eagleye.eld.R;
//import com.eagleye.eld.pt.devicemanager.usb.DevicesFragment;
//
//import java.util.UUID;
//
//
//public class TrackerManagerActivity extends BleProfileServiceReadyActivity<TrackerService.TrackerBinder>
//        implements TrackerUpdateProgressFragment.OnTrackerUpdateClosedListener,
//        FragmentManager.OnBackStackChangedListener
//{
//    public static final String TAG = AppModel.TAG;
//
//    Dialog mPrivacyDlg = null;
//
//    // May be null, depends when the service gets bounded
//    protected TrackerService.TrackerBinder mTrackerBinder;
//
//    @Override
//    protected void onServiceBound(final TrackerService.TrackerBinder binder) {
//        mTrackerBinder = binder;
//        Intent broadcast = new Intent("SVC-BOUND-REFRESH");
//        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
//
//        //Log.v(TAG, "A:>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>Tracker service bounded.");
//        Log.i(TAG, "A:Tracker service bounded.");
//    }
//
//    @Override
//    protected void onServiceUnbound() {
//        mTrackerBinder = null;
//        Log.i(TAG, "A:Tracker service unbounded.");
//        //Log.v(TAG, "A:<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<Tracker service unbounded.");
//    }
//
//    @Override
//    protected Class<? extends BleProfileService> getServiceClass() {
//        return  TrackerService.class;
//    }
//
//    @Override
//    protected void onCreateView(Bundle savedInstanceState) {
//
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        AppModel.getInstance().privacyAccepted = sharedPreferences.getBoolean("privacy_accepted", false);
//
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean("privacy_accepted", true);
//        editor.putBoolean("usb_mode", false);
//        AppModel.MODE_USB = false;
//        editor.apply();
//        AppModel.getInstance().privacyAccepted = true;
//
//        if (! AppModel.getInstance().privacyAccepted) {
//            getPrivacyConsent(this, sharedPreferences);
//        }
//
//        // During the initial run, this would be treated as false
//        AppModel.MODE_USB = sharedPreferences.getBoolean("usb_mode", false);
//
//        setContentView(R.layout.activity_tracker_manager);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportFragmentManager().addOnBackStackChangedListener(this);
//        //Handle when activity is recreated like on orientation Change
//        shouldDisplayHomeUp();
////        getSupportActionBar().setHomeButtonEnabled(true);
////        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
////        getSupportActionBar().setDisplayUseLogoEnabled(true);
//
//        // Dont need drawer
//        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
//        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
//
////        NavigationView navigationView = findViewById(R.id.nav_view);
////        mAppBarConfiguration = new AppBarConfiguration.Builder(
////                R.id.nav_manage, R.id.content_privacy)
////                .setDrawerLayout(drawer)
////                .build();
////        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
////        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
////        NavigationUI.setupWithNavController(navigationView, navController);
//
//        if (savedInstanceState == null) {
//            FragmentManager fm = getSupportFragmentManager();
//            FragmentTransaction ft = fm.beginTransaction();
//
//            if (AppModel.MODE_USB) {
//                ft.replace(R.id.fragment_container, DevicesFragment.newInstance(), DevicesFragment.FRAG_TAG);
//            } else {
//                ft.replace(R.id.fragment_container, DefaultFragment.newInstance());
//            }
//            ft.commit();
//        }
//    }
//
//    @Override
//    public void onBackStackChanged() {
//        shouldDisplayHomeUp();
//    }
//
//    public void shouldDisplayHomeUp(){
//        //Enable Up button only  if there are entries in the back stack
//        boolean canGoBack =true;
//                //getSupportFragmentManager().getBackStackEntryCount()>0;
//        getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);
//        getSupportActionBar().setDisplayShowHomeEnabled(canGoBack);
//    }
//
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//
//        int id = item.getItemId();
//        if (id == android.R.id.home) {
//            // Handle up button navigation for fragments
//            finish();
//            //getSupportFragmentManager().popBackStack();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//
//
//
//
//    void getPrivacyConsent(final Activity activity, final SharedPreferences sharedPreferences)
//    {
//        mPrivacyDlg = new Dialog(this);
//        mPrivacyDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        mPrivacyDlg.setContentView(R.layout.privacy_notice);
//        mPrivacyDlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        mPrivacyDlg.setCanceledOnTouchOutside(false);
//        mPrivacyDlg.setOwnerActivity(this);
//
//        TextView link = (TextView) mPrivacyDlg.findViewById(R.id.privacy_link);
//        link.setMovementMethod(LinkMovementMethod.getInstance());
//
//        SwitchCompat modeUsb = mPrivacyDlg.findViewById(R.id.switchUsb);
//        Button accept = mPrivacyDlg.findViewById(R.id.privacy_accept);
//        accept.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putBoolean("privacy_accepted", true);
//
//                if (modeUsb.isChecked()) {
//                    editor.putBoolean("usb_mode", true);
//
//                    // Set the USB mode
//                    AppModel.MODE_USB = true;
//
//                    // Replace default fragment with the USB
//                    FragmentManager fm = getSupportFragmentManager();
//                    Fragment fragment = fm.findFragmentById(R.id.fragment_container);
//                    if (fragment != null) {
//                        FragmentTransaction ft = fm.beginTransaction();
//                        ft.replace(R.id.fragment_container, DevicesFragment.newInstance(), DevicesFragment.FRAG_TAG);
//                        ft.commit();
//                    }
//                    TrackerManagerActivity.this.mConnectButton.setVisibility(View.INVISIBLE);
//                } else {
//                    editor.putBoolean("usb_mode", false);
//                    AppModel.MODE_USB = false;
//                }
//
//                editor.apply();
//                AppModel.getInstance().privacyAccepted = true;
//                mPrivacyDlg.dismiss();
//
//                // Show Disclosure
//                ProminentDisclosureFragment.newInstance().show(getSupportFragmentManager(), ProminentDisclosureFragment.FRAG_TAG);
//            }
//        });
//
//        mPrivacyDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            @Override
//            public void onCancel(DialogInterface dialogInterface) {
//                finish();
//            }
//        });
//
//        mPrivacyDlg.show();
//    }
//
//    @Override
//    protected int getConnectionToggleResourceId() {
//        return R.id.fab;
//    }
//
//    /**
//     * View to be shown before 'Select a device'
//     */
//    @Override
//    protected void setDefaultUI() {
//
//    }
//
//
//    @Override
//    protected UUID getFilterUUID() {
//        return UUID.fromString(Uart.RX_SERVICE_UUID.toString());
//    }
//
//
//    @Override
//    synchronized public void onDeviceConnecting(BluetoothDevice device) {
//        super.onDeviceConnecting(device);
//        Log.i(TAG, "A: Tracker connecting  ....");
//        FragmentManager fm = getSupportFragmentManager();
//
//        if (fm.findFragmentByTag(TrackerConnectingFragment.FRAG_TAG) == null) {
//
//            FragmentTransaction ft = fm.beginTransaction();
//
//            // Show Tracker waiting view
//            TrackerConnectingFragment tcf = TrackerConnectingFragment.newInstance();
//            ft.replace(R.id.fragment_container, tcf, TrackerConnectingFragment.FRAG_TAG);
//            ft.commit();
//        } else {
//            TrackerConnectingFragment tcf = (TrackerConnectingFragment)fm.findFragmentByTag(TrackerConnectingFragment.FRAG_TAG);
//        }
//    }
//
//    @Override
//    public void onDeviceConnected(final BluetoothDevice device) {
//        super.onDeviceConnected(device);
//        _onConnected();
//    }
//
//    @Override
//    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
//        showToast("BA: Failed to Connect :"+device.getAddress() + " rc = "+reason);
//    }
//
//    @Override
//    public void onDeviceConnected(final UsbDevice device) {
//        super.onDeviceConnected(device);
//        _onConnected();
//    }
//
//
//    void _onConnected() {
//        Log.i(TAG, "A: Tracker connected.");
//
//        FragmentManager fm = getSupportFragmentManager();
//
//        // Create only, if it does not exist, else reuse
//        if (fm.findFragmentByTag(TrackerViewFragment.FRAG_TAG) == null) {
//
//            FragmentTransaction ft = fm.beginTransaction();
//
////        // Is it reconnection
////        if (AppModel.getInstance().mTrackerLostLink) {
////            // replace wait with view
////            AppModel.getInstance().mTrackerLostLink = false;
////        }
//            // Show Tracker View
//            TrackerViewFragment tvf = TrackerViewFragment.newInstance().init();
//            //tvf.onServiceBound(mTrackerBinder);
//
//            ft.add(R.id.fragment_container, tvf, TrackerViewFragment.FRAG_TAG);
//            ft.commit();
//        } else {
//            TrackerViewFragment tvf = (TrackerViewFragment)fm.findFragmentByTag(TrackerViewFragment.FRAG_TAG);
//            tvf.init();
//        }
//
//        // Show fab
//        if (AppModel.MODE_USB) {
//            mConnectButton.setVisibility(View.VISIBLE);
//        }
//
//    }
//
//
//    @Override
//    public void onDeviceDisconnecting(final BluetoothDevice device) {
//
//        super.onDeviceDisconnecting(device);
//
//        Log.i(TAG, "A: Tracker disconnecting...");
//
//        // Blank
//        FragmentManager fm = getSupportFragmentManager();
//        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
//        if (fragment != null) {
//            FragmentTransaction ft = fm.beginTransaction();
//            DefaultFragment df = DefaultFragment.newInstance().init();
//            ft.replace(R.id.fragment_container,df,DefaultFragment.FRAG_TAG );
//            ft.commit();
//        }
//    }
//
//    @Override
//    public void onDeviceDisconnected(final BluetoothDevice device, int reason) {
//        super.onDeviceDisconnected(device, reason);
//        _onDisconnected();
//    }
//
//    @Override
//    public void onDeviceDisconnected(final UsbDevice device) {
//        super.onDeviceDisconnected(device);
//       _onDisconnected();
//    }
//
//    void _onDisconnected()
//    {
//        Log.i(TAG, "A: Tracker disconnected.");
//        //AppModel.getInstance().mTrackerLostLink = false;
//        // Blank
//        FragmentManager fm = getSupportFragmentManager();
//        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
//        if (fragment != null) {
//            FragmentTransaction ft = fm.beginTransaction();
//            if (AppModel.MODE_USB) {
//                ft.replace(R.id.fragment_container, DevicesFragment.newInstance(), DevicesFragment.FRAG_TAG);
//            } else {
//                DefaultFragment df = DefaultFragment.newInstance().init();
//                ft.replace(R.id.fragment_container, df, DefaultFragment.FRAG_TAG);
//            }
//            ft.commit();
//        }
//
//        // hide fab
//        if (AppModel.MODE_USB) {
//            mConnectButton.setVisibility(View.GONE);
//        }
//    }
//
//    @Override
//    public void onLinkLossOccurred(final BluetoothDevice device) {
//        super.onLinkLossOccurred(device);
//
//        Log.i(TAG, "A: Tracker lost link ...");
//
//        // Wait for device
//        FragmentManager fm = getSupportFragmentManager();
//        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
//        if (fragment != null) {
//            FragmentTransaction ft = fm.beginTransaction();
//            TrackerConnectingFragment tcf = TrackerConnectingFragment.newInstance();
//            ft.replace(R.id.fragment_container, tcf, TrackerConnectingFragment.FRAG_TAG);
//            ft.commit();
//        }
//    }
//
//
//    @Override
//    public void onTrackerUpdateClosed() {
//        // If update is in progress, cancel it
//        if (mTrackerBinder != null) {
//            //if (mTrackerBinder.getTracker().isUpdating()) {
//                Log.i(TAG, "A: Tracker update canceled");
//                mTrackerBinder.getTracker().cancelUpdate();
//                mTrackerBinder.cancelUpdateNotifications();
//            //}
//        }
//    }
//
//}
package com.eagleye.eld.pt.devicemanager;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.pt.sdk.Uart;
import com.eagleye.eld.R;
import com.eagleye.eld.fragment.Dashboard;
import com.eagleye.eld.pt.devicemanager.usb.DevicesFragment;

import java.util.UUID;

public class TrackerManagerActivity extends BleProfileServiceReadyActivity<TrackerService.TrackerBinder>
        implements TrackerUpdateProgressFragment.OnTrackerUpdateClosedListener,
        FragmentManager.OnBackStackChangedListener {
    public static final String TAG = AppModel.TAG;

    Dialog mPrivacyDlg = null;

    // May be null, depends when the service gets bounded
    protected TrackerService.TrackerBinder mTrackerBinder;

    @Override
    protected void onServiceBound(final TrackerService.TrackerBinder binder) {
        mTrackerBinder = binder;
        Intent broadcast = new Intent("SVC-BOUND-REFRESH");
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

        Log.i(TAG, "A:Tracker service bounded.");
    }

    @Override
    protected void onServiceUnbound() {
        mTrackerBinder = null;
        Log.i(TAG, "A:Tracker service unbounded.");
    }

    @Override
    protected Class<? extends BleProfileService> getServiceClass() {
        return TrackerService.class;
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        AppModel.getInstance().privacyAccepted = sharedPreferences.getBoolean("privacy_accepted", false);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("privacy_accepted", true);
        editor.putBoolean("usb_mode", false);
        AppModel.MODE_USB = false;
        editor.apply();
        AppModel.getInstance().privacyAccepted = true;

        if (!AppModel.getInstance().privacyAccepted) {
            getPrivacyConsent(this, sharedPreferences);
        }

        // During the initial run, this would be treated as false
        AppModel.MODE_USB = sharedPreferences.getBoolean("usb_mode", false);

        setContentView(R.layout.activity_tracker_manager);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        //Handle when activity is recreated like on orientation Change
        shouldDisplayHomeUp();

        // Dont need drawer
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        if (savedInstanceState == null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();

            if (AppModel.MODE_USB) {
                ft.replace(R.id.fragment_container, DevicesFragment.newInstance(), DevicesFragment.FRAG_TAG);
            } else {
                ft.replace(R.id.fragment_container, DefaultFragment.newInstance());
            }
            ft.commit();
        }
    }

    @Override
    protected void onViewCreated(Bundle savedInstanceState) {
        super.onViewCreated(savedInstanceState);
        if (savedInstanceState != null || AppModel.MODE_USB || !isBLEEnabled()) {
            return;
        }

        String autoAddress = getIntent().getStringExtra("auto_connect_address");
        if (autoAddress == null || autoAddress.trim().isEmpty()) {
            return;
        }

        String autoName = getIntent().getStringExtra("auto_connect_name");
        if (autoName == null || autoName.trim().isEmpty()) {
            autoName = "ELD Device";
        }

        try {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(autoAddress);
            onDeviceSelected(device, autoName);
        } catch (Exception e) {
            Log.e(TAG, "Auto-connect failed for saved ELD device: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }

    public void shouldDisplayHomeUp() {
        //Enable Up button only  if there are entries in the back stack
        boolean canGoBack = true;
        getSupportActionBar().setDisplayHomeAsUpEnabled(canGoBack);
        getSupportActionBar().setDisplayShowHomeEnabled(canGoBack);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // Handle up button navigation for fragments
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
       startActivity(new Intent(this, Dashboard.class));
       finish();
        // Properly handle back press
        if (mPrivacyDlg != null && mPrivacyDlg.isShowing()) {
            mPrivacyDlg.dismiss();
            return;
        }

        // Clean up before finishing
        cleanup();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        // Clean up resources
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        // Dismiss dialog if showing
        if (mPrivacyDlg != null && mPrivacyDlg.isShowing()) {
            mPrivacyDlg.dismiss();
            mPrivacyDlg = null;
        }

        // Cancel any ongoing operations
        if (mTrackerBinder != null) {
            try {
                mTrackerBinder.getTracker().cancelUpdate();
                mTrackerBinder.cancelUpdateNotifications();
            } catch (Exception e) {
                Log.e(TAG, "Error during cleanup", e);
            }
        }
    }

    void getPrivacyConsent(final Activity activity, final SharedPreferences sharedPreferences) {
        mPrivacyDlg = new Dialog(this);
        mPrivacyDlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mPrivacyDlg.setContentView(R.layout.privacy_notice);
        mPrivacyDlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mPrivacyDlg.setCanceledOnTouchOutside(false);
        mPrivacyDlg.setOwnerActivity(this);

        TextView link = mPrivacyDlg.findViewById(R.id.privacy_link);
        link.setMovementMethod(LinkMovementMethod.getInstance());

        SwitchCompat modeUsb = mPrivacyDlg.findViewById(R.id.switchUsb);
        Button accept = mPrivacyDlg.findViewById(R.id.privacy_accept);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("privacy_accepted", true);

                if (modeUsb.isChecked()) {
                    editor.putBoolean("usb_mode", true);

                    // Set the USB mode
                    AppModel.MODE_USB = true;

                    // Replace default fragment with the USB
                    FragmentManager fm = getSupportFragmentManager();
                    Fragment fragment = fm.findFragmentById(R.id.fragment_container);
                    if (fragment != null) {
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(R.id.fragment_container, DevicesFragment.newInstance(), DevicesFragment.FRAG_TAG);
                        ft.commit();
                    }
                    TrackerManagerActivity.this.mConnectButton.setVisibility(View.INVISIBLE);
                } else {
                    editor.putBoolean("usb_mode", false);
                    AppModel.MODE_USB = false;
                }

                editor.apply();
                AppModel.getInstance().privacyAccepted = true;
                mPrivacyDlg.dismiss();

                // Show Disclosure
                ProminentDisclosureFragment.newInstance().show(getSupportFragmentManager(), ProminentDisclosureFragment.FRAG_TAG);
            }
        });

        mPrivacyDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });

        mPrivacyDlg.show();
    }

    @Override
    protected int getConnectionToggleResourceId() {
        return R.id.fab;
    }

    /**
     * View to be shown before 'Select a device'
     */
    @Override
    protected void setDefaultUI() {

    }

    @Override
    protected UUID getFilterUUID() {
        return UUID.fromString(Uart.RX_SERVICE_UUID.toString());
    }

    @Override
    synchronized public void onDeviceConnecting(BluetoothDevice device) {
        super.onDeviceConnecting(device);
        Log.i(TAG, "A: Tracker connecting  ....");
        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentByTag(TrackerConnectingFragment.FRAG_TAG) == null) {

            FragmentTransaction ft = fm.beginTransaction();

            // Show Tracker waiting view
            TrackerConnectingFragment tcf = TrackerConnectingFragment.newInstance();
            ft.replace(R.id.fragment_container, tcf, TrackerConnectingFragment.FRAG_TAG);
            ft.commit();
        } else {
            TrackerConnectingFragment tcf = (TrackerConnectingFragment) fm.findFragmentByTag(TrackerConnectingFragment.FRAG_TAG);
        }
    }

    @Override
    public void onDeviceConnected(final BluetoothDevice device) {
        super.onDeviceConnected(device);
        // Persist the last connected ELD device so the reconnect dialog can use it
        try {
            com.eagleye.eld.utils.PrefRepository prefRepo =
                    new com.eagleye.eld.utils.PrefRepository(getApplicationContext());
            String deviceName = device.getName() != null ? device.getName() : "ELD Device";
            prefRepo.setLastEldDevice(deviceName, device.getAddress());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save ELD device info: " + e.getMessage());
        }
        _onConnected();
    }

    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        showToast("BA: Failed to Connect :" + device.getAddress() + " rc = " + reason);
    }

    @Override
    public void onDeviceConnected(final UsbDevice device) {
        super.onDeviceConnected(device);
        _onConnected();
    }

    void _onConnected() {
        Log.i(TAG, "A: Tracker connected.");

        FragmentManager fm = getSupportFragmentManager();

        // Create only, if it does not exist, else reuse
        if (fm.findFragmentByTag(TrackerViewFragment.FRAG_TAG) == null) {

            FragmentTransaction ft = fm.beginTransaction();

            // Show Tracker View
            TrackerViewFragment tvf = TrackerViewFragment.newInstance().init();
            ft.add(R.id.fragment_container, tvf, TrackerViewFragment.FRAG_TAG);
            ft.commit();
        } else {
            TrackerViewFragment tvf = (TrackerViewFragment) fm.findFragmentByTag(TrackerViewFragment.FRAG_TAG);
            tvf.init();
        }

        // Show fab
        if (AppModel.MODE_USB) {
            mConnectButton.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onDeviceDisconnecting(final BluetoothDevice device) {

        super.onDeviceDisconnecting(device);

        Log.i(TAG, "A: Tracker disconnecting...");

        // Blank
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            DefaultFragment df = DefaultFragment.newInstance().init();
            ft.replace(R.id.fragment_container, df, DefaultFragment.FRAG_TAG);
            ft.commit();
        }
    }

    @Override
    public void onDeviceDisconnected(final BluetoothDevice device, int reason) {
        super.onDeviceDisconnected(device, reason);
        _onDisconnected();
    }

    @Override
    public void onDeviceDisconnected(final UsbDevice device) {
        super.onDeviceDisconnected(device);
        _onDisconnected();
    }

    void _onDisconnected() {
        Log.i(TAG, "A: Tracker disconnected.");
        // Blank
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            if (AppModel.MODE_USB) {
                ft.replace(R.id.fragment_container, DevicesFragment.newInstance(), DevicesFragment.FRAG_TAG);
            } else {
                DefaultFragment df = DefaultFragment.newInstance().init();
                ft.replace(R.id.fragment_container, df, DefaultFragment.FRAG_TAG);
            }
            ft.commit();
        }

        // hide fab
        if (AppModel.MODE_USB) {
            mConnectButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLinkLossOccurred(final BluetoothDevice device) {
        super.onLinkLossOccurred(device);

        Log.i(TAG, "A: Tracker lost link ...");

        // Wait for device
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            FragmentTransaction ft = fm.beginTransaction();
            TrackerConnectingFragment tcf = TrackerConnectingFragment.newInstance();
            ft.replace(R.id.fragment_container, tcf, TrackerConnectingFragment.FRAG_TAG);
            ft.commit();
        }
    }

    @Override
    public void onTrackerUpdateClosed() {
        // If update is in progress, cancel it
        if (mTrackerBinder != null) {
            try {
                Log.i(TAG, "A: Tracker update canceled");
                mTrackerBinder.getTracker().cancelUpdate();
                mTrackerBinder.cancelUpdateNotifications();
            } catch (Exception e) {
                Log.e(TAG, "Error canceling tracker update", e);
            }
        }
    }
}


