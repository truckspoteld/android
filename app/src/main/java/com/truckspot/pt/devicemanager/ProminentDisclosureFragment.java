package com.truckspot.pt.devicemanager;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.truckspot.R;


/**
 * A simple {@link DialogFragment} subclass.
 * create an instance of this fragment.
 */
public class ProminentDisclosureFragment extends DialogFragment {


    public static ProminentDisclosureFragment newInstance() {
        return new ProminentDisclosureFragment();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dlg = new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.disclosure))
                .setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
                    // NOP
                } )
                .create();
        dlg.setCanceledOnTouchOutside(false);
        return dlg;
    }

    public static String FRAG_TAG = "prominent_disc";

}