package com.truckspot.eld.pt.devicemanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.pt.sdk.SystemVar;
import com.pt.sdk.request.SetSystemVar;
import com.pt.ws.TrackerInfo;
import com.truckspot.eld.R;

public class TrackerPrefFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = AppModel.TAG;

    ActionBar mActionBar = null;

    public static TrackerPrefFragment newInstance() {
        TrackerPrefFragment fragment = new TrackerPrefFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        AppCompatActivity act = ((AppCompatActivity)getActivity());

        mActionBar = act.getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setSubtitle("Tracker System Variables");
         }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActionBar.setSubtitle("");
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.tracker_perf);
        setHasOptionsMenu(false);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        // Default PE = 10
        Log.v(TAG, " >>> Preference " + key+ ":" + sharedPreferences.getString(key, "10"));

        SetSystemVar ssv = new SetSystemVar();


        TrackerManagerActivity tma = (TrackerManagerActivity)getActivity();
        if (tma.mTrackerBinder != null && tma.mTrackerBinder.isConnected()) {
            TrackerInfo ti = AppModel.getInstance().mTrackerInfo;
            if ((ti != null) && (ti.product != null) && ti.product.contains("30")) {
                ssv.setValue(SystemVar.PERIODIC_EVENT_GAP, sharedPreferences.getString(key, "10"));
            } else {
                ssv.setVar("HUC", sharedPreferences.getString(key, "10"));
            }
            tma.mTrackerBinder.getTracker().sendRequest(ssv, null, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);

        // Disable fab to disallow disconnect
        getActivity().findViewById(R.id.fab).setEnabled(false);

    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        // Enable fab
        getActivity().findViewById(R.id.fab).setEnabled(true);

    }
}
