package com.truckspot.pt.devicemanager;


import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.INTERNET;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.truckspot.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrackerConnectingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackerConnectingFragment extends Fragment {
    public static final String TAG = AppModel.TAG;
    public static final String FRAG_TAG = "tracker_connecting";

    //private TrackerService.TrackerBinder mBinder = null;
    TextView tvConnecting;

    final IntentFilter svcIf = new IntentFilter();

    BroadcastReceiver svcRefresh = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            updateViews();
        }
    };

    @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT})
    void updateViews()
    {
        if (getBinder() != null) {
            if (AppModel.MODE_USB) {
                if (getBinder().getTracker().getUsbDevice() != null)
                    tvConnecting.setText("Connected to " + getBinder().getTracker().getUsbDevice().getDeviceName());
                else
                    tvConnecting.setText("Connected to UNKNOWN!"  );
            } else {
                if (getBinder().getTracker().getBluetoothDevice() != null)
                    tvConnecting.setText("Waiting for " + getBinder().getTracker().getBluetoothDevice().getName() + "...");
                else
                    tvConnecting.setText("Connected to UNKNOWN!"  );
            }
        }
    }

    public TrackerConnectingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment PrivacyFragment.
     */

    @NonNull
    public static TrackerConnectingFragment newInstance() {
        TrackerConnectingFragment fragment = new TrackerConnectingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "TCF: onCreate:"+this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracker_connecting, container, false);
        tvConnecting = (TextView) view.findViewById(R.id.tracker_connecting);
        svcIf.addAction("SVC-BOUND-REFRESH");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(svcRefresh, svcIf);
        Log.v(TAG, "TCF: onResume:"+this);

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "TCF: onPause:"+this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "TCF: onDestroy:"+this);
    }

    TrackerService.TrackerBinder getBinder() {
        TrackerManagerActivity tma = (TrackerManagerActivity) getActivity();
        if (tma == null) return null;
        if (tma.mTrackerBinder==null) Log.w(TAG, "TCF: getBinder NULL");
        return tma.mTrackerBinder;
    }
}
