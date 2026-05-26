package com.krishna.crimedetection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.auth.ProfileActivity;
import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.CrimeRecord;
import com.krishna.crimedetection.utils.TimeUtils;
import com.krishna.crimedetection.viewmodel.CrimeViewModel;

import java.util.ArrayList;
import java.util.List;

public class
DashboardActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private HistoryAdapter adapter;
    private BottomNavigationView bottomNavigation;
    private CrimeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewModel = new ViewModelProvider(this).get(CrimeViewModel.class);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        rvHistory = findViewById(R.id.rvHistory);
        tvEmpty = findViewById(R.id.tvEmpty);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        setupNavigation();

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        rvHistory.setAdapter(adapter);

        viewModel.getAllCrimeRecords().observe(this, records -> {
            if (records == null || records.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvHistory.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvHistory.setVisibility(View.VISIBLE);
                adapter.setRecords(records);
            }
        });
    }

    private void setupNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_dashboard);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, IncidentHistoryActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_emergency) {
                startActivity(new Intent(this, EmergencyActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<CrimeRecord> records = new ArrayList<>();

        void setRecords(List<CrimeRecord> records) {
            this.records = records;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crime_record, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CrimeRecord r = records.get(position);
            holder.tvPrediction.setText(r.getPrediction().toUpperCase());
            holder.tvConfidence.setText(String.format("%.1f%%", r.getConfidence() * 100));
            holder.tvTimestamp.setText(TimeUtils.formatTimestamp(r.getTimestamp()));

            String locationText = String.format("Location: %.4f, %.4f", r.getLatitude(), r.getLongitude());
            if ("realtime".equalsIgnoreCase(r.getDetectionType())) {
                locationText += " (Real-time)";
            }
            holder.tvLocation.setText(locationText);

            if ("violent".equalsIgnoreCase(r.getPrediction()) || "crime".equalsIgnoreCase(r.getPrediction())) {
                holder.tvPrediction.setTextColor(0xFFEF4444); // Red
            } else {
                holder.tvPrediction.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.crime_safe));
            }
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvPrediction, tvConfidence, tvTimestamp, tvLocation;

            ViewHolder(View v) {
                super(v);
                tvPrediction = v.findViewById(R.id.tvPrediction);
                tvConfidence = v.findViewById(R.id.tvConfidence);
                tvTimestamp = v.findViewById(R.id.tvTimestamp);
                tvLocation = v.findViewById(R.id.tvLocation);
            }
        }
    }
}