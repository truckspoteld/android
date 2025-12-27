package com.truckspot.pt.devicemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.pt.sdk.GeolocParam;
import com.pt.sdk.TelemetryEvent;
import com.truckspot.R;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TrackerViewTabGpsFragment extends Fragment {

    private static final String TAG = "TabGpsFragment";

    protected TrackerViewFragment parentFrag = null;

    // Telemetery tile
    TextView tvEvent;
    TextView tvSeq;
    TextView tvDateTime;
    TextView tvGeoloc;
    TextView tvGeolocExtra;
    TextView tvSatStatus;
    TextView tvOdo;
    TextView tvVelo;
    TextView tvEh;
    TextView tvRpm;


    Switch swOdoUnit;   // Default (off) = Km
    final IntentFilter svcIf = new IntentFilter();

    final IntentFilter tmIf = new IntentFilter();
    BroadcastReceiver tmRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTelemetryInfo();
        }
    };

    BroadcastReceiver svcRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTelemetryInfo();
        }
    };

    // Gets called from a different thread
    void updateTelemetryInfo()
    {
        if (AppModel.getInstance().mLastEvent != null) {
            try {

                final TelemetryEvent te = AppModel.getInstance().mLastEvent;

                tvEvent.setText(te.mEvent.toString());
                tvSeq.setText(te.mSeq.toString());
                tvDateTime.setText(te.mDateTime.toString());

                GeolocParam gp = te.mGeoloc;
                tvGeoloc.setText(gp.latitude + "/" + gp.longitude);
                tvGeolocExtra.setText(gp.heading.toString());

                tvSatStatus.setText("LOCK:" + (gp.isLocked ? "1" : "0") + ", SAT:" + gp.satCount);

                if (swOdoUnit.isChecked()) {    // to Mile
                    BigDecimal km = new BigDecimal(te.mOdometer);
                    BigDecimal miles = km.multiply(BigDecimal.valueOf(0.621371));
                    miles = miles.setScale(2, RoundingMode.FLOOR);
                    tvOdo.setText(miles.toString());
                } else {
                    tvOdo.setText(te.mOdometer);
                }

                tvVelo.setText(te.mVelocity);
                tvEh.setText(te.mEngineHours);
                tvRpm.setText(te.mRpm.toString());

            } catch (Exception e) {
                Log.e(TAG, e.fillInStackTrace().toString());
            }
        }
    }


    public TrackerViewTabGpsFragment() {
        // Required empty public constructor
    }

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment TrackerViewTabGpsFragment.
//     */
//    // TODO: Rename and change types and number of parameters
//    public static TrackerViewTabGpsFragment newInstance(String param1, String param2) {
//        TrackerViewTabGpsFragment fragment = new TrackerViewTabGpsFragment();
//        return fragment;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tracker_view_tab_gps, container, false);

        tvEvent = (TextView)view.findViewById(R.id.tvEvent);
        tvSeq = (TextView)view.findViewById(R.id.tvSeq);
        tvDateTime = (TextView)view.findViewById(R.id.tvDateTime);
        tvGeoloc = (TextView)view.findViewById(R.id.tvGeoloc);
        tvGeolocExtra = (TextView)view.findViewById(R.id.tvGeolocExtra);
        tvSatStatus = (TextView)view.findViewById(R.id.tvSatStatus);
        tvOdo = (TextView)view.findViewById(R.id.tvOdo);
        tvVelo = (TextView)view.findViewById(R.id.tvVelo);
        tvEh = (TextView)view.findViewById(R.id.tvEh);
        tvRpm = (TextView)view.findViewById(R.id.tvRpm);

        swOdoUnit = (Switch)view.findViewById(R.id.swOdoUnit);

        swOdoUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (swOdoUnit.isChecked()) {    // to Mile
                    BigDecimal km = new BigDecimal(tvOdo.getText().toString());
                    BigDecimal miles = km.multiply(BigDecimal.valueOf(0.621371));
                    miles = miles.setScale(2, RoundingMode.CEILING);
                    tvOdo.setText(miles.toString());
                } else {
                    BigDecimal miles = new BigDecimal(tvOdo.getText().toString());
                    BigDecimal km = miles.divide(BigDecimal.valueOf(0.621371), BigDecimal.ROUND_HALF_DOWN);
                    tvOdo.setText(km.toString());
                }
            }
        });

        tmIf.addAction("REFRESH");
        svcIf.addAction("SVC-BOUND-REFRESH");
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(tmRefresh, tmIf);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(svcRefresh, svcIf);
        updateTelemetryInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        //LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(tmRefresh);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(tmRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(svcRefresh);
    }
}