package com.truckspot.pt.devicemanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.pt.sdk.Sdk;
import com.truckspot.R;


public class AppPrefFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String TAG = AppModel.TAG;
    ActionBar mActionBar = null;

    public static AppPrefFragment newInstance() {
        AppPrefFragment fragment = new AppPrefFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);

        AppCompatActivity act = ((AppCompatActivity) getActivity());

        mActionBar = act.getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setSubtitle("App Preferences");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActionBar.setSubtitle("");
    }


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_pref);
        setHasOptionsMenu(false);
    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("dev_mode_switch")) {
            Boolean devmode = sharedPreferences.getBoolean("dev_mode_switch", false);

            Sdk.getInstance().setDevMode(devmode);
            Log.d(TAG, "Dev mode changed" + ((devmode) ? "ON" : "OFF"));

        } else if (key.equals("version_override_switch")) {
            Boolean vo = sharedPreferences.getBoolean("version_override_switch", false);

            Sdk.getInstance().setProp("service.override.version", vo);
            Log.d(TAG, "Version override changed" + ((vo) ? "ON" : "OFF"));
        } else if (key.equals("user_version")) {
            String version = sharedPreferences.getString("user_version", "");

            if (!TextUtils.isEmpty(version)) {
                Sdk.getInstance().setProp("service.user.version", version);
                Log.d(TAG, "User Version " + version);
            }

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
