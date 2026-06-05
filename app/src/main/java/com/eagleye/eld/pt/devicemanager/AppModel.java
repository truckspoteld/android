package com.eagleye.eld.pt.devicemanager;

import com.pt.sdk.SPNEventParam;
import com.pt.sdk.TelemetryEvent;
import com.pt.sdk.VehicleDiagTroubleCode;
import com.pt.sdk.VDbParser;
import com.pt.sdk.VirtualDashboard;
import com.pt.ws.TrackerInfo;
import com.pt.ws.VehicleInfo;

import java.util.HashSet;

public class AppModel {
    public static final String TAG = "EldMan";
    public static Boolean MODE_USB = false;


    private static AppModel ourInstance = new AppModel();

    public static AppModel getInstance() {
        return ourInstance;
    }



    // Per App Install
    public boolean privacyAccepted = false;

    // Per tracker session
    public TelemetryEvent mLastEvent= null;
    public TelemetryEvent mLastSEvent= null;

    public Integer mLastSen = 0;
    public SPNEventParam mLastSPNEv = null;
    public VehicleDiagTroubleCode mLastDTC = null;
    public TrackerInfo mTrackerInfo = null;
    public VehicleInfo mVehicleInfo = null;
    public String mPT30Vin = "n/a";
    public Integer mLastSECount = 0;

    public Integer mUpgradefromFileSelected = 0;
    public byte[] mFileContent;
    //public int mFupType;

    public long mConnectTime = 0;
    public Boolean mTrackerLostLink = false;

    public boolean wereSERequested = false;

    public VirtualDashboard.Snapshot dashboard = null;
    // Bound to a TrackerService instance
    public HashSet<Integer> vdbParams = new HashSet<>();

    // System variable tracking
    public String mPE = "n/a";

    private AppModel() {

    }

    /** True only for a real 17-char alphanumeric VIN (rejects "n/a", "1111" and other junk). */
    public static boolean isRealVin(String v) {
        return v != null && v.trim().matches("[A-Za-z0-9]{17}");
    }

    /**
     * The VIN the connected ECM is currently reporting, across BOTH PT-30 ({@link #mPT30Vin})
     * and PT-40 ({@link #mVehicleInfo}). Returns "" when neither reports a valid VIN yet.
     *
     * This is the RAW live VIN — compare it against the driver-confirmed anchored VIN to detect
     * an (unconfirmed) truck change. Everything that lands on a log (VIN, odometer, engine hours,
     * power-unit number) must stay on the anchored VIN until the driver confirms the switch.
     */
    public String getLiveEcmVin() {
        if (isRealVin(mPT30Vin)) return mPT30Vin.trim();
        if (mVehicleInfo != null && isRealVin(mVehicleInfo.VIN)) return mVehicleInfo.VIN.trim();
        return "";
    }

    public void invalidate()
    {
        mLastEvent= null;
        mLastSEvent = null;
        mLastSECount = 0;
        mLastDTC = null;
        mTrackerInfo = null;
        mVehicleInfo = null;
        mPT30Vin = "n/a";
        mUpgradefromFileSelected = 0;
        mLastSen = 0;
        mLastSPNEv = null;
    }
}
