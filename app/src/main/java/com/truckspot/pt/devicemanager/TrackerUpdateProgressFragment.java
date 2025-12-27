package com.truckspot.pt.devicemanager;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.pt.sdk.TSError;
import com.truckspot.R;

public class TrackerUpdateProgressFragment extends DialogFragment {
    public static final String TAG = AppModel.TAG;
    public static final String FRAG_TAG = "update_fragment";


    final IntentFilter updIf = new IntentFilter();
    private OnTrackerUpdateClosedListener mListener;

    ProgressBar mPb;
    TextView mError;
    TextView mPercentage;
    private Button mCancelButton;
    AlertDialog progressDlg = null;

    public interface OnTrackerUpdateClosedListener {
        void onTrackerUpdateClosed();
    }

    BroadcastReceiver progressRefresh = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update pb from bcast
            Integer action = intent.getIntExtra(TrackerService.EXTRA_RESP_ACTION_KEY, 0);

            switch (action) {
                case TrackerService.EXTRA_TRACKER_UPDATE_ACTION_UPTODATE:
                    mPb.setVisibility(View.GONE);
                    mPercentage.setVisibility(View.GONE);

                    mError.setVisibility(View.VISIBLE);
                    mError.setTextColor(getResources().getColor(R.color.colorAccent));
                    mError.setText("Tracker is up to date.");
                    mCancelButton.setText(getResources().getString(R.string.upgrade_uptodate));
                    break;
                case TrackerService.EXTRA_TRACKER_UPDATE_ACTION_STARTED:
                    break;
                case TrackerService.EXTRA_TRACKER_UPDATE_ACTION_PROG:
                    int val = intent.getIntExtra(TrackerService.EXTRA_TRACKER_UPDATE_ARG_KEY, 0);
                    mPercentage.setText(val+"%");
                    if (val == 100) {
                        mPb.setIndeterminate(true);
                    } else {
                        mPb.setProgress(val);
                    }
                    break;
                case TrackerService.EXTRA_TRACKER_UPDATE_ACTION_COMPLETED:
                    //mCancelButton.performClick();
                    progressDlg.dismiss();
                    break;
                case TrackerService.EXTRA_TRACKER_UPDATE_ACTION_UPDATED:
                    // nop - this is handled in the View
                    break;
                default: // FAILED
                    mPb.setVisibility(View.GONE);
                    mPercentage.setVisibility(View.GONE);

                    mError.setVisibility(View.VISIBLE);
                    String cause = intent.getStringExtra(TSError.KEY_CAUSE);
                    Integer code = intent.getIntExtra(TSError.KEY_CODE, TSError.ERROR_FAIL);
                    mError.setText(code+","+cause);
            }
        }
    };

    public static TrackerUpdateProgressFragment getInstance() {
        final TrackerUpdateProgressFragment fragment = new TrackerUpdateProgressFragment();

        final Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        updIf.addAction("TRACKER-UPDATE");

        final AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_tracker_update_progress, null);

        builder.setTitle(R.string.upgrade_in_progress);
        progressDlg = builder.setView(dialogView).create();
        progressDlg.setCanceledOnTouchOutside(false);

        mPb = (ProgressBar)dialogView.findViewById(R.id.progressBar);
        mPb.setMax(100);

        mError = (TextView) dialogView.findViewById(R.id.errorText);
        mError.setVisibility(View.INVISIBLE);

        mCancelButton = dialogView.findViewById(R.id.action_cancel);
        mCancelButton.setOnClickListener(v -> {
            if (v.getId() == R.id.action_cancel) {
                progressDlg.cancel();
            }
        });

        mPercentage = (TextView) dialogView.findViewById(R.id.percent);

        return progressDlg;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mListener.onTrackerUpdateClosed();
    }

    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(progressRefresh, updIf);

    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(progressRefresh);
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnTrackerUpdateClosedListener) context;
        } catch (final ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTrackerUpdateClosedListener");
        }
    }
}
