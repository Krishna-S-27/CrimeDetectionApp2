package com.krishna.crimedetection.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.krishna.crimedetection.R;
import com.krishna.crimedetection.databinding.IncidentListItemBinding;
import com.krishna.crimedetection.network.models.IncidentResponse;
import com.krishna.crimedetection.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.List;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.ViewHolder> {

    private final List<IncidentResponse> incidents = new ArrayList<>();
    private final OnIncidentClickListener listener;
    private int lastPosition = -1;

    public interface OnIncidentClickListener {
        void onIncidentClick(IncidentResponse incident);
        void onMenuClick(View view, IncidentResponse incident);
        void onDeleteSwipe(IncidentResponse incident);
    }

    public IncidentAdapter(OnIncidentClickListener listener) {
        this.listener = listener;
    }

    public void setIncidents(List<IncidentResponse> newIncidents, boolean append) {
        if (!append) {
            incidents.clear();
            lastPosition = -1;
        }
        incidents.addAll(newIncidents);
        notifyDataSetChanged();
    }

    public IncidentResponse getIncidentAt(int position) {
        if (position >= 0 && position < incidents.size()) {
            return incidents.get(position);
        }
        return null;
    }

    public void removeIncident(IncidentResponse incident) {
        int position = incidents.indexOf(incident);
        if (position != -1) {
            incidents.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        IncidentListItemBinding binding = IncidentListItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IncidentResponse incident = incidents.get(position);
        holder.bind(incident);
        setAnimation(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return incidents.size();
    }

    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(viewToAnimate.getContext(), android.R.anim.slide_in_left);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final IncidentListItemBinding binding;

        ViewHolder(IncidentListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(IncidentResponse incident) {
            Context context = itemView.getContext();
            
            // Prediction Badge
            String prediction = incident.getPrediction().toUpperCase();
            binding.tvPredictionBadge.setText(prediction);
            
            boolean isViolent = "VIOLENT".equalsIgnoreCase(prediction) || "CRIME".equalsIgnoreCase(prediction);
            int color = isViolent ? 0xFFEF4444 : 0xFF10B981; // Red vs Green
            binding.tvPredictionBadge.setBackgroundTintList(ColorStateList.valueOf(color));
            
            // Confidence
            double confidence = incident.getConfidence();
            binding.tvConfidenceValue.setText(String.format(Locale.getDefault(), "%.1f%%", confidence * 100));
            binding.confidenceProgress.setProgress((int) (confidence * 100));
            binding.confidenceProgress.setIndicatorColor(color);

            // Video Name
            String path = incident.getVideoPath();
            String fileName = path != null ? path.substring(path.lastIndexOf("/") + 1) : "Unknown Video";
            binding.tvVideoName.setText(fileName);

            // Detection Type
            binding.chipType.setText(incident.getDetectionType() != null ? incident.getDetectionType().toUpperCase() : "UNKNOWN");

            // Timestamp - assuming incident.getTimestamp() is a readable string or ISO
            binding.tvTimestamp.setText(incident.getTimestamp());

            // Listeners
            itemView.setOnClickListener(v -> listener.onIncidentClick(incident));
            binding.btnMenu.setOnClickListener(v -> listener.onMenuClick(v, incident));
        }
    }
}
