package com.truckspot.pt.devicemanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pt.sdk.GeolocParam;
import com.pt.sdk.TelemetryEvent;
import com.pt.sdk.TrackerManager;
import com.pt.sdk.VirtualDashboard;
import com.pt.sdk.vdash.report.constants.EngineParamId;
import com.truckspot.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class TrackerViewTabEngineFragment extends Fragment {

    private static final String TAG = "TabEngineFragment";
    public static final String FRAG_TAG = "tracker_view_tab_engine";

    TextView tvBus;
    TextView tvGear;
    TextView tvSeatBelt;
    TextView tvBrakePedal;
    TextView tvEngineRPM;
    TextView tvRetarder;
    TextView tvEngineSpeed;
    TextView tvOilPressure;
    TextView tvOilTemperature;
    TextView tvCoolantTemperature;
    TextView tvIntakeTemperature;
    TextView tvIntakePressure;
    TextView tvFuelTankTemperature;
    TextView tvIntercoolerTemperature;
    TextView tvTurboOilTemperature;
    TextView tvTransmissionOilTemperature;
    TextView tvDtcNo;
    TextView tvAmbientTemperature;
    TextView tvEngineOdometer;
    TextView tvEngineLoad;
    TextView tvOilLevelPercent;
    TextView tvCoolantLevelPercent;
    TextView tvFuelLevelPercent;
    TextView tvFuelLevel2Percent;
    TextView tvDefLevelPercent;
    TextView tvEngineFuelRate;
    TextView tvEngineFuelEconomy;
    TextView tvAmbientPressure;
    TextView tvTotalEngineHours;
    TextView tvTotalEngineIdleTime;
    TextView tvTotalPtoTime;
    TextView tvTotalFuelUsed;
    TextView tvTotalEngineIdleFuel;

    final IntentFilter dbdIf = new IntentFilter();
    BroadcastReceiver dbdRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDashboard();
        }
    };

    BroadcastReceiver svcRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDashboard();
        }
    };

    final IntentFilter svcIf = new IntentFilter();

    public TrackerViewTabEngineFragment() {
        // Required empty public constructor
    }

    String toFormattedNumber(Double d) {
        String s = String.format("%.2f", d);
        return s;
    }

    String toFormattedNumber(Long l) {
        String s = String.format("%d", l);
        return s;
    }


    Boolean isParamAvailable(Integer paramId)
    {
        return (AppModel.getInstance().vdbParams.contains(paramId)) ? true : false;
    }


    void updateDashboard()
    {
        if (AppModel.getInstance().dashboard != null) {

            VirtualDashboard.Snapshot ss = AppModel.getInstance().dashboard;

            StringBuilder sb = new StringBuilder();
            if (isParamAvailable(EngineParamId.ID_BUS)) {
                 // Test OBD-II
                if ((ss.bus & 0x01) != 0) {
                    sb.append("OBD-II");
                }
                // Test J1708
                if ((ss.bus & 0x02) != 0) {
                    if (sb.length() != 0) {
                        sb.append("/");
                    }
                    sb.append("J1708");
                }
                // Test J1939
                if ((ss.bus & 0x04) != 0) {
                    if (sb.length() != 0) {
                        sb.append("/");
                    }
                    sb.append("J1939");
                }

            }
            if (sb.length() == 0) {
                sb.append("--");
            }
            tvBus.setText(sb);

            tvGear.setText(isParamAvailable(EngineParamId.ID_GEAR) ? ss.gear.toString() : "--");
            tvSeatBelt.setText(isParamAvailable(EngineParamId.ID_SEATBELT) ? ss.seatBelt.toString() : "--");
            tvBrakePedal.setText(isParamAvailable(EngineParamId.ID_BRAKE_INFO) ? ss.brakePedal.toString() : "--");
            tvRetarder.setText(isParamAvailable(EngineParamId.ID_BRAKE_INFO) ? toFormattedNumber(ss.retarderPercent) : "--");
            tvEngineRPM.setText(isParamAvailable(EngineParamId.ID_RPM) ? ss.engineRPM.toString() : "--");
            tvEngineSpeed.setText(isParamAvailable(EngineParamId.ID_SPEED) ? ss.engineSpeed.toString() : "--");
            tvOilPressure.setText(isParamAvailable(EngineParamId.ID_OIL_P) ? ss.oilPressure.toString() : "--");
            tvOilTemperature.setText(isParamAvailable(EngineParamId.ID_OIL_T) ? toFormattedNumber(ss.oilTemperature) : "--");
            tvCoolantTemperature.setText(isParamAvailable(EngineParamId.ID_COOL_T) ? toFormattedNumber(ss.coolantTemperature) : "--");
            tvIntakeTemperature.setText(isParamAvailable(EngineParamId.ID_INTAKE_T) ? toFormattedNumber(ss.intakeTemperature) : "--");
            tvIntakePressure.setText(isParamAvailable(EngineParamId.ID_INTAKE_P) ? toFormattedNumber(ss.intakePressure) : "--");
            tvFuelTankTemperature.setText(isParamAvailable(EngineParamId.ID_FUEL_T) ? toFormattedNumber(ss.fuelTankTemperature) : "--");
            tvIntercoolerTemperature.setText(isParamAvailable(EngineParamId.ID_INTERCOOLER_T) ? toFormattedNumber(ss.intercoolerTemperature) : "--");
            tvTurboOilTemperature.setText(isParamAvailable(EngineParamId.ID_TURBO_OIL_T) ? toFormattedNumber(ss.turboOilTemperature) : "--");
            tvTransmissionOilTemperature.setText(isParamAvailable(EngineParamId.ID_TRANS_OIL_T) ? toFormattedNumber(ss.transmissionOilTemperature) : "--");
            tvDtcNo.setText(isParamAvailable(EngineParamId.ID_DTC_N) ? toFormattedNumber(ss.dtcNo) : "--");
            tvAmbientTemperature.setText(isParamAvailable(EngineParamId.ID_AMB_T) ? toFormattedNumber(ss.ambientTemperature) : "--");
            tvEngineOdometer.setText(isParamAvailable(EngineParamId.ID_ODOMETER) ? toFormattedNumber(ss.engineOdometer) : "--");
            tvEngineLoad.setText(isParamAvailable(EngineParamId.ID_ENG_LOAD) ? toFormattedNumber(ss.engineLoad) : "--");
            tvOilLevelPercent.setText(isParamAvailable(EngineParamId.ID_OIL_L) ? toFormattedNumber(ss.oilLevelPercent) : "--");
            tvCoolantLevelPercent.setText(isParamAvailable(EngineParamId.ID_COOL_L) ? toFormattedNumber(ss.coolantLevelPercent) : "--");
            tvFuelLevelPercent.setText(isParamAvailable(EngineParamId.ID_FUEL_L) ? toFormattedNumber(ss.fuelLevelPercent) : "--");
            tvFuelLevel2Percent.setText(isParamAvailable(EngineParamId.ID_FUEL_L2) ? toFormattedNumber(ss.fuelLevel2Percent) : "--");
            tvDefLevelPercent.setText(isParamAvailable(EngineParamId.ID_DEF_L) ? toFormattedNumber(ss.defLevelPercent) : "--");
            tvEngineFuelRate.setText(isParamAvailable(EngineParamId.ID_FUEL_R) ?  toFormattedNumber(ss.engineFuelRate) : "--");
            tvEngineFuelEconomy.setText(isParamAvailable(EngineParamId.ID_FUEL_E) ? toFormattedNumber(ss.engineFuelEconomy) : "--");
            tvAmbientPressure.setText(isParamAvailable(EngineParamId.ID_AMB_P) ? toFormattedNumber(ss.ambientPressure) : "--");
            tvTotalEngineHours.setText(isParamAvailable(EngineParamId.ID_ENG_HOURS) ? toFormattedNumber(ss.totalEngineHours) : "--");
            tvTotalEngineIdleTime.setText(isParamAvailable(EngineParamId.ID_IDLE_HOURS) ? toFormattedNumber(ss.totalEngineIdleTime) : "--");
            tvTotalPtoTime.setText(isParamAvailable(EngineParamId.ID_PTO_HOURS) ? toFormattedNumber(ss.totalPtoTime) : "--");
            tvTotalFuelUsed.setText(isParamAvailable(EngineParamId.ID_FUEL_U) ? toFormattedNumber(ss.totalFuelUsed) : "--");
            tvTotalEngineIdleFuel.setText(isParamAvailable(EngineParamId.ID_FUEL_I) ? toFormattedNumber(ss.totalEngineIdleFuel) : "--");
        }
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tracker_view_tab_engine, container, false);

        tvBus =  (TextView)view.findViewById(R.id.tvBus);
        tvGear = (TextView)view.findViewById(R.id.tvGear);
        tvSeatBelt = (TextView)view.findViewById(R.id.tvSeatBelt);
        tvBrakePedal = (TextView)view.findViewById(R.id.tvBrakePedal);
        tvRetarder = (TextView)view.findViewById(R.id.tvRetarder);
        tvEngineRPM = (TextView)view.findViewById(R.id.tvEngineRPM);
        tvEngineSpeed = (TextView)view.findViewById(R.id.tvEngineSpeed);
        tvOilPressure = (TextView)view.findViewById(R.id.tvOilPressure);
        tvOilTemperature = (TextView)view.findViewById(R.id.tvOilTemperature);
        tvCoolantTemperature = (TextView)view.findViewById(R.id.tvCoolantTemperature);
        tvIntakeTemperature = (TextView)view.findViewById(R.id.tvIntakeTemperature);
        tvIntakePressure = (TextView)view.findViewById(R.id.tvIntakePressure);
        tvFuelTankTemperature = (TextView)view.findViewById(R.id.tvFuelTankTemperature);;
        tvIntercoolerTemperature = (TextView)view.findViewById(R.id.tvIntercoolerTemperature);
        tvTurboOilTemperature = (TextView)view.findViewById(R.id.tvTurboOilTemperature);
        tvTransmissionOilTemperature = (TextView)view.findViewById(R.id.tvTransmissionOilTemperature);
        tvDtcNo = (TextView)view.findViewById(R.id.tvDtcNo);
        tvAmbientTemperature = (TextView)view.findViewById(R.id.tvAmbientTemperature);
        tvEngineOdometer = (TextView)view.findViewById(R.id.tvEngineOdometer);
        tvEngineLoad = (TextView)view.findViewById(R.id.tvEngineLoad);
        tvOilLevelPercent = (TextView)view.findViewById(R.id.tvOilLevelPercent);
        tvCoolantLevelPercent = (TextView)view.findViewById(R.id.tvCoolantLevelPercent);
        tvFuelLevelPercent = (TextView)view.findViewById(R.id.tvFuelLevelPercent);
        tvFuelLevel2Percent = (TextView)view.findViewById(R.id.tvFuelLevel2Percent);
        tvDefLevelPercent = (TextView)view.findViewById(R.id.tvDefLevelPercent);
        tvEngineFuelRate = (TextView)view.findViewById(R.id.tvEngineFuelRate);
        tvEngineFuelEconomy = (TextView)view.findViewById(R.id.tvEngineFuelEconomy);
        tvAmbientPressure = (TextView)view.findViewById(R.id.tvAmbientPressure);
        tvTotalEngineHours = (TextView)view.findViewById(R.id.tvTotalEngineHours);
        tvTotalEngineIdleTime = (TextView)view.findViewById(R.id.tvTotalEngineIdleTime);
        tvTotalPtoTime = (TextView)view.findViewById(R.id.tvTotalPtoTime);
        tvTotalFuelUsed = (TextView)view.findViewById(R.id.tvTotalFuelUsed);
        tvTotalEngineIdleFuel = (TextView)view.findViewById(R.id.tvTotalEngineIdleFuel);

        dbdIf.addAction("TRACKER-DASHBOARD-REFRESH");
        svcIf.addAction("SVC-BOUND-REFRESH");

        return view;
    }

    // View get updated upon service bound refresh
    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(dbdRefresh, dbdIf);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(svcRefresh, svcIf);
        updateDashboard();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(dbdRefresh);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(svcRefresh);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.v(TAG, "TVF:ENG onViewCreated:"+this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "TVF:ENG onDestroy:"+this);
    }

    TrackerService.TrackerBinder getBinder() {
        TrackerManagerActivity tma = (TrackerManagerActivity) getActivity();
        if (tma == null) return null;
        if (tma.mTrackerBinder==null) Log.w(TAG, "TVTA: getBinder NULL");
        return tma.mTrackerBinder;
    }
}