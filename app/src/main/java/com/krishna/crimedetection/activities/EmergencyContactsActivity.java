package com.krishna.crimedetection.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.adapters.EmergencyContactAdapter;
import com.krishna.crimedetection.databinding.ActivityEmergencyContactsBinding;
import com.krishna.crimedetection.models.EmergencyContact;
import com.krishna.crimedetection.utils.TokenManager;
import com.krishna.crimedetection.viewmodel.EmergencyContactViewModel;

public class EmergencyContactsActivity extends AppCompatActivity {

    private ActivityEmergencyContactsBinding binding;
    private EmergencyContactViewModel viewModel;
    private EmergencyContactAdapter adapter;
    private int userId = 1; // Default for now

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEmergencyContactsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TokenManager tokenManager = new TokenManager(this);
        // Assuming we can get userId from TokenManager or Profile
        // userId = tokenManager.getUserId(); 

        viewModel = new ViewModelProvider(this).get(EmergencyContactViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupListeners();
        observeViewModel();

        viewModel.loadContacts(userId);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        binding.rvContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmergencyContactAdapter(new EmergencyContactAdapter.OnContactActionListener() {
            @Override
            public void onToggleActive(EmergencyContact contact, boolean isActive) {
                viewModel.toggleContactActive(userId, contact.getId(), isActive);
            }

            @Override
            public void onDelete(EmergencyContact contact) {
                viewModel.deleteContact(userId, contact.getId());
            }

            @Override
            public void onEdit(EmergencyContact contact) {
                showAddEditDialog(contact);
            }
        });
        binding.rvContacts.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.fabAdd.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void observeViewModel() {
        viewModel.getEmergencyContacts().observe(this, contacts -> {
            adapter.setContacts(contacts);
        });
    }

    private void showAddEditDialog(EmergencyContact contactToEdit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_emergency_contact, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etPhone);
        AutoCompleteTextView spinnerRelationship = dialogView.findViewById(R.id.spinnerRelationship);
        MaterialSwitch switchPrimary = dialogView.findViewById(R.id.switchPrimary);
        MaterialSwitch switchActive = dialogView.findViewById(R.id.switchActive);

        String[] relationships = {"FAMILY", "POLICE", "HOSPITAL", "FRIEND", "OTHER"};
        ArrayAdapter<String> adapterRelationship = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, relationships);
        spinnerRelationship.setAdapter(adapterRelationship);

        if (contactToEdit != null) {
            etName.setText(contactToEdit.getContactName());
            etPhone.setText(contactToEdit.getPhoneNumber());
            spinnerRelationship.setText(contactToEdit.getRelationship(), false);
            switchPrimary.setChecked(contactToEdit.isPrimary());
            switchActive.setChecked(contactToEdit.isActive());
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(contactToEdit == null ? "Add Contact" : "Edit Contact")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    String relationship = spinnerRelationship.getText().toString();

                    if (name.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    EmergencyContact contact = contactToEdit == null ? new EmergencyContact() : contactToEdit;
                    contact.setUserId(userId);
                    contact.setContactName(name);
                    contact.setPhoneNumber(phone);
                    contact.setRelationship(relationship);
                    contact.setPrimary(switchPrimary.isChecked());
                    contact.setActive(switchActive.isChecked());
                    contact.setUpdatedAt(System.currentTimeMillis());

                    if (contactToEdit == null) {
                        viewModel.addContact(contact);
                    } else {
                        viewModel.updateContact(contact);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
