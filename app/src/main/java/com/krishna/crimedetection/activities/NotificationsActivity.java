package com.krishna.crimedetection.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.krishna.crimedetection.adapters.NotificationsAdapter;
import com.krishna.crimedetection.databinding.ActivityNotificationsBinding;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.network.models.MessageResponse;
import com.krishna.crimedetection.network.models.NotificationListResponse;
import com.krishna.crimedetection.network.models.NotificationResponse;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity implements NotificationsAdapter.OnNotificationClickListener {

    private ActivityNotificationsBinding binding;
    private NotificationsAdapter adapter;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getApiService(this);

        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();

        fetchNotifications();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new NotificationsAdapter(this);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotifications.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::fetchNotifications);
        binding.swipeRefresh.setColorSchemeResources(com.krishna.crimedetection.R.color.colorPrimary);
    }

    private void fetchNotifications() {
        binding.swipeRefresh.setRefreshing(true);
        apiService.getNotifications(0, 50).enqueue(new Callback<NotificationListResponse>() {
            @Override
            public void onResponse(@NonNull Call<NotificationListResponse> call, @NonNull Response<NotificationListResponse> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    NotificationListResponse notificationList = response.body();
                    if (notificationList.getNotifications().isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.rvNotifications.setVisibility(View.GONE);
                    } else {
                        binding.emptyState.setVisibility(View.GONE);
                        binding.rvNotifications.setVisibility(View.VISIBLE);
                        adapter.setNotifications(notificationList.getNotifications());
                    }
                } else {
                    Toast.makeText(NotificationsActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NotificationListResponse> call, @NonNull Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Log.e("NotificationsActivity", "Error fetching notifications", t);
                Toast.makeText(NotificationsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onNotificationClick(NotificationResponse notification) {
        if (!notification.isRead()) {
            markAsRead(notification.getId());
        }

        if (notification.getIncidentId() != null) {
            Intent intent = new Intent(this, IncidentDetailsActivity.class);
            intent.putExtra(IncidentDetailsActivity.EXTRA_INCIDENT_ID, notification.getIncidentId().intValue());
            startActivity(intent);
        }
    }

    private void markAsRead(int notificationId) {
        apiService.markNotificationRead(notificationId).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    // Optional: refresh locally to show as read without full fetch
                    // For simplicity, we can just fetch again or rely on the next refresh
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                Log.e("NotificationsActivity", "Error marking notification as read", t);
            }
        });
    }
}
