package com.habitrpg.taskmanager.presentation.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.habitrpg.taskmanager.R;
import com.habitrpg.taskmanager.presentation.fragments.ShopFragment;

import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private final List<ShopFragment.ShopItem> shopItems;
    private final ShopItemClickListener clickListener;

    public interface ShopItemClickListener {
        void onItemClick(ShopFragment.ShopItem item);
    }

    public ShopAdapter(List<ShopFragment.ShopItem> shopItems, ShopItemClickListener clickListener) {
        this.shopItems = shopItems;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        ShopFragment.ShopItem item = shopItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return shopItems.size();
    }

    class ShopViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconImageView;
        private final TextView nameTextView;
        private final TextView descriptionTextView;
        private final TextView priceTextView;
        private final TextView bonusTextView;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.imageViewIcon);
            nameTextView = itemView.findViewById(R.id.textViewName);
            descriptionTextView = itemView.findViewById(R.id.textViewDescription);
            priceTextView = itemView.findViewById(R.id.textViewPrice);
            bonusTextView = itemView.findViewById(R.id.textViewBonus);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(shopItems.get(position));
                }
            });
        }

        public void bind(ShopFragment.ShopItem item) {
            nameTextView.setText(item.getName());
            descriptionTextView.setText(item.getDescription());
            priceTextView.setText(item.getPrice() + " coins");

            // Set icon based on icon resource name
            int iconResource = getIconResource(item.getIconResource());
            iconImageView.setImageResource(iconResource);

            // Set bonus information
            String bonusText = getBonusText(item);
            bonusTextView.setText(bonusText);
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

        private String getBonusText(ShopFragment.ShopItem item) {
            String bonusType = item.getBonusType();
            double bonusValue = item.getBonusValue();
            String bonusDuration = item.getBonusDuration();

            String bonusText = "";
            switch (bonusType) {
                case "strength":
                    bonusText = "+" + (int)bonusValue + "% Strength";
                    break;
                case "attack_chance":
                    bonusText = "+" + (int)bonusValue + "% Attack Success";
                    break;
                case "extra_attack":
                    bonusText = "+" + (int)bonusValue + "% Extra Attack Chance";
                    break;
                case "coin_bonus":
                    bonusText = "+" + (int)bonusValue + "% Coin Rewards";
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
