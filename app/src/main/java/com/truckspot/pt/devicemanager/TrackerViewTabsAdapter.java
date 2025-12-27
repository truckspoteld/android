package com.truckspot.pt.devicemanager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TrackerViewTabsAdapter extends FragmentStateAdapter {

    private static final String TAG = AppModel.TAG;


    protected static String[] TABS = {"ELD Data", "Virtual Dashboard"};
    public TrackerViewTabsAdapter(TrackerViewFragment fragment) {
        super(fragment);
    }


    // Fragment will be destroyed when it gets too far from the viewport, and its state will be saved.
    // When the item is close to the viewport again, a new Fragment will be requested, and a previously saved state will be used to initialize it.
    // Default ctor used to restore the fragment
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = null;
        if (position == 0) {
            fragment = new TrackerViewTabGpsFragment();
        } else {
            fragment = new TrackerViewTabEngineFragment();
        }
        //Log.v(TAG, "TVF:TVTA createFragment:"+this);

        return fragment;
    }

    @Override
    public int getItemCount() {
        return TABS.length;
    }
}
