package com.krishna.crimedetection.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.krishna.crimedetection.R;

public class EmergencyResponseDialog extends DialogFragment {

    private String address;
    private String driveLink;

    public static EmergencyResponseDialog newInstance(String address, String driveLink) {
        EmergencyResponseDialog frag = new EmergencyResponseDialog();
        Bundle args = new Bundle();
        args.putString("address", address);
        args.putString("driveLink", driveLink);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            address = getArguments().getString("address");
            driveLink = getArguments().getString("driveLink");
        }

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_emergency_response, null);
        TextView tvLocation = view.findViewById(R.id.tvLocationDetails);
        TextView tvDrive = view.findViewById(R.id.tvDriveLink);

        tvLocation.setText("📍 Location: " + address);
        tvDrive.setText("📹 Evidence: " + driveLink);

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setPositiveButton("OK", (dialog, which) -> dismiss())
                .setNeutralButton("View Map", (dialog, which) -> {
                    // TODO: Open Map
                })
                .create();
    }
}
