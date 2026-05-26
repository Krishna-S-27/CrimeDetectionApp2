package com.krishna.crimedetection.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.adapters.IncidentAdapter;
import com.krishna.crimedetection.databinding.ActivityIncidentHistoryBinding;
import com.krishna.crimedetection.models.FilterState;
import com.krishna.crimedetection.models.PaginationState;
import com.krishna.crimedetection.network.RetrofitClient;
import com.krishna.crimedetection.network.ApiService;
import com.krishna.crimedetection.network.models.IncidentListResponse;
import com.krishna.crimedetection.network.models.IncidentResponse;
import com.krishna.crimedetection.network.models.StatisticsResponse;
import com.krishna.crimedetection.viewmodel.CrimeViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncidentHistoryActivity extends AppCompatActivity implements IncidentAdapter.OnIncidentClickListener {

    private static final String TAG = "IncidentHistory";
    private ActivityIncidentHistoryBinding binding;
    private IncidentAdapter adapter;
    private ApiService apiService;
    private FilterState filterState;
    private PaginationState paginationState;
    private CrimeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIncidentHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CrimeViewModel.class);
        apiService = RetrofitClient.getApiService(this);
        filterState = new FilterState();
        paginationState = new PaginationState();

        setupUI();
        loadIncidents(false);
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.rvIncidents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncidentAdapter(this);
        binding.rvIncidents.setAdapter(adapter);

        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                IncidentResponse incident = adapter.getIncidentAt(position);
                if (incident != null) {
                    onDeleteSwipe(incident, position);
                }
            }
        }).attachToRecyclerView(binding.rvIncidents);

        // Filter Chips
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipAll)) filterState.setPrediction(null);
            else if (checkedIds.contains(R.id.chipViolent)) filterState.setPrediction("VIOLENT");
            else if (checkedIds.contains(R.id.chipNonViolent)) filterState.setPrediction("NONVIOLENT");
            
            resetAndLoad();
        });

        binding.chipDateRange.setOnClickListener(v -> showDateRangePicker());

        binding.swipeRefresh.setOnRefreshListener(this::resetAndLoad);
        binding.btnLoadMore.setOnClickListener(v -> {
            paginationState.nextPage();
            loadIncidents(true);
        });

        binding.btnResetFilters.setOnClickListener(v -> {
            filterState.reset();
            binding.chipAll.setChecked(true);
            resetAndLoad();
        });
    }

    private void resetAndLoad() {
        paginationState.reset();
        loadIncidents(false);
    }

    private void loadIncidents(boolean append) {
        binding.topProgressBar.setVisibility(View.VISIBLE);
        binding.btnLoadMore.setEnabled(false);

        apiService.getIncidents(
                paginationState.getSkip(),
                paginationState.getLimit(),
                filterState.getPrediction(),
                filterState.getSearchQuery(),
                filterState.getDateFrom(),
                filterState.getDateTo(),
                filterState.getSortBy()
        ).enqueue(new Callback<IncidentListResponse>() {
            @Override
            public void onResponse(Call<IncidentListResponse> call, Response<IncidentListResponse> response) {
                binding.topProgressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                binding.btnLoadMore.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    IncidentListResponse data = response.body();
                    paginationState.setTotalCount(data.getTotal());
                    
                    adapter.setIncidents(data.getIncidents(), append);
                    updateUIState(data.getIncidents().isEmpty() && !append);
                    updateStats(data);
                } else {
                    showError("Failed to load: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<IncidentListResponse> call, Throwable t) {
                binding.topProgressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                binding.btnLoadMore.setEnabled(true);
                showError("Network Error: " + t.getMessage());
            }
        });
    }

    private void updateUIState(boolean isEmpty) {
        binding.layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.rvIncidents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        boolean hasMore = paginationState.hasMore();
        binding.btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
        
        if (paginationState.getTotalCount() > 0) {
            binding.tvPaginationStatus.setVisibility(View.VISIBLE);
            String status = getString(R.string.showing_incidents_format, 
                    adapter.getItemCount(), paginationState.getTotalCount());
            binding.tvPaginationStatus.setText(status);
        } else {
            binding.tvPaginationStatus.setVisibility(View.GONE);
        }
    }

    private void updateStats(IncidentListResponse data) {
        // Fetch real stats from API
        apiService.getStatistics().enqueue(new Callback<StatisticsResponse>() {
            @Override
            public void onResponse(Call<StatisticsResponse> call, Response<StatisticsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatisticsResponse stats = response.body();
                    
                    binding.cardTotalIncidents.tvStatLabel.setText(R.string.stat_total_incidents);
                    binding.cardTotalIncidents.tvStatValue.setText(String.valueOf(stats.getTotalIncidents()));
                    binding.cardTotalIncidents.tvStatSub.setText(R.string.stat_all_time);

                    binding.cardViolentIncidents.tvStatLabel.setText(R.string.stat_violent);
                    binding.cardViolentIncidents.tvStatValue.setText(String.valueOf(stats.getViolentCount()));
                    binding.cardViolentIncidents.tvStatSub.setText(getString(R.string.stat_percent_of_total_format, 
                            (double) stats.getViolentCount() / Math.max(1, stats.getTotalIncidents()) * 100));

                    binding.cardAvgConfidence.tvStatLabel.setText(R.string.stat_avg_confidence);
                    binding.cardAvgConfidence.tvStatValue.setText(String.format(Locale.US, "%.1f%%", stats.getAvgConfidence() * 100));
                    binding.cardAvgConfidence.tvStatSub.setText(R.string.stat_model_precision);
                }
            }

            @Override
            public void onFailure(Call<StatisticsResponse> call, Throwable t) {
                Log.e(TAG, "Failed to load statistics", t);
            }
        });
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            filterState.setDateFrom(sdf.format(new Date(selection.first)));
            filterState.setDateTo(sdf.format(new Date(selection.second)));
            binding.chipDateRange.setText(getString(R.string.date_range_format, filterState.getDateFrom(), filterState.getDateTo()));
            resetAndLoad();
        });

        picker.show(getSupportFragmentManager(), "date_picker");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_incident_history, menu);
        
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search video name...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterState.setSearchQuery(query);
                resetAndLoad();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    filterState.setSearchQuery("");
                    resetAndLoad();
                }
                return false;
            }
        });
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.sort_newest) {
            filterState.setSortBy("newest");
            resetAndLoad();
            return true;
        } else if (id == R.id.sort_oldest) {
            filterState.setSortBy("oldest");
            resetAndLoad();
            return true;
        } else if (id == R.id.sort_confidence) {
            filterState.setSortBy("confidence_high");
            resetAndLoad();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onIncidentClick(IncidentResponse incident) {
        android.content.Intent intent = new android.content.Intent(this, IncidentDetailsActivity.class);
        intent.putExtra(IncidentDetailsActivity.EXTRA_INCIDENT_ID, incident.getId());
        startActivity(intent);
    }

    @Override
    public void onMenuClick(View view, IncidentResponse incident) {
        // Show popup menu for options like Download, Share, Delete
    }

    public void onDeleteSwipe(IncidentResponse incident, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_incident_title)
                .setMessage(R.string.delete_incident_msg)
                .setPositiveButton("Delete", (dialog, which) -> deleteIncident(incident))
                .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyItemChanged(position))
                .setOnCancelListener(dialog -> adapter.notifyItemChanged(position))
                .show();
    }

    @Override
    public void onDeleteSwipe(IncidentResponse incident) {
        // This method is from interface, but we use the one with position for swipe
    }

    private void deleteIncident(IncidentResponse incident) {
        // Since we don't have a direct delete endpoint in ApiService yet, 
        // this is a placeholder for the API call
        Toast.makeText(this, "Deleting incident " + incident.getId() + "...", Toast.LENGTH_SHORT).show();
        
        // Optimistically remove from UI
        adapter.removeIncident(incident);
        
        if (adapter.getItemCount() == 0) {
            updateUIState(true);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
