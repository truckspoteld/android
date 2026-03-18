package com.truckspot.eld.pt.devicemanager;


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.pt.sdk.DateTimeParam;
import com.pt.sdk.GeolocParam;
import com.pt.sdk.SPNEventDefinitionParam;
import com.pt.sdk.TelemetryEvent;
import com.pt.sdk.TrackerManager;
import com.pt.sdk.VehicleDiagTroubleCode;
import com.pt.sdk.request.ClearDiagTroubleCodes;
import com.pt.sdk.request.ClearStoredEvents;
import com.pt.sdk.request.ConfigureSPNEvent;
import com.pt.sdk.request.GetDiagTroubleCodes;
import com.pt.sdk.request.GetStoredEventsCount;
import com.pt.sdk.request.GetTrackerInfo;
import com.pt.sdk.request.GetVehicleInfo;
import com.pt.sdk.request.RetrieveStoredEvents;
import com.pt.sdk.response.outbound.AckStoredEvent;
import com.pt.ws.TrackerInfo;
import com.pt.ws.VehicleInfo;
import com.truckspot.eld.R;

import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
public class TrackerViewFragment extends Fragment  {
    public static final String TAG = AppModel.TAG;
    public static final String FRAG_TAG = "tracker_view";
    protected static final int REQUEST_SELECT_CTRL_FW = 3;
    protected static final int REQUEST_SELECT_BLE_FW = 4;

