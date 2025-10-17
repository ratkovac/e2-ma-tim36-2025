package com.habitrpg.taskmanager.data.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "equipment")
public class Equipment {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "equipment_id")
    private String equipmentId;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "equipment_type")
    private String equipmentType; // "potion", "clothing", "weapon"

    @ColumnInfo(name = "equipment_name")
    private String equipmentName;

    @ColumnInfo(name = "equipment_description")
    private String equipmentDescription;

    @ColumnInfo(name = "price")
    private int price;

    @ColumnInfo(name = "icon_resource")
    private String iconResource;

    @ColumnInfo(name = "bonus_type")
    private String bonusType; // "strength", "attack_chance", "extra_attack"

    @ColumnInfo(name = "bonus_value")
    private double bonusValue; // percentage or fixed value

    @ColumnInfo(name = "bonus_duration")
    private String bonusDuration; // "permanent", "single_use", "temporary"

    @ColumnInfo(name = "purchase_date")
    private long purchaseDate;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    public Equipment() {
        this.equipmentId = "";
        this.userId = "";
        this.equipmentType = "";
        this.equipmentName = "";
        this.equipmentDescription = "";
        this.price = 0;
        this.iconResource = "";
        this.bonusType = "";
        this.bonusValue = 0.0;
        this.bonusDuration = "";
        this.purchaseDate = System.currentTimeMillis();
        this.isActive = false;
    }

    @Ignore
    public Equipment(@NonNull String equipmentId, String userId, String equipmentType, String equipmentName, 
                    String equipmentDescription, int price, String iconResource, String bonusType, 
                    double bonusValue, String bonusDuration, long purchaseDate, boolean isActive) {
        this.equipmentId = equipmentId;
        this.userId = userId;
        this.equipmentType = equipmentType;
        this.equipmentName = equipmentName;
        this.equipmentDescription = equipmentDescription;
        this.price = price;
        this.iconResource = iconResource;
        this.bonusType = bonusType;
        this.bonusValue = bonusValue;
        this.bonusDuration = bonusDuration;
        this.purchaseDate = purchaseDate;
        this.isActive = isActive;
    }

    @NonNull
    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(@NonNull String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public void setEquipmentName(String equipmentName) {
        this.equipmentName = equipmentName;
    }

    public String getEquipmentDescription() {
        return equipmentDescription;
    }

    public void setEquipmentDescription(String equipmentDescription) {
        this.equipmentDescription = equipmentDescription;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getIconResource() {
        return iconResource;
    }

    public void setIconResource(String iconResource) {
        this.iconResource = iconResource;
    }

    public String getBonusType() {
        return bonusType;
    }

    public void setBonusType(String bonusType) {
        this.bonusType = bonusType;
    }

    public double getBonusValue() {
        return bonusValue;
    }

    public void setBonusValue(double bonusValue) {
        this.bonusValue = bonusValue;
    }

    public String getBonusDuration() {
        return bonusDuration;
    }

    public void setBonusDuration(String bonusDuration) {
        this.bonusDuration = bonusDuration;
    }

    public long getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(long purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
