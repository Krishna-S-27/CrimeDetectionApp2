package com.krishna.crimedetection.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.models.EmergencyContact;
import java.util.ArrayList;
import java.util.List;

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ViewHolder> {

    private List<EmergencyContact> contacts = new ArrayList<>();
    private final OnContactActionListener listener;

    public interface OnContactActionListener {
        void onToggleActive(EmergencyContact contact, boolean isActive);
        void onDelete(EmergencyContact contact);
        void onEdit(EmergencyContact contact);
    }

    public EmergencyContactAdapter(OnContactActionListener listener) {
        this.listener = listener;
    }

    public void setContacts(List<EmergencyContact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.emergency_contact_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.tvName.setText(contact.getContactName());
        holder.tvPhone.setText(maskPhoneNumber(contact.getPhoneNumber()));
        holder.tvRelationship.setText(contact.getRelationship());
        holder.switchActive.setChecked(contact.isActive());

        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onToggleActive(contact, isChecked);
        });

        holder.btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenu().add("Edit");
            popup.getMenu().add("Delete");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Edit")) {
                    listener.onEdit(contact);
                } else if (item.getTitle().equals("Delete")) {
                    listener.onDelete(contact);
                }
                return true;
            });
            popup.show();
        });
    }

    private String maskPhoneNumber(String phone) {
        if (phone.length() < 7) return phone;
        return phone.substring(0, 3) + " *** " + phone.substring(phone.length() - 4);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvRelationship;
        MaterialSwitch switchActive;
        ImageButton btnMenu;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvContactName);
            tvPhone = v.findViewById(R.id.tvPhoneNumber);
            tvRelationship = v.findViewById(R.id.tvRelationship);
            switchActive = v.findViewById(R.id.switchActive);
            btnMenu = v.findViewById(R.id.btnMenu);
        }
    }
}
