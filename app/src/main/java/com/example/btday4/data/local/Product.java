package com.example.btday4.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Room and Firestore entity representing a store product with full details and specs.
 */
@Entity(tableName = "products")
public class Product {

    @PrimaryKey
    @NonNull
    private String id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "price")
    private String price;

    @ColumnInfo(name = "original_price")
    private String originalPrice;

    @ColumnInfo(name = "short_info")
    private String shortInfo;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "image_url")
    private String imageUrl;

    @ColumnInfo(name = "brand")
    private String brand;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "specs")
    private String specs;

    @ColumnInfo(name = "warranty")
    private String warranty;

    @ColumnInfo(name = "stock", defaultValue = "15")
    private int stock;

    @Ignore
    public Product() {
        this.id = "";
        this.stock = 15;
    }

    public Product(
            @NonNull String id,
            String name,
            String price,
            String originalPrice,
            String shortInfo,
            String category,
            String imageUrl,
            String brand,
            String description,
            String specs,
            String warranty,
            int stock
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.originalPrice = originalPrice;
        this.shortInfo = shortInfo;
        this.category = category;
        this.imageUrl = imageUrl;
        this.brand = brand;
        this.description = description;
        this.specs = specs;
        this.warranty = warranty;
        this.stock = stock;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(String originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getShortInfo() {
        return shortInfo;
    }

    public void setShortInfo(String shortInfo) {
        this.shortInfo = shortInfo;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecs() {
        return specs;
    }

    public void setSpecs(String specs) {
        this.specs = specs;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    /**
     * Parses pipe-delimited "Key:Value|Key:Value" specs into an ordered map for UI rendering.
     */
    public Map<String, String> parseSpecs() {
        Map<String, String> result = new LinkedHashMap<>();
        if (specs != null && !specs.trim().isEmpty()) {
            String[] entries = specs.split("\\|");
            for (String entry : entries) {
                int colonIndex = entry.indexOf(':');
                if (colonIndex > 0 && colonIndex < entry.length() - 1) {
                    String key = entry.substring(0, colonIndex).trim();
                    String value = entry.substring(colonIndex + 1).trim();
                    if (!key.isEmpty() && !value.isEmpty()) {
                        result.put(key, value);
                    }
                }
            }
        }
        if (warranty != null && !warranty.trim().isEmpty()) {
            result.put("Bảo hành", warranty.trim());
        }
        return result;
    }
}
