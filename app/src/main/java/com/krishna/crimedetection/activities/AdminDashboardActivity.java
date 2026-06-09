package com.krishna.crimedetection.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.krishna.crimedetection.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.krishna.crimedetection.databinding.ActivityAdminDashboardBinding;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.network.models.AdminDashboardResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {
    private ActivityAdminDashboardBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        apiService = RetrofitClient.getApiService(this);

        setupClickListeners();
        fetchDashboardData();
    }

    private void setupClickListeners() {
        binding.fabRefresh.setOnClickListener(v -> fetchDashboardData());
        binding.btnReviewIncidents.setOnClickListener(v -> {
            // startActivity(new Intent(this, IncidentReviewActivity.class));
            Toast.makeText(this, getString(R.string.msg_feature_coming_soon, "Review activity"), Toast.LENGTH_SHORT).show();
        });
        binding.btnManageUsers.setOnClickListener(v -> {
            // startActivity(new Intent(this, UserManagementActivity.class));
            Toast.makeText(this, getString(R.string.msg_feature_coming_soon, "User management"), Toast.LENGTH_SHORT).show();
        });
        binding.btnViewReports.setOnClickListener(v -> {
            // startActivity(new Intent(this, ReportsActivity.class));
            Toast.makeText(this, getString(R.string.msg_feature_coming_soon, "Reports activity"), Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchDashboardData() {
        apiService.getAdminDashboard().enqueue(new Callback<AdminDashboardResponse>() {
            @Override
            public void onResponse(Call<AdminDashboardResponse> call, Response<AdminDashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    Toast.makeText(AdminDashboardActivity.this, R.string.error_dashboard_load_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AdminDashboardResponse> call, Throwable t) {
                Log.e("AdminDashboard", "Error fetching data", t);
                Toast.makeText(AdminDashboardActivity.this, R.string.msg_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(AdminDashboardResponse data) {
        binding.tvTotalUsers.setText(String.valueOf(data.getTotalUsers()));
        binding.tvTotalIncidents.setText(String.valueOf(data.getTotalIncidents()));
        binding.tvPendingReviews.setText(String.valueOf(data.getPendingReviews()));
        binding.tvCriticalAlerts.setText(String.valueOf(data.getCriticalAlerts()));

        setupChart(data.getDailyTrends());
        setupRecentActivity(data.getRecentActivity());
    }

    private void setupChart(List<AdminDashboardResponse.ChartData> trends) {
        if (trends == null || trends.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < trends.size(); i++) {
            entries.add(new Entry(i, trends.get(i).getValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, getString(R.string.label_chart_detections));
        dataSet.setColor(Color.RED);
        dataSet.setCircleColor(Color.RED);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.RED);
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        binding.lineChart.setData(lineData);
        binding.lineChart.getDescription().setEnabled(false);
        binding.lineChart.getXAxis().setDrawGridLines(false);
        binding.lineChart.invalidate();
    }

    private void setupRecentActivity(List<AdminDashboardResponse.UserActivity> activity) {
        if (activity == null) return;
        binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentActivity.setAdapter(new ActivityAdapter(activity));
    }

    private static class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        private final List<AdminDashboardResponse.UserActivity> list;

        ActivityAdapter(List<AdminDashboardResponse.UserActivity> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AdminDashboardResponse.UserActivity item = list.get(position);
            holder.text1.setText(holder.itemView.getContext().getString(R.string.label_user_action_format, item.getUserName(), item.getAction()));
            holder.text2.setText(holder.itemView.getContext().getString(R.string.label_incident_id_format, item.getIncidentId()));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