    private final Handler mHandler = new Handler();
    // Activity Result handling androidX way
    private ActivityResultLauncher<String>         mStoragePermissionResult = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if(result) {
                        Toast.makeText(getContext(), "Permission granted. Pl. try again.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.w(TAG, "onActivityResult: PERMISSION DENIED");
                    }
                }
            });



    // Device tile
    TextView tvModel;
    TextView tvSerial;
    TextView tvMac;
    TextView tvVersion;
    TextView tvVin;
    TextView tvRssi;
    View vSETile;
    TextView tvSEventCount;
    TextView tvSESeq;
    TextView tvSEvent;
    TrackerViewTabsAdapter tabsAdapter;
    ViewPager2 viewPager;
    final IntentFilter svcIf = new IntentFilter();
    final IntentFilter trackerIf = new IntentFilter();
    final IntentFilter tupIf = new IntentFilter();
    final IntentFilter dtcIf = new IntentFilter();
    final IntentFilter tviIf = new IntentFilter();
    final IntentFilter seIf = new IntentFilter();
    final IntentFilter spnIf = new IntentFilter();

    BroadcastReceiver svcRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "TVF: Service available ...");
            updateTrackerInfo();
            updateSE();
            Integer fwType = AppModel.getInstance().mUpgradefromFileSelected;
            if (fwType > 0) {
                upgradeFromFile(fwType == REQUEST_SELECT_BLE_FW ? TrackerManager.FT_BLE : TrackerManager.FT_CTRL);
            }
        }
    };
    BroadcastReceiver tiRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTrackerInfo();
        }
    };

    BroadcastReceiver tupRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Integer action = intent.getIntExtra(TrackerService.EXTRA_RESP_ACTION_KEY, 0);
            switch (action) {
                case TrackerService.EXTRA_TRACKER_UPDATE_ACTION_UPDATED:
                    Toast.makeText(getContext(), "Tracker was successfully updated.", Toast.LENGTH_SHORT).show();
                    break;
                default:
            }
        }
    };
    BroadcastReceiver dtcRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra(TrackerService.EXTRA_RESP_ACTION_KEY);
             if (action.equals("GET")) {
                VehicleDiagTroubleCode dtc = AppModel.getInstance().mLastDTC;
                StringBuilder sb = new StringBuilder();
                sb.append("Malfunction Indicator:").append(dtc.mDtc.mil).append("\n")
                        .append("Bus:").append(dtc.mDtc.busType.name()).append("\n");
                if (dtc.mDtc.codes.size() != 0) {
                    for (String code: dtc.mDtc.codes) {
                        sb.append(code).append(",");
                    }
                    // remove the trailing comma
                    int sz = sb.length();
                    sb.deleteCharAt(sz-1);
                } else {
                    sb.append("No codes.");
                }

                Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), sb.toString(), Snackbar.LENGTH_LONG);
                TextView textView = (TextView) snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                textView.setMaxLines(4);  
                snackbar.getView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSbSuccess));
                snackbar.show();

            } else if (action.equals("CLEAR")) {
                Integer status = intent.getIntExtra(TrackerService.EXTRA_RESP_STATUS_KEY, 0);
                if (status == 0) {
                    Toast.makeText(context, "DTC cleared!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "DTC clear failed!", Toast.LENGTH_LONG).show();
                }
            }
        }
    };
    BroadcastReceiver viRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateVehicleInfo();
        }
    };
     BroadcastReceiver spnRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
             StringBuilder sb = new StringBuilder();
            Integer fl = Math.round(AppModel.getInstance().mLastSPNEv.value * 0.4f);
            sb.append("Tank 1 level: ").append(fl).append("%");
             Snackbar snackbar = Snackbar.make(getActivity().findViewById(android.R.id.content), sb.toString(), Snackbar.LENGTH_LONG);
            snackbar.getView().setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSbSuccess));
            snackbar.show();
        }
    };
    TrackerInfo ti = AppModel.getInstance().mTrackerInfo;
     void updateTrackerInfo() {
         if (getBinder() != null) {
            tvMac.setText(getBinder().getDeviceAddress());
        }
         if (AppModel.getInstance().mTrackerInfo != null) {
            TrackerInfo ti = AppModel.getInstance().mTrackerInfo;
             if (ti.product != null) {
                tvModel.setText(ti.product);
            } else {
                tvModel.setText("Generic");
            }
            tvSerial.setText(ti.SN);
            if (ti.product.contains("30")) {
                 if (getBinder() != null && AppModel.getInstance().mTrackerInfo != null) {
                     GetTrackerInfo gti = new GetTrackerInfo();
                    getBinder().getTracker().sendRequest(gti, null, null);
                }
            }
             String ver = "F/W:" + ti.mvi.toString() + "  BLE:" + ti.bvi.toString();
            tvVersion.setText(ver);
            try {
                if (ti.product.contains("30")) {
                    String vinNumber = AppModel.getInstance().mPT30Vin;
                    if (vinNumber != null && !vinNumber.isEmpty()) {
                        tvVin.setText(vinNumber);
                        getActivity().invalidateOptionsMenu();
                    } else {
                        updateVehicleInfo();
                        // If VIN is not available, fetch it
                        GetTrackerInfo gti = new GetTrackerInfo();
                        getBinder().getTracker().sendRequest(gti, null, null);
                    }
                }
            } catch (NullPointerException ex) {
             }
        } else if (getBinder() != null) {
            GetTrackerInfo gti = new GetTrackerInfo();
            getBinder().getTracker().sendRequest(gti, null, null);
        }
     }
     void updateVehicleInfo() {

        if (AppModel.getInstance().mVehicleInfo != null) {
            VehicleInfo vi = AppModel.getInstance().mVehicleInfo;

            tvVin.setText(vi.VIN);
        }
    }

    BroadcastReceiver seRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            updateSE();
        }
    };

    void updateSE()
    {
        if (AppModel.getInstance().mLastSECount == 0) {
            vSETile.setVisibility(View.GONE);
        } else {
            vSETile.setVisibility(View.VISIBLE);
            tvSEventCount.setText(AppModel.getInstance().mLastSECount.toString());
        }

        // Update Event
        if (AppModel.getInstance().mLastSEvent != null) {
            tvSESeq.setText(AppModel.getInstance().mLastSEvent.mSeq.toString());
            tvSEvent.setText(AppModel.getInstance().mLastSEvent.mEvent.toString());
        }
    }

    void getStoredEventsCount()
    {
        // Get Stored Events count
        Log.i(TAG, "Get Stored Events count ...");
        GetStoredEventsCount gSec = new GetStoredEventsCount();
        if (getBinder() != null) {
            getBinder().getTracker().sendRequest(gSec, null, null);
        }
    }

    void ackStoredEvent(TelemetryEvent tm)
    {
        // Ack
        GeolocParam geoLoc = tm.mGeoloc;
        DateTimeParam dt = tm.mDateTime;

        StringBuilder params = new StringBuilder();

        String id = "";


        params.append("id=").append(id)
                .append("&lat=").append(geoLoc.latitude)
                .append("&lon=").append(geoLoc.longitude)
                .append("&sat=").append(geoLoc.satCount)
                .append("&speed=").append(geoLoc.speed)
                .append("&head=").append(geoLoc.heading)
                .append("&date=").append(dt.date)
                .append("&time=").append(dt.time);

        // Do something
        Log.i(TAG, "EVENT:" + tm.mEvent.toString() + ":" + tm.mSeq);

        // ACK the event
        AckStoredEvent ack = new AckStoredEvent(0, tm.mSeq.toString(), dt.toDateString());
        if (getBinder() != null) {
            getBinder().getTracker().sendResponse(ack, null, null);
        }

        // Refresh count
        getStoredEventsCount();
    }




    public TrackerViewFragment() {
        // Required empty public constructor
    }

    public static TrackerViewFragment newInstance() {
        TrackerViewFragment fragment = new TrackerViewFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TrackerViewFragment init()
    {
        AppModel.getInstance().mConnectTime = System.currentTimeMillis();
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Log.v(TAG, "TVF: onCreate: "+this);
    }


    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putString("SIS_DA", getBinder().getDeviceAddress());
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "TVF: onDestroy:"+this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_tracker_view, container, false);
        tvModel = view.findViewById(R.id.tvModel);
        tvSerial = view.findViewById(R.id.tvSerial);
        tvMac = view.findViewById(R.id.tvMac);
        tvVersion = view.findViewById(R.id.tvVersion);
        tvVin = view.findViewById(R.id.tvVIN);
        tvRssi = view.findViewById(R.id.tvRssi);

        Button refreshVin = view.findViewById(R.id.refresh_vin);
        refreshVin();
        refreshVin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshVin();
            }
        });
        vSETile = view.findViewById(R.id.stored_events_tile);
        vSETile.setVisibility(View.GONE);
        tvSEventCount = view.findViewById(R.id.tvSEventCount);
        tvSESeq = view.findViewById(R.id.tvSESeq);
        tvSEvent = view.findViewById(R.id.tvSEvent);
        Button clearSE = view.findViewById(R.id.clear_stored_events);
        clearSE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 if (getBinder() == null) {
                    return;
                }
                 ClearStoredEvents cse = new ClearStoredEvents();
                getBinder().getTracker().sendRequest(cse, null, null);
            }
        });
         Button detailsSE = view.findViewById(R.id.details_stored_event);
        detailsSE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 if (getBinder() == null) {
                    return;
                }
                 if (AppModel.getInstance().mLastSEvent == null){
                    return;
                }
                 TelemetryEvent te = AppModel.getInstance().mLastSEvent;
                 LayoutInflater inflater = requireActivity().getLayoutInflater();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                 final View dialog_se_view = getLayoutInflater().inflate(R.layout.dialog_se_view, null);
                 builder.setTitle("Stored Event #: "+AppModel.getInstance().mLastSEvent.mSeq.toString())
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .setView(dialog_se_view);
                AlertDialog dialog = builder.create();
                 TextView tvDateTime = dialog_se_view.findViewById(R.id.tv_se_DateTime);
                TextView  tvGeoLoc = dialog_se_view.findViewById(R.id.tv_se_Geoloc);
                TextView  tvGeoLocExtra = dialog_se_view.findViewById(R.id.tv_se_GeolocExtra);
                TextView  tvSatStatus = dialog_se_view.findViewById(R.id.tv_se_SatStatus);
                TextView  tvOdo = dialog_se_view.findViewById(R.id.tv_se_Odo);
                TextView  tvVelo = dialog_se_view.findViewById(R.id.tv_se_Velo);
                TextView tvEh = dialog_se_view.findViewById(R.id.tv_se_Eh);
                TextView tvRpm = dialog_se_view.findViewById(R.id.tv_se_Rpm);
                 tvDateTime.setText(te.mDateTime.toString());
                 GeolocParam gp = te.mGeoloc;
                tvGeoLoc.setText(gp.latitude + "/" + gp.longitude);
                tvGeoLocExtra.setText(gp.heading.toString());
                 tvSatStatus.setText("LOCK:" + (gp.isLocked ? "1" : "0") + ", SAT:" + gp.satCount);
                tvVelo.setText(te.mVelocity);
                tvEh.setText(te.mEngineHours);
                 tvRpm.setText(te.mRpm.toString());
                 dialog.show();
             }
        });
        Button ackSE = view.findViewById(R.id.ack_stored_event);
        ackSE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 if (getBinder() == null) {
                    return;
                }
                 if (AppModel.getInstance().mLastSEvent != null) {
                    ackStoredEvent(AppModel.getInstance().mLastSEvent);
                }
            }
        });
         Button retrieveSE = view.findViewById(R.id.retrieve_stored_events);
        retrieveSE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 if (getBinder() == null) {
                    return;
                }
                 RetrieveStoredEvents rse = new RetrieveStoredEvents();
                getBinder().getTracker().sendRequest(rse, null, null);
                // Hide the button after initiating the retrieval
                if (!AppModel.getInstance().wereSERequested) {
                    view.setVisibility(View.GONE);
                    AppModel.getInstance().wereSERequested = true;
                }
            }
        });

        // End Stored Events tile


        svcIf.addAction("SVC-BOUND-REFRESH");
        trackerIf.addAction("TRACKER-REFRESH");
        tupIf.addAction("TRACKER-UPDATE");
        dtcIf.addAction("TRACKER-DTC-REFRESH");
        dtcIf.addAction("TRACKER-DTC-CLEAR");
        tviIf.addAction("TRACKER-VIN-REFRESH");
        seIf.addAction("TRACKER-SE-REFRESH");
        spnIf.addAction("TRACKER-SPN-REFRESH");

        // If Debug is ON, change panel bkg color
        View diPanel = view.findViewById(R.id.device_info_panel);

        SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.perf_file), Context.MODE_PRIVATE);
        Boolean devMode = sharedPref.getBoolean("dev_mode_switch", false);
        if (devMode && getContext() != null) {
            diPanel.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSbWarn));
        }

        // Restore view
        if (savedInstanceState != null) {
            //tvMac.setText(savedInstanceState.getString("SIS_DA"));
            updateTrackerInfo();
            updateVehicleInfo();
//            updateTelemetryInfo();
        }

        Log.v(TAG, "TVF: view initialized");

        return view;
    }

    void refreshVin(){
        fetchVinInfo();
        fetchVinInfo();
        if (getBinder() == null) {
            Toast.makeText(getContext(),"Error fetching VIN", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tracker Info was retrieved during onSync
        if (AppModel.getInstance().mTrackerInfo != null) {
            TrackerInfo ti = AppModel.getInstance().mTrackerInfo;

            if (ti.product.contains("30")) {
                GetTrackerInfo gti = new GetTrackerInfo();
                getBinder().getTracker().sendRequest(gti, null, null);
            } else {
                GetVehicleInfo gvi = new GetVehicleInfo();
                getBinder().getTracker().sendRequest(gvi, null, null);
            }
        } else {
            GetTrackerInfo gti = new GetTrackerInfo();
            getBinder().getTracker().sendRequest(gti, null, null);
        }
    }

    private void fetchVinInfo() {
        if (getBinder() == null) {
            Toast.makeText(getContext(), "Error fetching VIN", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tracker Info was retrieved during onSync
        if (AppModel.getInstance().mTrackerInfo != null) {
            TrackerInfo ti = AppModel.getInstance().mTrackerInfo;

            if (ti.product.contains("30")) {
                GetTrackerInfo gti = new GetTrackerInfo();
                getBinder().getTracker().sendRequest(gti, null, null);
            } else {
                GetVehicleInfo gvi = new GetVehicleInfo();
                getBinder().getTracker().sendRequest(gvi, null, null);
            }
        } else {
            GetTrackerInfo gti = new GetTrackerInfo();
            getBinder().getTracker().sendRequest(gti, null, null);
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tabsAdapter = new TrackerViewTabsAdapter(this);
        viewPager = view.findViewById(R.id.pager);
        viewPager.setAdapter(tabsAdapter);
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager,
                 (tab, position) -> tab.setText(TrackerViewTabsAdapter.TABS[position])
        ).attach();
fetchVinInfo();
        Log.v(TAG, "TVF: onViewCreated");
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "TVF: onResume:"+this);
        Context ctx = null;
        if ( (ctx = getContext()) != null) {
            LocalBroadcastManager.getInstance(ctx).registerReceiver(svcRefresh, svcIf);
            LocalBroadcastManager.getInstance(ctx).registerReceiver(tiRefresh, trackerIf);
            LocalBroadcastManager.getInstance(ctx).registerReceiver(tupRefresh, tupIf);
            LocalBroadcastManager.getInstance(ctx).registerReceiver(dtcRefresh, dtcIf);
            LocalBroadcastManager.getInstance(ctx).registerReceiver(viRefresh, tviIf);
            LocalBroadcastManager.getInstance(ctx).registerReceiver(seRefresh, seIf);
            LocalBroadcastManager.getInstance(ctx).registerReceiver(spnRefresh, spnIf);
        }

        updateTrackerInfo();
        updateVehicleInfo();
        updateSE();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(TAG, "TVF: onPause:"+this);

        Context ctx = null;
        if ( (ctx = getContext()) != null) {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(svcRefresh);
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(tiRefresh);
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(tupRefresh);
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(dtcRefresh);
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(viRefresh);
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(seRefresh);
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(spnRefresh);
        }
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_tracker_view, menu);
    }

    @Override
    public void onPrepareOptionsMenu (Menu menu) {

        // FIXME
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        boolean isNetAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        if (isNetAvailable ){
            menu.findItem(R.id.action_fup).setEnabled(true);
            menu.findItem(R.id.action_chk_fup).setEnabled(true);
        }else {
            menu.findItem(R.id.action_fup).setEnabled(false);
            menu.findItem(R.id.action_chk_fup).setEnabled(false);
        }

        menu.findItem(R.id.action_fup_file).setEnabled(true);

        // SPN only supported on PT30
        // Invalidate menu on TrackerInfo refresh
        TrackerInfo ti = AppModel.getInstance().mTrackerInfo;
        if ((ti != null) && (ti.product != null) && ti.product.contains("30")) {
            menu.findItem(R.id.action_sv).setEnabled(true);
            menu.findItem(R.id.action_demo_spn).setEnabled(true);
        } else {
            menu.findItem(R.id.action_sv).setEnabled(true);
            menu.findItem(R.id.action_demo_spn).setEnabled(false);
        }


        menu.findItem(R.id.action_get_dtc).setEnabled(true);
        menu.findItem(R.id.action_clear_dtc).setEnabled(true);
    }


    class ChkUpdateTask extends AsyncTask<Void, Void, Long> {

        boolean mIsAvailable = false;
        boolean mError = false;
        String msg;

        protected Long doInBackground(Void... voids ) {

            if (getBinder() == null) {
                mError = true;
                msg = "Try again";
                return 0L;
            }

            try {
                mIsAvailable = getBinder().getTracker().isUpdateAvailable(getContext());

            } catch (IllegalStateException ise) {
                mError = true;
                msg = ise.getLocalizedMessage();
            } catch (IllegalArgumentException iae) {
                mError = true;
                msg = iae.getLocalizedMessage();
            }

            return 0L;
        }


        protected void onPostExecute(Long result) {
            if (!mError) {
                if (mIsAvailable) {
                    Toast.makeText(getContext(), "An update is available!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "No updates available.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "Error:"+msg, Toast.LENGTH_LONG).show();
            }
        }
    }


    ActivityResultLauncher<String> getControllerFW = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {

                    if (uri == null) return;

                    Log.v(TAG, "File Uri: "+ uri);
                    try {

                        DataInputStream dis = new DataInputStream(getActivity().getContentResolver().openInputStream(uri));
                        AppModel.getInstance().mFileContent = new byte[(int) dis.available()];
                        Log.i(TAG, "File size: "+ dis.available());
                        dis.readFully(AppModel.getInstance().mFileContent);
                        dis.close();

                        AppModel.getInstance().mUpgradefromFileSelected = REQUEST_SELECT_CTRL_FW;

                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "FNF", e.fillInStackTrace());
                    } catch (IOException e) {
                        Log.e(TAG, "IO",e.fillInStackTrace());
                    }
                }
            });


    ActivityResultLauncher<String> getBLEFW = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {

                    if (uri == null) return;

                    Log.v(TAG, "File Uri: "+ uri);
                    try {

                        DataInputStream dis = new DataInputStream(getActivity().getContentResolver().openInputStream(uri));
                        AppModel.getInstance().mFileContent = new byte[(int) dis.available()];
                        Log.i(TAG, "File size: "+ dis.available());
                        dis.readFully(AppModel.getInstance().mFileContent);
                        dis.close();

                        AppModel.getInstance().mUpgradefromFileSelected = REQUEST_SELECT_BLE_FW;
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "FNF", e.fillInStackTrace());
                    } catch (IOException e) {
                        Log.e(TAG, "IO",e.fillInStackTrace());
                    }
                }
            });


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        int id = item.getItemId();
        TrackerInfo ti = AppModel.getInstance().mTrackerInfo;

        if (getBinder() == null) {
            super.onOptionsItemSelected(item);
        }
        //final WeakReference<Activity> activityRef = new WeakReference<Activity>(TrackerViewActivity.this);
        if (id == R.id.action_fup) {
            if (getBinder().getTracker() != null) {
                getBinder().getTracker().update(getContext());
                showUpdateProgressDialog();
            }
            return true;
        } else if (id == R.id.action_chk_fup) {
            new ChkUpdateTask().execute();
            return true;
        }  else if (id == R.id.action_fup_file_ctrl) {

            int res = getActivity().checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

            if (res != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Storage permission not available!", Toast.LENGTH_LONG).show();
                //this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_READ_STORAGE_PERMISSION);
                mStoragePermissionResult.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                getControllerFW.launch("*/*");
            }

            return true;
        } else if (id == R.id.action_fup_file_ble) {

            int res = getActivity().checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (res != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Storage permission not available!", Toast.LENGTH_LONG).show();
                //this.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_READ_STORAGE_PERMISSION);
                mStoragePermissionResult.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                getBLEFW.launch("*/*");
            }
            return true;
        } else if (id == R.id.action_sv) {
            FragmentManager fm = getActivity().getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            TrackerPrefFragment frag = TrackerPrefFragment.newInstance();
            ft.replace(R.id.fragment_container,frag );
            ft.addToBackStack("tracker_perf");
            ft.commit();
            return true;
        } else if (id == R.id.action_get_dtc) {

            GetDiagTroubleCodes getDTC = new GetDiagTroubleCodes();
            if (getBinder() != null) {
                getBinder().getTracker().sendRequest(getDTC, null, null);
            }
            return true;
        } else if (id == R.id.action_clear_dtc) {

            ClearDiagTroubleCodes clearDTC = new ClearDiagTroubleCodes();
            if (getBinder() != null) {
                getBinder().getTracker().sendRequest(clearDTC, null, null);
            }
            return true;
        } else if (id == R.id.action_demo_spn) {
            // Fuel gauge example
            SPNEventDefinitionParam.DefinitionBuilder builder = new SPNEventDefinitionParam.DefinitionBuilder(0)
                    .setSpn(96)
                    .setMode(0)
                    .setTimer(15)
                    .setValue(0)
                    .setPgn(65276)
                    .setAddress(255)
                    .setStartByte(2)
                    .setStartBit(0)
                    .setLength(8);
            ConfigureSPNEvent config_spn = new ConfigureSPNEvent(builder.build());
            if ((ti != null) && (ti.product != null) && ti.product.contains("30")) {
                config_spn.enableLegacy();
            }
            if (getBinder() != null) {
                getBinder().getTracker().sendRequest(config_spn, null, null);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showUpdateProgressDialog() {
        final TrackerUpdateProgressFragment dialog = TrackerUpdateProgressFragment.getInstance();
        dialog.show(getActivity().getSupportFragmentManager(), TrackerUpdateProgressFragment.FRAG_TAG);
    }


    public void upgradeFromFile(Integer filetype) {
        if (getBinder() != null) {
            getBinder().getTracker().update(getContext(), "file", (long)filetype, AppModel.getInstance().mFileContent);
            showUpdateProgressDialog();
            AppModel.getInstance().mUpgradefromFileSelected = 0;
        }
    }

     TrackerService.TrackerBinder getBinder() {
         TrackerManagerActivity tma = (TrackerManagerActivity) getActivity();
         if (tma == null) return null;
         if (tma.mTrackerBinder==null) Log.w(TAG, "TVF: getBinder NULL");
         return tma.mTrackerBinder;
     }
}
