package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.data.database.entities.Equipment;

import java.util.List;

public class EquipmentDisplayAdapter extends RecyclerView.Adapter<EquipmentDisplayAdapter.EquipmentViewHolder> {

    private final List<Equipment> equipmentList;
    private final EquipmentClickListener clickListener;

    public interface EquipmentClickListener {
        void onActivateClick(Equipment equipment);
        void onDeactivateClick(Equipment equipment);
        void onUpgradeClick(Equipment equipment);
    }
    

    public EquipmentDisplayAdapter(List<Equipment> equipmentList, EquipmentClickListener clickListener) {
        this.equipmentList = equipmentList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public EquipmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment_display, parent, false);
        return new EquipmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EquipmentViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.bind(equipment);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    class EquipmentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconImageView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final TextView bonusTextView;
        private final TextView statusTextView;
        private final TextView actionButton;

        public EquipmentViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.imageViewIcon);
            nameTextView = itemView.findViewById(R.id.textViewName);
            descriptionTextView = itemView.findViewById(R.id.textViewDescription);
            bonusTextView = itemView.findViewById(R.id.textViewBonus);
            statusTextView = itemView.findViewById(R.id.textViewStatus);
            actionButton = itemView.findViewById(R.id.textViewAction);
        }

        public void bind(Equipment equipment) {
            nameTextView.setText(equipment.getEquipmentName());
            descriptionTextView.setText(equipment.getEquipmentDescription());
            
            // Set icon based on icon resource name
            int iconResource = getIconResource(equipment.getIconResource());
            iconImageView.setImageResource(iconResource);

            // Set bonus information
            String bonusText = getBonusText(equipment);
            
            // For weapons, show current bonus value with upgrade level
            if ("weapon".equals(equipment.getEquipmentType())) {
                bonusText += " (Current: " + String.format("%.1f", equipment.getBonusValue()) + "%)";
            }
            
            bonusTextView.setText(bonusText);

            // Handle button logic and status based on equipment type
            if ("weapon".equals(equipment.getEquipmentType())) {
                // Weapons: Show "Permanent" status and "Upgrade" button
                statusTextView.setText("Permanent");
                statusTextView.setTextColor(itemView.getContext().getColor(R.color.accent_color));
                
                actionButton.setText("Upgrade");
                actionButton.setTextColor(itemView.getContext().getColor(R.color.primary_color));
                actionButton.setOnClickListener(v -> clickListener.onUpgradeClick(equipment));
            } else {
                // Other equipment: Show Active/Inactive status with durability
                String statusText = equipment.isActive() ? "Active" : "Inactive";
                if (equipment.getDurability() == -1) {
                    statusText += " (Forever)";
                } else {
                    statusText += " (" + equipment.getDurability() + " uses left)";
                }
                statusTextView.setText(statusText);
                
                // Other equipment (potions, clothing) can be activated/deactivated
                if (equipment.isActive()) {
                    statusTextView.setTextColor(itemView.getContext().getColor(R.color.accent_color));
                    actionButton.setText("Deactivate");
                    actionButton.setTextColor(itemView.getContext().getColor(R.color.error_color));
                    actionButton.setOnClickListener(v -> clickListener.onDeactivateClick(equipment));
                } else {
                    statusTextView.setTextColor(itemView.getContext().getColor(R.color.text_secondary));
                    actionButton.setText("Activate");
                    actionButton.setTextColor(itemView.getContext().getColor(R.color.accent_color));
                    actionButton.setOnClickListener(v -> clickListener.onActivateClick(equipment));
                }
            }
        }

        private int getIconResource(String iconResourceName) {
            switch (iconResourceName) {
                case "ic_potion1":
                    return R.drawable.ic_potion1;
                case "ic_potion2":
                    return R.drawable.ic_potion2;
                case "ic_potion3":
                    return R.drawable.ic_potion3;
                case "ic_potion4":
                    return R.drawable.ic_potion4;
                case "ic_gloves":
                    return R.drawable.ic_gloves;
                case "ic_shield":
                    return R.drawable.ic_shield;
                case "ic_boots":
                    return R.drawable.ic_boots;
                case "ic_sword":
                    return R.drawable.ic_sword;
                case "ic_bow":
                    return R.drawable.ic_bow;
                default:
                    return R.drawable.ic_potion1; // Default icon
            }
        }

        private String getBonusText(Equipment equipment) {
            String bonusType = equipment.getBonusType();
            double bonusValue = equipment.getBonusValue();
            String bonusDuration = equipment.getBonusDuration();

            String bonusText = "";
            switch (bonusType) {
                case "strength":
                    bonusText = "+" + String.format("%.1f", bonusValue) + "% Strength";
                    break;
                case "attack_chance":
                    bonusText = "+" + String.format("%.1f", bonusValue) + "% Attack Success";
                    break;
                case "extra_attack":
                    bonusText = "+" + String.format("%.1f", bonusValue) + "% Extra Attack Chance";
                    break;
                case "coin_bonus":
                    bonusText = "+" + String.format("%.1f", bonusValue) + "% Coin Rewards";
                    break;
            }

            if (bonusDuration.equals("single_use")) {
                bonusText += " (Single Use)";
            } else if (bonusDuration.equals("permanent")) {
                bonusText += " (Permanent)";
            }

            return bonusText;
        }
    }
}
